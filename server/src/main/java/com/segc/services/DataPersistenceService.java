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

/**
 * @author fc54685 Francisco Correia
 * @author fc55955 Alexandre Fonseca
 * @author fc56272 Filipe Egipto
 */
public final class DataPersistenceService<T extends Serializable> {
    public final String digestAlgorithm;

    public DataPersistenceService() {
        this(Configuration.getInstance().getValue("digestAlgorithm"));
    }

    public DataPersistenceService(String digestAlgorithm) {
        this.digestAlgorithm = digestAlgorithm;
    }

    @SuppressWarnings("unchecked")
    public T getObject(String fileName) {
        synchronized(fileName) {
            try (FileInputStream fin = new FileInputStream(fileName);
                 ObjectInputStream in = new ObjectInputStream(fin)) {
                return (T) in.readObject();
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public T getObject(Path filePath) {
        return getObject(filePath.toString());
    }

    public void putObject(T obj, String fileName) {
        synchronized(fileName) {
            try (FileOutputStream fout = new FileOutputStream(fileName);
                 ObjectOutputStream out = new ObjectOutputStream(fout)) {
                out.writeObject(obj);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void putObject(T obj, Path filePath) {
        putObject(obj, filePath.toString());
    }


    public void putObjectAndDigest(T obj, String fileName) {
        byte[] digest = getDigest(obj);
        String digestFileName = "." + fileName + "." + digestAlgorithm.toLowerCase();
        synchronized(digestFileName) {
            try (FileOutputStream fout = new FileOutputStream(digestFileName)) {
                putObject(obj, fileName);
                fout.write(digest);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void putObjectAndDigest(T obj, Path filePath) {
        putObjectAndDigest(obj, filePath.toString());
    }

    public List<T> getObjects(String directoryName) {
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

    public List<T> getObjectsAndVerify(String directoryName, boolean verifyDigest) throws DataIntegrityException {
        List<T> list = new LinkedList<>();
        File dir = new File(directoryName);
        if (dir.mkdirs()) {
            System.out.println("Created directory: " + dir.getAbsolutePath());
        }
        for (File file : Objects.requireNonNull(dir.listFiles())) {
            Path filePath = file.toPath();
            T obj = getObject(filePath);
            if (verifyDigest && !isMatchingDigests(obj, filePath)) {
                String fileName = filePath.getFileName().toString();
                String message = String.format("%s digests do not match for file '%s'", digestAlgorithm, fileName);
                throw new DataIntegrityException(message);
            }
            list.add(obj);
            list.add(getObject(filePath));
        }
        return list;
    }

    private Path getDigestFilePath(Path filePath) {
        String fileName = filePath.getFileName().toString();
        String directoryName = filePath.getParent().toString();
        return Paths.get(directoryName, "." + fileName.toLowerCase() + ".digest");
    }

    public byte[] getDigest(Path filePath) {
        Path digestFilePath = getDigestFilePath(filePath);
        synchronized(digestFilePath) {
            try (FileInputStream fin = new FileInputStream(digestFilePath.toString())) {
                return fin.readAllBytes();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public byte[] getDigest(T obj) {
        synchronized(obj) {
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

    public boolean isMatchingDigests(T obj, Path filePath) {
        byte[] actualDigest = getDigest(obj);
        byte[] expectedDigest = getDigest(getDigestFilePath(filePath));
        return MessageDigest.isEqual(actualDigest, expectedDigest);
    }
}
