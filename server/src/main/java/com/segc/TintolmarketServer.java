/**
 *
 */
package com.segc;

import java.awt.Image;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author fc54685 Francisco Correia
 * @author fc55955 Alexandre Fonseca
 * @author fc56272 Filipe Egipto
 */
public class TintolmarketServer {
    private final int port;
    private Map<String, User> users;
    private File winesFile;
    private Map<String, Wine> wines;
    private File wineSaleFile;
    private Map<String, WineSale> winesSale;
    private final Path userCredentials;

    public TintolmarketServer() {
        this(12345);
    }

    public TintolmarketServer(int port) {
        this(port, Configuration.getInstance().getValue("userCredentials"));
    }

    public TintolmarketServer(int port, String userCredentialsFilename) {
        Configuration conf = Configuration.getInstance();
        this.port = port;
        userCredentials = Path.of(userCredentialsFilename);

        try {
            if (userCredentials.toFile().createNewFile()) {
                System.out.println("ficheiro '" + userCredentialsFilename + "' criado");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Map<String, String> getUsersFromFile(File file) {
        try (BufferedReader usersReader = new BufferedReader(new FileReader(file))) {
            return usersReader.lines().map(s -> s.split(":", 2)).collect(Collectors.toMap(x -> x[0], x -> x[1]));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        TintolmarketServer tms = null;

        if (args.length > 1) {
            System.out.println("Badly formed Server start\n");
        } else if (args.length == 1) {
            int n = 0;
            try {
                n = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("Badly formed Server start\n");
                System.exit(1);
            }
            tms = new TintolmarketServer(n);
        } else {
            tms = new TintolmarketServer();
        }
        tms.startServer();
    }

    private void addUserCredentials(String clientId, String password) throws IOException {
        synchronized (userCredentials) {
            Files.writeString(userCredentials,
                    clientId + ":" + password + System.lineSeparator(),
                    StandardOpenOption.APPEND,
                    StandardOpenOption.CREATE);
        }
    }

    private void addWineToFile(String wine, Image image) {
        try (FileWriter fw = new FileWriter(winesFile, true); BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write(wine + ":" + image); //TODO
            bw.newLine();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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

    //Falta colocar unidades a 0
    //Fazer log para o ficheiro
    public boolean add(String wine, Image image) {
        if (!this.wines.containsKey(wine)) {
            this.wines.put(wine, new Wine(wine, image));
            this.addWineToFile(wine, image);
            return true;
        }
        return false; //Erro
    }

    public boolean sell(String wine, int value, int quantity, User user) {
        if (this.wines.containsKey(wine)) {
            this.winesSale.put(wine, new WineSale(this.wines.get(wine), user, value, quantity));
            return true;
        }
        return false; //Erro
    }

    public String view(String wine) {
        if (this.winesSale.containsKey(wine)) {
            WineSale wineSale = this.winesSale.get(wine);
            Wine w = this.wines.get(wine);
            String s = "Wine " + wine + ":\n" + w.getImage() + "\nAverage classification: " + w.getRating();
            int qt = wineSale.getQuantity();
            if (qt > 0) {
                return s + "\nSelling user: " + wineSale.getUser().getID() + "\nPrice:" + wineSale.getValue() +
                        "\nQuantity available:" + qt;
            }
            return s;
        }
        return "false"; //Erro
    }

    public boolean buy(String wine, String seller, int quantity, String buyer) {
        User b = this.users.get(buyer);
        User s = this.users.get(seller);
        if (!this.winesSale.containsKey(wine)) {
            return false; //Erro - não existe o vinho
        }
        WineSale wineSale = this.winesSale.get(wine);
        double total = wineSale.getValue() * quantity;
        if (wineSale.getQuantity() < quantity) {
            return false; //Erro - não existem unidades suficientes
        } else if (total > b.getBalance()) {
            return false; //Erro - não tem saldo suficiente
        }
        wineSale.removeQuantity(quantity);
        s.addBalance(total);
        b.removeBalance(total);
        return true;
    }

    public double wallet(String clientId) {
        return this.users.get(clientId).getBalance();
    }

    public boolean classify(String wine, int stars) {
        if (this.wines.containsKey(wine)) {
            this.wines.get(wine).addRating(stars);
            return true;
        }
        return false; //Erro
    }

    public boolean talk(String recipient, String message, String sender) {
        if (users.containsKey(recipient)) {
            users.get(recipient).addMessage(new Message(sender, message));
            return true;
        }
        return false; //Erro
    }

    public Message read(String clientId) {
        return users.get(clientId).readMessage();
    }

    class ServerThread extends Thread {

        private final Socket socket;
        private User sessionUser;

        ServerThread(Socket inSoc) {
            this.socket = inSoc;
            System.out.println("thread do server para cada cliente");
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

                sessionUser = users.get(clientId);
                if (sessionUser == null) {
                    sessionUser = new User(clientId);
                    users.put(clientId, sessionUser);
                    addUserCredentials(sessionUser.getID(), password);
                    outStream.writeObject(true);
                } else {
                    outStream.writeObject(validateUser(clientId, password));
                }
                socket.shutdownOutput();
                socket.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean validateUser(String clientId, String password) {
        return false; // TODO
    }
}
