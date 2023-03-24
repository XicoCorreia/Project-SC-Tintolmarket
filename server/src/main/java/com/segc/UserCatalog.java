package com.segc;

import com.segc.exception.DuplicateElementException;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * @author fc54685 Francisco Correia
 * @author fc55955 Alexandre Fonseca
 * @author fc56272 Filipe Egipto
 */
public class UserCatalog {
    public final String userDataDir;
    private final Map<String, User> users;
    private final DataPersistenceService<User> dps;

    public UserCatalog() {
        this.users = new HashMap<>();
        this.dps = new DataPersistenceService<>();
        this.userDataDir = Configuration.getInstance().getValue("userDataDir");
        dps.getObjects(userDataDir).forEach(user -> users.put(user.getId(), user));
    }

    public void add(String clientId) throws DuplicateElementException {
        if (users.containsKey(clientId)) {
            throw new DuplicateElementException();
        }
        User user = new User(clientId);
        users.put(clientId, user);
        dps.putObject(user, Path.of(userDataDir, clientId));
    }

    public double getBalance(String clientId) throws NoSuchElementException {
        return Optional.ofNullable(users.get(clientId)).orElseThrow().getBalance();
    }

    public void addBalance(String clientId, double balance) throws NoSuchElementException, IllegalArgumentException {
        User user = Optional.ofNullable(users.get(clientId)).orElseThrow();
        user.addBalance(balance);
        dps.putObject(user, Path.of(userDataDir, user.getId()));
    }

    public void removeBalance(String clientId, double balance)
            throws NoSuchElementException, IllegalArgumentException {
        User user = Optional.ofNullable(users.get(clientId)).orElseThrow();
        user.removeBalance(balance);
        dps.putObject(user, Path.of(userDataDir, clientId));
    }

    public void transferBalance(String senderId, String recipientId, double amount) {
        // BOTH conditions must be true before we transfer the balance
        if (users.containsKey(senderId) && users.containsKey(recipientId)) {
            removeBalance(senderId, amount);
            addBalance(recipientId, amount);
        } else {
            throw new NoSuchElementException();
        }

    }

    public void talk(String senderId, String recipientId, String message) throws NoSuchElementException {
        User recipient = Optional.ofNullable(users.get(recipientId)).orElseThrow();
        recipient.addMessage(senderId, message);
        dps.putObject(recipient, Path.of(userDataDir, recipientId));
    }

    public Message read(String clientId) {
        User recipient = Optional.ofNullable(users.get(clientId)).orElseThrow();
        Message message = recipient.readMessage();
        dps.putObject(recipient, Path.of(userDataDir, clientId));
        return message;
    }
}
