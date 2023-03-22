package com.segc;

/**
 * @author fc54685 Francisco Correia
 * @author fc55955 Alexandre Fonseca
 * @author fc56272 Filipe Egipto
 */
class UserException extends Exception {

    private static final long serialVersionUID = 2787527871214726700L;

    /**
     * Creates a new {@code UserException} with the given message.
     *
     * @param message the exception description.
     */
    public UserException(String message){ super(message); }
}
