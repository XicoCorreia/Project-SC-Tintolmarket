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
            
            System.out.print("Authenticated.\n" + COMMANDS);
            
            Scanner sc = new Scanner(System.in);

            while (sc.hasNext()) {
                String command = sc.next();
                String[] c = command.split(" ");
                switch (c[0]) {
                    case "add":
                    case "a":
                    	add(outStream, c);
                        break;
                    case "sell":
                    case "s":
                        sell(outStream, c);
                        break;
                    case "view":
                    case "v":
                        view(outStream, c);
                        break;
                    case "buy":
                    case "b":
                        buy(outStream, c);
                        break;
                    case "wallet":
                    case "w":
                        wallet(outStream, c);
                        break;
                    case "classify":
                    case "c":
                        classify(outStream, c);
                        break;
                    case "talk":
                    case "t":
                    	talk(outStream, c);
                        break;
                    case "read":
                    case "r":
                    	read(outStream, c);
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
    
    private static void add(ObjectOutputStream outStream, String[] command) throws IOException {
        if (command.length != 3) {
            System.out.println("Error in the command");
        }
        outStream.writeObject(command[0]);
        outStream.writeObject(command[1]);
        uploadFile(new File(command[2]), outStream);
    }
    
    private static void sell(ObjectOutputStream outStream, String[] command) throws IOException {
        if (command.length != 4) {
            System.out.println("Error in the command");
        }
        outStream.writeObject(command[0]);
        outStream.writeObject(command[1]);
        outStream.writeObject(command[2]);
        outStream.writeObject(command[3]);
    }
    
    private static void view(ObjectOutputStream outStream, String[] command) throws IOException {
        if (command.length != 2) {
            System.out.println("Error in the command");
        }
        outStream.writeObject(command[0]);
        outStream.writeObject(command[1]);
    }
    
    
    private static void buy(ObjectOutputStream outStream, String[] command) throws IOException {
        if (command.length != 4) {
            System.out.println("Error in the command");
        }
        outStream.writeObject(command[0]);
        outStream.writeObject(command[1]);
        outStream.writeObject(command[2]);
        outStream.writeObject(command[3]);
    }
    
    private static void wallet(ObjectOutputStream outStream, String[] command) throws IOException {
        if (command.length != 1) {
            System.out.println("Error in the command");
        }
        outStream.writeObject(command[0]);
    }
    
    private static void classify(ObjectOutputStream outStream, String[] command) throws IOException {
        if (command.length != 3) {
            System.out.println("Error in the command");
        }
        outStream.writeObject(command[0]);
        outStream.writeObject(command[1]);
        outStream.writeObject(command[2]);
    }
    
    private static void talk(ObjectOutputStream outStream, String[] command) throws IOException {
        if (command.length != 3) {
            System.out.println("Error in the command");
        }
        outStream.writeObject(command[0]);
        outStream.writeObject(command[1]);
        outStream.writeObject(command[2]);
    }
    
    private static void read(ObjectOutputStream outStream, String[] command) throws IOException {
        if (command.length != 1) {
            System.out.println("Error in the command");
        }
        outStream.writeObject(command[0]);
    }
    
    private static void uploadFile(File file, ObjectOutputStream out) throws IOException {
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
