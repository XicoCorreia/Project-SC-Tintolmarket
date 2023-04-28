/**
 *
 */
package com.segc;

import com.segc.exception.DuplicateElementException;
import com.segc.services.AuthenticationService;
import com.segc.services.BlockchainService;
import com.segc.services.CipherService;
import com.segc.services.DataPersistenceService;
import com.segc.transaction.SignedTransaction;
import com.segc.transaction.Transaction;
import com.segc.transaction.WineTransaction;
import com.segc.users.UserCatalog;
import com.segc.wines.WineCatalog;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.security.*;
import java.security.cert.Certificate;
import java.util.LinkedList;
import java.util.NoSuchElementException;

/**
 * @author fc54685 Francisco Correia
 * @author fc55955 Alexandre Fonseca
 * @author fc56272 Filipe Egipto
 */
public class TintolmarketServer {
    private static final Configuration config = Configuration.getInstance();
    private static final int DEFAULT_PORT = 12345;
    private final int port;
    private final UserCatalog userCatalog;
    private final WineCatalog wineCatalog;
    private final AuthenticationService authService;
    private final CipherService cipherService;
    private final BlockchainService blockchainService;

    public TintolmarketServer(int port,
                              AuthenticationService authService,
                              CipherService cipherService,
                              BlockchainService blockchainService,
                              DataPersistenceService dps) {
        this(port, new WineCatalog(dps), new UserCatalog(dps), authService, cipherService, blockchainService);
    }

