/**
 * 
 */
package pt.ul.fc;

import java.awt.Image;
import java.util.LinkedList;
import java.util.List;

/**
 * @author fc54685 Francisco Correia
 * @author fc55955 Alexandre Fonseca
 * @author fc56272 Filipe Egipto
 *
 */
public class Wine {
    private String name;
    private Image image;
    private List<Integer> ratings;

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
     * @param name  The name for this wine.
     * @param image A label for this wine model.
     */
    public Wine(String name, Image image) {

        this(name, image, new LinkedList<>());
    }

    /**
     * Creates a {@code Wine} with the given name, label and list of ratings.
     * 
     * @param name    The name for this wine.
     * @param image   A label for this wine model.
     * @param ratings A list of ratings (between 0 and 5, inclusive).
     */
    public Wine(String name, Image image, List<Integer> ratings) {
        this.name = name;
        this.image = image;
        this.ratings = ratings;
    }
}
