/**
 *
 */
package com.segc.users;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;

import com.segc.Message;

/**
 * @author fc54685 Francisco Correia
 * @author fc55955 Alexandre Fonseca
 * @author fc56272 Filipe Egipto
 */
public class User implements Serializable {
    private static final long serialVersionUID = -7564097017110086431L;
    private final String clientId;
    private final Queue<Message> messages;
    private double balance;

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
    public User(String clientId, double balance, Queue<Message> messages) {
        this.clientId = clientId;
        this.balance = balance;
        this.messages = messages;
    }

    /**
     * @return The ID of this User.
     */
    public String getId() {
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
    public void addBalance(double balance) throws IllegalArgumentException {
        if (balance < 0) {
            throw new IllegalArgumentException("Balance amount must be a positive integer.");
        }
        this.balance += balance;
    }

    /**
     * @param balance The balance to remove from this client's balance.
     */
    public void removeBalance(double balance) {
        if (balance < 0) {
            throw new IllegalArgumentException("Balance amount must be a positive integer.");
        }
        this.balance -= balance;
    }

    /**
     * @param message The {@link Message} to add to this client's messages.
     */
    public void addMessage(Message message) {
        messages.add(message);
    }

    /**
     * @param senderId The id of the sender ({@link User}) of the message.
     * @param message  The content of the message to add to this client's messages.
     */
    public void addMessage(String senderId, byte[] message) {
        messages.add(new Message(senderId, message));
    }

    /**
     * Returns a {@code message} from the message queue in FIFO order.
     * The message is discarded after calling this method.
     *
     * @return a message from the message queue in FIFO order
     * @throws NoSuchElementException if the message queue is empty
     */
    public Message readMessage() throws NoSuchElementException {
        return messages.remove();
    }

}
