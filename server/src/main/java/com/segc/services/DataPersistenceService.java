package com.segc.services;

import com.segc.Configuration;
import com.segc.exception.DataIntegrityException;

import java.io.*;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * @author fc54685 Francisco Correia
 * @author fc55955 Alexandre Fonseca
 * @author fc56272 Filipe Egipto
 */
public final class DataPersistenceService {
    public final String digestAlgorithm;

    public DataPersistenceService() {
        this(Configuration.getInstance().getValue("digestAlgorithm"));
    }

    public DataPersistenceService(String digestAlgorithm) {
        this.digestAlgorithm = digestAlgorithm;
    }

    @SuppressWarnings({"unchecked", "unused"})
    public <T extends Serializable> T getObject(Class<T> clazz, String fileName) throws FileNotFoundException {
        synchronized (this) {
            try (FileInputStream fin = new FileInputStream(fileName);
                 ObjectInputStream in = new ObjectInputStream(fin)) {
                return (T) in.readObject();
            } catch (FileNotFoundException e) {
                throw e;
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public <T extends Serializable> T getObject(Class<T> clazz, Path filePath) throws FileNotFoundException {
        return getObject(clazz, filePath.toString());
    }

    public <T extends Serializable> void putObject(T obj, String fileName) {
        synchronized (this) {
            File dir = new File(fileName).getParentFile();
            if (dir.mkdirs()) {
                System.out.println("Created directory: " + dir.getAbsolutePath());
            }
            try (FileOutputStream fout = new FileOutputStream(fileName);
                 ObjectOutputStream out = new ObjectOutputStream(fout)) {
                out.writeObject(obj);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public <T extends Serializable> void putObject(T obj, Path filePath) {
        putObject(obj, filePath.toString());
    }

    public <T extends Serializable> boolean putObject(T obj, Path filePath, Path newFilePath) {
        putObject(obj, filePath.toString());
        return filePath.toFile().renameTo(newFilePath.toFile());
    }

    public <T extends Serializable> void putObjectAndDigest(T obj, String fileName) {
        byte[] digest = getDigest(obj);
        File digestFile = getDigestFilePath(Path.of(fileName)).toFile();
        synchronized (this) {
            try (FileOutputStream fout = new FileOutputStream(digestFile)) {
                putObject(obj, fileName);
                fout.write(digest);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public <T extends Serializable> void putObjectAndDigest(T obj, Path filePath) {
        putObjectAndDigest(obj, filePath.toString());
    }

    public <T extends Serializable> List<T> getObjects(Class<T> clazz,
                                                       String directoryName,
                                                       Predicate<File> pred) throws FileNotFoundException {
        List<T> list = new LinkedList<>();
        File dir = new File(directoryName);
        if (dir.mkdirs()) {
            System.out.println("Created directory: " + dir.getAbsolutePath());
        }
        for (File file : Objects.requireNonNull(dir.listFiles())) {
            if (pred.test(file)) {
                T obj = getObject(clazz, file.toPath());
                list.add(obj);
            }
        }
        return list;
    }

    public <T extends Serializable> List<T> getObjectsAndVerify(Class<T> clazz,
                                                                String directoryName)
            throws DataIntegrityException, FileNotFoundException {
        List<T> list = new LinkedList<>();
        File dir = new File(directoryName);
        if (dir.mkdirs()) {
            System.out.println("Created directory: " + dir.getAbsolutePath());
        }
        for (File file : Objects.requireNonNull(dir.listFiles())) {
            Path filePath = file.toPath();
            if (isDigestFilePath(filePath)) {
                continue;
            }
            T obj = getObject(clazz, filePath);
            if (!isMatchingDigests(obj, filePath)) {
                String fileName = filePath.getFileName().toString();
                String message = String.format("%s digests do not match for file '%s'", digestAlgorithm, fileName);
                throw new DataIntegrityException(message);
            }
            list.add(obj);
        }
        return list;
    }

    private Path getDigestFilePath(Path filePath) {
        String fileName = filePath.getFileName().toString();
        String directoryName = filePath.getParent().toString();
        return Paths.get(directoryName, fileName.toLowerCase() + "." + digestAlgorithm);
    }

    public byte[] getDigest(Path filePath) {
        Path digestFilePath = getDigestFilePath(filePath);
        synchronized (this) {
            try (FileInputStream fin = new FileInputStream(digestFilePath.toString())) {
                return fin.readAllBytes();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public <T extends Serializable> byte[] getDigest(T obj) {
        synchronized (this) {
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                 ObjectOutputStream out = new ObjectOutputStream(baos)) {
                out.writeObject(obj);
                byte[] bytes = baos.toByteArray();
                return MessageDigest.getInstance(digestAlgorithm).digest(bytes);
            } catch (IOException | NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private boolean isDigestFilePath(Path filePath) {
        return filePath.getFileName().toString().endsWith("." + digestAlgorithm);
    }

    private <T extends Serializable> boolean isMatchingDigests(T obj, Path filePath) {
        byte[] actualDigest = getDigest(obj);
        byte[] expectedDigest = getDigest(filePath);
        return MessageDigest.isEqual(actualDigest, expectedDigest);
    }
}
