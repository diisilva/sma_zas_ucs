package agents;

import jade.content.Concept;
import jade.core.AID;
import jade.core.Agent;
import jade.content.abs.AbsPredicate;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.basic.Action;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.AchieveREResponder;
import jade.core.behaviours.TickerBehaviour;
import jade.core.behaviours.CyclicBehaviour;

import java.sql.*;

/**
 * MonitorDeDadosAgent (versão “modo-configurável”)
 *
 * - Se iniciado com parâmetro "responseOnly", registra apenas DatabaseResponderBehaviour.
 * - Se iniciado com parâmetro "full", registra DatabaseResponderBehaviour + PeriodicCheckBehaviour + EnriquecimentoListenerBehaviour.
 * - Se não houver argumento, assume "full" por padrão.
 */
public class MonitorDeDadosAgent extends Agent {
    private AID coordenadorAID;

    // Constantes JDBC (ajuste se necessário)
    private static final String DB_URL  = "jdbc:postgresql://localhost:5430/db_test_dev_diego";
    private static final String DB_USER = "admin";
    private static final String DB_PASS = "admin";

    @Override
    protected void setup() {
        System.out.println(getLocalName() + " iniciado.");

        // Lê argumentos para definir o “modo” de operação
        String mode = "full"; // padrão
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            String raw = args[0].toString().trim().toLowerCase();
            if (raw.equals("responseonly") || raw.equals("response-only")) {
                mode = "responseOnly";
            } else if (raw.equals("full")) {
                mode = "full";
            } else if (raw.equals("no-enrichment")) {
                mode = "noEnrichment";
            }
        }
        System.out.println("MonitorDeDadosAgent modo = " + mode);

        // 1) Registrar linguagem SLCodec e ontologia DashboardOntology
        getContentManager().registerLanguage(new SLCodec());
        getContentManager().registerOntology(DashboardOntology.getInstance());

        // 2) Registrar serviço no DF como "monitor-de-dados"
        registerService("monitor-de-dados");

        // 3) Descobrir o Coordenador (serviço "coordenador")
        coordenadorAID = discoverService("coordenador");
        if (coordenadorAID == null) {
            System.err.println("MonitorDeDadosAgent: não encontrou 'coordenador' no DF. Encerrando.");
            doDelete();
            return;
        }
        System.out.println(getLocalName() + " descobriu Coordenador: " + coordenadorAID.getLocalName());

        // 4) Comportamento de responder a REQUEST – sempre registrado
        addBehaviour(new DatabaseResponderBehaviour(this, MessageTemplate.and(
                MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                MessageTemplate.MatchOntology(DashboardOntology.getInstance().getName())
        )));

        // 5) Se modo == "full" ou "noEnrichment" (ou qualquer outro que inclua a varredura periódica), registra periodicCheck
        if (mode.equals("full") || mode.equals("noEnrichment")) {
            addBehaviour(new PeriodicCheckBehaviour(this, 10000)); // a cada 10 segundos
        }

