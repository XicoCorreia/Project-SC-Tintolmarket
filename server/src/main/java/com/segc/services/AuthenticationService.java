package com.segc.services;

import com.segc.CipherService;
import com.segc.Configuration;
import com.segc.exception.DuplicateElementException;

import javax.crypto.*;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
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
        this(userCredentials, password, cipherService, Configuration.getInstance().getValue("userCredentialsPBEAlgorithm"));
    }

    public AuthenticationService(File userCredentials, char[] password, CipherService cipherService,
                                 String userCredentialsAlgorithm) {
        this.cipherService = cipherService;
        this.password = password;
        this.userCredentials = userCredentials;
        this.userCredentialsAlgorithm = userCredentialsAlgorithm;
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
        try (BufferedReader usersReader = new BufferedReader(new FileReader(userCredentials))) {
            return usersReader.lines().dropWhile(line -> !decrypted(line).startsWith(pattern)).findFirst().orElseThrow();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String decrypted(String line) {
        try {
            Cipher c = Cipher.getInstance(userCredentialsAlgorithm);
            c.init(Cipher.DECRYPT_MODE, getKeyFromPassword(userCredentialsAlgorithm));
            return new String(c.doFinal(line.getBytes()));
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException |
                 BadPaddingException e) {
            throw new RuntimeException(e);
        }
    }

    //private String decrypted(String line) {
    //        try {
    //            //TODO: Alias
    //            return new String(cipherService.decrypt(line.getBytes(), "ALIAS", password));
    //        } catch (UnrecoverableKeyException | DestroyFailedException e) {
    //            throw new RuntimeException(e);
    //        }
    //    }

    public void registerUser(String clientId, String password) throws DuplicateElementException {
        try {
            getUserCredentials(clientId);
            throw new DuplicateElementException();
        } catch (NoSuchElementException e) {
            try (FileWriter fw = new FileWriter(userCredentials, true); BufferedWriter bw = new BufferedWriter(fw)) {
                bw.write(encrypted(clientId + ":" + password + "\n"));
            } catch (IOException ioException) {
                throw new RuntimeException(ioException);
            }
        }
    }

    private String encrypted(String line) {
        try {
            Cipher c = Cipher.getInstance(userCredentialsAlgorithm);
            c.init(Cipher.ENCRYPT_MODE, getKeyFromPassword(userCredentialsAlgorithm));
            return new String(c.doFinal(line.getBytes()));
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException |
                 BadPaddingException e) {
            throw new RuntimeException(e);
        }
    }

    //private String encrypted(String line) {
    //        try {
    //            //TODO: Alias
    //            return new String(cipherService.encrypt(line.getBytes(), "ALIAS", password));
    //        } catch (UnrecoverableKeyException | DestroyFailedException e) {
    //            throw new RuntimeException(e);
    //        }
    //    }

    private SecretKey getKeyFromPassword(String usersAlgorithm) {
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(usersAlgorithm);
            KeySpec spec = new PBEKeySpec(password.toString().toCharArray());
            return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), usersAlgorithm);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }

	public boolean isRegisteredUser(String clientId) {
		// TODO Auto-generated method stub
		return false;
	}

	public PublicKey getPublicKey(String clientId) {
		// TODO Auto-generated method stub
		return null;
	}
}
