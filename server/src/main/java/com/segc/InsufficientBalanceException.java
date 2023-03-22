package com.segc;

/**
 * @author fc54685 Francisco Correia
 * @author fc55955 Alexandre Fonseca
 * @author fc56272 Filipe Egipto
 */
class InsufficientBalanceException extends Exception {

    private static final long serialVersionUID = 1851746558032987501L;

    /**
     * Creates a new {@code InsufficientBalanceException} with the given message.
     *
     * @param message the exception description.
     */
    public InsufficientBalanceException(String message) { super(message); }
}
