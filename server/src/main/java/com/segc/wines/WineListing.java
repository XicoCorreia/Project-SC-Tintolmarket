/**
 *
 */
package com.segc.wines;

import java.io.Serializable;

/**
 * @author fc54685 Francisco Correia
 * @author fc55955 Alexandre Fonseca
 * @author fc56272 Filipe Egipto
 */
public class WineListing implements Serializable {
    private static final long serialVersionUID = -224700426049399779L;
    private final String sellerId;
    private double costPerUnit;
    private int quantity;

    public WineListing(String sellerId, double costPerUnit, int quantity) {
        this.sellerId = sellerId;
        setCostPerUnit(costPerUnit);
        addQuantity(quantity);
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
    public void addQuantity(int quantity) throws IllegalArgumentException {
        if (quantity < 0) {
            throw new IllegalArgumentException("Quantity must be a positive integer.");
        }
        this.quantity += quantity;
    }

    /**
     * @param quantity The quantity to remove from this {@link WineListing}.
     */
    public void removeQuantity(int quantity) throws IllegalArgumentException {
        if (quantity < 0) {
            throw new IllegalArgumentException("Quantity must be a positive integer.");
        }
        if (quantity > this.quantity) {
            String msg = String.format("Requested qty. (%d) exceeds available qty. (%d).", quantity, this.quantity);
            throw new IllegalArgumentException(msg);
        }
        this.quantity -= quantity;
    }

    /**
     * @return The price of this {@link WineListing}.
     */
    public double getCostPerUnit() {
        return this.costPerUnit;
    }

    public void setCostPerUnit(double costPerUnit) throws IllegalArgumentException {
        if (costPerUnit < 0) {
            throw new IllegalArgumentException("Cost per unit must be a positive integer.");
        }
        this.costPerUnit = costPerUnit;
    }

    @Override
    public String toString() {
        return String.format("  Seller: %s%n  Cost (per unit): %.2f%n  Quantity: %d%n",
                sellerId,
                costPerUnit,
                quantity);
    }
}