    public TintolmarketServer(int port,
                              WineCatalog wineCatalog,
                              UserCatalog userCatalog,
                              AuthenticationService authService,
                              CipherService cipherService,
                              BlockchainService blockchainService) {
        this.port = port;
        this.wineCatalog = wineCatalog;
        this.userCatalog = userCatalog;
        this.authService = authService;
        this.cipherService = cipherService;
        this.blockchainService = blockchainService;
    }

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length < 3 || args.length > 4) {
            throw new IllegalArgumentException("Too many arguments: expected 3 or 4, got " + args.length);
        } else if (args.length == 4) {
            port = Integer.parseInt(args[0]);
        }
        char[] password = args[1].toCharArray();
        String keyStore = args[2];
        String keyStorePassword = args[3];

        String signatureAlgorithm = config.getValue("signatureAlgorithm");
        String blockchainDir = config.getValue("blockchainDir");

        System.setProperty("javax.net.ssl.keyStore", keyStore);
        System.setProperty("javax.net.ssl.keyStorePassword", keyStorePassword);
        System.setProperty("javax.net.ssl.keyStoreType", config.getValue("keyStoreType"));

        CipherService cipherService = new CipherService(config.getValue("keyStoreAlias"), signatureAlgorithm);
        AuthenticationService authService = new AuthenticationService(password, cipherService);

        DataPersistenceService dps = new DataPersistenceService();
        BlockchainService blockchainService = new BlockchainService(blockchainDir, cipherService, dps);
        TintolmarketServer tms = new TintolmarketServer(port, authService, cipherService, blockchainService, dps);
        tms.startServer();
    }

    @SuppressWarnings("InfiniteLoopStatement")
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
            throws NoSuchElementException, IllegalArgumentException {
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

    public void talk(String recipient, byte[] message, String sender) throws NoSuchElementException {
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

    public LinkedList<Transaction> list() {
        return blockchainService.getTransactions();
    }

    private void interactionLoop(ObjectOutputStream outStream, ObjectInputStream inStream, String clientId)
            throws ClassNotFoundException, IOException, InvalidKeyException, SignatureException {
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
                    SignedTransaction st = (SignedTransaction) inStream.readObject();
                    Certificate cert = authService.getCertificate(clientId);

                    if (!cipherService.verify(st.getSignedObject(), cert)) {
                        outStream.writeObject(Opcode.ERROR);
                        outStream.writeObject("Server couldn't verify the signature.");
                        break;
                    }

                    try {
                        sell(st.getItemId(), st.getAuthorId(), st.getUnitPrice(), st.getUnitCount());
                        blockchainService.addTransaction(st);
                        outStream.writeObject(Opcode.OK);
                        outStream.writeObject("Wine '" + st.getItemId() + "' successfully added to the market.");
                    } catch (NoSuchElementException e) {
                        outStream.writeObject(Opcode.ERROR);
                        outStream.writeObject("Wine '" + st.getItemId() + "' does not exist.");
                    } catch (DuplicateElementException e) {
                        outStream.writeObject(Opcode.ERROR);
                        outStream.writeObject("You are already selling wine '" + st.getItemId() + "'.");
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
                    double price;
                    try {
                        price = wineCatalog.getPrice(wineName, sellerId); // cost per unit
                    } catch (NoSuchElementException e) {
                        outStream.writeObject(Opcode.ERROR); // twice due to how the client handles messaging
                        outStream.writeObject(Opcode.ERROR);
                        outStream.writeObject("Wine '" + wineName + "' does not exist.");
                        break;
                    } catch (IllegalArgumentException e) {
                        outStream.writeObject(Opcode.ERROR); // twice due to how the client handles messaging
                        outStream.writeObject(Opcode.ERROR);
                        outStream.writeObject(e.getMessage());
                        break;
                    }
                    outStream.writeObject(Opcode.OK);
                    WineTransaction wt = new WineTransaction(wineName, sellerId, quantity, price, Transaction.Type.BUY);
                    outStream.writeObject(wt);

                    SignedTransaction st = (SignedTransaction) inStream.readObject();
                    Certificate cert = authService.getCertificate(clientId);

                    if (!cipherService.verify(st.getSignedObject(), cert)) {
                        outStream.writeObject(Opcode.ERROR);
                        outStream.writeObject("Server couldn't verify the signature.");
                        break;
                    }

                    try {
                        buy(st.getAuthorId(), st.getItemId(), sellerId, st.getUnitCount());
                        blockchainService.addTransaction(st);
                        outStream.writeObject(Opcode.OK);
                        outStream.writeObject("Wine '" + st.getItemId() + "' bought successfully.");
                    } catch (NoSuchElementException e) {
                        String message = wineCatalog.contains(wineName)
                                         ? "Wine '" + wineName + "' is not listed by the that seller."
                                         : "Wine '" + wineName + "' does not exist.";
                        outStream.writeObject(Opcode.ERROR);
                        outStream.writeObject(message);
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
                    } catch (NoSuchElementException e) {
                        outStream.writeObject(Opcode.ERROR);
                        outStream.writeObject("Wine '" + wineName + "' does not exist.");
                    } catch (Exception e) {
                        outStream.writeObject(Opcode.ERROR);
                        outStream.writeObject(e.getMessage());
                    }
                    break;
                }
                case TALK: {
                    String recipientId = (String) inStream.readObject();
                    byte[] message = (byte[]) inStream.readObject();
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
                        outStream.writeObject(m);
                    } catch (Exception e) {
                        outStream.writeObject(Opcode.ERROR);
                        outStream.writeObject("No messages to read.");
                    }
                    break;
                }
                case LIST: {
                    LinkedList<Transaction> transactions = blockchainService.getTransactions();
                    outStream.writeObject(Opcode.OK);
                    outStream.writeObject(transactions);
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
                String clientId;
                Long nonce = CipherService.genNonce();

                clientId = (String) inStream.readObject();

                boolean isRegistered = authService.isRegisteredUser(clientId);
                boolean isAuthenticated = false;

                outStream.writeObject(nonce);
                outStream.writeObject(isRegistered);

                SignedObject receivedNonce = (SignedObject) inStream.readObject();
                Certificate cert = isRegistered ? authService.getCertificate(clientId) // utilizador existente
                                                : (Certificate) inStream.readObject(); // novo utilizador
                if (!isRegistered) {
                    authService.registerUser(clientId, cert);
                    userCatalog.add(clientId);
                }

                if (cipherService.verify(receivedNonce, cert)) {
                    isAuthenticated = true;
                }

                outStream.writeObject(isAuthenticated);
                if (isAuthenticated) {
                    interactionLoop(outStream, inStream, clientId);
                } else {
                    System.out.println("Authentication failed for user '" + clientId + "'.");
                }
                socket.shutdownOutput();
                socket.close();
            } catch (IOException | ClassNotFoundException | InvalidKeyException | SignatureException e) {
                throw new RuntimeException(e);
            }
        }
    }
}