# Tintolmarket (Segurança e Confiabilidade 2022/2023)

## Autores

- Francisco Correia (fc54685)
- Alexandre Fonseca (fc55955)
- Filipe Egipto (fc56272)

## Compilação

### Eclipse IDE

1. Abrir o menu de importação de projeto: `File > Import...`.
2. Importar um projeto de Gradle: `Gradle > Existing Gradle Project`.
3. Selecionar o diretório obtido após extrair o arquivo e terminar.
4. Na barra de ferramentas, selecionar `Window > Show View > Other...`.
5. Selecionar `Gradle Tasks` e carregar OK.
6. Na janela de tarefas de Gradle, compilar os ficheiros `.jar`:
    1. Cliente: `client > build > jar` produz o ficheiro `...tintolmarket/client/build/Tintolmarket.jar`.
    2. Servidor: `server > build > jar` produz o ficheiro `...tintolmarket/server/build/TintolmarketServer.jar`.

### Linha de comandos

1. Na raíz do diretório do projeto, correr `./gradlew jar` para compilar os ficheiros `.jar`:
    1. Cliente: disponível em `...tintolmarket/client/build/Tintolmarket.jar`.
    2. Servidor: disponível em `...tintolmarket/server/build/TintolmarketServer.jar`.

### Criação de truststores e keystores

Ao correr o script `gen_keys.sh` é criada a informação de 6 utilizadores. São criadas:

- uma truststore comum para todos os clientes
- uma keystore para o servidor
- uma keystore para cada cliente
