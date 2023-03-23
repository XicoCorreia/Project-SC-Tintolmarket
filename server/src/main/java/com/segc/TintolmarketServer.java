/**
 *
 */
package com.segc;

import com.segc.exception.DuplicateElementException;

import java.awt.Image;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author fc54685 Francisco Correia
 * @author fc55955 Alexandre Fonseca
 * @author fc56272 Filipe Egipto
 */
public class TintolmarketServer {
    private static final int DEFAULT_PORT = 12345;

    /* Usage: String.format(NO_SUCH_ELEMENT_EXCEPTION_MESSAGE, x.getClass().getSimpleName(), x.getName()) */
    private static final String DUPLICATE_ELEMENT_EXCEPTION_MESSAGE = "There is already a %s named '%s'.";
    private static final String NO_SUCH_ELEMENT_EXCEPTION_MESSAGE = "The %s named '%s' does not exist.";
    private static final String INSUFFICIENT_BALANCE_EXCEPTION_MESSAGE = "Insufficient balance: needed %.2f; got %.2f";
    private static final String INVALID_QUANTITY_EXCEPTION_MESSAGE
            = "Requested %d units but there are %d units available.";
    private final int port;
    private final Path userCredentials;
    private Map<String, User> users = new HashMap<>();
    private Map<String, String> credentials;
    private File winesFile;
    private Map<String, Wine> wines;
    private File wineSaleFile;
    private Map<String, WineSale> winesSale;

    public TintolmarketServer(int port) {
        this(port, Configuration.getInstance().getValue("userCredentials"));
    }

    public TintolmarketServer(int port, String userCredentialsFilename) {
        this.port = port;
        userCredentials = Path.of(userCredentialsFilename);
        File f = userCredentials.toFile();

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

    public void add(String wine, Image image) {
        if (!this.wines.containsKey(wine)) {
            this.wines.put(wine, new Wine(wine, image));
            this.addWineToFile(wine, image);
        } else {
            throw new DuplicateElementException();
        }
    }

    public void sell(String wine, int value, int quantity, User user) {
        if (this.wines.containsKey(wine)) {
            this.winesSale.put(wine, new WineSale(this.wines.get(wine), user, value, quantity));
        } else {
            throw new NoSuchElementException();
        }
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
        throw new NoSuchElementException();
    }

    public void buy(String wine, String seller, int quantity, String buyer) {
        User b = this.users.get(buyer);
        User s = this.users.get(seller);
        if (!this.winesSale.containsKey(wine)) {
            throw new NoSuchElementException();
        }
        WineSale wineSale = this.winesSale.get(wine);
        double total = wineSale.getValue() * quantity;
        if (wineSale.getQuantity() < quantity) {
            throw new IllegalArgumentException(INVALID_QUANTITY_EXCEPTION_MESSAGE);
        } else if (total > b.getBalance()) {
            throw new IllegalArgumentException(INSUFFICIENT_BALANCE_EXCEPTION_MESSAGE);
        }
        wineSale.removeQuantity(quantity);
        s.addBalance(total);
        b.removeBalance(total);
    }

    public double wallet(String clientId) {
        return this.users.get(clientId).getBalance();
    }

    public void classify(String wine, int stars) {
        if (this.wines.containsKey(wine)) {
            this.wines.get(wine).addRating(stars);
            return;
        }
        throw new NoSuchElementException();
    }

    public void talk(String recipient, String message, String sender) {
        if (users.containsKey(recipient)) {
            users.get(recipient).addMessage(new Message(sender, message));
            return;
        }
        throw new NoSuchElementException();
    }

    public Message read(String clientId) {
        return users.get(clientId).readMessage();
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
                sessionUser = users.get(clientId);
                if (sessionUser == null) {
                    sessionUser = new User(clientId);
                    users.put(clientId, sessionUser);
                    isAuthenticated = true;
                    AuthenticationService.getInstance().registerUser(clientId, password);
                } else {
                    isAuthenticated = AuthenticationService.getInstance().authenticateUser(clientId, password);
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
