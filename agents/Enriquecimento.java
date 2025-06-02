package agents;

import jade.content.Concept;

/**
 * Representa um objeto “Enriquecimento” para enviar via JADE ACL.
 *
 * Para que getContentManager().fillContent(msg, e) compile,
 * esta classe implementa jade.content.Concept.
 */
public class Enriquecimento implements Concept {
    private String fonte;
    private String timestamp;
    private String detalhes;

    public Enriquecimento() {
        // Construtor vazio (JADE exige)
    }

    public String getFonte() {
        return fonte;
    }
    public void setFonte(String fonte) {
        this.fonte = fonte;
    }

    public String getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getDetalhes() {
        return detalhes;
    }
    public void setDetalhes(String detalhes) {
        this.detalhes = detalhes;
    }
}
