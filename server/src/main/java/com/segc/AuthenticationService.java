package com.segc;

import java.io.*;

/**
 * A singleton class that handles authentication.
 *
 * @author fc54685 Francisco Correia
 * @author fc55955 Alexandre Fonseca
 * @author fc56272 Filipe Egipto
 */
public class AuthenticationService {

    private static final AuthenticationService instance = new AuthenticationService();
    private final File userCredentials;

    private AuthenticationService() {
        userCredentials = new File(Configuration.getInstance().getValue("userCredentials"));
        try {
            if (userCredentials.getParentFile().mkdirs() || userCredentials.createNewFile()) {
                System.out.println(getClass().getName() + ": Ficheiro de credenciais de utilizadores criado.");
                System.out.println(getClass().getName() + ": " + userCredentials.getAbsolutePath());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the singleton instance of {@code AuthenticationService}.
     *
     * @return singleton instance of this class
     */
    public static AuthenticationService getInstance() {
        return instance;
    }

    public boolean authenticateUser(String clientId, String password) {
        String pattern = clientId + ":" + password;
        try (BufferedReader usersReader = new BufferedReader(new FileReader(userCredentials))) {
            return usersReader.lines().anyMatch(line -> line.equals(pattern));
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void registerUser(String clientId, String password) {
        try (FileWriter fw = new FileWriter(userCredentials, true)) {
            fw.write(clientId + ":" + password + "\n");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}