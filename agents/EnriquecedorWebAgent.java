package agents;

import jade.content.onto.basic.Action;
import jade.content.lang.sl.SLCodec;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.*;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * EnriquecedorWebAgent
 *
 * - Registra-se no DF como "enriquecedor-web".
 * - A cada 15s envia um objeto Enriquecimento ao MonitorDeDadosAgent (embrulhado num Action).
 *
 * Ajustes necessários para que fillContent(...) compile:
 * 1) A classe Enriquecimento implementa jade.content.Concept.
 * 2) Usamos Thread.sleep(2000) em vez de block(...) dentro de setup().
 */
public class EnriquecedorWebAgent extends Agent {
    private AID monitorAID;
    private AID insightsAID;

    @Override
    protected void setup() {
        System.out.println("EnriquecedorWebAgent iniciado: " + getLocalName());

        // 1) Registrar linguagem e ontologia
        getContentManager().registerLanguage(new SLCodec());
        getContentManager().registerOntology(DashboardOntology.getInstance());

        // 2) Registrar serviço no DF como "enriquecedor-web"
        registerService("enriquecedor-web");

        // 3) Pequeno delay para que Monitor e Insights se registrem
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }

        // 4) Descobrir Monitor e Insights no DF
        monitorAID = discoverService("monitor-de-dados");
        insightsAID = discoverService("insights");
        if (monitorAID == null || insightsAID == null) {
            System.err.println("EnriquecedorWebAgent: não encontrou Monitor ou Insights no DF. Encerrando.");
            doDelete();
            return;
        }
        System.out.println("EnriquecedorWebAgent descobriu Monitor=" + monitorAID.getLocalName()
                + " e Insights=" + insightsAID.getLocalName());

        // 5) TickerBehaviour: a cada 15s envia objeto Enriquecimento embalado num Action
        addBehaviour(new TickerBehaviour(this, 15000) {
            @Override
            protected void onTick() {
                // Cria e popula o objeto Enriquecimento
                Enriquecimento e = new Enriquecimento();
                e.setFonte("INPE");
                e.setTimestamp(ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT));
                e.setDetalhes("{ \"incendios\": 42, \"desmatamento\": 17 }");

                // Monta a mensagem INFORM para o Monitor
                ACLMessage inform = new ACLMessage(ACLMessage.INFORM);
                inform.addReceiver(monitorAID);
                inform.setOntology(DashboardOntology.getInstance().getName());
                inform.setLanguage(new SLCodec().getName());

                try {
                    // Envolve o Concept (Enriquecimento) num Predicate (Action)
                    Action payload = new Action(getAID(), e);
                    getContentManager().fillContent(inform, payload);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    inform.setContent("ERRO_NO_FILLCONTENT_ENRIQUECIMENTO");
                }

                send(inform);
                System.out.println("EnriquecedorWebAgent enviou Enriquecimento (objeto).");
            }
        });
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        System.out.println("EnriquecedorWebAgent \"" + getLocalName() + "\" finalizado.");
    }

    private void registerService(String serviceType) {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType(serviceType);
        sd.setName(getLocalName());
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

    private AID discoverService(String serviceType) {
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType(serviceType);
        template.addServices(sd);
        try {
            DFAgentDescription[] results = DFService.search(this, template);
            if (results != null && results.length > 0) {
                return results[0].getName();
            }
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        return null;
    }
}
