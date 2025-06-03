# Projeto SMA\_ZAS\_UCS

Este projeto demonstra o uso do framework JADE (Java Agent DEvelopment Framework) para construção e teste de agentes inteligentes em Java.

## Pré-requisitos

* **Java 22** (JDK e JRE)

  ```powershell
  PS C:\sma_zas_ucs> javac --version
  javac 22
  PS C:\sma_zas_ucs> java --version
  java 22 2024-03-19
  Java(TM) SE Runtime Environment (build 22+36-2370)
  Java HotSpot(TM) 64-Bit Server VM (build 22+36-2370, mixed mode, sharing)
  ```
* **JADE 4.6.0** instalado em `C:\JADE-all-4.6.0`
* Windows PowerShell (ou terminal compatível)

## Estrutura do Projeto

```
C:\sma_zas_ucs
├── .gitignore           # Arquivos e pastas ignoradas pelo Git
├── README.md            # Este arquivo de documentação
├── libs
│   └── jade.jar         # Biblioteca do JADE (não versionar alterações)
├── agents               # Código-fonte dos agentes e behaviours
│   ├── CoordenadorAgent.java
│   ├── InsightsAgent.java
│   ├── EnriquecedorWebAgent.java
│   ├── ...              # Outros behaviours e ontologia
│   └── testing          # Testes de agentes
│       ├── OntologyTestAgent.java
│       ├── PingAgent.java
│       ├── PongAgent.java
│       └── ...
├── bin                  # Classes compiladas (gerado pelo javac)
└── out                  # Saídas da IDE IntelliJ (não versionar)
```

## .gitignore

Define padrões de arquivos/pastas que não devem ser versionados:

```gitignore
/bin/
/out/
*.class
/.idea/
/*.iml
/libs/*.jar
Thumbs.db
.DS_Store
*.log
*~
/target/
/build/
```

## Como Buildar

1. Abra o terminal na raiz do projeto:

   ```powershell
   PS C:\sma_zas_ucs>
   ```
2. Crie (se não existir) o diretório de saída das classes:

   ```powershell
   PS C:\sma_zas_ucs> mkdir bin
   ```
3. Compile os agentes principais:

   ```powershell
   PS C:\sma_zas_ucs>  javac -encoding UTF-8 -cp ".;libs\\jade.jar;libs\\postgresql-42.5.6.jar" -d bin agents\\*.java
   ```
4. Compile os testes de agentes:

   ```powershell
   PS C:\sma_zas_ucs> javac -cp ".;libs\jade.jar" -d bin agents\testing\*.java
   ```

## Como Executar

### Agentes Principais

Roda o container JADE com Coordenador, Insights e Enriquecedor:

```powershell
 java -cp ".;libs\jade.jar;libs\postgresql-42.5.6.jar;bin" jade.Boot -gui "coord:agents.CoordenadorAgent(full);mon:agents.MonitorDeDadosAgent;ins:agents.InsightsAgent;enr:agents.EnriquecedorWebAgent"     

```

### Testes Automatizados

1. **Teste de Ontologia** (fill/extract)

   ```powershell
   PS C:\sma_zas_ucs> java -cp ".;libs\jade.jar;bin" jade.Boot -gui testOnto:agents.testing.OntologyTestAgent
   ```
2. **Teste Ping-Pong** (comunicação básica)

   ```powershell
   PS C:\sma_zas_ucs> java -cp ".;libs\jade.jar;bin" jade.Boot -gui "ping:agents.testing.PingAgent;pong:agents.testing.PongAgent"
   ```
3. **Teste de Integração** (fluxo completo)

   ```powershell
   PS C:\sma_zas_ucs> java -cp ".;libs\jade.jar;bin" jade.Boot -gui \
     coord:agents.CoordenadorAgent \
     ins:agents.InsightsAgent \
     enr:agents.EnriquecedorWebAgent
   ```

## Descrição dos Diretórios e Arquivos

* **agents/**: código-fonte dos agentes JADE e behaviours. Contém também a ontologia (`DashboardOntology.java`).
* **agents/testing/**: classes de teste para validar ontologia, comunicação básica e fluxo integrado.
* **libs/**: bibliotecas externas necessárias (neste caso, `jade.jar`).
* **bin/**: pasta de saída das classes compiladas. Usada no classpath de execução.
* **out/**: saída gerada pela IDE IntelliJ (não versionar).
* **.idea/**: configurações de projeto do IntelliJ (não versionar).
* **.gitignore**: define o que deve ser ignorado pelo Git.
* **README.md**: documentação do projeto.

## Suporte e Contribuições

Sugestões e correções são bem-vindas! Abra issues ou envie pull requests para incorporar melhorias.

---

*Desenvolvido por Diego Silva | Edudardo Oliveira*
