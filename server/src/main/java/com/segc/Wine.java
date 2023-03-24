/**
 *
 */
package com.segc;

import javax.swing.*;
import java.awt.*;
import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * @author fc54685 Francisco Correia
 * @author fc55955 Alexandre Fonseca
 * @author fc56272 Filipe Egipto
 */
public class Wine implements Serializable {
    private static final long serialVersionUID = 102672917111219812L;
    private final String name;
    private final ImageIcon label;
    private final LinkedList<Integer> ratings;
    private final HashMap<String, WineListing> wineListings;

    /**
     * Creates a {@link Wine} with the given name and label.
     *
     * @param name  the name for this wine.
     * @param label an image of this wine's label.
     */
    public Wine(String name, ImageIcon label) {
        this(name, label, new LinkedList<>());
    }

    /**
     * Creates a {@link Wine} with the given name, label and list of ratings.
     *
     * @param name    The name for this wine.
     * @param label   an image of this wine's label.
     * @param ratings A list of ratings (between 1 and 5, inclusive).
     */
    public Wine(String name, ImageIcon label, LinkedList<Integer> ratings) {
        this(name, label, ratings, new HashMap<>());
    }

    /**
     * Creates a {@link Wine} with the given name, label, list of ratings and collection of wine listings.
     *
     * @param name         The name for this wine.
     * @param label        an image of this wine's label.
     * @param ratings      A list of ratings (between 1 and 5, inclusive).
     * @param wineListings A collection of {@link WineListing} listed by a seller ({@link User}) with the given id.
     */
    public Wine(String name, ImageIcon label, LinkedList<Integer> ratings, HashMap<String, WineListing> wineListings) {
        this.name = name;
        this.label = label;
        this.ratings = ratings;
        this.wineListings = wineListings;
    }

    /**
     * @return an image of this wine's label.
     */
    public ImageIcon getLabel() {
        return label;
    }

    /**
     * Draws this wine's label.
     */
    public void drawLabel() {
        JFrame jFrame = new JFrame(this.name);
        jFrame.setLayout(new BorderLayout());
        JLabel jLabel = new JLabel(this.label);
        jFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        jFrame.add(jLabel);
        jFrame.setLocationRelativeTo(null);
        jFrame.pack();
        jFrame.setVisible(true);
    }

    /**
     * @return The name of this wine.
     */
    public String getName() {
        return name;
    }

    /**
     * @return The average rating of this wine.
     */
    public double getRating() {
        if (ratings.size() == 0) {
            return 0;
        }
        Integer sum = 0;
        for (Integer x : this.ratings) {
            sum += x;
        }
        return (double) sum / (double) this.ratings.size();
    }

    /**
     * @param stars The rating to add to this wine's ratings.
     */
    public void addRating(int stars) {
        this.ratings.add(stars);
    }

    public WineListing getListing(String sellerId) throws NoSuchElementException {
        return Optional.ofNullable(wineListings.get(sellerId)).orElseThrow();
    }

    public HashMap<String, WineListing> getListings() {
        return this.wineListings;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Wine '%s' has an average rating of %.2f.%n", name, getRating()));
        wineListings.values().forEach(sb::append);
        return sb.toString();
    }

    public void removeListing(String sellerId) {
        this.wineListings.remove(sellerId);
    }
}
