package agents.testing;

import jade.core.Agent;
import jade.core.AID;
import jade.content.lang.sl.SLCodec;
import jade.lang.acl.ACLMessage;
import jade.core.behaviours.CyclicBehaviour;

public class PingAgent extends Agent {
    @Override
    protected void setup() {
        getContentManager().registerLanguage(new SLCodec());
        // Envia um ping após 1s
        addBehaviour(new CyclicBehaviour(this) {
            private boolean sent = false;
            @Override
            public void action() {
                if (!sent) {
                    ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                    msg.addReceiver(new AID("pong", AID.ISLOCALNAME));
                    msg.setContent("PING");
                    send(msg);
                    System.out.println("[Ping] Mensagem enviada: PING");
                    sent = true;
                }
                // Aguarda resposta
                ACLMessage reply = receive();
                if (reply != null) {
                    System.out.println("[Ping] Recebido: " + reply.getContent());
                    myAgent.doDelete();
                } else {
                    block();
                }
            }
        });
    }
}
