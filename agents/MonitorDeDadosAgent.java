// File: CyclicDatabaseCheckBehaviour.java
package agents;

import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

import java.sql.*;

/**
 * Behaviour cíclico que verifica periodicamente no PostgreSQL se há novos registros
 * (por exemplo, indicadores ou linhas de tabela) a serem processados pelo Coordenador.
 * Se encontrar algo novo, marca como “processado” e envia FIPA-Inform ao Coordenador.
 */
public class CyclicDatabaseCheckBehaviour extends CyclicBehaviour {
    private final MonitorDeDadosAgent agent;

    // Parâmetros de conexão ao PostgreSQL (ajuste conforme seu ambiente)
    private static final String DB_URL  = "jdbc:postgresql://localhost:5432/test_db";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "root";

    public CyclicDatabaseCheckBehaviour(MonitorDeDadosAgent aThis) {
        super(aThis);
        this.agent = aThis;

        // Carrega o driver do PostgreSQL (caso necessário)
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void action() {
        Connection conn = null;
        PreparedStatement selectStmt   = null;
        PreparedStatement updateStmt   = null;
        ResultSet rs = null;

        String selectSQL = 
            "SELECT id, descricao " 
          + "FROM indicadores_risco "
          + "WHERE processado = FALSE";

        String updateSQL = 
            "UPDATE indicadores_risco "
          + "SET processado = TRUE "
          + "WHERE id = ?";

        try {
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
            selectStmt = conn.prepareStatement(selectSQL);
            rs = selectStmt.executeQuery();

            while (rs.next()) {
                long id = rs.getLong("id");
                String descricao = rs.getString("descricao");

                // Envia mensagem ao Coordenador com o ID do registro recém-detectado
                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.addReceiver(new AID("CoordenadorAgent", AID.ISLOCALNAME)); 
                msg.setOntology("DatabaseCheck");
                msg.setContent("NEW_RECORD:" + id + ";" + descricao);
                agent.send(msg);
                System.out.println("Enviado INFORM ao Coordenador para indicador " + id);

                // Marca como processado para não reenviar continuamente
                updateStmt = conn.prepareStatement(updateSQL);
                updateStmt.setLong(1, id);
                updateStmt.executeUpdate();
                updateStmt.close();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try { if (rs        != null) rs.close();        } catch (Exception e) { e.printStackTrace(); }
            try { if (selectStmt!= null) selectStmt.close(); } catch (Exception e) { e.printStackTrace(); }
            try { if (updateStmt!= null) updateStmt.close(); } catch (Exception e) { e.printStackTrace(); }
            try { if (conn      != null) conn.close();      } catch (Exception e) { e.printStackTrace(); }
        }

        // Aguarda 10 segundos antes de rodar de novo (por exemplo)
        block(10000);
    }
}
