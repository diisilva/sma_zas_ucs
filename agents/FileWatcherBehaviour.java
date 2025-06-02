package agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

import java.io.IOException;
import java.nio.file.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

public class FileWatcherBehaviour extends CyclicBehaviour {
    private final Agent agent;
    private WatchService watchService;
    private static final String DIRECTORY_PATH = "C:/caminho/real/para/csv";
    private static final String DB_URL  = "jdbc:postgresql://localhost:5432/seu_banco";
    private static final String DB_USER = "seu_usuario";
    private static final String DB_PASS = "sua_senha";

    public FileWatcherBehaviour(Agent aThis) {
        super(aThis);
        this.agent = aThis;
        try {
            this.watchService = FileSystems.getDefault().newWatchService();
            Paths.get(DIRECTORY_PATH)
                    .register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
        } catch (IOException e) {
            e.printStackTrace();
            // Se falhar na inicialização, remove o behaviour do agente
            myAgent.removeBehaviour(this);
        }
    }

    @Override
    public void action() {
        WatchKey key;
        try {
            key = watchService.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
            myAgent.removeBehaviour(this);
            return;
        }

        for (WatchEvent<?> event : key.pollEvents()) {
            if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                @SuppressWarnings("unchecked")
                Path fileName = ((WatchEvent<Path>) event).context();
                String filePath = Paths.get(DIRECTORY_PATH, fileName.toString()).toString();

                if (fileName.toString().toLowerCase().endsWith(".csv")) {
                    System.out.println("Novo CSV: " + filePath);

                    if (validarCSV(filePath)) {
                        marcarArquivoNoBanco(fileName.toString());
                        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                        msg.addReceiver(new AID("CoordenadorAgent", AID.ISLOCALNAME));
                        msg.setOntology("FileWatcher");
                        msg.setContent("NEW_CSV:" + filePath);
                        agent.send(msg);
                        System.out.println("INFORM enviado sobre " + filePath);
                    } else {
                        System.out.println("CSV inválido: " + filePath);
                    }
                }
            }
        }

        boolean valid = key.reset();
        if (!valid) {
            System.out.println("WatchKey inválida. Removendo behaviour.");
            myAgent.removeBehaviour(this);
        }
    }

    private boolean validarCSV(String filePath) {
        Path p = Paths.get(filePath);
        try {
            return Files.exists(p) && Files.size(p) > 0;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void marcarArquivoNoBanco(String fileName) {
        String sql =
                "INSERT INTO arquivos_recebidos(nome_arquivo, data_detectada, status) " +
                        "VALUES (?, NOW(), ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, fileName);
            stmt.setString(2, "PENDENTE");
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
