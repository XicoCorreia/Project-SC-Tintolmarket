package com.segc.services;

import javax.crypto.*;
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
    private final String defaultAlias;
    private final PrivateKey defaultPrivateKey;
    private final Certificate defaultCertificate;
    private final Signature signature;

    /**
     * Creates a {@link CipherService} with the given key store file, password and format.
     *
     * @param signatureEngine the signature engine to use.
     * @param defaultAlias    the default alias to use.
     */
    public CipherService(String defaultAlias, String signatureEngine) {

        String keyStore = System.getProperty("javax.net.ssl.keyStore");
        char[] keyStorePassword = System.getProperty("javax.net.ssl.keyStorePassword").toCharArray();
        String keyStoreType = System.getProperty("javax.net.ssl.keyStoreType");

        ks = initKeyStore(keyStore, keyStorePassword, keyStoreType);
        try {
            this.defaultAlias = defaultAlias;
            this.defaultPrivateKey = getPrivateKey(defaultAlias, keyStorePassword);
            this.defaultCertificate = getCertificate(defaultAlias);
            this.signature = Signature.getInstance(signatureEngine);
        } catch (UnrecoverableKeyException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the default certificate (i.e., that which is identified by the default alias).
     *
     * @return the certificate identified by the default alias.
     */
    public Certificate getCertificate() {
        return defaultCertificate;
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
     * Returns the {@link PrivateKey} identified by the given alias and password.
     *
     * @param alias    the alias of the private key to return.
     * @param password the password to recover the key with.
     * @return the private key.
     * @throws UnrecoverableKeyException if the key cannot be recovered.
     */
    public PrivateKey getPrivateKey(String alias, char[] password) throws UnrecoverableKeyException {
        try {
            return (PrivateKey) ks.getKey(alias, password);
        } catch (KeyStoreException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Signs the given object using the default private key.
     *
     * @param obj the object to sign (encrypt).
     * @return the signed (encrypted) object.
     */
    public SignedObject sign(Serializable obj) {
        return sign(obj, defaultPrivateKey);
    }

    /**
     * Signs the given object using the given private key.
     *
     * @param obj the object to sign (encrypt).
     * @return the signed (encrypted) object.
     */
    public SignedObject sign(Serializable obj, PrivateKey privateKey) {
        try {
            return new SignedObject(obj, privateKey, signature);
        } catch (IOException | InvalidKeyException | SignatureException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Verifies the given {@link SignedObject} using the {@link PublicKey} from the default {@link Certificate}.
     *
     * @param obj the object to verify.
     * @return the decrypted data.
     */
    public boolean verify(SignedObject obj) throws SignatureException, InvalidKeyException {
        return verify(obj, defaultCertificate);
    }

    /**
     * Verifies the given {@link SignedObject} using the {@link PublicKey} from the given {@link Certificate}.
     *
     * @param obj the object to verify.
     * @return the decrypted data.
     */
    public boolean verify(SignedObject obj, Certificate certificate) throws SignatureException, InvalidKeyException {
        return obj.verify(certificate.getPublicKey(), signature);
    }

    /**
     * Decrypts the given data using the {@link PrivateKey} identified by the given alias and password.
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
        PrivateKey key = getPrivateKey(alias, password);
        byte[] result = decrypt(data, key);
        key.destroy();
        return result;
    }

    /**
     * Encrypts the given data using the {@link PublicKey} from the default {@link Certificate}.
     *
     * @param data the data to encrypt.
     * @return the encrypted data.
     */
    public byte[] encrypt(byte[] data) {
        return encrypt(data, defaultAlias);
    }

    /**
     * Encrypts the given data using the {@link PublicKey} from the {@link Certificate} identified by the given alias.
     *
     * @param data  the data to encrypt.
     * @param alias the alias of the key to use.
     * @return the encrypt data.
     */
    public byte[] encrypt(byte[] data, String alias) {
        PublicKey key = getCertificate(alias).getPublicKey();
        return encrypt(data, key);
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
    
    public byte[] decrypt(byte[] data) {
        return decrypt(data, defaultAlias);
    }
    
    public byte[] decrypt(byte[] data, String alias) {
        PublicKey key = getCertificate(alias).getPublicKey();
        return decrypt(data, key);
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

    public void decrypt(FileInputStream fis, FileOutputStream fos, Key key, AlgorithmParameters params) {
        try {
            Cipher cipher = Cipher.getInstance(key.getAlgorithm());
            cipher.init(Cipher.DECRYPT_MODE, key, params);
            cipher(fis, fos, cipher);
        } catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException |
                 InvalidAlgorithmParameterException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void encrypt(FileInputStream fis, FileOutputStream fos, Key key, AlgorithmParameters params) {
        try {
            Cipher cipher = Cipher.getInstance(key.getAlgorithm());
            cipher.init(Cipher.ENCRYPT_MODE, key, params);
            cipher(fis, fos, cipher);
        } catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException |
                 InvalidAlgorithmParameterException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void cipher(FileInputStream fis, FileOutputStream fos, Cipher cipher) throws IOException {
        CipherInputStream cis = new CipherInputStream(fis, cipher);
        byte[] b = new byte[16];
        int i = cis.read(b);
        while (i != -1) {
            fos.write(b, 0, i);
            i = cis.read(b);
        }
        cis.close();
        fis.close();
        fos.close();
    }

    private KeyStore initKeyStore(String keyStore, char[] keyStorePassword, String keyStoreType) {
        try (FileInputStream kfile = new FileInputStream(keyStore)) {
            KeyStore ks = KeyStore.getInstance(keyStoreType);
            ks.load(kfile, keyStorePassword);
            return ks;
        } catch (FileNotFoundException e) {
            throw new RuntimeException("You must generate a keystore before running this application.", e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
