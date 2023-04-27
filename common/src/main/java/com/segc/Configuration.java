package com.segc;

import java.io.IOException;
import java.util.Properties;

/**
 * A singleton class that loads a {@code config.properties} file.
 *
 * @author fc54685 Francisco Correia
 * @author fc55955 Alexandre Fonseca
 * @author fc56272 Filipe Egipto
 */
public class Configuration {

    private static Configuration instance;
    private final Properties props;

    private Configuration() {
        props = new Properties(); // lazy load
        try {
            props.load(getClass().getResourceAsStream("/config.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the singleton instance of {@code Configuration}.
     *
     * @return singleton instance of this class
     */
    public static Configuration getInstance() {
        if (instance == null) {
            instance = new Configuration(); // lazy load
        }
        return instance;
    }

    /**
     * Returns the value mapped to the given {@code key} or {@code null} otherwise.
     *
     * @param key the property key to look up
     * @return the value mapped to the given {@code key} or {@code null} otherwise
     */
    public String getValue(String key) {
        return props.getProperty(key);
    }
}
