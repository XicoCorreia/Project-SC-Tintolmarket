package com.segc;

import java.io.*;
import java.util.LinkedList;
import java.util.List;
import java.nio.file.Path;
import java.util.Objects;

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

    public final List<T> getObjects(String directoryName) {
        List<T> list = new LinkedList<>();
        File dir = new File(directoryName);
        if (dir.mkdirs()) {
            System.out.println("Created directory: " + dir.getAbsolutePath());
        }
        for (File file : Objects.requireNonNull(dir.listFiles())) {
            list.add(getObject(file.toPath()));
        }
        return list;
    }
}
