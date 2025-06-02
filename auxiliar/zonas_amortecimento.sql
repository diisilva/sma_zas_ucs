drop table zonas_amortecimento

CREATE TABLE zonas_amortecimento (
  ANO                          INTEGER,
  nome_uc                      VARCHAR(255),
  qtde_polygons_prodes         INTEGER,
  sum_area_prodes_ha           DOUBLE PRECISION,
  qtde_polygons_mapbiomas_alertas INTEGER,
  sum_area_mapbiomas_alertas_ha DOUBLE PRECISION,
  qtde_firms_modis             INTEGER,
  qtde_firms_j1_viirs          INTEGER,
  qtde_firms_suomi_viirs       INTEGER,
  sum_area_total_ha            DOUBLE PRECISION,
  cluster                      INTEGER,
  risk_level                   DOUBLE PRECISION,
  risk_category                VARCHAR(50),
  processado                   BOOLEAN DEFAULT FALSE    -- para o Agente MonitorDeDados
);

ALTER TABLE zonas_amortecimento
ADD COLUMN id SERIAL;

-- 2. Define a coluna id como PRIMARY KEY (se ainda não estiver definida)
ALTER TABLE zonas_amortecimento
ADD CONSTRAINT zonas_amortecimento_pk PRIMARY KEY (id);


INSERT INTO public.zonas_amortecimento (
  ANO, nome_uc, qtde_polygons_prodes, sum_area_prodes_ha,
  qtde_polygons_mapbiomas_alertas, sum_area_mapbiomas_alertas_ha,
  qtde_firms_modis, qtde_firms_j1_viirs, qtde_firms_suomi_viirs,
  sum_area_total_ha, cluster, risk_level, risk_category, processado
) VALUES
  (2022, 'UC Santa Maria', 5, 1500.0, 3, 800.0, 2, 1, 0, 2300.0, 1, 0.25, 'Médio', FALSE),
  (2023, 'UC São Jorge', 2, 500.0, 1, 200.0, 0, 0, 1, 700.0, 2, 0.70, 'Alto', FALSE),
  (2004, 'ESTAÇÃO ECOLÓGICA DA MATA PRETA', 10, 1606.83, 5, 900.0, 1, 1, 0, 2506.83, 3, 1.00, 'Médio', FALSE);

