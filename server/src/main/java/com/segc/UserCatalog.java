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
    private final Map<String, User> users;
    private final DataPersistenceService<User> dps;
    public final String userDataDir;

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
        return Optional.of(users.get(clientId)).orElseThrow().getBalance();
    }

    public double addBalance(String clientId, double balance) throws NoSuchElementException, IllegalArgumentException {
        User user = Optional.of(users.get(clientId)).orElseThrow();
        double newBalance = user.addBalance(balance);
        dps.putObject(user, Path.of(userDataDir, user.getId()));
        return newBalance;
    }

    public double removeBalance(String clientId, double balance)
            throws NoSuchElementException, IllegalArgumentException {
        User user = Optional.of(users.get(clientId)).orElseThrow();
        double newBalance = user.removeBalance(balance);
        dps.putObject(user, Path.of(userDataDir, clientId));
        return newBalance;
    }

    public void talk(String senderId, String recipientId, String message) throws NoSuchElementException {
        User recipient = Optional.of(users.get(recipientId)).orElseThrow();
        recipient.addMessage(senderId, message);
        dps.putObject(recipient, Path.of(userDataDir, recipientId));
    }

    public Message read(String clientId) {
        User recipient = Optional.of(users.get(clientId)).orElseThrow();
        Message message = recipient.readMessage();
        dps.putObject(recipient, Path.of(userDataDir, clientId));
        return message;
    }
}
