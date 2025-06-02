package agents;

import jade.content.onto.basic.Action;
import jade.content.ContentElement;
import jade.content.Concept;
import jade.content.lang.sl.SLCodec;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

/**
 * CoordenadorAgent (versão configurável):
 * - Se iniciado com "simple", faz apenas Monitor → AguardarZona (omitir Insights e Enriquecimento).
 * - Se iniciado com "insightsOnly", faz Monitor → Insights → fim (ignora Enriquecedor).
 * - Se iniciado com "full", faz Monitor → Insights → Enriquecedor → fim.
 */
public class CoordenadorAgent extends Agent {

    private AID monitorAID;
    private AID insightsAID;
    private AID enriquecedorAID;
    private String mode = "full"; // padrão

    @Override
    protected void setup() {
        System.out.println("CoordenadorAgent iniciado: " + getLocalName());

        // 1) Definir modo a partir dos argumentos (apenas o primeiro importa aqui)
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            String raw = args[0].toString().trim().toLowerCase();
            if (raw.equals("simple")) {
                mode = "simple";
            } else if (raw.equals("insightsonly")) {
                mode = "insightsOnly";
            } else if (raw.equals("full")) {
                mode = "full";
            }
        }
        System.out.println("CoordenadorAgent modo = " + mode);

        // 2) Registrar ontologia/linguagem
        getContentManager().registerLanguage(new SLCodec());
        getContentManager().registerOntology(DashboardOntology.getInstance());

        // 3) Registrar serviço no DF como “coordenador”
        registerService("coordenador");

