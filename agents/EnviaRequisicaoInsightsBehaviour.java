package agents;

import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.core.AID;
import jade.content.onto.basic.Action;
import jade.content.Concept;
import jade.content.lang.sl.SLCodec;

public class EnviaRequisicaoInsightsBehaviour extends OneShotBehaviour {
    @Override
    public void action() {
        // 1. Cria a mensagem de requisição
        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.addReceiver(new AID("InsightsAgent", AID.ISLOCALNAME));
        msg.setLanguage(new SLCodec().getName());
        msg.setOntology(DashboardOntology.getInstance().getName());

        // 2. Monta o conceito Informacao
        Informacao info = new Informacao();
        info.setMensagem("Por favor, analise os dados de risco atualizados.");

        try {
            // 3. Envolve o conceito num Action antes de preencher o conteúdo
            Action act = new Action(myAgent.getAID(), (Concept) info);
            myAgent.getContentManager().fillContent(msg, act);
            myAgent.send(msg);
            System.out.println("Requisição enviada ao InsightsAgent.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