        // 6) Se modo == "full", adiciona também o listener de enriquecimento
        if (mode.equals("full")) {
            addBehaviour(new EnriquecimentoListenerBehaviour());
        }
    }

    @Override
    protected void takeDown() {
        try { DFService.deregister(this); }
        catch (FIPAException fe) { fe.printStackTrace(); }
        System.out.println(getLocalName() + " finalizando.");
    }

    private void registerService(String serviceType) {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType(serviceType);
        sd.setName(getLocalName());
        dfd.addServices(sd);
        try { DFService.register(this, dfd); }
        catch (FIPAException fe) { fe.printStackTrace(); }
    }

    private AID discoverService(String serviceType) {
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType(serviceType);
        template.addServices(sd);
        try {
            DFAgentDescription[] results = DFService.search(this, template);
            if (results != null && results.length > 0) {
                return results[0].getName();
            }
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        return null;
    }

    // Responde a REQUESTs com ontologia “DatabaseCheckRequest” (sempre ativo)
    private static class DatabaseResponderBehaviour extends AchieveREResponder {
        DatabaseResponderBehaviour(Agent a, MessageTemplate mt) {
            super(a, mt);
        }
        @Override
        protected ACLMessage handleRequest(ACLMessage request) {
            ACLMessage inform = request.createReply();
            inform.setPerformative(ACLMessage.INFORM);
            inform.setOntology(DashboardOntology.getInstance().getName());
            inform.setContent("REQUEST_RECEBIDO");
            return inform;
        }
    }

    // Verifica periodicamente o banco e envia Zona ao Coordenador
    private static class PeriodicCheckBehaviour extends TickerBehaviour {
        PeriodicCheckBehaviour(Agent a, long period) { super(a, period); }
        @Override
        protected void onTick() {
            MonitorDeDadosAgent agent = (MonitorDeDadosAgent) getAgent();
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
                String selectSQL = ""
                        + "SELECT id, ANO, nome_uc, sum_area_total_ha, risk_level, risk_category "
                        + "FROM zonas_amortecimento WHERE processado = FALSE";

                try (PreparedStatement selStmt = conn.prepareStatement(selectSQL);
                     ResultSet rs = selStmt.executeQuery()) {

                    while (rs.next()) {
                        int id = rs.getInt("id");
                        int ano = rs.getInt("ANO");
                        String nomeUc = rs.getString("nome_uc");
                        double areaTotal = rs.getDouble("sum_area_total_ha");
                        double risco = rs.getDouble("risk_level");
                        String categoria = rs.getString("risk_category");

                        // Cria e popula objeto Zona
                        Zona zona = new Zona();
                        zona.setId(id);
                        zona.setAno(ano);
                        zona.setNome_uc(nomeUc);
                        zona.setSum_area_total_ha((float) areaTotal);
                        zona.setRisk_level((float) risco);
                        zona.setRisk_category(categoria);

                        // Monta INFORM com fillContent(Action(zona))
                        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                        msg.addReceiver(agent.coordenadorAID);
                        msg.setOntology(DashboardOntology.getInstance().getName());
                        msg.setLanguage(new SLCodec().getName());
                        try {
                            jade.content.onto.basic.Action payload =
                                    new jade.content.onto.basic.Action(agent.getAID(), zona);
                            agent.getContentManager().fillContent(msg, payload);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            // Fallback de conteúdo textual
                            String fallback = String.format(
                                    "ERRO_NO_FILLCONTENT_ZONA|%d|%s|%.2f|%.2f|%s",
                                    ano, nomeUc, areaTotal, risco, categoria
                            );
                            msg.setContent(fallback);
                        }
                        agent.send(msg);
                        System.out.println(agent.getLocalName() +
                                " enviou INFORM (Zona): " + zona.getNome_uc() + " (risco=" + zona.getRisk_level() + ")");

                        // Marca como processado
                        String updateSQL = "UPDATE zonas_amortecimento SET processado = TRUE WHERE id = ?";
                        try (PreparedStatement updStmt = conn.prepareStatement(updateSQL)) {
                            updStmt.setInt(1, id);
                            updStmt.executeUpdate();
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // Escuta objetos Enriquecimento via ontologia “dashboard-ontology”
    private static class EnriquecimentoListenerBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            MonitorDeDadosAgent agent = (MonitorDeDadosAgent) getAgent();
            MessageTemplate mtPerfOnt = MessageTemplate.and(
                    MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                    MessageTemplate.MatchOntology(DashboardOntology.getInstance().getName())
            );
            MessageTemplate mt = MessageTemplate.and(
                    mtPerfOnt,
                    MessageTemplate.MatchLanguage(new SLCodec().getName())
            );
            ACLMessage msg = agent.receive(mt);
            if (msg != null) {
                try {
                    // 1) Extrair o ContentElement (deve ser um Action que “embrulha” o Enriquecimento)
                    jade.content.ContentElement content =
                            agent.getContentManager().extractContent(msg);

                    // 2) Verificar se é um Action; se sim, recuperar o payload
                    if (content instanceof jade.content.onto.basic.Action) {
                        jade.content.onto.basic.Action act =
                                (jade.content.onto.basic.Action) content;

                        // 3) O payload de getAction() deve ser o nosso Enriquecimento
                        Object payload = act.getAction();
                        if (payload instanceof Enriquecimento) {
                            Enriquecimento e = (Enriquecimento) payload;
                            System.out.println("MonitorDeDadosAgent recebeu Enriquecimento:");
                            System.out.println("  fonte     = " + e.getFonte());
                            System.out.println("  timestamp = " + e.getTimestamp());
                            System.out.println("  detalhes  = " + e.getDetalhes());

                            // (Opcional: disparar nova verificação do banco, se necessário)
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            } else {
                block();
            }
        }
    }
}