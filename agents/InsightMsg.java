package agents;

import jade.content.Concept;

/**
 * Bean que implementa Concept para enviar dados de insights via ontologia.
 */
public class InsightMsg implements Concept {
    private int total_linhas;
    private float media_risco;
    private String uc_maior_risco;
    private float maior_risco;

    public InsightMsg() {
        // Construtor vazio (JADE exige)
    }

    public int getTotal_linhas() {
        return total_linhas;
    }
    public void setTotal_linhas(int total_linhas) {
        this.total_linhas = total_linhas;
    }

    public float getMedia_risco() {
        return media_risco;
    }
    public void setMedia_risco(float media_risco) {
        this.media_risco = media_risco;
    }

    public String getUc_maior_risco() {
        return uc_maior_risco;
    }
    public void setUc_maior_risco(String uc_maior_risco) {
        this.uc_maior_risco = uc_maior_risco;
    }

    public float getMaior_risco() {
        return maior_risco;
    }
    public void setMaior_risco(float maior_risco) {
        this.maior_risco = maior_risco;
    }
}
