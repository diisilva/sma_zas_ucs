package agents.testing;

import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.content.Concept;
import jade.content.onto.basic.Action;
import jade.content.lang.sl.SLCodec;
import agents.Informacao;
import agents.DashboardOntology;

public class OntologyTestBehaviour extends OneShotBehaviour {
    @Override
    public void action() {
        try {
            // Mensagem de teste
            ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
            msg.addReceiver(myAgent.getAID());
            msg.setLanguage(new SLCodec().getName());
            msg.setOntology(DashboardOntology.getInstance().getName());

            // Prepara o conceito e envolve num Action
            Informacao info = new Informacao();
            info.setMensagem("Teste de Ontologia");
            Action act = new Action(myAgent.getAID(), (Concept) info);

            // Preenche e extrai
            myAgent.getContentManager().fillContent(msg, act);
            Object content = myAgent.getContentManager().extractContent(msg);
            Action received = (Action) content;
            Informacao info2 = (Informacao) received.getAction();

            // Verifica
            if ("Teste de Ontologia".equals(info2.getMensagem())) {
                System.out.println("[OK] Ontology fill/extract funcionou!");
            } else {
                System.err.println("[ERRO] Mensagem diferente: " + info2.getMensagem());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            myAgent.doDelete();
        }
    }
}
