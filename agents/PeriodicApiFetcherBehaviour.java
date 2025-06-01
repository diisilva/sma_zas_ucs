// File: PeriodicApiFetcherBehaviour.java
package agents;

import jade.core.AID;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.*;

/**
 * Behaviour do tipo TickerBehaviour que, em intervalos fixos,
 * consulta uma ou mais APIs públicas para obter dados de desmatamento, incêndios etc.,
 * insere esses dados no PostgreSQL e notifica os agentes MonitorDeDadosAgent
 * e InsightsAgent sobre a existência de novos dados para processamento.
 */
public class PeriodicApiFetcherBehaviour extends TickerBehaviour {
    private static final long serialVersionUID = 1L;

    // Ajuste conforme seu ambiente de banco de dados
    private static final String DB_URL  = "jdbc:postgresql://localhost:5432/seu_banco";
    private static final String DB_USER = "seu_usuario";
    private static final String DB_PASS = "sua_senha";

    // Exemplos de endpoints de APIs (substitua pelos reais que você deseja usar)
    private static final String API_INPE_URL   = "https://api.inpe.br/desmatamento/latest";
    private static final String API_IBGE_URL   = "https://servicodados.ibge.gov.br/api/v1/prodes/latest";
    // Você pode adicionar quantos endpoints forem necessários

    private final HttpClient httpClient;

    public PeriodicApiFetcherBehaviour(jade.core.Agent a, long period) {
        super(a, period);
        this.httpClient = HttpClient.newHttpClient();

        // Carrega driver PostgreSQL, se necessário
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onTick() {
        // 1) Buscar dados na API INPE
        fetchAndStoreFromApi(API_INPE_URL, "inpe_desmatamento");

        // 2) Buscar dados na API IBGE
        fetchAndStoreFromApi(API_IBGE_URL, "ibge_prodes");

        // Caso queira notificar especificamente cada agente separadamente, pode-se chamar
        // notifyAgents("Enriquecimento periódico concluído");
    }

    /**
     * Faz uma requisição HTTP GET ao endpoint fornecido, processa o JSON de resposta,
     * insere registros no PostgreSQL na tabela indicada e notifica os agentes do sistema.
     *
     * @param apiUrl   URL da API pública a ser chamada
     * @param table    Nome da tabela no PostgreSQL onde os dados serão inseridos
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

                // Aqui você deve parsear o JSON de acordo com o formato retornado pela API.
                // Para simplificar, suponhamos que cada endpoint retorne um array JSON de objetos,
                // e que cada objeto tenha, por exemplo, campos "id", "valor" e "data".
                //
                // Você pode usar qualquer biblioteca JSON (Jackson, org.json, etc.). Exemplo abaixo
                // usa org.json, mas ajuste caso prefira outra.

                org.json.JSONArray array = new org.json.JSONArray(responseBody);

                // 3) Inserir cada objeto JSON na tabela PostgreSQL
                Connection conn = null;
                PreparedStatement stmt = null;
                String insertSQL = "INSERT INTO " + table + " (external_id, valor, data_coleta) VALUES (?, ?, ?) "
                                 + "ON CONFLICT (external_id) DO NOTHING"; 
                // Ajuste ON CONFLICT conforme sua chave primária/única

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

                // 4) Notificar agentes MonitorDeDadosAgent e InsightsAgent que há novos dados
                notifyAgents("NEW_ENRICHMENT:" + table);
            } else {
                System.out.println("Falha ao consultar API: " + apiUrl + " HTTP status: " + response.statusCode());
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Envia uma mensagem ACL-Inform para MonitorDeDadosAgent e para InsightsAgent,
     * informando que há novos dados de enriquecimento prontos para validação/análise.
     *
     * @param content conteúdo da mensagem, ex.: "NEW_ENRICHMENT:inpe_desmatamento"
     */
    private void notifyAgents(String content) {
        // Criar mensagem INFORM para o MonitorDeDadosAgent
        ACLMessage msgMonitor = new ACLMessage(ACLMessage.INFORM);
        msgMonitor.addReceiver(new AID("MonitorDeDadosAgent", AID.ISLOCALNAME));
        msgMonitor.setOntology("EnrichmentNotification");
        msgMonitor.setContent(content);
        myAgent.send(msgMonitor);

        // Criar mensagem INFORM para o InsightsAgent
        ACLMessage msgInsights = new ACLMessage(ACLMessage.INFORM);
        msgInsights.addReceiver(new AID("InsightsAgent", AID.ISLOCALNAME));
        msgInsights.setOntology("EnrichmentNotification");
        msgInsights.setContent(content);
        myAgent.send(msgInsights);

        System.out.println("Enviado INFORM de enriquecimento para MonitorDeDadosAgent e InsightsAgent: " + content);
    }

    @Override
    protected void onEnd() {
        // Opcional: liberar recursos se necessário
        super.onEnd();
    }
}
