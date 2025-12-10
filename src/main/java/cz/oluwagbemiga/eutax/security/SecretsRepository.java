package cz.oluwagbemiga.eutax.security;

import cz.oluwagbemiga.eutax.exceptions.LoginException;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.CodeSource;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;

/**
 * Repository for loading and managing sensitive credentials from PKCS#12 keystores.
 * <p>
 * This class provides secure access to application secrets, specifically Google service account
 * credentials stored in encrypted keystores. It supports two keystore locations:
 * <ol>
 *     <li><b>Bundled keystore</b>: A keystore packaged within the application resources ({@code /secrets.p12})</li>
 *     <li><b>External keystore</b>: A user-created keystore in the application's directory ({@code secrets.p12})</li>
 * </ol>
 * </p>
 * <p>
 * The bundled keystore is checked first; if not found, the external keystore is used.
 * This allows users to provide their own credentials without modifying the application package.
 * </p>
 *
 * @see KeystoreCreator
 */
@Slf4j
public final class SecretsRepository {

    /** Resource path for the bundled keystore */
    private static final String KEYSTORE_RESOURCE = "/secrets.p12";

    /** Filename for the external keystore */
    private static final String KEYSTORE_FILENAME = "secrets.p12";

    /** Alias under which the Google service account JSON is stored */
    private static final String GOOGLE_ACCOUNT_ALIAS = "google-service-account-json";

    /**
     * Private constructor to prevent instantiation of this utility class.
     *
     * @throws IllegalStateException always thrown to prevent instantiation
     */
    private SecretsRepository() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Loads the Google service account JSON credentials from the application keystore.
     * <p>
     * This method attempts to open the keystore using the provided password and extract
     * the stored Google service account JSON. The keystore is searched in the following order:
     * <ol>
     *     <li>Bundled keystore in application resources</li>
     *     <li>External keystore in the application directory</li>
     * </ol>
     * </p>
     *
     * @param password the keystore password entered by the user
     * @return the Google service account JSON as a string
     * @throws LoginException if the keystore cannot be loaded, the password is incorrect,
     *                        or the Google service account entry is missing
     */
    public static String loadGoogleServiceAccountJson(char[] password) {
        log.debug("Loading Google service account JSON from keystore");
        KeyStore keyStore = loadKeyStore(password);
        try {
            log.debug("Retrieving secret entry with alias: {}", GOOGLE_ACCOUNT_ALIAS);
            KeyStore.SecretKeyEntry entry = (KeyStore.SecretKeyEntry) keyStore.getEntry(
                    GOOGLE_ACCOUNT_ALIAS,
                    new KeyStore.PasswordProtection(password)
            );
            if (entry == null) {
                log.error("Google service account secret not found in keystore (alias: {})", GOOGLE_ACCOUNT_ALIAS);
                throw new LoginException("Google service account secret missing in keystore");
            }
            byte[] encoded = entry.getSecretKey().getEncoded();
            log.info("Successfully loaded Google service account JSON ({} bytes)", encoded.length);
            return new String(encoded, StandardCharsets.UTF_8);
        } catch (NoSuchAlgorithmException | UnrecoverableEntryException | KeyStoreException ex) {
            log.error("Unable to read Google service account secret from keystore", ex);
            throw new LoginException("Unable to read Google service account secret", ex);
        }
    }

