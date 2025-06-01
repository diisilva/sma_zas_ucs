// File: MonitorDeDadosAgent.java
package agents;

import jade.core.Agent;

/**
 * Agente responsável por monitorar diretórios e banco de dados,
 * além de responder a solicitações do CoordenadorAgent.
 */
public class MonitorDeDadosAgent extends Agent {
    @Override
    protected void setup() {
        System.out.println("MonitorDeDados iniciado: " + getLocalName());

        // Behaviour que monitora um diretório local para novos arquivos CSV
        addBehaviour(new FileWatcherBehaviour(this));

        // Behaviour cíclico que verifica periodicamente no banco de dados por novos registros
        addBehaviour(new CyclicDatabaseCheckBehaviour(this));

        // Behaviour que responde a REQUESTs do CoordenadorAgent com informações de pendências
        addBehaviour(new RespostaCoordenadorBehaviour(this));
    }
}
