package com.segc;

import java.io.*;
import java.security.*;
import java.security.cert.Certificate;

/**
 * A class that handles encryption and decryption.
 *
 * @author fc54685 Francisco Correia
 * @author fc55955 Alexandre Fonseca
 * @author fc56272 Filipe Egipto
 */
public class CipherService {

    private final KeyStore ks;

    public CipherService(File keyStoreFile, char[] keyStorePassword, String keyStoreFormat) {
        ks = initKeyStore(keyStoreFile, keyStorePassword, keyStoreFormat);
    }

    public Key getKey(String alias, char[] password) throws UnrecoverableKeyException {
        try {
            return ks.getKey(alias, password);
        } catch (KeyStoreException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public Certificate getCertificate(String alias) {
        try {
            return ks.getCertificate(alias);
        } catch (KeyStoreException e) {
            throw new RuntimeException(e);
        }
    }

    private KeyStore initKeyStore(File keyStoreFile, char[] keyStorePassword, String keyStoreFormat) {
        try (FileInputStream kfile = new FileInputStream(keyStoreFile)) {
            KeyStore ks = KeyStore.getInstance(keyStoreFormat);
            ks.load(kfile, keyStorePassword);
            return ks;
        } catch (FileNotFoundException e) {
            throw new RuntimeException("You must generate a keystore before running this application.", e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
