package cz.oluwagbemiga.eutax.security;

import cz.oluwagbemiga.eutax.exceptions.LoginException;
import lombok.extern.slf4j.Slf4j;

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
 * Utility class for creating and validating PKCS12 keystores containing Google service account credentials.
 * <p>
 * This class provides functionality to:
 * <ul>
 *     <li>Create a PKCS12 keystore from a Google service account JSON file</li>
 *     <li>Create a PKCS12 keystore from a JSON string</li>
 *     <li>Validate keystore passwords</li>
 * </ul>
 * </p>
 * <p>
 * The Google service account JSON is stored as a secret key entry within the keystore,
 * encrypted using AES algorithm and protected by the user-provided password.
 * </p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * File jsonFile = new File("service-account.json");
 * File keystoreFile = new File("secrets.p12");
 * char[] password = "mySecretPassword".toCharArray();
 *
 * KeystoreCreator.createKeystoreFromJson(jsonFile, keystoreFile, password);
 * }</pre>
 *
 * @see SecretsRepository
 */
@Slf4j
public final class KeystoreCreator {

    /** Alias used to store the Google service account JSON in the keystore */
    private static final String GOOGLE_ACCOUNT_ALIAS = "google-service-account-json";

    /** Algorithm used for the secret key that wraps the JSON content */
    private static final String SECRET_KEY_ALGORITHM = "AES";

    /**
     * Private constructor to prevent instantiation of this utility class.
     *
     * @throws IllegalStateException always thrown to prevent instantiation
     */
    private KeystoreCreator() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Creates a PKCS12 keystore from a Google service account JSON file.
     * <p>
     * The method validates that:
     * <ul>
     *     <li>The JSON file exists and is readable</li>
     *     <li>The file contains valid Google service account JSON (contains "type" and "service_account")</li>
     *     <li>The output file path and password are provided</li>
     * </ul>
     * </p>
     *
     * @param jsonFile   the JSON file containing the Google service account credentials
     * @param outputFile the output PKCS12 file to create (will be overwritten if exists)
     * @param password   the password to protect the keystore (must not be null or empty)
     * @throws LoginException if the JSON file doesn't exist, is invalid, or keystore creation fails
     */
    public static void createKeystoreFromJson(File jsonFile, File outputFile, char[] password) {
        log.debug("Creating keystore from JSON file: {} to output: {}",
                jsonFile != null ? jsonFile.getAbsolutePath() : "null",
                outputFile != null ? outputFile.getAbsolutePath() : "null");
        if (jsonFile == null || !jsonFile.exists()) {
            log.error("JSON file does not exist: {}", jsonFile);
            throw new LoginException("JSON file does not exist");
        }
        if (outputFile == null) {
            log.error("Output file path is required");
            throw new LoginException("Output file path is required");
        }
        if (password == null || password.length == 0) {
            log.error("Password is required for keystore creation");
            throw new LoginException("Password is required");
        }

        try {
            // Read JSON content
            String jsonContent = Files.readString(jsonFile.toPath(), StandardCharsets.UTF_8);
            log.debug("Successfully read JSON file, content length: {} bytes", jsonContent.length());

            // Validate it looks like a Google service account JSON
            if (!jsonContent.contains("\"type\"") || !jsonContent.contains("service_account")) {
                log.error("Invalid Google service account JSON format in file: {}", jsonFile.getAbsolutePath());
                throw new LoginException("The selected file does not appear to be a valid Google service account JSON");
            }

            // Create keystore and store the JSON as a secret key
            createKeystoreFromJsonString(jsonContent, outputFile, password);
            log.info("Successfully created keystore from JSON file: {}", jsonFile.getName());

        } catch (IOException ex) {
            log.error("Failed to read JSON file: {}", jsonFile.getAbsolutePath(), ex);
            throw new LoginException("Failed to read JSON file: " + ex.getMessage(), ex);
        }
    }

    /**
     * Creates a PKCS12 keystore from a JSON string.
     * <p>
     * The JSON content is stored as a secret key entry in the keystore, encrypted with
     * the provided password. The parent directory of the output file will be created
     * if it doesn't exist.
     * </p>
     *
     * @param jsonContent the JSON content to store in the keystore
     * @param outputFile  the output PKCS12 file to create (will be overwritten if exists)
     * @param password    the password to protect the keystore
     * @throws LoginException if keystore creation or file writing fails
     */
    public static void createKeystoreFromJsonString(String jsonContent, File outputFile, char[] password) {
        log.debug("Creating keystore from JSON string to output: {}", outputFile.getAbsolutePath());
        try {
            byte[] jsonBytes = jsonContent.getBytes(StandardCharsets.UTF_8);
            SecretKey secretKey = new SecretKeySpec(jsonBytes, SECRET_KEY_ALGORITHM);
            log.debug("Created secret key from JSON content ({} bytes)", jsonBytes.length);

            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(null, password); // Initialize empty keystore
            log.debug("Initialized empty PKCS12 keystore");

            KeyStore.SecretKeyEntry secretEntry = new KeyStore.SecretKeyEntry(secretKey);
            KeyStore.PasswordProtection protection = new KeyStore.PasswordProtection(password);
            keyStore.setEntry(GOOGLE_ACCOUNT_ALIAS, secretEntry, protection);
            log.debug("Added secret entry with alias: {}", GOOGLE_ACCOUNT_ALIAS);

            // Ensure parent directory exists
            File parentDir = outputFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                if (!parentDir.mkdirs()) {
                    log.error("Failed to create directory: {}", parentDir.getAbsolutePath());
                    throw new LoginException("Failed to create directory: " + parentDir.getAbsolutePath());
                }
                log.debug("Created parent directory: {}", parentDir.getAbsolutePath());
            }

            // Write keystore to file
            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                keyStore.store(fos, password);
            }
            log.info("Successfully created keystore: {}", outputFile.getAbsolutePath());

        } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException ex) {
            log.error("Failed to create keystore", ex);
            throw new LoginException("Failed to create keystore: " + ex.getMessage(), ex);
        } catch (IOException ex) {
            log.error("Failed to write keystore file: {}", outputFile.getAbsolutePath(), ex);
            throw new LoginException("Failed to write keystore file: " + ex.getMessage(), ex);
        }
    }

    /**
     * Validates that the given password can successfully open the specified keystore.
     * <p>
     * This method attempts to load the keystore with the provided password and returns
     * {@code true} if successful. Any exception during loading (wrong password, corrupted
     * file, etc.) results in {@code false} being returned.
     * </p>
     *
     * @param keystoreFile the PKCS12 keystore file to validate
     * @param password     the password to test
     * @return {@code true} if the password successfully opens the keystore; {@code false} otherwise
     */
    public static boolean validatePassword(File keystoreFile, char[] password) {
        log.debug("Validating password for keystore: {}",
                keystoreFile != null ? keystoreFile.getAbsolutePath() : "null");

        if (keystoreFile == null || !keystoreFile.exists()) {
            log.debug("Keystore file does not exist");
            return false;
        }
        try (var fis = new java.io.FileInputStream(keystoreFile)) {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(fis, password);
            log.debug("Password validation successful for keystore: {}", keystoreFile.getName());
            return true;
        } catch (Exception ex) {
            log.debug("Password validation failed for keystore: {} - {}", keystoreFile.getName(), ex.getMessage());
            return false;
        }
    }
}
