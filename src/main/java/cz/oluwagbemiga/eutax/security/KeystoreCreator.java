package cz.oluwagbemiga.eutax.security;

import cz.oluwagbemiga.eutax.exceptions.LoginException;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

/**
 * Utility class for creating PKCS12 keystores from JSON files.
 */
public final class KeystoreCreator {

    private static final String GOOGLE_ACCOUNT_ALIAS = "google-service-account-json";
    private static final String SECRET_KEY_ALGORITHM = "AES";

    private KeystoreCreator() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Creates a PKCS12 keystore from a Google service account JSON file.
     *
     * @param jsonFile   the JSON file containing the service account credentials
     * @param outputFile the output PKCS12 file to create
     * @param password   the password to protect the keystore
     * @throws LoginException if keystore creation fails
     */
    public static void createKeystoreFromJson(File jsonFile, File outputFile, char[] password) {
        if (jsonFile == null || !jsonFile.exists()) {
            throw new LoginException("JSON file does not exist");
        }
        if (outputFile == null) {
            throw new LoginException("Output file path is required");
        }
        if (password == null || password.length == 0) {
            throw new LoginException("Password is required");
        }

        try {
            // Read JSON content
            String jsonContent = Files.readString(jsonFile.toPath(), StandardCharsets.UTF_8);

            // Validate it looks like a Google service account JSON
            if (!jsonContent.contains("\"type\"") || !jsonContent.contains("service_account")) {
                throw new LoginException("The selected file does not appear to be a valid Google service account JSON");
            }

            // Create keystore and store the JSON as a secret key
            createKeystoreFromJsonString(jsonContent, outputFile, password);

        } catch (IOException ex) {
            throw new LoginException("Failed to read JSON file: " + ex.getMessage(), ex);
        }
    }

    /**
     * Creates a PKCS12 keystore from a JSON string.
     *
     * @param jsonContent the JSON content to store
     * @param outputFile  the output PKCS12 file to create
     * @param password    the password to protect the keystore
     * @throws LoginException if keystore creation fails
     */
    public static void createKeystoreFromJsonString(String jsonContent, File outputFile, char[] password) {
        try {
            byte[] jsonBytes = jsonContent.getBytes(StandardCharsets.UTF_8);
            SecretKey secretKey = new SecretKeySpec(jsonBytes, SECRET_KEY_ALGORITHM);

            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(null, password); // Initialize empty keystore

            KeyStore.SecretKeyEntry secretEntry = new KeyStore.SecretKeyEntry(secretKey);
            KeyStore.PasswordProtection protection = new KeyStore.PasswordProtection(password);
            keyStore.setEntry(GOOGLE_ACCOUNT_ALIAS, secretEntry, protection);

            // Ensure parent directory exists
            File parentDir = outputFile.getParentFile();
            if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
                throw new LoginException("Failed to create directory: " + parentDir.getAbsolutePath());
            }

            // Write keystore to file
            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                keyStore.store(fos, password);
            }

        } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException ex) {
            throw new LoginException("Failed to create keystore: " + ex.getMessage(), ex);
        } catch (IOException ex) {
            throw new LoginException("Failed to write keystore file: " + ex.getMessage(), ex);
        }
    }

    /**
     * Validates that the given password can open the specified keystore.
     *
     * @param keystoreFile the keystore file to validate
     * @param password     the password to test
     * @return true if the password is valid
     */
    public static boolean validatePassword(File keystoreFile, char[] password) {
        if (keystoreFile == null || !keystoreFile.exists()) {
            return false;
        }
        try (var fis = new java.io.FileInputStream(keystoreFile)) {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(fis, password);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}

