#!/bin/sh
CLIENT_KS_PASSWORD=${CLIENT_KS_PASSWORD:-client123}
TS_PASSWORD=${TS_PASSWORD:-trustpass}
SERVER_KS_PASSWORD=${SERVER_KS_PASSWORD:-server123}
SERVER_ALIAS=${SERVER_ALIAS:-server}
OUTPUT_DIR=${OUTPUT_DIR:-keytool_data}

set 'ana' 'joao' 'maria' 'pedro' 'ines' 'tiago'

# 1. Criar diretórios para os dados do cliente e do servidor.
mkdir -p "$OUTPUT_DIR/server"
for i do
    mkdir -p "$OUTPUT_DIR/client/$i"
done

# 2. Criar o par de chaves RSA de 2048 bits e o certificado auto-assinado do servidor.
keytool -genkeypair \
    -alias "$SERVER_ALIAS" \
    -dname "CN=server, OU=TintolmarketServer, OU=Grupo005, O=SegC, L=Lisbon, S=Lisbon, C=PT" \
    -keyalg "RSA" \
    -keysize "2048" \
    -storetype "PKCS12" \
    -keystore "$OUTPUT_DIR/server/keystore.p12" \
    -storepass "$SERVER_KS_PASSWORD"

# 3. Exportar o certificado do servidor para um ficheiro de format cer.
keytool -exportcert \
    -alias "$SERVER_ALIAS" \
    -keystore "$OUTPUT_DIR/server/keystore.p12" \
    -file "$OUTPUT_DIR/server/server.cer" \
    -storepass "$SERVER_KS_PASSWORD"

# 4. Criar uma truststore comum para todos os clientes com o certificado do servidor
keytool -importcert \
    -alias "$SERVER_ALIAS" \
    -file "$OUTPUT_DIR/server/server.cer" \
    -keystore "$OUTPUT_DIR/client/truststore.p12" \
    -storepass "$TS_PASSWORD" \
    -noprompt

# 5. Criar keystores para os vários clientes e acrescentar certificados à truststore comum.
for i do
    # 5.1. Criar keystore do cliente.
    keytool -genkeypair \
        -alias "$i" \
        -dname "CN=client-$i, OU=Tintolmarket, OU=Grupo005, O=SegC, L=Lisbon, S=Lisbon, C=PT" \
        -keyalg "RSA" \
        -keysize "2048" \
        -storetype "PKCS12" \
        -keystore "$OUTPUT_DIR/client/$i/keystore.p12" \
        -storepass "$CLIENT_KS_PASSWORD"

    # 5.2. Exportar o certificado do cliente para um ficheiro de format cer.
    keytool -exportcert \
        -alias "$i" \
        -keystore "$OUTPUT_DIR/client/$i/keystore.p12" \
        -file "$OUTPUT_DIR/client/$i/client.cer" \
        -storepass "$CLIENT_KS_PASSWORD"

    # 5.3. Importar o certificado do cliente para a truststore comum
    keytool -importcert \
        -alias "$i" \
        -file "$OUTPUT_DIR/client/$i/client.cer" \
        -keystore "$OUTPUT_DIR/client/truststore.p12" \
        -storepass "$TS_PASSWORD" \
        -noprompt
done
