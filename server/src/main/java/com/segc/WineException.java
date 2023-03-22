package com.segc;

/**
 * @author fc54685 Francisco Correia
 * @author fc55955 Alexandre Fonseca
 * @author fc56272 Filipe Egipto
 */
class WineException extends Exception {

    private static final long serialVersionUID = -2290961260150102110L;

    /**
     * Creates a new {@code WineException} with the given message.
     *
     * @param message the exception description.
     */
    public WineException(String message) { super(message); }
}
