/**
 *
 */
package com.segc;

import com.segc.services.CipherService;
import com.segc.transaction.WineTransaction;
import com.segc.transaction.Transaction.Type;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
            "- quit%n" );
    
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
            if (!inStream.readBoolean()) {
                System.out.println("Authentication failed.");
                System.exit(1);
            }


            System.out.printf("Authenticated.%n%s%n", COMMANDS);
            boolean isExiting = false;

            while (!isExiting && sc.hasNextLine()) {
                String command = sc.nextLine();
                String[] c = command.split(" ");
                Opcode opcode = opcodes.getOrDefault(c[0].toLowerCase(), Opcode.INVALID);
                if (opcode == Opcode.INVALID) {
                    System.out.println("Unexpected command: " + c[0]);
                    continue;
                }
                outStream.writeObject(opcode);
                switch (opcode) {
                    case ADD: {
                        add(outStream, c, sc);
                        break;
                    }
                    case SELL: {
                        sell(outStream, c, cipherService);
                        break;
                    }
                    case VIEW: {
                        view(outStream, c);
                        break;
                    }
                    case BUY: {
                        buy(outStream, inStream, c, cipherService);
                        break;
                    }
                    case CLASSIFY: {
                        classify(outStream, c);
                        break;
                    }
                    case TALK: {
                        talk(outStream, c, cipherService);
                        break;
                    }
                    case EXIT: {
                        isExiting = true;
                        break;
                    }
                    case WALLET:
                    case READ:
                    case LIST: {
                        break; //ERROR if length(c) > 1?
                    }
                    default: {
                        throw new RuntimeException();
                    }
                }
                Opcode status = (Opcode) inStream.readObject();
                
                
                String response = (String) inStream.readObject();
                if (status == Opcode.ERROR) {
                    System.err.println(response);
                } else {
                    if (opcode == Opcode.READ) {
                    	Message message = (Message) inStream.readObject();
                    	String author = message.getAuthor();
                    	byte[] content = message.getContent();
                    	content = cipherService.decrypt(content);
                    	System.out.println("Enviado por: '" + author + "'" + System.lineSeparator() + content.toString());
                    }
                    else {
                    	System.out.println(response);
                    }
                }
            }
            sc.close();

        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static void add(ObjectOutputStream outStream, String[] command, Scanner sc) throws IOException {
        if (command.length != 3) {
            System.out.println("Error in the command");
        }
        outStream.writeObject(command[1]);
        Path path = Paths.get(command[2]);

        while (!Files.exists(path)) {
            System.out.println("Enter the path to the wine image: ");
            path = Paths.get(sc.nextLine());
        }
        ImageIcon image = new ImageIcon(command[2]);
        outStream.writeObject(image);

    }

    private static void sell(ObjectOutputStream outStream, String[] command, CipherService cipherService) throws IOException {
        if (command.length != 4) {
            System.out.println("Error in the command");
        }
        String wine = command[1];
        double value = Double.parseDouble(command[2]);
        int quantity = Integer.parseInt(command[3]);
        WineTransaction t = new WineTransaction(wine, user, quantity, value, Type.SELL);
        //Verificar se é so usar sign(t) ou se preciso da privatekey
        SignedObject signedTransaction = cipherService.sign(t);
        outStream.writeObject(signedTransaction);
    }

    private static void view(ObjectOutputStream outStream, String[] command) throws IOException {
        if (command.length != 2) {
            System.out.println("Error in the command");
        }
        outStream.writeObject(command[1]);
    }


    private static void buy(ObjectOutputStream outStream, ObjectInputStream inStream, String[] command, CipherService cipherService) throws IOException {
        if (command.length != 4) {
            System.out.println("Error in the command");
        }
        String wine = command[1];
        String sellerID = command[2];
        int quantity = Integer.parseInt(command[3]);
        
        outStream.writeObject(wine);
        outStream.writeObject(sellerID);
        double value = inStream.readDouble();
        
        WineTransaction t = new WineTransaction(wine, user, quantity, value, Type.SELL);
        SignedObject signedTransaction = cipherService.sign(t);
        outStream.writeObject(signedTransaction);

    }

    private static void classify(ObjectOutputStream outStream, String[] command) throws IOException {
        if (command.length != 3) {
            System.out.println("Error in the command");
        }
        outStream.writeObject(command[1]);
        outStream.writeObject(command[2]);
    }

    private static void talk(ObjectOutputStream outStream, String[] command, CipherService cipherService)
            throws IOException {
        if (command.length != 3) {
            System.out.println("Error in the command");
        }
        String receiverId = command[1];
        String message = command[2];
        byte[] encryptedMessage = message.getBytes();
        encryptedMessage = cipherService.encrypt(encryptedMessage, receiverId);
        outStream.writeObject(receiverId);
        outStream.writeObject(encryptedMessage);
    }
}
