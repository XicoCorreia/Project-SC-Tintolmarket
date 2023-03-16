/**
 *
 */
package com.segc;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * @author fc54685 Francisco Correia
 * @author fc55955 Alexandre Fonseca
 * @author fc56272 Filipe Egipto
 */
public class Tintolmarket {
	
	
	
	
    public static void main(String[] args) {
    	if (args.length < 3)
        {
            System.out.println("Badly formed Server start.\n" + 
            				   "Use: Tintolmarket <serverAddress> <userID> [password]");
            System.exit(1);
        }
    	
        String serverAddress = args[0];
        String user = args[1];
        String pass = args[2];
        int port = 12345;
        
        if (serverAddress.split(":").length == 2)
        	port = Integer.parseInt(serverAddress.split(":")[1]);
        
        try (Socket socket = new Socket(serverAddress.split(":")[0], port);
                ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream());
                ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream()))
           {
        	   outStream.writeObject(user);
               outStream.writeObject(pass);
               boolean isAuthenticated = (Boolean) inStream.readObject();
               if (!isAuthenticated)
               {
                   System.out.println("Authentication failed.");
                   System.exit(1);
               }
               System.out.println("Authenticated.");
               
           } catch (IOException | ClassNotFoundException e)
           {
               throw new RuntimeException(e);
           }
        
    }
}
