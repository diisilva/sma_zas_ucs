package agents.testing;

import jade.core.Agent;
import jade.content.lang.sl.SLCodec;
import agents.DashboardOntology;

public class OntologyTestAgent extends Agent {
    @Override
    protected void setup() {
        // Registra SLCodec e Ontologia
        getContentManager().registerLanguage(new SLCodec());
        getContentManager().registerOntology(DashboardOntology.getInstance());
        // Executa o teste
        addBehaviour(new OntologyTestBehaviour());
    }
}
