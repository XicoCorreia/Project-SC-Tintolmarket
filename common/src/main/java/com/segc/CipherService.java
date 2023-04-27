package com.segc;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.security.auth.DestroyFailedException;
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

    /**
     * Creates a {@link CipherService} with the given key store file, password and format.
     *
     * @param keyStoreFile     the file containing the key store.
     * @param keyStorePassword the password for the key store.
     * @param keyStoreFormat   the format of the key store.
     */
    public CipherService(File keyStoreFile, char[] keyStorePassword, String keyStoreFormat) {
        ks = initKeyStore(keyStoreFile, keyStorePassword, keyStoreFormat);
    }

    /**
     * Returns the certificate identified by the given alias.
     *
     * @param alias the alias of the certificate to return.
     * @return the certificate identified by the given alias.
     */
    public Certificate getCertificate(String alias) {
        try {
            return ks.getCertificate(alias);
        } catch (KeyStoreException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Encrypts the given data using the private key identified by the given alias and password.
     *
     * @param data     the data to encrypt.
     * @param alias    the alias of the key to use.
     * @param password the password of the key to use.
     * @return the encrypted data.
     * @throws UnrecoverableKeyException if the key cannot be recovered.
     * @throws DestroyFailedException    if the key cannot be destroyed.
     */
    public byte[] encrypt(byte[] data, String alias, char[] password)
            throws UnrecoverableKeyException, DestroyFailedException {
        PrivateKey key = (PrivateKey) getKey(alias, password);
        byte[] result = encrypt(data, key);
        key.destroy();
        return result;
    }

    /**
     * Decrypts the given data using the private key identified by the given alias and password.
     *
     * @param data     the data to decrypt.
     * @param alias    the alias of the key to use.
     * @param password the password of the key to use.
     * @return the decrypted data.
     * @throws UnrecoverableKeyException if the key cannot be recovered.
     * @throws DestroyFailedException    if the key cannot be destroyed.
     */
    public byte[] decrypt(byte[] data, String alias, char[] password)
            throws UnrecoverableKeyException, DestroyFailedException {
        PrivateKey key = (PrivateKey) getKey(alias, password);
        byte[] result = decrypt(data, key);
        key.destroy();
        return result;
    }

    /**
     * Encrypts the given data using the public key from the certificated identified by the given alias.
     *
     * @param data  the data to encrypt.
     * @param alias the alias of the key to use.
     * @return the encrypt data.
     */
    public byte[] encrypt(byte[] data, String alias) {
        PublicKey key = getCertificate(alias).getPublicKey();
        return encrypt(data, key);
    }

    /**
     * Decrypts the given data using the public key from the certificated identified by the given alias.
     *
     * @param data  the data to decrypt.
     * @param alias the alias of the key to use.
     * @return the decrypted data.
     */
    public byte[] decrypt(byte[] data, String alias) {
        PublicKey key = getCertificate(alias).getPublicKey();
        return decrypt(data, key);
    }

    private byte[] encrypt(byte[] data, Key key) {
        try {
            Cipher cipher = Cipher.getInstance(key.getAlgorithm());
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return cipher.doFinal(data);
        } catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException | IllegalBlockSizeException |
                 BadPaddingException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] decrypt(byte[] data, Key key) {
        try {
            Cipher cipher = Cipher.getInstance(key.getAlgorithm());
            cipher.init(Cipher.DECRYPT_MODE, key);
            return cipher.doFinal(data);
        } catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException | IllegalBlockSizeException |
                 BadPaddingException e) {
            throw new RuntimeException(e);
        }
    }

    public Key getKey(String alias, char[] password) throws UnrecoverableKeyException {
        try {
            return ks.getKey(alias, password);
        } catch (KeyStoreException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private KeyStore initKeyStore(File keyStoreFile, char[] keyStorePassword, String keyStoreFormat) {
        try (FileInputStream kfile = new FileInputStream(keyStoreFile)) {
            KeyStore ks = KeyStore.getInstance(keyStoreFormat);
            ks.load(kfile, keyStorePassword);
            return ks;
        } catch (FileNotFoundException e) {
            System.out.println(keyStoreFile.getAbsolutePath());
            throw new RuntimeException("You must generate a keystore before running this application.", e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
