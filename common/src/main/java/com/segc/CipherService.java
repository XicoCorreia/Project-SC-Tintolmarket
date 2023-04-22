package com.segc;

import java.io.*;
import java.security.KeyStore;

public class CipherService {
    private static final Configuration config = Configuration.getInstance();
    private static CipherService instance;
    private static KeyStore ks;

    private CipherService() {
        instance = new CipherService();

        ks = initKeyStore(config.getValue("keyStoreFile"),
                config.getValue("keyStorePassword"),
                config.getValue("keyStoreFormat"));
    }

    public static CipherService getInstance() {
        return instance;
    }

    private KeyStore initKeyStore(String keyStoreFile, String keyStorePassword, String keyStoreFormat) {
        try (FileInputStream kfile = new FileInputStream(keyStoreFile)) {
            KeyStore ks = KeyStore.getInstance(keyStoreFormat);
            ks.load(kfile, keyStorePassword.toCharArray());
            return ks;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
