/**
 *
 */
package com.segc;

import com.segc.services.CipherService;
import com.segc.transaction.SignedTransaction;
import com.segc.transaction.WineTransaction;
import com.segc.transaction.Transaction.Type;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.Map;
import java.util.Scanner;
import java.security.SignedObject;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.swing.ImageIcon;

/**
 * @author fc54685 Francisco Correia
 * @author fc55955 Alexandre Fonseca
 * @author fc56272 Filipe Egipto
 */
public class Tintolmarket {

    public static final int DEFAULT_PORT = 12345;
    private static final Map<String, Opcode> opcodes = Map.ofEntries(
            Map.entry("a", Opcode.ADD),
            Map.entry("add", Opcode.ADD),
            Map.entry("s", Opcode.SELL),
            Map.entry("sell", Opcode.SELL),
            Map.entry("v", Opcode.VIEW),
            Map.entry("view", Opcode.VIEW),
            Map.entry("b", Opcode.BUY),
            Map.entry("buy", Opcode.BUY),
            Map.entry("w", Opcode.WALLET),
            Map.entry("wallet", Opcode.WALLET),
            Map.entry("c", Opcode.CLASSIFY),
            Map.entry("classify", Opcode.CLASSIFY),
            Map.entry("t", Opcode.TALK),
            Map.entry("talk", Opcode.TALK),
            Map.entry("r", Opcode.READ),
            Map.entry("read", Opcode.READ),
            Map.entry("l", Opcode.LIST),
            Map.entry("list", Opcode.LIST),
            Map.entry("exit", Opcode.EXIT),
            Map.entry("quit", Opcode.EXIT));

    private static final String COMMANDS = String.format("Available commands:%n" +
            "- add <wine> <image>%n" +
            "- sell <wine> <value> <quantity>%n" +
            "- view <wine>%n" +
            "- buy <wine> <seller> <quantity>%n" +
            "- wallet%n" +
            "- classify <wine> <stars>%n" +
            "- talk <user> <message>%n" +
            "- list%n" +
            "- read%n" +
            "- quit%n");

    private static String user;

