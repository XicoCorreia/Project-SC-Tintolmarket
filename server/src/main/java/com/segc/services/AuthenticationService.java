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
    private final CipherService cipherService;
    private final File userCredentials;
    private final char[] password;
    private final String userCredentialsAlgorithm;

    public AuthenticationService(File userCredentials, char[] password, CipherService cipherService) {
        this(userCredentials,
                password,
                cipherService,
                Configuration.getInstance().getValue("userCredentialsPBEAlgorithm"));
    }

    public AuthenticationService(File userCredentials, char[] password, CipherService cipherService,
                                 String userCredentialsPBEAlgorithm) {
        this(userCredentials,
                password,
                cipherService,
                userCredentialsPBEAlgorithm,
                new File(Configuration.getInstance().getValue("userDecryptedCredential")));
    }

    public AuthenticationService(File userCredentials, char[] password, CipherService cipherService,
                                 String userCredentialsAlgorithm, File decryptedUserCredentials) {
        this.cipherService = cipherService;
        this.password = password;
        this.userCredentialsAlgorithm = userCredentialsAlgorithm;
        this.userCredentials = decryptedUserCredentials;
        try {
            if (userCredentials.getParentFile().mkdirs() || userCredentials.createNewFile()) {
                System.out.println(getClass().getSimpleName() + ": created empty user credentials file.");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        decrypted(userCredentials);
    }

    public boolean authenticateUser(String clientId, String password) throws NoSuchElementException {
        return getUserCredentials(clientId).equals(clientId + ":" + password);
    }

    private String getUserCredentials(String clientId) throws NoSuchElementException {
        // TODO: decrypt entire file and then read lines
        String pattern = clientId + ":";
        synchronized (userCredentials) {
            try (BufferedReader usersReader = new BufferedReader(new FileReader(userCredentials))) {
                return usersReader.lines()
                                  .dropWhile(line -> !line.startsWith(pattern))
                                  .findFirst()
                                  .orElseThrow();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    //Decidir se decifra a cada uso do ficheiro ou só uma vez - no inicio
    //Tornar void e transformar file em atributo
    private void decrypted(File userCredentialsEncrypted) {
        try {
            AlgorithmParameters ap = AlgorithmParameters.getInstance(userCredentialsAlgorithm);
            cipherService.decrypt(new FileInputStream(userCredentialsEncrypted),
                    new FileOutputStream(userCredentials), getKeyFromPassword(userCredentialsAlgorithm),ap);
        } catch (NoSuchAlgorithmException | FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void registerUser(String clientId, Certificate cert) throws DuplicateElementException {
        // TODO: store cert instead of writing password
        try {
            getUserCredentials(clientId);
            throw new DuplicateElementException();
        } catch (NoSuchElementException e) {
            synchronized (userCredentials) {
                try (FileWriter fw = new FileWriter(userCredentials, true);
                     BufferedWriter bw = new BufferedWriter(fw)) {
                    bw.write(clientId + ":" + cert + "\n");
                } catch (IOException ioException) {
                    throw new RuntimeException(ioException);
                }
            }
        }
    }

    //TODO: when to encrypt the file?? Should we decrypt and encrypt at every usage??
    private void encrypted(File encryptedUserCredentials) {
        try {
            AlgorithmParameters ap = AlgorithmParameters.getInstance(userCredentialsAlgorithm);
            cipherService.encrypt(new FileInputStream(userCredentials),
                    new FileOutputStream(encryptedUserCredentials), getKeyFromPassword(userCredentialsAlgorithm), ap);
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
        // TODO: check if user already exists
        return false;
    }

    public Certificate getCertificate(String clientId) {
        // TODO: get certificate
        return null;
    }
}
