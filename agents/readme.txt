para instalar o jade 

INSTALAÇÃO (Integração com a IDE NetBeans)
• Vamos integrar as bibliotecas da plataforma JADE com a IDE Netbeans, para que
seja possível o desenvolvimento de agentes contando com as ferramentas da IDE.
• Realize o download do JADE e descompacte (marque o endereço da pasta onde
foram descompactados os arquivos do JADE, para continuação do roteiro vamos
utilizar o diretório raiz como referência C:\) As seguintes pastas devem ser exibidas:
o JADE-bin-x.x.x (x será referente a versão do JADE)
o JADE-doc-x.x.x (x será referente a versão do JADE)
o JADE-examples-x.x.x (x será referente a versão do JADE)
o JADE-src-x.x.x (x será referente a versão do JADE)
1-Crie um novo Projeto no NetBeans (Projeto Java With Ant->Java Application)
2-No NetBeans abra o menu Ferramentas (Tools) e clique na opção Bibliotecas (Library
Manager). Uma janela será aberta.
3- Na janela Gerenciador de Bibliotecas clique no botão Nova Biblioteca (new library).
4 - Uma janela de diálogo será aberta. No campo Nome da Biblioteca digite jade e deixe
marcado o campo Tipo de Biblioteca como Bibliotecas da Classe. E clique em ok. A janela
de Gerenciador de Bibliotecas agora apresentará a nova biblioteca adicionada.
5 - Nesta mesma janela, na guia Classpath clique no botão Adicionar JAR/Pasta e adicione
o arquivo (localizado em C:/JADE/JADE-bin-x.x.x/lib/): jade.jar e o arquivo (localizado em
C:/JADE/JADE-src-x.x.x\jade\lib\commons-codec/): commons-codec-1.3.jar. Clique em ok
e as bibliotecas JADE estarão integradas ao Netbeans.
6 - Quando estiver desenvolvendo um novo projeto de Sistema Multiagentes no NetBeans,
vá na guia Projetos e selecione o item Bibliotecas. Com o botão direito do mouse selecione
a opção Adicionar Biblioteca. Será aberta a janela Adicionar Biblioteca.
7 - Nesta, selecione a biblioteca JADE e clique em OK. Com isto, o NetBeans passará a
reconhecer os métodos e atributos fornecidos pela plataforma

java jade.Boot -gui -container Alarmado2:trocamensagens2.AgenteAlarmado2;