/**
 * 
 */
package com.segc;

/**
 * @author fc54685 Francisco Correia
 * @author fc55955 Alexandre Fonseca
 * @author fc56272 Filipe Egipto
 *
 */
public class WineSale {
    private Wine wine;
    private User seller;
    private double value;
    private int quantity;


    public WineSale(Wine wine, User seller, double value, int quantity) {
        this.wine = wine;
        this.seller = seller;
        this.value = value;
        this.quantity = quantity;
    }

    /**
     * @return The available quantity for this wine.
     */
    public int getQuantity() {
        return quantity;
    }

    /**
     * @param quantity The quantity to add to this wine.
     */
    public void addQuantity(int quantity) {
        this.quantity += quantity;
    }

    /**
     * @param quantity The quantity to remove to this wine.
     */
    public void removeQuantity(int quantity) {
        this.quantity -= quantity;
    }

    /**
     * @return The seller of this wine.
     */
    public User getUser()
    {
        return this.seller;
    }

    /**
     * @return The price of this wine.
     */
    public double getValue()
    {
        return this.value;
    }
}

