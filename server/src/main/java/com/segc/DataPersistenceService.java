package com.segc;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * @author fc54685 Francisco Correia
 * @author fc55955 Alexandre Fonseca
 * @author fc56272 Filipe Egipto
 */
public class DataPersistenceService<T extends Serializable> {

    @SuppressWarnings("unchecked")
    public final T getObject(String fileName) {
        try (FileInputStream fin = new FileInputStream(fileName);
             ObjectInputStream in = new ObjectInputStream(fin)) {
            return (T) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public final T getObject(Path filePath) {
        return getObject(filePath.toString());
    }

    public final void putObject(T obj, String fileName) {
        try (FileOutputStream fout = new FileOutputStream(fileName);
             ObjectOutputStream out = new ObjectOutputStream(fout)) {
            out.writeObject(obj);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public final void putObject(T obj, Path filePath) {
        putObject(obj, filePath.toString());
    }

    public final Stream<T> getObjects(String directoryName) {
        try (Stream<Path> filePaths = Files.walk(Path.of(directoryName))) {
            return filePaths.map(this::getObject);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
