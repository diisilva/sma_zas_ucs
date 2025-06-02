package agents;

import jade.content.Concept;

public class Zona implements Concept {
    private int id;
    private int ano;
    private String nome_uc;
    private float sum_area_total_ha;
    private float risk_level;
    private String risk_category;

    // getters e setters

    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

    public int getAno() {
        return ano;
    }
    public void setAno(int ano) {
        this.ano = ano;
    }

    public String getNome_uc() {
        return nome_uc;
    }
    public void setNome_uc(String nome_uc) {
        this.nome_uc = nome_uc;
    }

    public float getSum_area_total_ha() {
        return sum_area_total_ha;
    }
    public void setSum_area_total_ha(float sum_area_total_ha) {
        this.sum_area_total_ha = sum_area_total_ha;
    }

    public float getRisk_level() {
        return risk_level;
    }
    public void setRisk_level(float risk_level) {
        this.risk_level = risk_level;
    }

    public String getRisk_category() {
        return risk_category;
    }
    public void setRisk_category(String risk_category) {
        this.risk_category = risk_category;
    }
}
