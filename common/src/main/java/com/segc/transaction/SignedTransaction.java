package com.segc.transaction;

import java.io.IOException;
import java.security.*;
import java.util.Base64;

/**
 * @author fc54685 Francisco Correia
 * @author fc55955 Alexandre Fonseca
 * @author fc56272 Filipe Egipto
 */
public class SignedTransaction implements Transaction {
    private static final long serialVersionUID = 2428409078150592477L;
    private final SignedObject signedObject;
    transient private Transaction t;

    public SignedTransaction(SignedObject signedObject) {
        this.signedObject = signedObject;
    }

    public SignedObject getSignedObject() {
        return signedObject;
    }

    public Transaction getTransaction() {
        if (t == null) {
            try {
                t = (Transaction) signedObject.getObject();
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        return t;
    }

    @Override
    public String getItemId() {
        return getTransaction().getItemId();
    }

    @Override
    public int getUnitCount() {
        return getTransaction().getUnitCount();
    }

    @Override
    public double getUnitPrice() {
        return getTransaction().getUnitPrice();
    }

    @Override
    public String getAuthorId() {
        return getTransaction().getAuthorId();
    }

    @Override
    public Type getType() {
        return getTransaction().getType();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(1024);
        sb.append(String.format("Signed Transaction:%n" +
                        "  Algorithm: %s%n" +
                        "  Signature: %s%n",
                signedObject.getAlgorithm(),
                Base64.getUrlEncoder().encodeToString(signedObject.getSignature())));
        getTransaction().toString()
                        .lines()
                        .forEach(l -> sb.append("  ").append(l).append(System.lineSeparator()));
        return sb.toString();
    }
}
