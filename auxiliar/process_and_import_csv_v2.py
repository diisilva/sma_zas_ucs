#!/usr/bin/env python3
# -*- coding: utf-8 -*-
#
# process_and_import_csv.py
#
# Este script faz o seguinte:
# 1. Lê o CSV original (com ponto‐e‐vírgula como delimitador e vírgulas em decimais).
# 2. Adiciona a coluna “processado” no final e substitui vírgulas por pontos (para decimais).
# 3. Gera um arquivo processado no host.
# 4. Copia esse arquivo para dentro do container Docker (Windows).
# 5. Executa o comando psql dentro do container para importar no PostgreSQL.
#
# Uso:
#   python process_and_import_csv.py
#
# Ajuste, se necessário:
#   - input_path: caminho do CSV original no host Windows.
#   - output_host_path: onde será salvo o CSV processado no host Windows.
#   - container_name: nome do container Docker em execução.
#   - output_container_path: caminho dentro do container onde ficará o CSV processado (Unix style).
#   - tabela_target: esquema.tabela de destino no Postgres dentro do container.
#

import os
import subprocess
import sys

# --------------------------- CONFIGURAÇÕES ---------------------------

# Use raw strings (r"...") para evitar problemas com barras invertidas no Windows :contentReference[oaicite:2]{index=2}

# Caminho completo para o CSV original (host Windows)
input_path = r"C:\sma_zas_ucs\auxiliar\cluster_risk_by_year_buffer.csv"

# Onde o script irá salvar o CSV processado (host Windows)
output_host_path = r"C:\sma_zas_ucs\auxiliar\cluster_processed.csv"

# Nome do container Docker que já deve estar em execução
container_name = "db_test_dev_diego"

# Caminho (dentro do container) onde o arquivo processado será copiado
# Observe que dentro do container usa-se "/" (Linux)
output_container_path = "/cluster_processed.csv"

# Tabela de destino no PostgreSQL (esquema.tabela) dentro do container
tabela_target = "public.zonas_amortecimento"

# ---------------------------------------------------------------------

def main():
    # 1. Verifica existência do arquivo de entrada
    if not os.path.isfile(input_path):
        print(f"ERRO: arquivo de entrada não encontrado:\n  {input_path}")
        sys.exit(1)

    # 2. Abre o CSV original e cria o CSV processado
    try:
        with open(input_path, "r", encoding="utf-8") as fin, \
             open(output_host_path, "w", encoding="utf-8", newline="") as fout:

            primeira_linha = True
            for linha in fin:
                # Remove quebras de linha (LF ou CRLF)
                linha = linha.rstrip("\r\n")
                # Troca vírgulas por pontos (para decimais)
                linha = linha.replace(",", ".")

                if primeira_linha:
                    # Adiciona o cabeçalho "processado" ao final
                    fout.write(f"{linha};processado\n")
                    primeira_linha = False
                else:
                    # Para cada registro, adiciona um campo vazio em "processado"
                    fout.write(f"{linha};\n")

    except Exception as e:
        print("ERRO ao processar o CSV:", e)
        sys.exit(1)

    print(f"✔ CSV processado gerado em:\n    {output_host_path}")

    # 3. Copia o arquivo processado para dentro do container (Windows -> Container Linux)
    try:
        # No Windows, docker cp aceita caminhos com barras invertidas, mas o destino dentro
        # do container deve usar "/".
        subprocess.run(
            [
                "docker", "cp",
                output_host_path,
                f"{container_name}:{output_container_path}"
            ],
            check=True
        )
        print(f"✔ Arquivo copiado para o container:\n    {container_name}:{output_container_path}")
    except subprocess.CalledProcessError as e:
        print("ERRO ao executar 'docker cp':", e)
        sys.exit(1)

    # 4. Executa o comando psql dentro do container para importar o CSV
    #    Observe que o caminho interno é Unix style ("/cluster_processed.csv")
    copy_command = (
        f"\\copy {tabela_target} "
        f"FROM '{output_container_path}' "
        "(FORMAT csv, DELIMITER ';', HEADER true);"
    )

    try:
        subprocess.run(
            [
                "docker", "exec",
                container_name,
                "psql",
                "-U", "admin",
                "-d", "db_test_dev_diego",
                "-c", copy_command
            ],
            check=True
        )
        print("✔ Importação concluída no PostgreSQL dentro do container")
    except subprocess.CalledProcessError as e:
        print("ERRO ao executar o import no Postgres:", e)
        sys.exit(1)

if __name__ == "__main__":
    main()
