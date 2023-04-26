/**
 *
 */
package com.segc;

import com.segc.exception.DuplicateElementException;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.util.NoSuchElementException;
import java.util.Random;

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
    private static final Configuration config = Configuration.getInstance();

    public TintolmarketServer(int port, AuthenticationService authService) {
        this(port, new WineCatalog(), new UserCatalog(), authService);
    }

    public TintolmarketServer(int port,
                              WineCatalog wineCatalog,
                              UserCatalog userCatalog,
                              AuthenticationService authService) {
        this.port = port;
        this.wineCatalog = wineCatalog;
        this.userCatalog = userCatalog;
        this.authService = authService;
    }

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length < 3 || args.length > 4) {
            throw new IllegalArgumentException("Too many arguments: expected 3 or 4, got " + args.length);
        } else if (args.length == 4) {
            port = Integer.parseInt(args[0]);
        }
        char[] password = args[1].toCharArray();
        File keyStoreFile = new File(args[2]);
        char[] keyStorePassword = args[3].toCharArray();

        String keyStoreFormat = config.getValue("keyStoreFormat");
        File userCredentials = new File(config.getValue("userCredentials"));

        CipherService cipherService = new CipherService(keyStoreFile, keyStorePassword, keyStoreFormat);
        AuthenticationService authService = new AuthenticationService(userCredentials, password, cipherService);

        TintolmarketServer tms = new TintolmarketServer(port, authService);
        
    	System.setProperty("javax.net.ssl.keyStore", keyStoreFile.getAbsolutePath());
    	System.setProperty("javax.net.ssl.keyStorePassword", keyStorePassword.toString());
    	
        tms.startServer();
    }

    public void startServer() {

    	ServerSocketFactory ssf = SSLServerSocketFactory.getDefault();
        try (SSLServerSocket sSoc = (SSLServerSocket) ssf.createServerSocket(this.port)) {
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

    public void add(String wineName, ImageIcon label) throws DuplicateElementException {
        wineCatalog.add(wineName, label);
    }

    public void sell(String wineName, String sellerId, double value, int quantity)
            throws DuplicateElementException, NoSuchElementException, IllegalArgumentException {
        wineCatalog.sell(wineName, sellerId, value, quantity);
    }

    public void buy(String buyerId, String wineName, String sellerId, int quantity)
            throws DuplicateElementException, NoSuchElementException, IllegalArgumentException {
        double balance = userCatalog.getBalance(buyerId);
        if (balance < wineCatalog.getPrice(wineName, sellerId, quantity)) {
            String msg = String.format("Buyer '%s' has insufficient funds to make this purchase.", buyerId);
            throw new IllegalArgumentException(msg);
        }
        double price = wineCatalog.buy(wineName, sellerId, quantity);
        userCatalog.transferBalance(buyerId, sellerId, price);
    }

    public double wallet(String clientId) throws NoSuchElementException {
        return userCatalog.getBalance(clientId);
    }

    public void talk(String recipient, String message, String sender) throws NoSuchElementException {
        userCatalog.talk(sender, recipient, message);
    }

    public Message read(String clientId) throws NoSuchElementException, IllegalArgumentException {
        return userCatalog.read(clientId);
    }

    public String view(String wineName) throws NoSuchElementException {
        return wineCatalog.view(wineName);
    }

    public void classify(String wineName, int stars) throws NoSuchElementException, IllegalArgumentException {
        wineCatalog.classify(wineName, stars);
    }

    private void interactionLoop(ObjectOutputStream outStream, ObjectInputStream inStream, String clientId)
            throws ClassNotFoundException, IOException {
        boolean isExiting = false;
        while (!isExiting) {
            Opcode command = (Opcode) inStream.readObject();
            switch (command) {
                case ADD: {
                    String wineName = (String) inStream.readObject();
                    ImageIcon label = (ImageIcon) inStream.readObject();
                    try {
                        add(wineName, label);
                        outStream.writeObject(Opcode.OK);
                        outStream.writeObject("Wine '" + wineName + "' successfully added.");
                    } catch (Exception e) {
                        outStream.writeObject(Opcode.ERROR);
                        outStream.writeObject("Wine already exists");
                    }
                    break;
                }
                case SELL: {
                    String wineName = (String) inStream.readObject();
                    double value = (Double) inStream.readObject();
                    int quantity = (Integer) inStream.readObject();
                    try {
                        sell(wineName, clientId, value, quantity);
                        outStream.writeObject(Opcode.OK);
                        outStream.writeObject("Wine '" + wineName + "' successfully added to the market.");
                    } catch (NoSuchElementException e) {
                        outStream.writeObject(Opcode.ERROR);
                        outStream.writeObject("Wine '" + wineName + "' does not exist.");
                    } catch (DuplicateElementException e) {
                        outStream.writeObject(Opcode.ERROR);
                        outStream.writeObject("You are already selling wine '" + wineName + "'.");
                    }
                    break;
                }
                case VIEW: {
                    String wineName = (String) inStream.readObject();
                    try {
                        String s = view(wineName);
                        outStream.writeObject(Opcode.OK);
                        outStream.writeObject(s);
                    } catch (Exception e) {
                        outStream.writeObject(Opcode.ERROR);
                        outStream.writeObject("Wine '" + wineName + "' does not exist.");
                    }
                    break;
                }
                case BUY: {
                    String wineName = (String) inStream.readObject();
                    String sellerId = (String) inStream.readObject();
                    int quantity = (Integer) inStream.readObject();
                    try {
                        buy(clientId, wineName, sellerId, quantity);
                        outStream.writeObject(Opcode.OK);
                        outStream.writeObject("Wine '" + wineName + "' bought successfully.");
                    } catch (NoSuchElementException e) {
                        outStream.writeObject(Opcode.ERROR);
                        outStream.writeObject("Wine '" + wineName + "' does not exist.");
                    } catch (IllegalArgumentException e) {
                        outStream.writeObject(Opcode.ERROR);
                        outStream.writeObject(e.getMessage()); // exception message contains more details
                    }
                    break;
                }
                case WALLET: {
                    double d = wallet(clientId);
                    outStream.writeObject(Opcode.OK);
                    outStream.writeObject("Your balance is " + d + "$.");
                    break;
                }
                case CLASSIFY: {
                    String wineName = (String) inStream.readObject();
                    int stars = (Integer) inStream.readObject();
                    try {
                        classify(wineName, stars);
                        outStream.writeObject(Opcode.OK);
                        outStream.writeObject("Classification added successfully.");
                    } catch (Exception e) {
                        outStream.writeObject(Opcode.ERROR);
                        outStream.writeObject("Wine '" + wineName + "' does not exist.");
                    }
                    break;
                }
                case TALK: {
                    String recipientId = (String) inStream.readObject();
                    String message = (String) inStream.readObject();
                    try {
                        talk(recipientId, message, clientId);
                        outStream.writeObject(Opcode.OK);
                        outStream.writeObject("Message sent successfully.");
                    } catch (Exception e) {
                        outStream.writeObject(Opcode.ERROR);
                        outStream.writeObject("User '" + recipientId + "' does not exist.");
                    }
                    break;
                }
                case READ: {
                    try {
                        Message m = read(clientId);
                        outStream.writeObject(Opcode.OK);
                        outStream.writeObject(m.toString());
                    } catch (Exception e) {
                        outStream.writeObject(Opcode.ERROR);
                        outStream.writeObject("No messages to read.");
                    }
                    break;
                }
                case LIST: {
                    outStream.writeObject(Opcode.OK);
                    outStream.writeObject(""); //TODO
                }
                case EXIT: {
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
                Random rd = new Random();
                long nonce = rd.nextLong();

                try {
                    clientId = (String) inStream.readObject();
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
                boolean knownUser;
                boolean isAuthenticated = false;
                knownUser = authService.isRegisteredUser(clientId);
                outStream.writeObject(nonce);
                outStream.writeObject(knownUser);

                if(knownUser) {
                    //Ja registado
                   long receivedNonce = (long) inStream.readObject();
                   //TODO
                    	
                }
                else{
                    //Nao registado
                    long receivedNonce = (long) inStream.readObject();
                    //TODO
                }

                    

                outStream.writeObject(isAuthenticated);
                if (isAuthenticated) {
                    interactionLoop(outStream, inStream, clientId);
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