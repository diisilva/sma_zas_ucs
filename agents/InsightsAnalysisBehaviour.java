package agents;

import java.sql.*;

/**
 * InsightsAnalysisBehaviour
 *
 * - runAnalysisBean: faz consulta JDBC em zonas_amortecimento,
 *   identifica quais UC ou anos apresentam alta de risco etc. e popula um bean InsightMsg.
 * - Retorna o bean InsightMsg para ser enviado via fillContent().
 */
public class InsightsAnalysisBehaviour {

    // Constantes JDBC (mesmos parâmetros do Monitor)
    private static final String DB_URL  = "jdbc:postgresql://localhost:5430/db_test_dev_diego";
    private static final String DB_USER = "admin";
    private static final String DB_PASS = "admin";

    /**
     * Executa a análise e retorna o bean InsightMsg.
     *
     * Passos:
     *  1) Conta quantas linhas processadas existem e calcula média de risk_level.
     *  2) Encontra o registro com risk_level máximo.
     *  3) Popula o bean InsightMsg com esses valores.
     */
    public static InsightMsg runAnalysisBean(InsightsAgent agent) {
        InsightMsg im = new InsightMsg();
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            // 1) Contar total de registros processados e média de risk_level
            String countSql = "SELECT COUNT(*) AS total, AVG(risk_level) AS media_risco " +
                    "FROM zonas_amortecimento WHERE processado = TRUE";
            try (PreparedStatement pst = conn.prepareStatement(countSql);
                 ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    im.setTotal_linhas(rs.getInt("total"));
                    im.setMedia_risco((float) rs.getDouble("media_risco"));
                }
            }

            // 2) Encontrar o registro com risk_level máximo (entre as linhas processadas)
            String maxSql = "SELECT nome_uc, risk_level FROM zonas_amortecimento " +
                    "WHERE processado = TRUE ORDER BY risk_level DESC LIMIT 1";
            try (PreparedStatement pst2 = conn.prepareStatement(maxSql);
                 ResultSet rs2 = pst2.executeQuery()) {
                if (rs2.next()) {
                    im.setUc_maior_risco(rs2.getString("nome_uc"));
                    im.setMaior_risco((float) rs2.getDouble("risk_level"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            // Em caso de erro, podemos definir valores padrão ou mensagem de erro
            im.setUc_maior_risco("ERRO");
            im.setMaior_risco(0.0f);
        }

        System.out.println(agent.getLocalName() + " gerou InsightMsg: " +
                "total_linhas=" + im.getTotal_linhas() +
                ", media_risco=" + im.getMedia_risco() +
                ", uc_maior_risco=" + im.getUc_maior_risco() +
                ", maior_risco=" + im.getMaior_risco());
        return im;
    }
}
