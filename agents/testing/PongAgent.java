package agents.testing;

import jade.core.Agent;
import jade.content.lang.sl.SLCodec;
import jade.lang.acl.ACLMessage;
import jade.core.behaviours.CyclicBehaviour;

public class PongAgent extends Agent {
    @Override
    protected void setup() {
        getContentManager().registerLanguage(new SLCodec());
        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    System.out.println("[Pong] Recebido: " + msg.getContent());
                    ACLMessage resp = msg.createReply();
                    resp.setPerformative(ACLMessage.INFORM);
                    resp.setContent("PONG");
                    send(resp);
                    System.out.println("[Pong] Resposta enviada: PONG");
                    myAgent.doDelete();
                } else {
                    block();
                }
            }
        });
    }
}
