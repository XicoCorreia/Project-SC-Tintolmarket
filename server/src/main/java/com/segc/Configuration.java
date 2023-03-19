package com.segc;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
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

    /**
     * Returns a new instance of a class mapped by {@code key} cast as {@code T}.
     *
     * @param <T>          the type of the class to be instantiated
     * @param key          the property key to look up
     * @param defaultValue a default instance of {@code T} to fall back to
     * @return a new instance of a class mapped by {@code key} cast as {@code T},
     * {@code defaultValue} otherwise.
     */
    public <T> T getInstanceOfClass(String key, T defaultValue) {
        String klassName = (String) props.get(key);
        if (klassName == null) {
            return defaultValue;
        }
        try {
            @SuppressWarnings("unchecked") Class<T> klass = (Class<T>) Class.forName(klassName);
            Constructor<T> constructor = klass.getConstructor();
            return constructor.newInstance();
        } catch (ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException |
                 IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            e.printStackTrace();
        }

        return defaultValue;
    }

}
