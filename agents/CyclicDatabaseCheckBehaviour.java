package agents;

import jade.core.behaviours.Behaviour;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.core.Agent;

import java.sql.*;

/**
 * Behaviour cíclico que verifica periodicamente no PostgreSQL se há novos registros
 * na tabela zonas_amortecimento com processado = FALSE.
 * Quando encontra, envia FIPA-INFORM ao Coordenador e marca como processado = TRUE.
 */
public class CyclicDatabaseCheckBehaviour extends Behaviour {

    private final Agent agent;


    // Ajuste para usar o container Docker: porta 5430, banco db_test_dev_diego
    private static final String DB_URL  = "jdbc:postgresql://localhost:5430/db_test_dev_diego";
    private static final String DB_USER = "admin";
    private static final String DB_PASS = "admin";

    public CyclicDatabaseCheckBehaviour(Agent aThis) {
        super(aThis);
        this.agent = aThis;
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void action() {
        Connection conn = null;
        PreparedStatement selectStmt = null;
        PreparedStatement updateStmt = null;
        ResultSet rs = null;

        String selectSQL =
                "SELECT id, ANO, nome_uc, sum_area_total_ha, risk_level, risk_category " +
                        "FROM zonas_amortecimento " +
                        "WHERE processado = FALSE";  // só pega novos registros

        // Agora usamos apenas o id para marcar como processado
        String updateSQL =
                "UPDATE zonas_amortecimento SET processado = TRUE WHERE id = ?";

        try {
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
            selectStmt = conn.prepareStatement(selectSQL);
            rs = selectStmt.executeQuery();

            while (rs.next()) {
                int id = rs.getInt("id");  // obtém o id único
                int ano = rs.getInt("ANO");
                String nomeUc = rs.getString("nome_uc");
                double areaTotal = rs.getDouble("sum_area_total_ha");
                double risco = rs.getDouble("risk_level");
                String categoria = rs.getString("risk_category");

                // Monta conteúdo simples para o Coordenador
                String informContent = String.format(
                        "ZONA|%d|%s|%.2f|%.2f|%s",
                        ano, nomeUc, areaTotal, risco, categoria
                );

                // Envia INFORM ao Coordenador
                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.addReceiver(new AID("CoordenadorAgent", AID.ISLOCALNAME));
                msg.setOntology("DatabaseCheck");
                msg.setContent(informContent);
                agent.send(msg);
                System.out.println(agent.getLocalName() +
                        " enviou INFORM: " + informContent);

                // Marca como processado usando apenas o id
                updateStmt = conn.prepareStatement(updateSQL);
                updateStmt.setInt(1, id);
                updateStmt.executeUpdate();
                updateStmt.close();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try { if (rs        != null) rs.close();        } catch (Exception ignored) {}
            try { if (selectStmt!= null) selectStmt.close(); } catch (Exception ignored) {}
            try { if (updateStmt!= null) updateStmt.close(); } catch (Exception ignored) {}
            try { if (conn      != null) conn.close();      } catch (Exception ignored) {}
        }

        // Espera 10 segundos antes de verificar de novo
        block(10000);
    }

    @Override
    public boolean done() {
        return false;  // roda indefinidamente
    }
}
