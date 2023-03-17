/**
 *
 */
package com.segc;

import java.util.LinkedList;
import java.util.List;

/**
 * @author fc54685 Francisco Correia
 * @author fc55955 Alexandre Fonseca
 * @author fc56272 Filipe Egipto
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

    /**
     * @return The ID of this User.
     */
    public String getID() {
        return this.clientId;
    }

    /**
     * @return The balance of this User.
     */
    public double getBalance() {
        return this.balance;
    }

    /**
     * @param balance The balance to add to this client's balance.
     */
    public void addBalance(double balance) {
        this.balance += balance;
    }

    /**
     * @param balance The balance to remove from this client's balance.
     */
    public void removeBalance(double balance) {
        this.balance -= balance;
    }

    /**
     * @param message The message to add to this client's messages.
     */
    public void addMessage(String message) {
        this.messages.add(message);
    }

    /**
     * Eliminates all the messages of this client after reading them.
     * 
     * @return The messages of this client.
     */
    public String readMessages() {
        StringBuilder sb = new StringBuilder();
        for(String m: this.messages) {
            String[] s = m.split(":");
            sb.append("Message from " + s[0] + ":\n" + s[1] + "\n");
        }
        this.messages.clear();
        return sb.toString();
    }
}
