package com.segc.transaction;

import java.io.Serializable;

/**
 * @author fc54685 Francisco Correia
 * @author fc55955 Alexandre Fonseca
 * @author fc56272 Filipe Egipto
 */
public interface Transaction extends Serializable {
    String getItemId();

    int getUnitCount();

    double getUnitPrice();

    String getAuthorId();

    Type getType();

    enum Type {
        BUY,
        SELL
    }
}

