// File: OrquestradorBehaviour.java
package agents;

import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

/**
 * Behaviour que orquestra a sequência de interação entre agentes:
 * 1) Solicita ao MonitorDeDadosAgent verificação de dados;
 * 2) Aguarda resposta do Monitor;
 * 3) Solicita ao InsightsAgent a análise dos dados;
 * 4) Aguarda resposta do Insights;
 * 5) Solicita ao EnriquecedorWebAgent o enriquecimento externo;
 * 6) Aguarda confirmação do Enriquecedor e finaliza.
 */
public class OrquestradorBehaviour extends Behaviour {
    private final CoordenadorAgent agent;
    private int step = 0;
    private boolean finished = false;

    // MessageTemplates para filtrar respostas
    private MessageTemplate mtMonitorResponse;
    private MessageTemplate mtInsightsResponse;
    private MessageTemplate mtEnricherResponse;

    public OrquestradorBehaviour(CoordenadorAgent aThis) {
        this.agent = aThis;
        // Montar templates de mensagens que iremos esperar:
        // - Resposta do Monitor (INFORM, ontology “FileWatcher” ou “DatabaseCheck”)
        mtMonitorResponse = MessageTemplate.and(
                MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                MessageTemplate.MatchSender(new AID("MonitorDeDadosAgent", AID.ISLOCALNAME))
        );
        // - Resposta do Insights (INFORM, ontology “InsightsResult”)
        mtInsightsResponse = MessageTemplate.and(
                MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                MessageTemplate.MatchSender(new AID("InsightsAgent", AID.ISLOCALNAME))
        );
        // - Resposta do Enriquecedor (INFORM, ontology “EnrichmentResult”)
        mtEnricherResponse = MessageTemplate.and(
                MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                MessageTemplate.MatchSender(new AID("EnriquecedorWebAgent", AID.ISLOCALNAME))
        );
    }

    @Override
    public void action() {
        switch (step) {
            case 0:
                // 1) Enviar REQUEST ao MonitorDeDadosAgent para verificar atualizações
                ACLMessage reqMonitor = new ACLMessage(ACLMessage.REQUEST);
                reqMonitor.addReceiver(new AID("MonitorDeDadosAgent", AID.ISLOCALNAME));
                reqMonitor.setOntology("CheckData");
                reqMonitor.setContent("CHECK_FOR_UPDATES");
                agent.send(reqMonitor);
                System.out.println("Orquestrador: REQUEST enviado ao MonitorDeDadosAgent");
                step = 1;
                break;

            case 1:
                // 2) Aguardar INFORM do MonitorDeDadosAgent
                ACLMessage infMonitor = agent.receive(mtMonitorResponse);
                if (infMonitor != null) {
                    System.out.println("Orquestrador: RECEBIDO INFORM do MonitorDeDadosAgent: " 
                                       + infMonitor.getContent());
                    step = 2;
                } else {
                    block();
                }
                break;

            case 2:
                // 3) Enviar REQUEST ao InsightsAgent para analisar dados validados
                ACLMessage reqInsights = new ACLMessage(ACLMessage.REQUEST);
                reqInsights.addReceiver(new AID("InsightsAgent", AID.ISLOCALNAME));
                reqInsights.setOntology("AnalyzeData");
                reqInsights.setContent("ANALYZE_VALIDATED_DATA");
                agent.send(reqInsights);
                System.out.println("Orquestrador: REQUEST enviado ao InsightsAgent");
                step = 3;
                break;

            case 3:
                // 4) Aguardar INFORM do InsightsAgent
                ACLMessage infInsights = agent.receive(mtInsightsResponse);
                if (infInsights != null) {
                    System.out.println("Orquestrador: RECEBIDO INFORM do InsightsAgent: " 
                                       + infInsights.getContent());
                    step = 4;
                } else {
                    block();
                }
                break;

            case 4:
                // 5) Enviar REQUEST ao EnriquecedorWebAgent para enriquecimento externo
                ACLMessage reqEnricher = new ACLMessage(ACLMessage.REQUEST);
                reqEnricher.addReceiver(new AID("EnriquecedorWebAgent", AID.ISLOCALNAME));
                reqEnricher.setOntology("EnrichRequest");
                reqEnricher.setContent("ENRICH_EXTERNAL_DATA");
                agent.send(reqEnricher);
                System.out.println("Orquestrador: REQUEST enviado ao EnriquecedorWebAgent");
                step = 5;
                break;

            case 5:
                // 6) Aguardar INFORM de confirmação do EnriquecedorWebAgent
                ACLMessage infEnricher = agent.receive(mtEnricherResponse);
                if (infEnricher != null) {
                    System.out.println("Orquestrador: RECEBIDO INFORM do EnriquecedorWebAgent: " 
                                       + infEnricher.getContent());
                    finished = true; // Concluiu a sequência
                } else {
                    block();
                }
                break;

            default:
                // Nunca deve chegar aqui
                finished = true;
                break;
        }
    }

    @Override
    public boolean done() {
        return finished;
    }
}
