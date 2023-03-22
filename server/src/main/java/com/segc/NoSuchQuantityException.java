package com.segc;

/**
 * @author fc54685 Francisco Correia
 * @author fc55955 Alexandre Fonseca
 * @author fc56272 Filipe Egipto
 */
class NoSuchQuantityException extends Exception {

    private static final long serialVersionUID = 4496503038474815495L;

    /**
     * Creates a new {@code NoSuchQuantityException} with the given message.
     *
     * @param message the exception description.
     */
    public NoSuchQuantityException(String message) { super(message); }
}
