/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package agents;
import jade.core.Agent;
import jade.content.lang.sl.SLCodec;

public class InsightsAgent extends Agent {
    @Override
    protected void setup() {
        System.out.println("InsightsAgent iniciado: " + getLocalName());

        // 🔗 Registra linguagem e ontologia
        getContentManager().registerLanguage(new SLCodec());
        getContentManager().registerOntology(DashboardOntology.getInstance());

        // ➕ Adiciona comportamentos
        addBehaviour(new CyclicRequestListenerBehaviour(this));
        addBehaviour(new InsightsAnalysisBehaviour(this));
    }
}

