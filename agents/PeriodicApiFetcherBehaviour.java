package agents;

import jade.core.behaviours.Behaviour;
import jade.core.Agent;

/**
 * Behaviour temporário que bloqueia por um intervalo (evitando exceção),
 * até você implementar de fato a lógica de requisição periódica.
 */
public class PeriodicApiFetcherBehaviour extends Behaviour {

    private final Agent agent;
    private final long interval;
    private long lastExecutionTime;

    public PeriodicApiFetcherBehaviour(Agent aThis, int i) {
        super(aThis);
        this.agent = aThis;
        this.interval = i * 1000L; // i em segundos → milissegundos
        this.lastExecutionTime = 0;
    }

    @Override
    public void action() {
        long now = System.currentTimeMillis();
        if (now - lastExecutionTime >= interval) {
            // ★ Aqui, coloque a lógica real de chamar a API
            System.out.println(agent.getLocalName() + " executando fetch de API...");

            // Atualiza timestamp para não executar de novo até passar o intervalo
            lastExecutionTime = now;
        }

        // Bloqueia por 1 segundo antes de checar novamente
        block(1000);
    }

    @Override
    public boolean done() {
        // Nunca termina; roda indefinidamente
        return false;
    }
}
