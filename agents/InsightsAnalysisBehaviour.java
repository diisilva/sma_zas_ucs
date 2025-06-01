// File: InsightsAnalysisBehaviour.java
package agents;

import jade.core.AID;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;

import java.sql.*;

/**
 * Behaviour que executa, uma única vez, a análise dos dados validados/enriquecidos
 * e envia um INFORM ao CoordenadorAgent com os insights gerados.
 */
public class InsightsAnalysisBehaviour extends OneShotBehaviour {
    private static final long serialVersionUID = 1L;
    private final InsightsAgent agent;
    private final String triggeringOntology;
    private final String triggeringContent;

    // Configurações do PostgreSQL (ajuste conforme seu ambiente)
    private static final String DB_URL  = "jdbc:postgresql://localhost:5432/seu_banco";
    private static final String DB_USER = "seu_usuario";
    private static final String DB_PASS = "sua_senha";

    public InsightsAnalysisBehaviour(InsightsAgent a, String ontology, String content) {
        super(a);
        this.agent = a;
        this.triggeringOntology = ontology;
        this.triggeringContent  = content;
        // Carrega driver PostgreSQL, caso ainda não esteja carregado
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void action() {
        System.out.println("InsightsAnalysisBehaviour iniciado por ontologia='" 
                            + triggeringOntology + "', conteúdo='" + triggeringContent + "'");

        // 1) Conectar ao banco e buscar indicadores de risco validados/enriquecidos
        //    No exemplo, contamos quantos registros existem na tabela 'indicadores_risco' 
        //    e calculamos um insight fictício (por exemplo, porcentagem de aumento entre dois últimos valores).
        String insightResult = gerarInsightBasico();

        // 2) Enviar um ACL INFORM ao CoordenadorAgent com a ontologia "InsightsResult"
        ACLMessage inform = new ACLMessage(ACLMessage.INFORM);
        inform.addReceiver(new AID("CoordenadorAgent", AID.ISLOCALNAME));
        inform.setOntology("InsightsResult");
        inform.setContent(insightResult);
        agent.send(inform);

        System.out.println("InsightsAgent enviou INFORM ao CoordenadorAgent: " + insightResult);
    }

    /**
     * Gera um insight simples a partir dos dados atuais em 'indicadores_risco'.
     * Neste exemplo, conta o total de registros e retorna uma mensagem genérica.
     *
     * @return String contendo o insight a ser enviado ao Coordenador.
     */
    private String gerarInsightBasico() {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        int totalRegistros = 0;

        String sqlCount = "SELECT COUNT(*) FROM indicadores_risco WHERE processado = TRUE";

        try {
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
            stmt = conn.prepareStatement(sqlCount);
            rs = stmt.executeQuery();
            if (rs.next()) {
                totalRegistros = rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try { if (rs   != null) rs.close();   } catch (Exception e) { e.printStackTrace(); }
            try { if (stmt != null) stmt.close(); } catch (Exception e) { e.printStackTrace(); }
            try { if (conn != null) conn.close(); } catch (Exception e) { e.printStackTrace(); }
        }

        // Monta um insight fictício usando o total de registros processados
        // (ajuste a lógica conforme os indicadores reais do seu modelo de dados)
        return "ANALYSIS_COMPLETE: TOTAL_PROCESSED_INDICATORS=" + totalRegistros;
    }
}
