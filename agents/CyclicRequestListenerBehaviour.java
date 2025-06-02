package agents;

import jade.core.behaviours.Behaviour;
import jade.core.Agent;

/**
 * Behaviour temporário que não faz nada além de bloquear,
 * evitando que o agente lance UnsupportedOperationException.
 * Posteriormente, você pode implementar a lógica de escuta de requisições aqui.
 */
public class CyclicRequestListenerBehaviour extends Behaviour {

    public CyclicRequestListenerBehaviour(Agent aThis) {
        super(aThis);
    }

    @Override
    public void action() {
        // Apenas bloqueia para economizar CPU; será chamado de novo porque done() retorna false
        block(1000);
    }

    @Override
    public boolean done() {
        // Retorna false para nunca terminar; comportamento ficará ativo indefinidamente
        return false;
    }
}
