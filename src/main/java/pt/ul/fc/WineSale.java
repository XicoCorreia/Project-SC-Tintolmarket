/**
 * 
 */
package pt.ul.fc;

/**
 * @author fc54685 Francisco Correia
 * @author fc55955 Alexandre Fonseca
 * @author fc56272 Filipe Egipto
 *
 */
public class WineSale {
    private Wine wine;
    private User seller;
    private int value;
    private int quantity;


    public WineSale(Wine wine, User seller, int value, int quantity) {
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
}

