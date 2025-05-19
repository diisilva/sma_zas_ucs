/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package trabalho;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

/**
 *
 * @author eduar
 */
public class MonitorDeDadosAgent extends Agent {
    @Override
    protected void setup() {
        System.out.println("MonitorDeDados iniciado: " + getLocalName());

        addBehaviour(new FileWatcherBehaviour(this));
        addBehaviour(new CyclicDatabaseCheckBehaviour(this));
    }
}
