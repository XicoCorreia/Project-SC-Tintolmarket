package com.segc;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;

/**
 * Uma classe singleton para carregar propriedades de um ficheiro
 * {@code config.properties}.
 * 
 * O ficheiro de propriedades associado a esta configuracao encontra-se em
 * {@code src/main/java/resources/config.properties}.
 *
 * @author fc54685 Francisco Correia
 * @author fc55955 Alexandre Fonseca
 * @author fc56272 Filipe Egipto
 * 
 */
public class Configuration {

    private static Configuration instance;
    private Properties props;

    private Configuration() {
        props = new Properties(); // lazy load
        try {
            props.load(getClass().getResourceAsStream("/config.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Devolve a instancia singleton desta configuracao.
     * 
     * @return a instancia singleton desta configuracao
     */
    public static Configuration getInstance() {
        if (instance == null) {
            instance = new Configuration(); // lazy load
        }
        return instance;
    }

    /**
     * @param key Devolve o valor {@link String} associado a esta chave, ou
     *            {@code null} caso nao haja esse mapeamento na configuracao.
     * @return o valor {@link String} associado a chave dada, ou {@code null} caso
     *         nao haja mapeamento
     */
    public String getValue(String key) {
        return props.getProperty(key);
    }

    /**
     * Devolve uma nova instância de tipo {@code T} da classe com o nome associado a
     * chave dada, devolvendo o valor por omissao em caso contrario.
     * 
     * @param <T>          o tipo da classe a instanciar
     * @param key          a chave associada ao valor na configuracao
     * @param defaultValue o valor por omissao caso a chave nao esteja mapeada
     * @return uma nova instância de tipo {@code T}, {@code defaultValue} em caso
     *         contrário
     */
    public <T> T getInstanceOfClass(String key, T defaultValue) {
        String klassName = (String) props.get(key);
        if (klassName == null) {
            return defaultValue;
        }
        try {
            @SuppressWarnings("unchecked")
            Class<T> klass = (Class<T>) Class.forName(klassName);
            Constructor<T> constructor = klass.getConstructor();
            return constructor.newInstance();
        } catch (ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException |
                 IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            e.printStackTrace();
        }

        return defaultValue;
    }

}
