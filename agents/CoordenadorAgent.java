/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package agents;
    import jade.content.lang.sl.SLCodec;
    import jade.core.Agent;

/**
 *
 * @author eduar
 */
public class CoordenadorAgent extends Agent {
    @Override
    protected void setup() {
        System.out.println("CoordenadorAgent iniciado: " + getLocalName());

        getContentManager().registerLanguage(new SLCodec());
        getContentManager().registerOntology(DashboardOntology.getInstance());

        addBehaviour(new EnviaRequisicaoInsightsBehaviour());
    }
}
