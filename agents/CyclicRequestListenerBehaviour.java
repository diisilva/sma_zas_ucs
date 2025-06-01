// File: CyclicRequestListenerBehaviour.java
package agents;

import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

/**
 * Behaviour cíclico que escuta requests do CoordenadorAgent para análise de dados
 * e notificações de enriquecimento do EnriquecedorWebAgent. 
 * Ao receber uma mensagem compatível, agenda um InsightsAnalysisBehaviour.
 */
public class CyclicRequestListenerBehaviour extends CyclicBehaviour {
    private static final long serialVersionUID = 1L;
    private final InsightsAgent agent;
    // Template para REQUEST do Coordenador ("AnalyzeData") ou INFORM de enriquecimento ("EnrichmentNotification")
    private final MessageTemplate mt;

    public CyclicRequestListenerBehaviour(InsightsAgent a) {
        super(a);
        this.agent = a;
        MessageTemplate reqFromCoordinator = MessageTemplate.and(
                MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                MessageTemplate.MatchOntology("AnalyzeData")
        );
        MessageTemplate enrichNotification = MessageTemplate.and(
                MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                MessageTemplate.MatchOntology("EnrichmentNotification")
        );
        this.mt = MessageTemplate.or(reqFromCoordinator, enrichNotification);
    }

    @Override
    public void action() {
        ACLMessage msg = myAgent.receive(mt);
        if (msg != null) {
            String senderName = msg.getSender().getLocalName();
            String ontologia  = msg.getOntology();
            String conteudo   = msg.getContent();

            System.out.println("InsightsAgent recebeu de " + senderName +
                    " (ontologia='" + ontologia + "') → " + conteudo);

            /*
             * Se for REQUEST do CoordenadorAgent (AnalyzeData), inicia análise dos dados recém-validados.
             * Se for INFORM de EnriquecedorWebAgent (EnrichmentNotification), reanalisa com o contexto enriquecido.
             */
            agent.addBehaviour(new InsightsAnalysisBehaviour(agent, ontologia, conteudo));
        } else {
            block();
        }
    }
}
