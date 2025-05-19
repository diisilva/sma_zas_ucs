package agents;
import jade.content.onto.*;
import jade.content.schema.*;

public class DashboardOntology extends Ontology {
    public static final String ONTOLOGY_NAME = "dashboard-ontology";
    private static Ontology instance = new DashboardOntology();

    public static Ontology getInstance() {
        return instance;
    }

    // Elementos
    public static final String INFORMACAO = "Informacao";
    public static final String INSIGHT = "Insight";

    protected DashboardOntology() {
        super(ONTOLOGY_NAME, BasicOntology.getInstance());

        try {
            // Classe Informacao
            ConceptSchema informacaoSchema = new ConceptSchema(INFORMACAO);
            add(informacaoSchema, Informacao.class);
            informacaoSchema.add("mensagem", (PrimitiveSchema) getSchema(BasicOntology.STRING));

            // Classe Insight
            ConceptSchema insightSchema = new ConceptSchema(INSIGHT);
            add(insightSchema, Insight.class);
            insightSchema.add("descricao", (PrimitiveSchema) getSchema(BasicOntology.STRING));

        } catch (OntologyException e) {
            e.printStackTrace();
        }
    }
}
