/**
 *
 */
package com.segc;

import com.segc.exception.DuplicateElementException;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author fc54685 Francisco Correia
 * @author fc55955 Alexandre Fonseca
 * @author fc56272 Filipe Egipto
 */
public class TintolmarketServer {
    private static final int DEFAULT_PORT = 12345;
    private final int port;
    private final UserCatalog userCatalog;
    private final WineCatalog wineCatalog;

    public TintolmarketServer(int port) {
        this(port, Configuration.getInstance().getValue("userCredentials"), new WineCatalog(), new UserCatalog());
    }

    public TintolmarketServer(int port,
                              String userCredentialsFilename,
                              WineCatalog wineCatalog,
                              UserCatalog userCatalog) {
        this.port = port;
        this.wineCatalog = wineCatalog;
        this.userCatalog = userCatalog;
        File f = new File(userCredentialsFilename);
        try {
            if (f.getParentFile().mkdirs() || f.createNewFile()) {
                System.out.println("ficheiro '" + userCredentialsFilename + "' criado");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Map<String, String> getUsersFromFile(File file) {
        try (BufferedReader usersReader = new BufferedReader(new FileReader(file))) {
            return usersReader.lines()
                              .map(s -> s.split(":", 2))
                              .collect(Collectors.toMap(x -> x[0], x -> x[1]));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 1) {
            throw new IllegalArgumentException("Too many arguments: expected 0 or 1, got " + args.length);
        } else if (args.length == 1) {
            port = Integer.parseInt(args[0]);
        }
        TintolmarketServer tms = new TintolmarketServer(port);
        tms.startServer();
    }

    public void startServer() {
        try (ServerSocket sSoc = new ServerSocket(this.port)) {
            while (true) {
                try {
                    Socket inSoc = sSoc.accept();
                    ServerThread newServerThread = new ServerThread(inSoc);
                    newServerThread.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(-1);
        }
    }

    public void sell(String wineName, String sellerId, int value, int quantity) {
        wineCatalog.sell(wineName, sellerId, value, quantity);
    }

    public void buy(String buyerId, String wineName, String sellerId, int quantity) {
        double price = wineCatalog.getPrice(wineName, sellerId, quantity);
        wineCatalog.buy(wineName, sellerId, quantity);
        userCatalog.removeBalance(buyerId, price);
    }

    public double wallet(String clientId) {
        return userCatalog.getBalance(clientId);
    }

    public void talk(String recipient, String message, String sender) {
        userCatalog.talk(sender, recipient, message);
    }

    public Message read(String clientId) {
        return userCatalog.read(clientId);
    }

    private void interactionLoop() {
        while (true) {
            // TODO: faz cenas
        }
    }

    class ServerThread extends Thread {

        private final Socket socket;
        private User sessionUser;

        ServerThread(Socket inSoc) {
            this.socket = inSoc;
        }

        @Override
        public void run() {
            try {
                ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream());
                String clientId, password;

                try {
                    clientId = (String) inStream.readObject();
                    password = (String) inStream.readObject();
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
                boolean isAuthenticated;
                try {
                    userCatalog.add(clientId);
                    AuthenticationService.getInstance().registerUser(clientId, password);
                    isAuthenticated = AuthenticationService.getInstance().authenticateUser(clientId, password);
                } catch (DuplicateElementException e) {
                    System.out.printf("Unable to register user '%s': user already exists.%n", clientId);
                    isAuthenticated = false;
                }
                outStream.writeObject(isAuthenticated);
                if (isAuthenticated) {
                    // interactionLoop(); // TODO
                } else {
                    System.out.println("Authentication failed for user '" + clientId + "'.");
                }
                socket.shutdownOutput();
                socket.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
