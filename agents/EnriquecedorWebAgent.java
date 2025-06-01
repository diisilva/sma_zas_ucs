// File: EnriquecedorWebAgent.java
package agents;

import jade.core.Agent;

/**
 * Agente responsável por buscar dados externos periodicamente
 * e responder a solicitações de enriquecimento imediato.
 */
public class EnriquecedorWebAgent extends Agent {
    @Override
    protected void setup() {
        System.out.println("EnriquecedorWebAgent iniciado: " + getLocalName());

        // Comportamento periódico que consulta APIs a cada 60 segundos (60000 ms)
        addBehaviour(new PeriodicApiFetcherBehaviour(this, 60000));

        // Comportamento que responde a pedidos FIPA-Request de enriquecimento imediato
        addBehaviour(new RequestHandlerBehaviour(this));
    }
}
