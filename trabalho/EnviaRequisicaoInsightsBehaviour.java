package trabalho;

import jade.content.abs.AbsContentElement;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.core.AID;
import jade.content.lang.sl.SLCodec;

public class EnviaRequisicaoInsightsBehaviour extends OneShotBehaviour {
    @Override
    public void action() {
        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.addReceiver(new AID("InsightsAgent", AID.ISLOCALNAME));
        msg.setLanguage(new SLCodec().getName());
        msg.setOntology(DashboardOntology.getInstance().getName());

        Informacao info = new Informacao();
        info.setMensagem("Por favor, analise os dados de risco atualizados.");

        try {
            myAgent.getContentManager().fillContent(msg, (AbsContentElement) info);
            myAgent.send(msg);
            System.out.println("Requisição enviada ao InsightsAgent.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
