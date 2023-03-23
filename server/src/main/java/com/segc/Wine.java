/**
 *
 */
package com.segc;

import javax.swing.*;
import java.awt.*;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 * @author fc54685 Francisco Correia
 * @author fc55955 Alexandre Fonseca
 * @author fc56272 Filipe Egipto
 */
public class Wine implements Serializable {
    private static final long serialVersionUID = 102672917111219812L;
    private final String name;
    private final String labelPath;
    private final List<Integer> ratings;

    /**
     * Creates a {@code Wine} with the given name.
     *
     * @param name The name for this wine.
     */
    public Wine(String name) {
        this(name, null);
    }

    /**
     * Creates a {@code Wine} with the given name and label.
     *
     * @param name      the name for this wine.
     * @param labelPath the path to an image of this wine's label.
     */
    public Wine(String name, String labelPath) {
        this(name, labelPath, new LinkedList<>());
    }

    /**
     * Creates a {@code Wine} with the given name, label and list of ratings.
     *
     * @param name      The name for this wine.
     * @param labelPath The path to an image of this wine's label.
     * @param ratings   A list of ratings (between 1 and 5, inclusive).
     */
    public Wine(String name, String labelPath, List<Integer> ratings) {
        this.name = name;
        this.labelPath = labelPath;
        this.ratings = ratings;
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
}