    /**
     * Loads the PKCS12 keystore from either bundled resources or an external file.
     * <p>
     * This method first attempts to load a keystore from the application resources.
     * If no bundled keystore is found, it falls back to loading from an external file
     * in the application directory.
     * </p>
     *
     * @param password the password to unlock the keystore
     * @return the loaded {@link KeyStore} instance
     * @throws LoginException if no keystore is found or loading fails
     */
    private static KeyStore loadKeyStore(char[] password) {
        log.debug("Attempting to load keystore");
        // First try to load from resources (bundled keystore)
        log.debug("Checking for bundled keystore in resources: {}", KEYSTORE_RESOURCE);
        try (InputStream stream = SecretsRepository.class.getResourceAsStream(KEYSTORE_RESOURCE)) {
            if (stream != null) {
                KeyStore keyStore = KeyStore.getInstance("PKCS12");
                keyStore.load(stream, password);
                log.info("Successfully loaded bundled keystore from resources");
                return keyStore;
            }
            log.debug("No bundled keystore found in resources");
        } catch (IOException | KeyStoreException | NoSuchAlgorithmException | java.security.cert.CertificateException ex) {
            log.debug("Failed to load bundled keystore, trying external file: {}", ex.getMessage());
            // Fall through to try external file
        }

        // Try to load from external file (user-created keystore)
        File externalFile = getExternalKeystoreFile();
        log.debug("Checking for external keystore: {}", externalFile.getAbsolutePath());
        if (externalFile.exists()) {
            try (FileInputStream fis = new FileInputStream(externalFile)) {
                KeyStore keyStore = KeyStore.getInstance("PKCS12");
                keyStore.load(fis, password);
                log.info("Successfully loaded external keystore: {}", externalFile.getName());
                return keyStore;
            } catch (IOException ex) {
                log.error("Unable to read external keystore: {}", externalFile.getAbsolutePath(), ex);
                throw new LoginException("Unable to read application keystore", ex);
            } catch (KeyStoreException | NoSuchAlgorithmException | java.security.cert.CertificateException ex) {
                log.error("Keystore initialization failed for: {}", externalFile.getAbsolutePath(), ex);
                throw new LoginException("Keystore initialization failed", ex);
            }
        }

        log.error("No keystore found (checked resources and external file: {})", externalFile.getAbsolutePath());
        throw new LoginException("Keystore secrets.p12 was not found");
    }

    /**
     * Checks if a keystore exists either in resources or as an external file.
     * <p>
     * This method does not validate the keystore contents or password; it only
     * checks for the existence of the keystore file.
     * </p>
     *
     * @return {@code true} if a keystore is available; {@code false} otherwise
     */
    public static boolean keystoreExists() {
        log.debug("Checking if keystore exists");
        // Check resources first
        try (InputStream stream = SecretsRepository.class.getResourceAsStream(KEYSTORE_RESOURCE)) {
            if (stream != null) {
                log.debug("Found bundled keystore in resources");
                return true;
            }
        } catch (IOException ex) {
            log.debug("Error checking bundled keystore: {}", ex.getMessage());
            // Ignore and check external file
        }

        // Check external file
        File externalFile = getExternalKeystoreFile();
        boolean exists = externalFile.exists();
        log.debug("External keystore exists: {} (path: {})", exists, externalFile.getAbsolutePath());
        return exists;
    }

    /**
     * Returns the path to the external keystore file in the application's directory.
     * <p>
     * The external keystore allows users to provide their own credentials without
     * modifying the application package. The file is expected to be named {@code secrets.p12}
     * and located in the same directory as the application JAR (or the classes directory
     * when running from an IDE).
     * </p>
     *
     * @return a {@link File} pointing to the external keystore location (may not exist)
     */
    public static File getExternalKeystoreFile() {
        File file = new File(getAppDirectory(), KEYSTORE_FILENAME);
        log.trace("External keystore file path: {}", file.getAbsolutePath());
        return file;
    }

    /**
     * Determines the directory where the application is running from.
     * <p>
     * For JAR execution, this returns the JAR's parent directory.
     * For IDE execution (running from classes directory), this returns the classes directory.
     * As a fallback, it returns the current working directory ({@code user.dir}).
     * </p>
     *
     * @return the application directory as a {@link File}
     */
    public static File getAppDirectory() {
        log.trace("Determining application directory");
        try {
            CodeSource codeSource = SecretsRepository.class.getProtectionDomain().getCodeSource();
            if (codeSource != null) {
                Path jarPath = Path.of(codeSource.getLocation().toURI());
                File jarFile = jarPath.toFile();
                if (jarFile.isFile()) {
                    File parentDir = jarFile.getParentFile();
                    log.trace("Running from JAR, app directory: {}", parentDir.getAbsolutePath());
                    return parentDir;
                } else {
                    // Running from classes directory (IDE)
                    log.trace("Running from classes directory (IDE): {}", jarFile.getAbsolutePath());
                    return jarFile;
                }
            }
        } catch (URISyntaxException ex) {
            log.debug("Failed to determine app directory from code source, using user.dir: {}", ex.getMessage());
            // Fall back to user.dir
        }
        File fallbackDir = new File(System.getProperty("user.dir"));
        log.trace("Using fallback app directory (user.dir): {}", fallbackDir.getAbsolutePath());
        return fallbackDir;
    }
}
