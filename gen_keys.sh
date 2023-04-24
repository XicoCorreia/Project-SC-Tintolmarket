#!/bin/sh
CLIENT_KS_PASSWORD=${CLIENT_KS_PASSWORD:-client123}
CLIENT_TS_PASSWORD=${CLIENT_TS_PASSWORD:-trustpass}
SERVER_KS_PASSWORD=${SERVER_KS_PASSWORD:-server123}
CLIENT_ALIAS_PREFIX=${CLIENT_ALIAS_PREFIX:-client}
SERVER_ALIAS=${SERVER_ALIAS:-server}

set 'ana' 'joao' 'maria' 'pedro' 'ines' 'tiago'

# 1. Criar diretórios para os dados do cliente e do servidor.
mkdir -p server_data
for i do
    mkdir -p "client_data/$i"
done

# 2. Criar o par de chaves RSA de 2048 bits e o certificado auto-assinado do servidor.
keytool -genkeypair \
    -alias "$SERVER_ALIAS" \
    -dname "CN=server, OU=Tintolmarket, OU=Grupo005, O=SegC, L=Lisbon, S=Lisbon, C=PT" \
    -keyalg "RSA" \
    -keysize "2048" \
    -storetype "PKCS12" \
    -keystore "server_data/keystore.p12" \
    -storepass "$SERVER_KS_PASSWORD"

# 3. Exportar o certificado do servidor para um ficheiro de format cer.
keytool -exportcert \
    -alias "$SERVER_ALIAS" \
    -keystore "server_data/keystore.p12" \
    -file "server_data/server.cer" \
    -storepass "$SERVER_KS_PASSWORD"

# 4. Criar keystores e truststores para os vários clientes.
for i do
    # 4.1. Criar keystore do cliente.
    keytool -genkeypair \
        -alias "$CLIENT_ALIAS_PREFIX" \
        -dname "CN=client-$i, OU=Tintolmarket, OU=Grupo005, O=SegC, L=Lisbon, S=Lisbon, C=PT" \
        -keyalg "RSA" \
        -keysize "2048" \
        -storetype "PKCS12" \
        -keystore "client_data/$i/keystore.p12" \
        -storepass "$CLIENT_KS_PASSWORD"

    # 4.2. Exportar o certificado do cliente para um ficheiro de format cer.
    keytool -exportcert \
        -alias "$CLIENT_ALIAS_PREFIX" \
        -keystore "client_data/$i/keystore.p12" \
        -file "client_data/$i/client.cer" \
        -storepass "$CLIENT_KS_PASSWORD"

    # 4.3. Criar truststore do cliente com o certificado do servidor.
    keytool -importcert \
        -alias "$SERVER_ALIAS" \
        -file "server_data/server.cer" \
        -keystore "client_data/$i/truststore.p12" \
        -storepass "$CLIENT_TS_PASSWORD" \
        -noprompt
done

# 5. Partilhar certificados entre as truststores dos clientes.
for i do
    for j do
        if [ "$i" = "$j" ]; then
            continue
        fi
        keytool -importcert \
            -alias "$CLIENT_ALIAS_PREFIX-$j" \
            -file "client_data/$j/client.cer" \
            -keystore "client_data/$i/truststore.p12" \
            -storepass "$CLIENT_TS_PASSWORD" \
            -noprompt
    done
done
