package com.segc.services;

import com.segc.Configuration;
import com.segc.exception.DuplicateElementException;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import java.io.*;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateFactory;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.NoSuchElementException;

/**
 * A class that handles authentication.
 *
 * @author fc54685 Francisco Correia
 * @author fc55955 Alexandre Fonseca
 * @author fc56272 Filipe Egipto
 */
public class AuthenticationService {
    private static final Configuration config = Configuration.getInstance();
    private final CipherService cipherService;
    private final File userCredentials;
    private final char[] password;
    private final String userCredentialsAlgorithm;
    private final String baseCertificatesPath;
    private PBEParameterSpec pbeParameterSpec;

    public AuthenticationService(char[] password, CipherService cipherService) {
        this.cipherService = cipherService;
        this.password = password;
        this.baseCertificatesPath = config.getValue("userCertificatesDir");
        this.userCredentials = new File(config.getValue("userCredentials"));
        this.userCredentialsAlgorithm = config.getValue("userCredentialsPBEAlgorithm");
        try {
            initUserCredentials();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void initUserCredentials() throws IOException {
        File userCertificatesDir = new File(baseCertificatesPath);
        userCertificatesDir.mkdirs();
        File userCredentialsParameters = new File(config.getValue("userCredentialsParameters"));
        boolean fileExists = userCredentials.isFile();
        if (fileExists) {
            try (BufferedReader br = new BufferedReader(new FileReader(userCredentialsParameters))) {
                int iterationCount = Integer.parseInt(br.readLine());
                String encodedSalt = br.readLine();
                String encodedIv = br.readLine();
                Base64.Decoder decoder = Base64.getUrlDecoder();
                byte[] salt = decoder.decode(encodedSalt);
                byte[] iv = decoder.decode(encodedIv);
                IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
                pbeParameterSpec = new PBEParameterSpec(salt, iterationCount, ivParameterSpec);
            }
        } else {
            userCredentials.getParentFile().mkdirs();
            userCredentials.createNewFile();
            userCredentialsParameters.createNewFile();
            int iterationCount = config.getInt("userCredentialsIterationCount");
            pbeParameterSpec = CipherService.genPBEParameterSpec(iterationCount);
            try (PrintWriter pw = new PrintWriter(new FileWriter(userCredentialsParameters))) {
                Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
                String encodedSalt = encoder.encodeToString(pbeParameterSpec.getSalt());
                IvParameterSpec ivParameterSpec = (IvParameterSpec) pbeParameterSpec.getParameterSpec();
                String encodedIv = encoder.encodeToString(ivParameterSpec.getIV());
                pw.println(pbeParameterSpec.getIterationCount());
                pw.println(encodedSalt);
                pw.println(encodedIv);
            }
        }
    }

    private String getUserCredentials(String clientId) throws NoSuchElementException {
        String pattern = clientId + ":";
        synchronized (userCredentials) {
            File decryptedUserCredentials = decrypt();
            try (BufferedReader usersReader = new BufferedReader(new FileReader(decryptedUserCredentials))) {
                return usersReader.lines()
                                  .dropWhile(line -> !line.startsWith(pattern))
                                  .findFirst()
                                  .orElseThrow();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void registerUser(String clientId, Certificate cert) throws DuplicateElementException {
        // TODO: store cert instead of writing password
        try {
            getUserCredentials(clientId);
            throw new DuplicateElementException();
        } catch (NoSuchElementException e) {
            synchronized (userCredentials) {
                String certPath = Path.of(baseCertificatesPath, clientId + ".cer").toString();
                try (FileOutputStream fos = new FileOutputStream(certPath)) {
                    byte[] data = cert.getEncoded();
                    fos.write(data);
                } catch (IOException | CertificateEncodingException ioException) {
                    throw new RuntimeException(ioException);
                }
                File decryptedUserCredentials = decrypt();
                try (FileWriter fw = new FileWriter(decryptedUserCredentials, true);
                     BufferedWriter bw = new BufferedWriter(fw)) {
                    bw.write(clientId + ":" + certPath + "\n");
                } catch (IOException ioException) {
                    throw new RuntimeException(ioException);
                }
                encrypt(decryptedUserCredentials);
            }
        }
    }

    private File decrypt() {
        try {
            File decrypted = new File(config.getValue("userDecryptedCredential"));
            SecretKey key = getKeyFromPassword();
            cipherService.decrypt(new FileInputStream(userCredentials),
                    new FileOutputStream(decrypted), key, pbeParameterSpec);
            return decrypted;
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void encrypt(File decryptedUserCredentials) {
        try {
            SecretKey key = getKeyFromPassword();
            cipherService.encrypt(new FileInputStream(decryptedUserCredentials),
                    new FileOutputStream(userCredentials), key, pbeParameterSpec);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private SecretKey getKeyFromPassword() {
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(userCredentialsAlgorithm);
            return factory.generateSecret(new PBEKeySpec(password));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isRegisteredUser(String clientId) {
        try {
            getUserCredentials(clientId);
        } catch (NoSuchElementException e) {
            return false;
        }
        return true;
    }

    public Certificate getCertificate(String clientId) {
        String certPath = this.getUserCredentials(clientId).split(":", 2)[1];
        try (FileInputStream fis = new FileInputStream(certPath)){
            // Certificate cert = (Certificate) ois.readObject();
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            return factory.generateCertificate(fis);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
