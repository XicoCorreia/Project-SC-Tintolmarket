/**
 *
 */
package com.segc;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

/**
 * @author fc54685 Francisco Correia
 * @author fc55955 Alexandre Fonseca
 * @author fc56272 Filipe Egipto
 */
public class Tintolmarket {

    public static final int DEFAULT_PORT = 12345;

    private static final String COMMANDS = "Available commands:\n" +
            "- add <wine> <image>\n" +
            "- sell <wine> <value> <quantity>\n" +
            "- view <wine>\n" +
            "- buy <wine> <seller> <quantity>\n" +
            "- wallet\n" +
            "- classify <wine> <stars>\n" +
            "- talk <user> <message>\n" +
            "- read\n";


    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Badly formed Server start.\n" +
                    "Use: java -jar Tintolmarket.jar <serverAddress> <userID> [password]");
            System.exit(1);
        }

        String serverAddress = args[0];
        String user = args[1];
        String pass = args.length > 2 ? args[2] : null; // TODO: ask for password if not specified
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
            outStream.writeObject(user);
            outStream.writeObject(pass);
            boolean isAuthenticated = (Boolean) inStream.readObject();
            if (!isAuthenticated) {
                System.out.println("Authentication failed.");
                System.exit(1);
            }
            System.out.println("Authenticated.");

            System.out.print(COMMANDS);
            Scanner sc = new Scanner(System.in);

            while (sc.hasNext()) {
                String command = sc.next();
                String[] c = command.split(" ");
                switch (c[0]) {
                    case "add":
                    case "a":
                        if (c.length != 3) {
                            System.out.println("Error in the command");
                        }
                        outStream.writeObject(c[0]);
                        outStream.writeObject(c[1]);
                        uploadFile(new File(c[2]), outStream);
                        break;
                    case "sell":
                    case "s":
                        if (c.length != 4) {
                            System.out.println("Error in the command");
                            break;
                        }
                        writeCommands(outStream, command);

                        break;
                    case "view":
                    case "v":
                        if (c.length != 2) {
                            System.out.println("Error in the command");
                            break;
                        }
                        writeCommands(outStream, command);

                        break;
                    case "buy":
                    case "b":
                        if (c.length != 4) {
                            System.out.println("Error in the command");
                            break;
                        }
                        writeCommands(outStream, command);

                        break;
                    case "wallet":
                    case "w":
                        if (c.length != 1) {
                            System.out.println("Error in the command");
                            break;
                        }
                        outStream.writeObject(command);

                        break;
                    case "classify":
                    case "c":
                        if (c.length != 3) {
                            System.out.println("Error in the command");
                            break;
                        }
                        writeCommands(outStream, command);

                        break;
                    case "talk":
                    case "t":
                        if (c.length != 3) {
                            System.out.println("Error in the command");
                            break;
                        }
                        writeCommands(outStream, command);

                        break;
                    case "read":
                    case "r":
                        if (c.length != 1) {
                            System.out.println("Error in the command");
                            break;
                        }
                        outStream.writeObject(command);

                        break;
                    default:
                        throw new IllegalArgumentException("Unexpected command: " + command);
                }
            }
            sc.close();

        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

    }

    private static void writeCommands(ObjectOutputStream outStream, String com) {
        String[] c = com.split(" ");

        for (String a : c) {
            try {
                outStream.writeObject(a);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void uploadFile(File file, ObjectOutputStream out) throws IOException {
        try (FileInputStream fin = new FileInputStream(file); InputStream fileStream = new BufferedInputStream(fin)) {
            long fileSize = file.length();
            out.writeObject(fileSize);
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fileStream.read(buffer)) > 0) {
                out.write(buffer, 0, bytesRead);
            }
            out.flush();
        }
    }
}