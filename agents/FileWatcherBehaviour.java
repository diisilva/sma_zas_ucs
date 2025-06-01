// File: FileWatcherBehaviour.java
package agents;

import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.lang.acl.ACLMessage;
import java.io.IOException;
import java.nio.file.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

/**
 * Behaviour que monitora um diretório em busca de novos arquivos CSV.
 * Ao detectar um novo arquivo, envia uma mensagem FIPA-Inform ao Coordenador
 * e marca a ocorrência no banco de dados (PostgreSQL).
 */
public class FileWatcherBehaviour extends Behaviour {
    private final MonitorDeDadosAgent agent;
    private WatchService watchService;
    private static final String DIRECTORY_PATH = "/caminho/para/o/diretorio/csv"; 
    // Ajuste acima para o diretório que deseja monitorar.

    // Parâmetros de conexão ao PostgreSQL (ajuste conforme seu ambiente)
    private static final String DB_URL  = "jdbc:postgresql://localhost:5432/seu_banco";
    private static final String DB_USER = "seu_usuario";
    private static final String DB_PASS = "sua_senha";

    private boolean stopped = false;

    public FileWatcherBehaviour(MonitorDeDadosAgent aThis) {
        this.agent = aThis;
        try {
            this.watchService = FileSystems.getDefault().newWatchService();
            Path dir = Paths.get(DIRECTORY_PATH);
            dir.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
        } catch (IOException e) {
            e.printStackTrace();
            // Se não conseguir inicializar o WatchService, encerra o behaviour.
            stopped = true;
        }
    }

    @Override
    public void action() {
        if (stopped) {
            // Se houve erro na inicialização, não faz nada.
            block();
            return;
        }

        WatchKey key;
        try {
            // Aguarda até que um evento ocorra (bloqueante)
            key = watchService.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
            block(1000);
            return;
        }

        for (WatchEvent<?> event : key.pollEvents()) {
            WatchEvent.Kind<?> kind = event.kind();

            // Apenas criações de arquivos
            if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                WatchEvent<Path> ev = (WatchEvent<Path>) event;
                Path fileName = ev.context();
                String filePath = Paths.get(DIRECTORY_PATH, fileName.toString()).toString();

                // Verifica se o arquivo é .csv (pode ajustar filtro conforme necessário)
                if (fileName.toString().toLowerCase().endsWith(".csv")) {
                    System.out.println("Novo arquivo CSV detectado: " + filePath);

                    // 1) Validação de integridade do CSV (por exemplo, checar colunas ou nulos)
                    boolean valido = validarCSV(filePath);

                    if (valido) {
                        // 2) Atualizar flag no banco de dados (por ex.: marcar como “pendente de processamento”)
                        marcarArquivoNoBanco(fileName.toString());

                        // 3) Enviar FIPA-Inform ao agente Coordenador informando localização do arquivo
                        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                        // Caso o nome do agente Coordenador seja, por exemplo, "CoordenadorAgent",
                        // ajuste abaixo:
                        msg.addReceiver(new AID("CoordenadorAgent", AID.ISLOCALNAME));
                        msg.setOntology("FileWatcher");
                        msg.setContent("NEW_CSV:" + filePath);
                        agent.send(msg);
                        System.out.println("Mensagem INFORM enviada ao Coordenador sobre " + filePath);
                    } else {
                        System.out.println("Arquivo CSV inválido ou com inconsistência: " + filePath);
                        // Opcional: enviar outro tipo de mensagem ou gravar log
                    }
                }
            }
        }

        // Reseta a chave para receber novos eventos
        boolean valid = key.reset();
        if (!valid) {
            System.out.println("WatchKey inválida. Encerrando FileWatcherBehaviour.");
            stopped = true;
        }
    }

    @Override
    public boolean done() {
        // Esse behaviour roda indefinidamente até haver erro ou interrupção do agente.
        return stopped;
    }

    /**
     * Exemplo simplificado de validação de CSV.
     * Aqui você pode abrir o arquivo, checar número de colunas, tipos, campos nulos, etc.
     * Retorna true se estiver válido; false caso contrário.
     */
    private boolean validarCSV(String filePath) {
        // TODO: implementar validações específicas (por ex.: abrir com Apache Commons CSV, etc.)
        // Por ora, apenas checamos se o arquivo existe e tem tamanho > 0.
        Path p = Paths.get(filePath);
        try {
            return Files.exists(p) && Files.size(p) > 0;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Marca no banco de dados que esse arquivo foi detectado e está aguardando processamento.
     * Exemplo: insere registro na tabela "arquivos_recebidos" ou atualiza flag em tabela existente.
     */
    private void marcarArquivoNoBanco(String fileName) {
        Connection conn = null;
        PreparedStatement stmt = null;
        String sql =
            "INSERT INTO arquivos_recebidos(nome_arquivo, data_detectada, status) "
          + "VALUES (?, NOW(), ?)";

        try {
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, fileName);
            stmt.setString(2, "PENDENTE");
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try { if (stmt != null) stmt.close(); } catch (Exception e) { e.printStackTrace(); }
            try { if (conn != null) conn.close(); } catch (Exception e) { e.printStackTrace(); }
        }
    }
}