    public static void main(String[] args) {
        if (args.length < 4) {
            System.out.printf("Invalid arguments.%n" +
                    "Use: java -jar Tintolmarket.jar  <serverAddress> <truststore> <keystore> <password-keystore> <userID>%n");
            System.exit(1);
        }

        Configuration config = Configuration.getInstance();
        String serverAddress = args[0];
        String trustStore = args[1];
        String keyStore = args[2];
        String keyStorePassword = args[3];
        user = args[4];
        String host = serverAddress;
        int port = DEFAULT_PORT;

        String[] hostPort = serverAddress.split(":");
        if (hostPort.length == 2) {
            host = hostPort[0];
            port = Integer.parseInt(hostPort[1]);
        }
        System.setProperty("javax.net.ssl.keyStore", keyStore);
        System.setProperty("javax.net.ssl.keyStorePassword", keyStorePassword);
        System.setProperty("javax.net.ssl.keyStoreType", config.getValue("keyStoreType"));

        System.setProperty("javax.net.ssl.trustStore", trustStore);
        System.setProperty("javax.net.ssl.trustStorePassword", config.getValue("trustStorePassword"));
        System.setProperty("javax.net.ssl.trustStoreType", config.getValue("trustStoreType"));

        String signatureAlgorithm = config.getValue("signatureAlgorithm");
        CipherService cipherService = new CipherService(user, signatureAlgorithm);

        SocketFactory sf = SSLSocketFactory.getDefault();
        try (SSLSocket socket = (SSLSocket) sf.createSocket(host, port);
             ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream())) {

            Scanner sc = new Scanner(System.in);
            outStream.writeObject(user);

            long nonce = (Long) inStream.readObject();
            boolean isRegistered = (Boolean) inStream.readObject();

            SignedObject signedNonce = cipherService.sign(nonce);
            outStream.writeObject(signedNonce);
            if (!isRegistered) {
                outStream.writeObject(cipherService.getCertificate());
            }

            boolean isAuthenticated = (Boolean) inStream.readObject();
            if (!isAuthenticated) {
                System.out.println("Authentication failed.");
                System.exit(1);
            }

            System.out.printf("Authenticated.%n%s%n", COMMANDS);

            boolean isExiting = false;
            while (sc.hasNextLine()) {
                String command = sc.nextLine();
                String[] c = command.split(" ");
                Opcode opcode = opcodes.getOrDefault(c[0].toLowerCase(), Opcode.INVALID);
                if (opcode == Opcode.INVALID) {
                    System.out.println("Unexpected command: " + c[0]);
                    continue;
                }

                Opcode status = Opcode.OK;
                switch (opcode) {
                    case ADD: {
                        status = add(outStream, c, sc);
                        break;
                    }
                    case SELL: {
                        status = sell(outStream, c, cipherService);
                        break;
                    }
                    case VIEW: {
                        status = view(outStream, c);
                        break;
                    }
                    case BUY: {
                        status = buy(outStream, inStream, c, cipherService);
                        break;
                    }
                    case CLASSIFY: {
                        status = classify(outStream, c);
                        break;
                    }
                    case TALK: {
                        status = talk(outStream, c, cipherService);
                        break;
                    }
                    case EXIT: {
                        outStream.writeObject(opcode);
                        isExiting = true;
                        break;
                    }
                    case WALLET:
                    case READ:
                    case LIST: {
                        outStream.writeObject(opcode);
                        break;
                    }
                    default: {
                        throw new RuntimeException();
                    }
                }
                if (isExiting) {
                    break;
                }
                if (status == Opcode.INVALID) {
                    continue;
                }

                if (status == Opcode.OK) {
                    status = (Opcode) inStream.readObject(); // if there was no error in the middle of an exchange
                }
                String response;
                if (opcode == Opcode.READ && status == Opcode.OK) {
                    Message message = (Message) inStream.readObject();
                    String author = message.getAuthor();
                    byte[] content = message.getContent();
                    content = cipherService.decrypt(content);
                    response = String.format("Enviado por: '%s'%n%s", author, new String(content));
                } else if (opcode == Opcode.LIST && status == Opcode.OK) {
                    @SuppressWarnings("unchecked")
                    LinkedList<SignedTransaction> transactions = (LinkedList<SignedTransaction>) inStream.readObject();
                    StringBuilder sb = new StringBuilder(transactions.size() * 128);
                    sb.append("Number of transactions: ").append(transactions.size()).append(System.lineSeparator());
                    transactions.forEach(sb::append);
                    response = sb.toString();
                } else {
                    response = (String) inStream.readObject();
                }
                PrintStream out = status == Opcode.ERROR ? System.err : System.out;
                out.println(response);
            }
            sc.close();

        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static Opcode add(ObjectOutputStream outStream, String[] command, Scanner sc) throws IOException {
        if (command.length != 3) {
            System.out.println("Error in the command");
            return Opcode.INVALID;
        }
        outStream.writeObject(Opcode.ADD);
        outStream.writeObject(command[1]);
        Path path = Paths.get(command[2]);
        ImageIcon image;

        while (!Files.exists(path) && !path.toString().equals("null")) {
            System.out.println("Enter the path to the wine image: ");
            path = Paths.get(sc.nextLine());
        }

        if (Files.exists(path)) {
            image = new ImageIcon(command[2]);
        } else {
            System.out.println("INFO: Wine does not have a label.");
            image = new ImageIcon();
        }
        outStream.writeObject(image);
        return Opcode.OK;
    }

    private static Opcode sell(ObjectOutputStream outStream, String[] command, CipherService cipherService)
            throws IOException {
        if (command.length != 4) {
            System.out.println("Error in the command");
            return Opcode.INVALID;
        }
        outStream.writeObject(Opcode.SELL);
        String wine = command[1];
        double value = Double.parseDouble(command[2]);
        int quantity = Integer.parseInt(command[3]);
        WineTransaction wt = new WineTransaction(wine, user, quantity, value, Type.SELL);
        SignedTransaction signedTransaction = new SignedTransaction(cipherService.sign(wt));
        outStream.writeObject(signedTransaction);
        return Opcode.OK;
    }

    private static Opcode view(ObjectOutputStream outStream, String[] command) throws IOException {
        if (command.length != 2) {
            System.out.println("Error in the command");
            return Opcode.INVALID;
        }
        outStream.writeObject(Opcode.VIEW);
        outStream.writeObject(command[1]);
        return Opcode.OK;
    }


    private static Opcode buy(ObjectOutputStream outStream,
                              ObjectInputStream inStream,
                              String[] command,
                              CipherService cipherService) throws IOException {
        if (command.length != 4) {
            System.out.println("Error in the command");
            return Opcode.INVALID;
        }
        outStream.writeObject(Opcode.BUY);
        String wine = command[1];
        String sellerId = command[2];
        int quantity = Integer.parseInt(command[3]);
        outStream.writeObject(wine);
        outStream.writeObject(sellerId);
        outStream.writeObject(quantity);
        try {
            Opcode requestStatus = (Opcode) inStream.readObject();
            if (requestStatus.equals(Opcode.ERROR)) {
                return Opcode.ERROR;
            }
            WineTransaction wt = (WineTransaction) inStream.readObject();
            assert user.equals(wt.getAuthorId()) && wine.equals(wt.getItemId()) && quantity == wt.getUnitCount();
            SignedTransaction signedTransaction = new SignedTransaction(cipherService.sign(wt));
            outStream.writeObject(signedTransaction);
            return Opcode.OK;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static Opcode classify(ObjectOutputStream outStream, String[] command) throws IOException {
        if (command.length != 3) {
            System.out.println("Error in the command");
            return Opcode.INVALID;
        }
        outStream.writeObject(Opcode.CLASSIFY);
        int stars;
        try {
            stars = Integer.parseInt(command[2]);
        } catch (NumberFormatException e) {
            stars = -1;
        }
        outStream.writeObject(command[1]);
        outStream.writeObject(stars);
        return Opcode.OK;
    }

    private static Opcode talk(ObjectOutputStream outStream, String[] command, CipherService cipherService)
            throws IOException {

        if (command.length < 3) {
            System.out.println("Error in the command");
            return Opcode.INVALID;
        }
        outStream.writeObject(Opcode.TALK);
        StringBuilder builder = new StringBuilder();
        builder.append(command[2]);
        for (int i = 3; i < command.length; i++) {
            builder.append(" ").append(command[i]);
        }

        String receiverId = command[1];
        String message = builder.toString();
        byte[] encryptedMessage = message.getBytes();
        encryptedMessage = cipherService.encrypt(encryptedMessage, receiverId);

        outStream.writeObject(receiverId);
        outStream.writeObject(encryptedMessage);
        return Opcode.OK;
    }
}
