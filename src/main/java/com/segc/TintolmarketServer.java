/**
 * 
 */
package com.segc;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author fc54685 Francisco Correia
 * @author fc55955 Alexandre Fonseca
 * @author fc56272 Filipe Egipto
 *
 */
public class TintolmarketServer 
{
    private final int port;
    private final File usersFile;
    private final Map<String, String> users;

    public TintolmarketServer() 
    {
        this(12345, "users.txt");
    }

    public TintolmarketServer(int serverPort) 
    {
        this(serverPort, "users.txt");
    }

    public TintolmarketServer(int serverPort, String usersFileName) {
        this.port = serverPort;
        this.usersFile = new File(usersFileName);
        try
        {
            if (usersFile.createNewFile())
            {
                System.out.println("getUsersFromFile: ficheiro '" + usersFileName + "' criado");
            }
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        this.users = getUsersFromFile(usersFile);
    }

    private static Map<String, String> getUsersFromFile(File file)
    {
        try (BufferedReader usersReader = new BufferedReader(new FileReader(file)))
        {
            return usersReader.lines()
                    .map(s -> s.split(":", 2))
                    .collect(Collectors.toMap(x -> x[0], x -> x[1]));
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    private void addUserToFile(String user, String password)
    {
        try (FileWriter fw = new FileWriter(usersFile, true);
             BufferedWriter bw = new BufferedWriter(fw))
        {
            bw.write(user + ":" + password);
            bw.newLine();
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) 
    {
        TintolmarketServer tms = null;

        if (args.length > 1) {
            System.out.println("Badly formed Server start\n");
        }
        else if (args.length == 1) {
            int n = 0;
            try 
            {
                n = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) 
            {
                System.out.println("Badly formed Server start\n");
                System.exit(1);
            }
            tms = new TintolmarketServer(n);
        } else 
        {
            tms = new TintolmarketServer();
        }
        tms.startServer();
    }

    public void startServer()
    {
        try (ServerSocket sSoc = new ServerSocket(this.port))
        {
            while (true)
            {
                try
                {
                    Socket inSoc = sSoc.accept();
                    ServerThread newServerThread = new ServerThread(inSoc);
                    newServerThread.start();
                } catch (IOException e)
                {
                    e.printStackTrace();
                }

            }
        } catch (IOException e)
        {
            System.err.println(e.getMessage());
            System.exit(-1);
        }
    }

    class ServerThread extends Thread
    {

        private final Socket socket;

        ServerThread(Socket inSoc)
        {
            socket = inSoc;
            System.out.println("thread do server para cada cliente");
        }

        @Override
        public void run()
        {

        }
    }
}
