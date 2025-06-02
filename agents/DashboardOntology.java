package agents;

import jade.content.onto.*;
import jade.content.schema.*;

/**
 * DashboardOntology
 *
 * Ontologia unificada para o nosso SMA, contendo conceitos de:
 *  - Informacao (genérico, mantém compatibilidade)
 *  - Insight (genérico, mantém compatibilidade)
 *  - Zona: correspondendo a um registro de zona_amortecimento
 *  - InsightMsg: correspondendo à estrutura de resultados gerados pelo InsightsAgent
 *  - Enriquecimento: correspondendo aos dados obtidos pelo EnriquecedorWebAgent
 */
public class DashboardOntology extends Ontology {
    // Nome da ontologia
    public static final String ONTOLOGY_NAME = "dashboard-ontology";
    private static Ontology instance = new DashboardOntology();

    public static Ontology getInstance() {
        return instance;
    }

    // ===== Nomes dos conceitos (identificadores de schema) =====

    // Conceitos originais (para compatibilidade)
    public static final String INFORMACAO = "Informacao";
    public static final String INSIGHT      = "Insight";

    // Novo conceito: Zona de Amortecimento
    public static final String ZONA = "Zona";
    public static final String ZONA_ANO                = "ano";
    public static final String ZONA_NOME_UC            = "nome_uc";
    public static final String ZONA_SUM_AREA_TOTAL_HA  = "sum_area_total_ha";
    public static final String ZONA_RISK_LEVEL         = "risk_level";
    public static final String ZONA_RISK_CATEGORY      = "risk_category";
    public static final String ZONA_ID                 = "id";

    // Novo conceito: InsightMsg (resultado da análise)
    public static final String INSIGHT_MSG                  = "InsightMsg";
    public static final String INSIGHT_MSG_TOTAL_LINHAS     = "total_linhas";
    public static final String INSIGHT_MSG_MEDIA_RISCO       = "media_risco";
    public static final String INSIGHT_MSG_UC_MAIOR_RISCO    = "uc_maior_risco";
    public static final String INSIGHT_MSG_MAIOR_RISCO       = "maior_risco";

    // Novo conceito: Enriquecimento (dados externos)
    public static final String ENRIQUECIMENTO             = "Enriquecimento";
    public static final String ENRIQUECIMENTO_FONTE       = "fonte";
    public static final String ENRIQUECIMENTO_TIMESTAMP   = "timestamp";
    public static final String ENRIQUECIMENTO_DETALHES    = "detalhes";

    protected DashboardOntology() {
        super(ONTOLOGY_NAME, BasicOntology.getInstance());

        try {
            // -------------------------------------------------
            // 1) Conceito Informacao (mantive por compatibilidade)
            // -------------------------------------------------
            ConceptSchema informacaoSchema = new ConceptSchema(INFORMACAO);
            add(informacaoSchema, Informacao.class);
            informacaoSchema.add("mensagem", (PrimitiveSchema) getSchema(BasicOntology.STRING));

            // ---------------------------------------
            // 2) Conceito Insight (mantido/exemplo)
            // ---------------------------------------
            ConceptSchema insightSchema = new ConceptSchema(INSIGHT);
            add(insightSchema, Insight.class);
            insightSchema.add("descricao", (PrimitiveSchema) getSchema(BasicOntology.STRING));

            // ---------------------------------------
            // 3) Novo conceito: Zona
            // ---------------------------------------
            ConceptSchema zonaSchema = new ConceptSchema(ZONA);
            add(zonaSchema, Zona.class);
            // Campos do schema “Zona”
            zonaSchema.add(ZONA_ID,                (PrimitiveSchema) getSchema(BasicOntology.INTEGER));
            zonaSchema.add(ZONA_ANO,               (PrimitiveSchema) getSchema(BasicOntology.INTEGER));
            zonaSchema.add(ZONA_NOME_UC,           (PrimitiveSchema) getSchema(BasicOntology.STRING));
            zonaSchema.add(ZONA_SUM_AREA_TOTAL_HA, (PrimitiveSchema) getSchema(BasicOntology.FLOAT));
            zonaSchema.add(ZONA_RISK_LEVEL,        (PrimitiveSchema) getSchema(BasicOntology.FLOAT));
            zonaSchema.add(ZONA_RISK_CATEGORY,     (PrimitiveSchema) getSchema(BasicOntology.STRING));

            // ---------------------------------------
            // 4) Novo conceito: InsightMsg
            // ---------------------------------------
            ConceptSchema insightMsgSchema = new ConceptSchema(INSIGHT_MSG);
            add(insightMsgSchema, InsightMsg.class);
            // Campos do schema “InsightMsg”
            insightMsgSchema.add(INSIGHT_MSG_TOTAL_LINHAS,  (PrimitiveSchema) getSchema(BasicOntology.INTEGER));
            insightMsgSchema.add(INSIGHT_MSG_MEDIA_RISCO,   (PrimitiveSchema) getSchema(BasicOntology.FLOAT));
            insightMsgSchema.add(INSIGHT_MSG_UC_MAIOR_RISCO,(PrimitiveSchema) getSchema(BasicOntology.STRING));
            insightMsgSchema.add(INSIGHT_MSG_MAIOR_RISCO,   (PrimitiveSchema) getSchema(BasicOntology.FLOAT));

            // ---------------------------------------
            // 5) Novo conceito: Enriquecimento
            // ---------------------------------------
            ConceptSchema enriquecimentoSchema = new ConceptSchema(ENRIQUECIMENTO);
            add(enriquecimentoSchema, Enriquecimento.class);
            // Campos do schema “Enriquecimento”
            enriquecimentoSchema.add(ENRIQUECIMENTO_FONTE,     (PrimitiveSchema) getSchema(BasicOntology.STRING));
            enriquecimentoSchema.add(ENRIQUECIMENTO_TIMESTAMP, (PrimitiveSchema) getSchema(BasicOntology.STRING)); // data/hora como string
            enriquecimentoSchema.add(ENRIQUECIMENTO_DETALHES,  (PrimitiveSchema) getSchema(BasicOntology.STRING));

        } catch (OntologyException e) {
            e.printStackTrace();
        }
    }
}
