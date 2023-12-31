package com.segc.users;

import com.segc.Configuration;
import com.segc.Message;
import com.segc.exception.DuplicateElementException;
import com.segc.services.DataPersistenceService;

import java.io.FileNotFoundException;
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
    private final DataPersistenceService dps;

    public UserCatalog(DataPersistenceService dps) {
        this.users = new HashMap<>();
        this.dps = dps;
        this.userDataDir = Configuration.getInstance().getValue("userDataDir");
        try {
            dps.getObjectsAndVerify(User.class, userDataDir).forEach(user -> users.put(user.getId(), user));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void add(String clientId) throws DuplicateElementException {
        if (users.containsKey(clientId)) {
            throw new DuplicateElementException();
        }
        User user = new User(clientId);
        users.put(clientId, user);
        dps.putObjectAndDigest(user, Path.of(userDataDir, clientId));
    }

    public double getBalance(String clientId) throws NoSuchElementException {
        return Optional.ofNullable(users.get(clientId)).orElseThrow().getBalance();
    }

    public void addBalance(String clientId, double balance) throws NoSuchElementException, IllegalArgumentException {
        User user = Optional.ofNullable(users.get(clientId)).orElseThrow();
        user.addBalance(balance);
        dps.putObjectAndDigest(user, Path.of(userDataDir, user.getId()));
    }

    public void removeBalance(String clientId, double balance)
            throws NoSuchElementException, IllegalArgumentException {
        User user = Optional.ofNullable(users.get(clientId)).orElseThrow();
        user.removeBalance(balance);
        dps.putObjectAndDigest(user, Path.of(userDataDir, clientId));
    }

    public void transferBalance(String senderId, String recipientId, double amount)
            throws NoSuchElementException, IllegalArgumentException {
        // BOTH conditions must be true before we transfer the balance
        if (users.containsKey(senderId) && users.containsKey(recipientId)) {
            removeBalance(senderId, amount);
            addBalance(recipientId, amount);
        } else {
            throw new NoSuchElementException();
        }

    }

    public void talk(String senderId, String recipientId, byte[] message) throws NoSuchElementException {
        User recipient = Optional.ofNullable(users.get(recipientId)).orElseThrow();
        recipient.addMessage(senderId, message);
        dps.putObjectAndDigest(recipient, Path.of(userDataDir, recipientId));
    }

    public Message read(String clientId) throws NoSuchElementException, IllegalArgumentException {
        User recipient = Optional.ofNullable(users.get(clientId)).orElseThrow();
        try {
            Message message = recipient.readMessage();
            dps.putObjectAndDigest(recipient, Path.of(userDataDir, clientId));
            return message;
        } catch (NoSuchElementException e) {
            String msg = String.format("User '%s' has no unread messages.", clientId);
            throw new IllegalArgumentException(msg);
        }

    }
}
