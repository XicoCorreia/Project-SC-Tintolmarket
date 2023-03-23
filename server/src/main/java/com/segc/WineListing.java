/**
 *
 */
package com.segc;

import java.io.Serializable;

/**
 * @author fc54685 Francisco Correia
 * @author fc55955 Alexandre Fonseca
 * @author fc56272 Filipe Egipto
 */
public class WineListing implements Serializable {
    private static final long serialVersionUID = -224700426049399779L;
    private final String sellerId;
    private final double costPerUnit;
    private int quantity;

    public WineListing(String sellerId, double costPerUnit, int quantity) {
        this.sellerId = sellerId;
        this.costPerUnit = costPerUnit;
        this.quantity = quantity;
    }

    /**
     * @return The available quantity for this {@link WineListing}.
     */
    public int getQuantity() {
        return quantity;
    }

    /**
     * @param quantity The quantity to add to this {@link WineListing}.
     */
    public void addQuantity(int quantity) {
        this.quantity += quantity;
    }

    /**
     * @param quantity The quantity to remove from this {@link WineListing}.
     */
    public void removeQuantity(int quantity) {
        this.quantity -= quantity;
    }

    /**
     * @return The clientId of the seller ({@link User}) of this {@link WineListing}.
     */
    public String getSellerId() {
        return this.sellerId;
    }

    /**
     * @return The price of this {@link WineListing}.
     */
    public double getCostPerUnit() {
        return this.costPerUnit;
    }
}

