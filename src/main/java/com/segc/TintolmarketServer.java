/**
 *
 */
package com.segc;

import java.awt.Image;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author fc54685 Francisco Correia
 * @author fc55955 Alexandre Fonseca
 * @author fc56272 Filipe Egipto
 */
public class TintolmarketServer {
    private final int port;
    private final File usersFile;
    private final Map<String, String> usersLog;
    private Map<String, User> users;
    private File winesFile;
    private Map<String, Wine> wines;
    private File wineSaleFile;
    private Map<String, WineSale> winesSale;

    public TintolmarketServer() {
        this(12345);
    }

    public TintolmarketServer(int serverPort) {
        this(serverPort, "users.txt");
    }

    public TintolmarketServer(int serverPort, String usersFileName) {
        this.port = serverPort;
        this.usersFile = new File(usersFileName);
        try {
            if (usersFile.createNewFile()) {
                System.out.println("getUsersFromFile: ficheiro '" + usersFileName + "' criado");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.usersLog = getUsersFromFile(usersFile);
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

    private void addUserToFile(String user, String password) {
        try (FileWriter fw = new FileWriter(usersFile, true);
             BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write(user + ":" + password);
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

    //Falta colocar unidades a 0 e sem classificação
    //Fazer log para o ficheiro
    public boolean add(String wine, Image image) {
        if (!this.wines.containsKey(wine)) {
            this.wines.put(wine, new Wine(wine, image));
            return true;
        }
        return false; //Erro
    }

    public boolean sell(String wine, int value, int quantity, User user) {
        if (this.wines.containsKey(wine)) {
            this.winesSale.put(wine, new WineSale(this.wines.get(wine),
                    user, value, quantity));
            return true;
        }
        return false; //Erro
    }

    public String view(String wine) {
        if (this.winesSale.containsKey(wine)) {
            WineSale wineSale = this.winesSale.get(wine);
            Wine w = this.wines.get(wine);
            String s = "Wine " + wine + ":\n" + w.getImage() + "\nAverage classification: "
                    + w.getClassification();
            int qt = wineSale.getQuantity();
            if (qt > 0) {
                return s + "\nSelling user: " + wineSale.getUser().getID() +
                        "\nPrice:" + wineSale.getValue() + "\nQuantity available:" + qt;
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

    public double wallet(String user) {
        return this.users.get(user).getBalance();
    }

    class ServerThread extends Thread {

        private final Socket socket;

        ServerThread(Socket inSoc) {
            this.socket = inSoc;
            System.out.println("thread do server para cada cliente");
        }

        @Override
        public void run() {
            try {
                ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream());
                String user, password;

                try {
                    user = (String) inStream.readObject();
                    password = (String) inStream.readObject();
                    System.out.println("thread: depois de receber a password e o user");
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }

                if (!usersLog.containsKey(user)) {
                    usersLog.put(user, password);
                    addUserToFile(user, password);
                    users.put(user, new User(user));
                    outStream.writeObject(true);
                } else if (password.equals(usersLog.get(user))) {
                    outStream.writeObject(true);
                } else {
                    outStream.writeObject(false);
                }
                socket.shutdownOutput();
                socket.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
