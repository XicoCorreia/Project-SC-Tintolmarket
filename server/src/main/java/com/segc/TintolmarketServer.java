/**
 *
 */
package com.segc;

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
    private final int port;
    private final Path userCredentials;
    private Map<String, User> users = new HashMap<>();
    private Map<String, String> credentials;
    private File winesFile;
    private Map<String, Wine> wines;
    private File wineSaleFile;
    private Map<String, WineSale> winesSale;
    private static final String BADLY_FORMED_START = "Badly formed Server start\n";

    private static String USER_EXCEPTION_MESSAGE(String recipient) {
        return "There is no user '" + recipient + "'.";
    }

    private static String NEW_WINE_EXCEPTION_MESSAGE(String wine) {
        return "There is already a wine named " + wine + ".";
    }

    private static String NO_WINE_EXCEPTION_MESSAGE(String wine) {
        return "There is no wine '" + wine + "'.";
    }

    private static String INSUFFICIENT_BALANCE_EXCEPTION_MESSAGE(double total, double balance) {
        return "Not have enough money. The total cost is " + total + "$$ and you have " + balance + "$$.";
    }

    private static String NO_SUCH_QUANTITY_EXCEPTION_MESSAGE(int quantity) {
        return "Tried to buy more units than possible. There are " + quantity + " units available.";
    }

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
        TintolmarketServer tms = null;
        if (args.length > 1) {
            System.out.println(BADLY_FORMED_START);
        } else if (args.length == 1) {
            int n = 0;
            try {
                n = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println(BADLY_FORMED_START);
                System.exit(1);
            }
            tms = new TintolmarketServer(n);
        } else {
            tms = new TintolmarketServer();
        }
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

    public void add(String wine, Image image) throws WineException {
        if (!this.wines.containsKey(wine)) {
            this.wines.put(wine, new Wine(wine, image));
            this.addWineToFile(wine, image);
            return;
        }
        throw new WineException(NEW_WINE_EXCEPTION_MESSAGE(wine));
    }

    public void sell(String wine, int value, int quantity, User user) throws WineException {
        if (this.wines.containsKey(wine)) {
            this.winesSale.put(wine, new WineSale(this.wines.get(wine), user, value, quantity));
            return;
        }
        throw new WineException(NO_WINE_EXCEPTION_MESSAGE(wine));
    }

    public String view(String wine) throws WineException {
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
        throw new WineException(NO_WINE_EXCEPTION_MESSAGE(wine));
    }

    public void buy(String wine, String seller, int quantity, String buyer)
            throws WineException, NoSuchQuantityException, InsufficientBalanceException {
        User b = this.users.get(buyer);
        User s = this.users.get(seller);
        if (!this.winesSale.containsKey(wine)) {
            throw new WineException(NO_WINE_EXCEPTION_MESSAGE(wine));
        }
        WineSale wineSale = this.winesSale.get(wine);
        double total = wineSale.getValue() * quantity;
        if (wineSale.getQuantity() < quantity) {
            throw new NoSuchQuantityException(NO_SUCH_QUANTITY_EXCEPTION_MESSAGE(wineSale.getQuantity()));
        } else if (total > b.getBalance()) {
            throw new InsufficientBalanceException(INSUFFICIENT_BALANCE_EXCEPTION_MESSAGE(total, b.getBalance()));
        }
        wineSale.removeQuantity(quantity);
        s.addBalance(total);
        b.removeBalance(total);
    }

    public double wallet(String clientId) {
        return this.users.get(clientId).getBalance();
    }

    public void classify(String wine, int stars) throws WineException {
        if (this.wines.containsKey(wine)) {
            this.wines.get(wine).addRating(stars);
            return;
        }
        throw new WineException(NO_WINE_EXCEPTION_MESSAGE(wine));
    }

    public void talk(String recipient, String message, String sender) throws UserException {
        if (users.containsKey(recipient)) {
            users.get(recipient).addMessage(new Message(sender, message));
            return;
        }
        throw new UserException(USER_EXCEPTION_MESSAGE(recipient));
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