        // 4) OneShotBehaviour para descobrir outros agentes no DF e iniciar o fluxo correto
        addBehaviour(new OneShotBehaviour() {
            @Override
            public void action() {
                block(2000); // aguarda 2s para que Monitor, Insights e Enriquecedor se registrem

                // Descobre Monitor
                monitorAID = discoverService("monitor-de-dados");
                if (monitorAID == null) {
                    System.err.println("CoordenadorAgent: monitor não encontrado. Encerrando.");
                    doDelete();
                    return;
                }

                if (mode.equals("simple")) {
                    // --- MODO SIMPLE: apenas receber Zona e imprimir no console ---
                    System.out.println("Modo SIMPLE: apenas receber Zona do Monitor.");
                    addBehaviour(new CyclicBehaviour() {
                        @Override
                        public void action() {
                            // Monta o template: INFORM + ontologia DashboardOntology + linguagem SL
                            MessageTemplate mtPerfOnt = MessageTemplate.and(
                                    MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                                    MessageTemplate.MatchOntology(DashboardOntology.getInstance().getName())
                            );
                            MessageTemplate mt = MessageTemplate.and(
                                    mtPerfOnt,
                                    MessageTemplate.MatchLanguage(new SLCodec().getName())
                            );

                            ACLMessage msg = receive(mt);
                            if (msg != null) {
                                try {
                                    ContentElement content = getContentManager().extractContent(msg);
                                    // CHECAGEM: vem um Action(agentAID, Zona)
                                    if (content instanceof Action) {
                                        Object payload = ((Action) content).getAction();
                                        if (payload instanceof Zona) {
                                            Zona z = (Zona) payload;
                                            System.out.println("CoordenadorAgent[SIMPLE] recebeu Zona: "
                                                    + z.getNome_uc() + ", risco=" + z.getRisk_level());
                                        }
                                    }
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                            } else {
                                block();
                            }
                        }
                    });
                    return;
                }

                // --------- A PARTIR DESTE PONTO: modos insightsOnly ou full ---------
                // Descobre InsightsAgent
                insightsAID = discoverService("insights");
                if (insightsAID == null) {
                    System.err.println("CoordenadorAgent: insights não encontrado. Encerrando.");
                    doDelete();
                    return;
                }

                if (mode.equals("insightsOnly")) {
                    // --- MODO INSIGHTSONLY: PedirMonitor → AguardarMonitor → PedirInsights → AguardarInsights → FINAL ---
                    System.out.println("Modo INSIGHTSONLY: Monitor → Insights.");
                    addBehaviour(createFSM_InsightsOnly());
                    return;
                }

                if (mode.equals("full")) {
                    // Descobre também EnriquecedorWeb
                    enriquecedorAID = discoverService("enriquecedor-web");
                    if (enriquecedorAID == null) {
                        System.err.println("CoordenadorAgent: enriquecedor-web não encontrado. Encerrando.");
                        doDelete();
                        return;
                    }
                    System.out.println("Descobertos: monitor=" + monitorAID.getLocalName()
                            + ", insights=" + insightsAID.getLocalName()
                            + ", enriquecedor=" + enriquecedorAID.getLocalName());
                    addBehaviour(createFSM_Full());
                }
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
        System.out.println("CoordenadorAgent \"" + getLocalName() + "\" finalizado.");
    }

    // Registra este agente no DF com o tipo fornecido
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

    // Descobre o AID de um agente pelo tipo de serviço registrado no DF
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

    // -------------------------------------------------------------
    // FSM para o modo “insightsOnly” (Monitor → Insights → End)
    private FSMBehaviour createFSM_InsightsOnly() {
        FSMBehaviour fsm = new FSMBehaviour(this) {
            @Override
            public int onEnd() {
                myAgent.doDelete();
                return super.onEnd();
            }
        };

        final String PEDIR_MONITOR     = "PedirMonitor";
        final String AGUARDAR_MONITOR  = "AguardarMonitor";
        final String PEDIR_INSIGHTS    = "PedirInsights";
        final String AGUARDAR_INSIGHTS = "AguardarInsights";
        final String FINAL             = "Final";

        // 1) PedirMonitor (envia REQUEST ao Monitor)
        fsm.registerFirstState(new OneShotBehaviour() {
            @Override
            public void action() {
                ACLMessage req = new ACLMessage(ACLMessage.REQUEST);
                req.addReceiver(monitorAID);
                req.setOntology("DatabaseCheckRequest");
                req.setLanguage(new SLCodec().getName());
                req.setContent("VERIFICAR_BASE");
                send(req);
                System.out.println("Coordenador: enviei REQUEST para monitor → \"VERIFICAR_BASE\"");
            }
        }, PEDIR_MONITOR);

        // 2) AguardarMonitor (ouvir INFORM com o objeto Zona)
        fsm.registerState(new SimpleBehaviour() {
            private boolean recebido = false;

            @Override
            public void action() {
                MessageTemplate mtPerfOnt = MessageTemplate.and(
                        MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                        MessageTemplate.MatchOntology(DashboardOntology.getInstance().getName())
                );
                MessageTemplate mt = MessageTemplate.and(
                        mtPerfOnt,
                        MessageTemplate.MatchLanguage(new SLCodec().getName())
                );
                ACLMessage msg = receive(mt);
                if (msg != null) {
                    try {
                        ContentElement content = getContentManager().extractContent(msg);
                        // CHECAGEM: vem um Action(agentAID, Zona)
                        if (content instanceof Action) {
                            Object payload = ((Action) content).getAction();
                            if (payload instanceof Zona) {
                                Zona z = (Zona) payload;
                                System.out.println("Coordenador recebeu (insightsOnly) Zona: "
                                        + z.getNome_uc() + ", risco=" + z.getRisk_level());
                            }
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    recebido = true;
                } else {
                    block();
                }
            }

            @Override
            public boolean done() {
                return recebido;
            }
        }, AGUARDAR_MONITOR);

        // 3) PedirInsights (envia REQUEST ao Insights)
        fsm.registerState(new OneShotBehaviour() {
            @Override
            public void action() {
                ACLMessage req = new ACLMessage(ACLMessage.REQUEST);
                req.addReceiver(insightsAID);
                req.setOntology("InsightsRequest");
                req.setLanguage(new SLCodec().getName());
                req.setContent("ANALISAR_DADOS_RECENTES");
                send(req);
                System.out.println("Coordenador: enviei REQUEST para insights → \"ANALISAR_DADOS_RECENTES\"");
            }
        }, PEDIR_INSIGHTS);

        // 4) AguardarInsights (ouvir INFORM com o objeto InsightMsg)
        fsm.registerState(new SimpleBehaviour() {
            private boolean recebido = false;

            @Override
            public void action() {
                MessageTemplate mtPerfOnt = MessageTemplate.and(
                        MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                        MessageTemplate.MatchOntology(DashboardOntology.getInstance().getName())
                );
                MessageTemplate mt = MessageTemplate.and(
                        mtPerfOnt,
                        MessageTemplate.MatchLanguage(new SLCodec().getName())
                );
                ACLMessage msg = receive(mt);
                if (msg != null) {
                    try {
                        ContentElement content = getContentManager().extractContent(msg);
                        // CHECAGEM: vem um Action(agentAID, InsightMsg)
                        if (content instanceof Action) {
                            Object payload = ((Action) content).getAction();
                            if (payload instanceof InsightMsg) {
                                InsightMsg im = (InsightMsg) payload;
                                System.out.println("Coordenador recebeu (insightsOnly) Insight: UC maior risco = "
                                        + im.getUc_maior_risco() + ", valor=" + im.getMaior_risco());
                            }
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    recebido = true;
                } else {
                    block();
                }
            }

            @Override
            public boolean done() {
                return recebido;
            }
        }, AGUARDAR_INSIGHTS);

        // 5) Estado Final
        fsm.registerLastState(new OneShotBehaviour() {
            @Override
            public void action() {
                System.out.println("Coordenador [insightsOnly]: fluxo concluído. Encerrando agente.");
            }
        }, FINAL);

        // Transições
        fsm.registerDefaultTransition(PEDIR_MONITOR, AGUARDAR_MONITOR);
        fsm.registerDefaultTransition(AGUARDAR_MONITOR, PEDIR_INSIGHTS);
        fsm.registerDefaultTransition(PEDIR_INSIGHTS, AGUARDAR_INSIGHTS);
        fsm.registerDefaultTransition(AGUARDAR_INSIGHTS, FINAL);

        return fsm;
    }

    // -------------------------------------------------------------
    // FSM para o modo “full” (Monitor → Insights → Enriquecer → Final)
    private FSMBehaviour createFSM_Full() {
        FSMBehaviour fsm = new FSMBehaviour(this) {
            @Override
            public int onEnd() {
                myAgent.doDelete();
                return super.onEnd();
            }
        };

        final String PEDIR_MONITOR        = "PedirMonitor";
        final String AGUARDAR_MONITOR     = "AguardarMonitor";
        final String PEDIR_INSIGHTS       = "PedirInsights";
        final String AGUARDAR_INSIGHTS    = "AguardarInsights";
        final String PEDIR_ENRIQUECER     = "PedirEnriquecer";
        final String AGUARDAR_ENRIQUECER  = "AguardarEnriquecer";
        final String FINAL                = "Final";

        // 1) PedirMonitor
        fsm.registerFirstState(new OneShotBehaviour() {
            @Override
            public void action() {
                ACLMessage req = new ACLMessage(ACLMessage.REQUEST);
                req.addReceiver(monitorAID);
                req.setOntology("DatabaseCheckRequest");
                req.setLanguage(new SLCodec().getName());
                req.setContent("VERIFICAR_BASE");
                send(req);
                System.out.println("Coordenador: enviei REQUEST para monitor → \"VERIFICAR_BASE\"");
            }
        }, PEDIR_MONITOR);

        // 2) AguardarMonitor
        fsm.registerState(new SimpleBehaviour() {
            private boolean recebido = false;

            @Override
            public void action() {
                MessageTemplate mtPerfOnt = MessageTemplate.and(
                        MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                        MessageTemplate.MatchOntology(DashboardOntology.getInstance().getName())
                );
                MessageTemplate mt = MessageTemplate.and(
                        mtPerfOnt,
                        MessageTemplate.MatchLanguage(new SLCodec().getName())
                );
                ACLMessage msg = receive(mt);
                if (msg != null) {
                    try {
                        ContentElement content = getContentManager().extractContent(msg);
                        // CHECAGEM: vem Action(agentAID, Zona)
                        if (content instanceof Action) {
                            Object payload = ((Action) content).getAction();
                            if (payload instanceof Zona) {
                                Zona z = (Zona) payload;
                                System.out.println("Coordenador recebeu zona: "
                                        + z.getNome_uc() + " com risco " + z.getRisk_level());
                            }
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    recebido = true;
                } else {
                    block();
                }
            }

            @Override
            public boolean done() {
                return recebido;
            }
        }, AGUARDAR_MONITOR);

        // 3) PedirInsights
        fsm.registerState(new OneShotBehaviour() {
            @Override
            public void action() {
                ACLMessage req = new ACLMessage(ACLMessage.REQUEST);
                req.addReceiver(insightsAID);
                req.setOntology("InsightsRequest");
                req.setLanguage(new SLCodec().getName());
                req.setContent("ANALISAR_DADOS_RECENTES");
                send(req);
                System.out.println("Coordenador: enviei REQUEST para insights → \"ANALISAR_DADOS_RECENTES\"");
            }
        }, PEDIR_INSIGHTS);

        // 4) AguardarInsights
        fsm.registerState(new SimpleBehaviour() {
            private boolean recebido = false;

            @Override
            public void action() {
                MessageTemplate mtPerfOnt = MessageTemplate.and(
                        MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                        MessageTemplate.MatchOntology(DashboardOntology.getInstance().getName())
                );
                MessageTemplate mt = MessageTemplate.and(
                        mtPerfOnt,
                        MessageTemplate.MatchLanguage(new SLCodec().getName())
                );
                ACLMessage msg = receive(mt);
                if (msg != null) {
                    try {
                        ContentElement content = getContentManager().extractContent(msg);
                        // CHECAGEM: vem Action(agentAID, InsightMsg)
                        if (content instanceof Action) {
                            Object payload = ((Action) content).getAction();
                            if (payload instanceof InsightMsg) {
                                InsightMsg im = (InsightMsg) payload;
                                System.out.println("Coordenador recebeu insight: UC com maior risco = "
                                        + im.getUc_maior_risco() + ", valor=" + im.getMaior_risco());
                            }
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    recebido = true;
                } else {
                    block();
                }
            }

            @Override
            public boolean done() {
                return recebido;
            }
        }, AGUARDAR_INSIGHTS);

        // 5) PedirEnriquecer
        fsm.registerState(new OneShotBehaviour() {
            @Override
            public void action() {
                ACLMessage req = new ACLMessage(ACLMessage.REQUEST);
                req.addReceiver(enriquecedorAID);
                req.setOntology("EnriquecerRequest");
                req.setLanguage(new SLCodec().getName());
                req.setContent("BUSCAR_DADOS_EXTERNOS");
                send(req);
                System.out.println("Coordenador: enviei REQUEST para enriquecedor → \"BUSCAR_DADOS_EXTERNOS\"");
            }
        }, PEDIR_ENRIQUECER);

        // 6) AguardarEnriquecer
        fsm.registerState(new SimpleBehaviour() {
            private boolean recebido = false;

            @Override
            public void action() {
                MessageTemplate mtPerfOnt = MessageTemplate.and(
                        MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                        MessageTemplate.MatchOntology(DashboardOntology.getInstance().getName())
                );
                MessageTemplate mt = MessageTemplate.and(
                        mtPerfOnt,
                        MessageTemplate.MatchLanguage(new SLCodec().getName())
                );
                ACLMessage msg = receive(mt);
                if (msg != null) {
                    System.out.println("Coordenador recebeu do Enriquecedor: " + msg.getContent());
                    recebido = true;
                } else {
                    block();
                }
            }

            @Override
            public boolean done() {
                return recebido;
            }
        }, AGUARDAR_ENRIQUECER);

        // 7) Estado FINAL
        fsm.registerLastState(new OneShotBehaviour() {
            @Override
            public void action() {
                System.out.println("Coordenador: fluxo concluído. Encerrando agente.");
            }
        }, FINAL);

        // Transições padrão
        fsm.registerDefaultTransition(PEDIR_MONITOR, AGUARDAR_MONITOR);
        fsm.registerDefaultTransition(AGUARDAR_MONITOR, PEDIR_INSIGHTS);
        fsm.registerDefaultTransition(PEDIR_INSIGHTS, AGUARDAR_INSIGHTS);
        fsm.registerDefaultTransition(AGUARDAR_INSIGHTS, PEDIR_ENRIQUECER);
        fsm.registerDefaultTransition(PEDIR_ENRIQUECER, AGUARDAR_ENRIQUECER);
        fsm.registerDefaultTransition(AGUARDAR_ENRIQUECER, FINAL);

        return fsm;
    }
}
