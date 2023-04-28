package com.segc.services;

import com.segc.Configuration;
import com.segc.exception.DuplicateElementException;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.AlgorithmParameters;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.NoSuchElementException;

/**
 * A class that handles authentication.
 *
 * @author fc54685 Francisco Correia
 * @author fc55955 Alexandre Fonseca
 * @author fc56272 Filipe Egipto
 */
public class AuthenticationService {
    private static final Configuration config = Configuration.getInstance();
    private final CipherService cipherService;
    private final File userCredentials;
    private final char[] password;
    private final String userCredentialsAlgorithm;

    public AuthenticationService(File userCredentials, char[] password, CipherService cipherService) {
        this(userCredentials,
                password,
                cipherService,
                config.getValue("userCredentialsPBEAlgorithm"));
    }

    public AuthenticationService(File userCredentials, char[] password, CipherService cipherService,
                                 String userCredentialsPBEAlgorithm) {
        this.cipherService = cipherService;
        this.password = password;
        this.userCredentials = userCredentials;
        this.userCredentialsAlgorithm = userCredentialsPBEAlgorithm;
        try {
            if (userCredentials.getParentFile().mkdirs() || userCredentials.createNewFile()) {
                System.out.println(getClass().getSimpleName() + ": created empty user credentials file.");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean authenticateUser(String clientId, String password) throws NoSuchElementException {
        return getUserCredentials(clientId).equals(clientId + ":" + password);
    }

    private String getUserCredentials(String clientId) throws NoSuchElementException {
        String pattern = clientId + ":";
        synchronized (userCredentials) {
            File decryptedUserCredentials = decrypt();
            try (BufferedReader usersReader = new BufferedReader(new FileReader(decryptedUserCredentials))) {
                return usersReader.lines()
                                  .dropWhile(line -> !line.startsWith(pattern))
                                  .findFirst()
                                  .orElseThrow();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void registerUser(String clientId, Certificate cert) throws DuplicateElementException {
        // TODO: store cert instead of writing password
        try {
            getUserCredentials(clientId);
            throw new DuplicateElementException();
        } catch (NoSuchElementException e) {
            synchronized (userCredentials) {
                File decryptedUserCredentials = decrypt();
                try (FileWriter fw = new FileWriter(decryptedUserCredentials, true);
                     BufferedWriter bw = new BufferedWriter(fw)) {
                    bw.write(clientId + ":" + cert + "\n");
                } catch (IOException ioException) {
                    throw new RuntimeException(ioException);
                }
                encrypt(decryptedUserCredentials);
            }
        }
    }

    private File decrypt() {
        try {
            File decrypted = new File(config.getValue("userDecryptedCredential"));
            AlgorithmParameters ap = AlgorithmParameters.getInstance(userCredentialsAlgorithm);
            cipherService.decrypt(new FileInputStream(userCredentials),
                    new FileOutputStream(decrypted), getKeyFromPassword(userCredentialsAlgorithm),ap);
            return decrypted;
        } catch (NoSuchAlgorithmException | FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void encrypt(File decryptedUserCredentials) {
        try {
            AlgorithmParameters ap = AlgorithmParameters.getInstance(userCredentialsAlgorithm);
            cipherService.encrypt(new FileInputStream(decryptedUserCredentials),
                    new FileOutputStream(userCredentials), getKeyFromPassword(userCredentialsAlgorithm), ap);
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private SecretKey getKeyFromPassword(String usersAlgorithm) {
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(usersAlgorithm);
            KeySpec spec = new PBEKeySpec(password);
            return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), usersAlgorithm);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isRegisteredUser(String clientId) {
        try {
            getUserCredentials(clientId);
        } catch (NoSuchElementException e) {
        	return false;
        }
        return true;
    }

    public Certificate getCertificate(String clientId) {
        String cerPath = this.getUserCredentials(clientId);
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(cerPath);
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            return factory.generateCertificate(fis);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
