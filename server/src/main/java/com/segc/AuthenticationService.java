package com.segc;

import com.segc.exception.DuplicateElementException;

import java.io.*;
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

    public AuthenticationService(File userCredentials, char[] password, CipherService cipherService) {
        this.cipherService = cipherService;
        this.password = password; // TODO: password encryption?
        this.userCredentials = userCredentials;
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
            return usersReader.lines().dropWhile(line -> !line.startsWith(pattern)).findFirst().orElseThrow();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void registerUser(String clientId, String password) throws DuplicateElementException {
        try {
            getUserCredentials(clientId);
            throw new DuplicateElementException();
        } catch (NoSuchElementException e) {
            try (FileWriter fw = new FileWriter(userCredentials, true); BufferedWriter bw = new BufferedWriter(fw)) {
                bw.write(clientId + ":" + password + "\n");
            } catch (IOException ioException) {
                throw new RuntimeException(ioException);
            }
        }
    }
}
