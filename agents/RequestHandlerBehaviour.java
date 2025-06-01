// File: RequestHandlerBehaviour.java
package agents;

import jade.core.AID;
import jade.core.Agent;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.AchieveREResponder;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.*;

/**
 * Behaviour do tipo AchieveREResponder que fica escutando
 * pedidos FIPA-REQUEST para enriquecimento imediato.
 * Ao receber um REQUEST, executa a coleta/armazenamento dos dados
 * (invertendo o ciclo periódico) e retorna uma resposta INFORM
 * ao solicitante. Também notifica MonitorDeDadosAgent e InsightsAgent
 * sobre o novo registro de enriquecimento.
 */
public class RequestHandlerBehaviour extends AchieveREResponder {
    private static final long serialVersionUID = 1L;

    // Configurações do banco de dados (ajuste conforme seu ambiente)
    private static final String DB_URL  = "jdbc:postgresql://localhost:5432/seu_banco";
    private static final String DB_USER = "seu_usuario";
    private static final String DB_PASS = "sua_senha";

    // Exemplos de endpoints (pode centralizar em uma lista ou mapa)
    private static final String API_INPE_URL = "https://api.inpe.br/desmatamento/latest";
    private static final String API_IBGE_URL = "https://servicodados.ibge.gov.br/api/v1/prodes/latest";

    private final HttpClient httpClient;

    public RequestHandlerBehaviour(Agent a) {
        super(a, MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
        this.httpClient = HttpClient.newHttpClient();

        // Carrega driver PostgreSQL, se necessário
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Método chamado automaticamente quando um REQUEST compatível é recebido.
     * Prepara e retorna a resposta informando que o pedido foi aceito.
     */
    @Override
    protected ACLMessage handleRequest(ACLMessage request) {
        String content = request.getContent(); 
        System.out.println("RequestHandlerBehaviour recebeu REQUEST de " 
                            + request.getSender().getLocalName() 
                            + " com conteúdo: " + content);

        // Você pode inspecionar o conteúdo da REQUEST para decidir quais APIs chamar.
        // Por exemplo, se content == "ENRICH_INPE", só chama a API INPE, etc.
        // Para simplificar, faremos todas as coletas:
        performImmediateEnrichment();

        // Prepara a mensagem de confirmação (inform) ao solicitante
        ACLMessage reply = request.createReply();
        reply.setPerformative(ACLMessage.INFORM);
        reply.setOntology("EnrichmentResult");
        reply.setContent("Enriquecimento imediato concluído com sucesso");
        return reply;
    }

    /**
     * Realiza imediatamente a consulta às APIs e armazena no banco,
     * exatamente como faz o comportamento periódico, mas como ação sob demanda.
     */
    private void performImmediateEnrichment() {
        // 1) Coleta de dados na API INPE
        fetchAndStoreFromApi(API_INPE_URL, "inpe_desmatamento");

        // 2) Coleta de dados na API IBGE
        fetchAndStoreFromApi(API_IBGE_URL, "ibge_prodes");

        // 3) Notifica MonitorDeDadosAgent e InsightsAgent sobre o novo enriquecimento
        notifyAgents("NEW_ENRICHMENT_IMMEDIATE");
    }

    /**
     * Reusa a lógica de fetch e armazenamento do comportamento periódico.
     */
    private void fetchAndStoreFromApi(String apiUrl, String table) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                String responseBody = response.body();
                org.json.JSONArray array = new org.json.JSONArray(responseBody);

                Connection conn = null;
                PreparedStatement stmt = null;
                String insertSQL = "INSERT INTO " + table + " (external_id, valor, data_coleta) VALUES (?, ?, ?) "
                                 + "ON CONFLICT (external_id) DO NOTHING";
                try {
                    conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                    stmt = conn.prepareStatement(insertSQL);

                    for (int i = 0; i < array.length(); i++) {
                        org.json.JSONObject obj = array.getJSONObject(i);
                        String externalId = obj.getString("id");
                        double valor       = obj.getDouble("valor");
                        String dataColeta  = obj.getString("data"); // ex.: "2025-05-31"

                        stmt.setString(1, externalId);
                        stmt.setDouble(2, valor);
                        stmt.setDate(3, Date.valueOf(dataColeta));
                        stmt.executeUpdate();
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                } finally {
                    try { if (stmt != null) stmt.close(); } catch (Exception e) { e.printStackTrace(); }
                    try { if (conn != null) conn.close(); } catch (Exception e) { e.printStackTrace(); }
                }
            } else {
                System.out.println("Falha ao consultar API: " + apiUrl + " HTTP status: " + response.statusCode());
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Envia mensagens INFORM para MonitorDeDadosAgent e InsightsAgent
     * informando que houve enriquecimento imediato.
     */
    private void notifyAgents(String content) {
        // Mensagem ao MonitorDeDadosAgent
        ACLMessage msgMonitor = new ACLMessage(ACLMessage.INFORM);
        msgMonitor.addReceiver(new AID("MonitorDeDadosAgent", AID.ISLOCALNAME));
        msgMonitor.setOntology("EnrichmentNotification");
        msgMonitor.setContent(content);
        myAgent.send(msgMonitor);

        // Mensagem ao InsightsAgent
        ACLMessage msgInsights = new ACLMessage(ACLMessage.INFORM);
        msgInsights.addReceiver(new AID("InsightsAgent", AID.ISLOCALNAME));
        msgInsights.setOntology("EnrichmentNotification");
        msgInsights.setContent(content);
        myAgent.send(msgInsights);

        System.out.println("Enviado INFORM imediato para MonitorDeDadosAgent e InsightsAgent: " + content);
    }
}
