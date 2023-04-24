/**
 *
 */
package com.segc;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Scanner;

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
            "- read%n" +
            "- quit%n");

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.printf("Invalid arguments.%n" +
                    "Use: java -jar Tintolmarket.jar <serverAddress> <clientId> [password]%n");
            System.exit(1);
        }

        String serverAddress = args[0];
        String user = args[1];
        String pass = args.length > 2 ? args[2] : null;
        String host = serverAddress;
        int port = DEFAULT_PORT;

        String[] hostPort = serverAddress.split(":");
        if (hostPort.length == 2) {
            host = hostPort[0];
            port = Integer.parseInt(hostPort[1]);
        }

        try (Socket socket = new Socket(host, port);
             ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream())) {

            Scanner sc = new Scanner(System.in);

            if (pass == null) {
                System.out.println("Enter your password: ");
                pass = sc.nextLine();
            }
            outStream.writeObject(user);
            outStream.writeObject(pass);
            boolean isAuthenticated = (Boolean) inStream.readObject();
            if (!isAuthenticated) {
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
                        sell(outStream, c);
                        break;
                    }
                    case VIEW: {
                        view(outStream, c);
                        break;
                    }
                    case BUY: {
                        buy(outStream, c);
                        break;
                    }
                    case CLASSIFY: {
                        classify(outStream, c);
                        break;
                    }
                    case TALK: {
                        talk(outStream, c);
                        break;
                    }
                    case EXIT: {
                        isExiting = true;
                        break;
                    }
                    case WALLET:
                    case READ: {
                        break;
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
                    System.out.println(response);
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

    private static void sell(ObjectOutputStream outStream, String[] command) throws IOException {
        if (command.length != 4) {
            System.out.println("Error in the command");
        }
        outStream.writeObject(command[1]);
        outStream.writeObject(command[2]);
        outStream.writeObject(command[3]);
    }

    private static void view(ObjectOutputStream outStream, String[] command) throws IOException {
        if (command.length != 2) {
            System.out.println("Error in the command");
        }
        outStream.writeObject(command[1]);
    }


    private static void buy(ObjectOutputStream outStream, String[] command) throws IOException {
        if (command.length != 4) {
            System.out.println("Error in the command");
        }
        outStream.writeObject(command[1]);
        outStream.writeObject(command[2]);
        outStream.writeObject(command[3]);
    }

    private static void classify(ObjectOutputStream outStream, String[] command) throws IOException {
        if (command.length != 3) {
            System.out.println("Error in the command");
        }
        outStream.writeObject(command[1]);
        outStream.writeObject(command[2]);
    }

    private static void talk(ObjectOutputStream outStream, String[] command) throws IOException {
        if (command.length != 3) {
            System.out.println("Error in the command");
        }
        outStream.writeObject(command[1]);
        outStream.writeObject(command[2]);
    }
}
