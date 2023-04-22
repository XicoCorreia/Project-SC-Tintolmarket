package com.segc.exception;

/**
 * @author fc54685 Francisco Correia
 * @author fc55955 Alexandre Fonseca
 * @author fc56272 Filipe Egipto
 */
public class DataIntegrityException extends Exception {
    private static final long serialVersionUID = 1820538674530978702L;

    public DataIntegrityException(String message) {
        super(message);
    }
}
