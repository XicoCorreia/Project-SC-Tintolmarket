/**
 *
 */
package com.segc;

import javax.swing.*;
import java.awt.*;
import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * @author fc54685 Francisco Correia
 * @author fc55955 Alexandre Fonseca
 * @author fc56272 Filipe Egipto
 */
public class Wine implements Serializable {
    private static final long serialVersionUID = 102672917111219812L;
    private final String name;
    private final String labelPath;
    private final LinkedList<Integer> ratings;
    private final HashMap<String, WineListing> wineListings;

    /**
     * Creates a {@link Wine} with the given name and label.
     *
     * @param name      the name for this wine.
     * @param labelPath the path to an image of this wine's label.
     */
    public Wine(String name, String labelPath) {
        this(name, labelPath, new LinkedList<>());
    }

    /**
     * Creates a {@link Wine} with the given name, label and list of ratings.
     *
     * @param name      The name for this wine.
     * @param labelPath The path to an image of this wine's label.
     * @param ratings   A list of ratings (between 1 and 5, inclusive).
     */
    public Wine(String name, String labelPath, LinkedList<Integer> ratings) {
        this(name, labelPath, ratings, new HashMap<>());
    }

    /**
     * Creates a {@link Wine} with the given name, label, list of ratings and collection of wine listings.
     *
     * @param name         The name for this wine.
     * @param labelPath    The path to an image of this wine's label.
     * @param ratings      A list of ratings (between 1 and 5, inclusive).
     * @param wineListings A collection of {@link WineListing} listed by a seller ({@link User}) with the given id.
     */
    public Wine(String name, String labelPath, LinkedList<Integer> ratings, HashMap<String, WineListing> wineListings) {
        this.name = name;
        this.labelPath = labelPath;
        this.ratings = ratings;
        this.wineListings = wineListings;
    }

    /**
     * @return The path to an image of this wine's label.
     */
    public String getLabelPath() {
        return labelPath;
    }

    /**
     * Draws this wine's label.
     */
    public void drawLabel() {
        ImageIcon image = new ImageIcon(labelPath);
        JFrame jFrame = new JFrame(this.name);
        jFrame.setLayout(new BorderLayout());
        JLabel jLabel = new JLabel(image);
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

    public HashMap<String, WineListing> getListings() {
        return this.wineListings;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Wine '%s' has an average rating of %.2f%n.", name, getRating()));
        wineListings.values().forEach(sb::append);
        return sb.toString();
    }
}
