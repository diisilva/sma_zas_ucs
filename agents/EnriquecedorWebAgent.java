package agents;
import jade.core.Agent;
/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author eduar
 */
public class EnriquecedorWebAgent extends Agent {
    @Override
    protected void setup() {
        System.out.println("EnriquecedorWebAgent iniciado: " + getLocalName());

        addBehaviour(new PeriodicApiFetcherBehaviour(this, 60000)); // a cada 60s
        addBehaviour(new RequestHandlerBehaviour(this));
    }
}

