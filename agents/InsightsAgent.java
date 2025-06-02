package agents;

import jade.core.Agent;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.basic.Action;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.AchieveREResponder;

/**
 * InsightsAgent
 *
 * - Registra-se no DF como "insights".
 * - Usa um AchieveREResponder para tratar REQUESTs com ontologia "InsightsRequest".
 * - Ao receber um REQUEST, envia AGREE, executa a análise e retorna um INFORM com objeto InsightMsg.
 */
public class InsightsAgent extends Agent {

    @Override
    protected void setup() {
        System.out.println("InsightsAgent iniciado: " + getLocalName());

        // 1) Registrar linguagem SLCodec e ontologia DashboardOntology
        getContentManager().registerLanguage(new SLCodec());
        getContentManager().registerOntology(DashboardOntology.getInstance());

        // 2) Registrar este agente no DF como "insights"
        registerService("insights");

        // 3) Criar e adicionar o AchieveREResponderBehaviour que vai escutar REQUESTs de "InsightsRequest"
        MessageTemplate template = MessageTemplate.and(
                MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                MessageTemplate.MatchOntology("InsightsRequest")
        );
        addBehaviour(new InsightsRequestResponder(this, template));
    }

    @Override
    protected void takeDown() {
        // Desregistra do DF ao terminar
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        System.out.println("InsightsAgent \"" + getLocalName() + "\" finalizado.");
    }

    /**
     * Registra este agente no DF com o tipo de serviço fornecido (ex.: "insights").
     */
    private void registerService(String serviceType) {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType(serviceType);
        sd.setName(getLocalName());
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

    /**
     * Behaviour que responde a qualquer REQUEST com ontologia "InsightsRequest".
     * Fluxo:
     *  1) Quando receber um REQUEST, envia um AGREE.
     *  2) Executa a análise (InsightsAnalysisBehaviour) e gera um InsightMsg.
     *  3) Retorna um INFORM com ontologia "InsightsInform" e o conteúdo do InsightMsg,
     *     usando fillContent(Action(agent, InsightMsg)).
     */
    private static class InsightsRequestResponder extends AchieveREResponder {
        InsightsRequestResponder(Agent a, MessageTemplate mt) {
            super(a, mt);
        }

        @Override
        protected ACLMessage handleRequest(ACLMessage request) {
            // 1) Responde AGREE para indicar que vai processar
            ACLMessage agree = request.createReply();
            agree.setPerformative(ACLMessage.AGREE);
            agree.setContent("AGREE_INSIGHTS");
            return agree;
        }

        @Override
        protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response) {
            // … executa a análise e obtém o InsightMsg
            InsightMsg im = InsightsAnalysisBehaviour.runAnalysisBean((InsightsAgent) getAgent());

            ACLMessage inform = request.createReply();
            inform.setPerformative(ACLMessage.INFORM);
            // 1) usar sempre a mesma ontologia registrada no ContentManager:
            inform.setOntology(DashboardOntology.getInstance().getName());
            inform.setLanguage(new SLCodec().getName());

            try {
                // 2) empacotar o InsightMsg dentro de um predicate Action:
                Action act = new Action(getAgent().getAID(), im);
                getAgent().getContentManager().fillContent(inform, act);
            } catch (Exception ex) {
                ex.printStackTrace();
                // se der erro, fallback em texto simples
                inform.setContent("ERRO_NO_FILLCONTENT_INSIGHT");
            }
            return inform;
        }
    }
}
