/**
 * 
 */
package pt.ul.fc;

import java.util.LinkedList;
import java.util.List;

/**
 * @author fc54685 Francisco Correia
 * @author fc55955 Alexandre Fonseca
 * @author fc56272 Filipe Egipto
 *
 */
public class User {
    private String clientId;
    private double balance;
    private List<String> messages;

    /**
     * @param clientId The client's id.
     */
    public User(String clientId) {
        this(clientId, 200.0);
    }

    /**
     * @param clientId The client's id.
     * @param balance  The initial balance for this client.
     */
    public User(String clientId, double balance) {

        this(clientId, balance, new LinkedList<>());
    }

    /**
     * @param clientId The client's id.
     * @param balance  The initial balance for this client.
     * @param messages The initial messages for this client.
     */
    public User(String clientId, double balance, List<String> messages) {
        this.clientId = clientId;
        this.balance = balance;
        this.messages = messages;
    }
}
