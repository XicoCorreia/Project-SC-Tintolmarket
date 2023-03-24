/**
 *
 */
package com.segc;

import com.segc.exception.DuplicateElementException;

import javax.swing.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.NoSuchElementException;

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
    private final AuthenticationService authService;

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
        this.authService = AuthenticationService.getInstance();
        File f = new File(userCredentialsFilename);
        try {
            if (f.getParentFile().mkdirs() || f.createNewFile()) {
                System.out.println("Created file: " + userCredentialsFilename);
            }
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
            throw new RuntimeException(e);
        }
    }

    public void add(String wineName, ImageIcon label) {
        wineCatalog.add(wineName, label);
    }

    public void sell(String wineName, String sellerId, double value, int quantity) {
        wineCatalog.sell(wineName, sellerId, value, quantity);
    }

    public void buy(String buyerId, String wineName, String sellerId, int quantity) {
        double balance = userCatalog.getBalance(buyerId);
        if (balance < wineCatalog.getPrice(wineName, sellerId, quantity)) {
            String msg = String.format("Buyer '%s' has insufficient funds to make this purchase.", buyerId);
            throw new IllegalArgumentException(msg);
        }
        double price = wineCatalog.buy(wineName, sellerId, quantity);
        userCatalog.transferBalance(buyerId, sellerId, price);
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

    public String view(String wineName) {
        return wineCatalog.view(wineName);
    }

    public void classify(String wineName, int stars) {
        wineCatalog.classify(wineName, stars);
    }

    private void interactionLoop(ObjectOutputStream outStream, ObjectInputStream inStream, String clientId)
            throws ClassNotFoundException, IOException {
        boolean isExiting = false;
        while (!isExiting) {
            String command = ((String) inStream.readObject()).toLowerCase();
            switch (command) {
                case "add":
                case "a": {
                    String wineName = (String) inStream.readObject();
                    ImageIcon label = (ImageIcon) inStream.readObject();
                    try {
                        add(wineName, label);
                        outStream.writeObject("Ok");
                        outStream.writeObject("Wine '" + wineName + "' successfully added.");
                    } catch (Exception e) {
                        outStream.writeObject("Error");
                        outStream.writeObject("Wine already exists");
                    }
                    break;
                }
                case "sell":
                case "s": {
                    String wineName = (String) inStream.readObject();
                    double value = (Double) inStream.readObject();
                    int quantity = (Integer) inStream.readObject();
                    try {
                        sell(wineName, clientId, value, quantity);
                        outStream.writeObject("Ok");
                        outStream.writeObject("Wine '" + wineName + "' successfully added to the market.");
                    } catch (NoSuchElementException e) {
                        outStream.writeObject("Error");
                        outStream.writeObject("Wine '" + wineName + "' does not exist.");
                    } catch (DuplicateElementException e) {
                        outStream.writeObject("Error");
                        outStream.writeObject("You are already selling wine '" + wineName + "'.");
                    }
                    break;
                }
                case "view":
                case "v": {
                    String wineName = (String) inStream.readObject();
                    try {
                        String s = view(wineName);
                        outStream.writeObject("Ok");
                        outStream.writeObject(s);
                    } catch (Exception e) {
                        outStream.writeObject("Error");
                        outStream.writeObject("Wine '" + wineName + "' does not exist.");
                    }
                    break;
                }
                case "buy":
                case "b": {
                    String wineName = (String) inStream.readObject();
                    String sellerId = (String) inStream.readObject();
                    int quantity = (Integer) inStream.readObject();
                    try {
                        buy(clientId, wineName, sellerId, quantity);
                        outStream.writeObject("Ok");
                        outStream.writeObject("Wine '" + wineName + "' bought successfully.");
                    } catch (NoSuchElementException e) {
                        outStream.writeObject("Error");
                        outStream.writeObject("Wine '" + wineName + "' does not exist.");
                    } catch (IllegalArgumentException e) {
                        //TODO - pode ser unidades insuficientes ou saldo insuficiente - msgs diferents
                        outStream.writeObject("Error");
                        outStream.writeObject(e.getMessage());
                    }
                    break;
                }
                case "wallet":
                case "w": {
                    double d = wallet(clientId);
                    outStream.writeObject("Ok");
                    outStream.writeObject("Your balance is " + d + "$.");
                    break;
                }
                case "classify":
                case "c": {
                    String wineName = (String) inStream.readObject();
                    int stars = (Integer) inStream.readObject();
                    try {
                        classify(wineName, stars);
                        outStream.writeObject("Ok");
                        outStream.writeObject("Classification added successfully.");
                    } catch (Exception e) {
                        outStream.writeObject("Error");
                        outStream.writeObject("Wine '" + wineName + "' does not exist.");
                    }
                    break;
                }
                case "talk":
                case "t": {
                    String recipientId = (String) inStream.readObject();
                    String message = (String) inStream.readObject();
                    try {
                        talk(recipientId, message, clientId);
                        outStream.writeObject("Ok");
                        outStream.writeObject("Message sent successfully.");
                    } catch (Exception e) {
                        outStream.writeObject("Error");
                        outStream.writeObject("User '" + recipientId + "' does not exist.");
                    }
                    break;
                }
                case "read":
                case "r": {
                    try {
                        Message m = read(clientId);
                        outStream.writeObject("Ok");
                        outStream.writeObject(m.toString());
                    } catch (Exception e) {
                        outStream.writeObject("Error");
                        outStream.writeObject("No messages to read.");
                    }
                    break;
                }
                case "exit":
                case "quit":
                case "stop": {
                    isExiting = true;
                    break;
                }
                default: {
                    throw new IllegalArgumentException("Unexpected command: " + command);
                }
            }
        }
    }

    class ServerThread extends Thread {

        private final Socket socket;

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
                    isAuthenticated = authService.authenticateUser(clientId, password);
                } catch (NoSuchElementException e) { // 1.b user does not exist
                    authService.registerUser(clientId, password);
                    userCatalog.add(clientId);
                    isAuthenticated = true;
                }
                outStream.writeObject(isAuthenticated);
                if (isAuthenticated) {
                    interactionLoop(outStream, inStream, clientId); // TODO
                } else {
                    System.out.println("Authentication failed for user '" + clientId + "'.");
                }
                socket.shutdownOutput();
                socket.close();

            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}