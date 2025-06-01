// File: DashboardOntology.java
package agents;

import jade.content.onto.BasicOntology;
import jade.content.onto.Ontology;
import jade.content.schema.ConceptSchema;
import jade.content.schema.PrimitiveSchema;
import jade.content.schema.TerminalSchema;

/**
 * Classe que define a ontologia “Dashboard-Ontology”, incluindo o conceito Insight.
 */
public class DashboardOntology extends Ontology implements DashboardVocabulary {
    private static final long serialVersionUID = 1L;

    // Única instância da ontologia (padrão singleton)
    private static Ontology theInstance = new DashboardOntology();

    /** Retorna a instância singleton desta ontologia. */
    public static Ontology getInstance() {
        return theInstance;
    }

    /** Construtor privado para registrar esquemas. */
    private DashboardOntology() {
        // Chama o construtor de Ontology indicando nome e ontologia-mãe (usamos BasicOntology)
        super(ONTOLOGY_NAME, BasicOntology.getInstance());

        try {
            // Definição do esquema do conceito "Insight"
            ConceptSchema insightSchema = new ConceptSchema(INSIGHT);
            add(insightSchema, Insight.class);

            // Adiciona o slot "descricao" ao Insight, tipo string
            insightSchema.add(INSIGHT_DESCRICAO, (PrimitiveSchema) getSchema(BasicOntology.STRING));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
