package cz.oluwagbemiga.eutax.security;

import cz.oluwagbemiga.eutax.exceptions.LoginException;

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
 * Loads sensitive data from the packaged PKCS#12 keystore.
 * Checks for keystore in resources first, then in the JAR's directory.
 */
public final class SecretsRepository {

    private static final String KEYSTORE_RESOURCE = "/secrets.p12";
    private static final String KEYSTORE_FILENAME = "secrets.p12";
    private static final String GOOGLE_ACCOUNT_ALIAS = "google-service-account-json";

    private SecretsRepository() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Attempts to open the application keystore using the provided password and
     * returns the stored service-account JSON as a String. The caller is expected
     * to validate the password before calling this method.
     *
     * @param password keystore password entered by the user
     * @return JSON payload as stored in the keystore
     */
    public static String loadGoogleServiceAccountJson(char[] password) {
        KeyStore keyStore = loadKeyStore(password);
        try {
            KeyStore.SecretKeyEntry entry = (KeyStore.SecretKeyEntry) keyStore.getEntry(
                    GOOGLE_ACCOUNT_ALIAS,
                    new KeyStore.PasswordProtection(password)
            );
            if (entry == null) {
                throw new LoginException("Google service account secret missing in keystore");
            }
            byte[] encoded = entry.getSecretKey().getEncoded();
            return new String(encoded, StandardCharsets.UTF_8);
        } catch (NoSuchAlgorithmException | UnrecoverableEntryException | KeyStoreException ex) {
            throw new LoginException("Unable to read Google service account secret", ex);
        }
    }

    private static KeyStore loadKeyStore(char[] password) {
        // First try to load from resources (bundled keystore)
        try (InputStream stream = SecretsRepository.class.getResourceAsStream(KEYSTORE_RESOURCE)) {
            if (stream != null) {
                KeyStore keyStore = KeyStore.getInstance("PKCS12");
                keyStore.load(stream, password);
                return keyStore;
            }
        } catch (IOException | KeyStoreException | NoSuchAlgorithmException | java.security.cert.CertificateException ex) {
            // Fall through to try external file
        }

        // Try to load from external file (user-created keystore)
        File externalFile = getExternalKeystoreFile();
        if (externalFile.exists()) {
            try (FileInputStream fis = new FileInputStream(externalFile)) {
                KeyStore keyStore = KeyStore.getInstance("PKCS12");
                keyStore.load(fis, password);
                return keyStore;
            } catch (IOException ex) {
                throw new LoginException("Unable to read application keystore", ex);
            } catch (KeyStoreException | NoSuchAlgorithmException | java.security.cert.CertificateException ex) {
                throw new LoginException("Keystore initialization failed", ex);
            }
        }

        throw new LoginException("Keystore secrets.p12 was not found");
    }

    /**
     * Checks if a keystore exists either in resources or as an external file.
     *
     * @return true if a keystore is available
     */
    public static boolean keystoreExists() {
        // Check resources first
        try (InputStream stream = SecretsRepository.class.getResourceAsStream(KEYSTORE_RESOURCE)) {
            if (stream != null) {
                return true;
            }
        } catch (IOException ex) {
            // Ignore and check external file
        }

        // Check external file
        File externalFile = getExternalKeystoreFile();
        return externalFile.exists();
    }

    /**
     * Returns the path to the external keystore file (in JAR's directory).
     *
     * @return File pointing to the external keystore location
     */
    public static File getExternalKeystoreFile() {
        return new File(getAppDirectory(), KEYSTORE_FILENAME);
    }

    /**
     * Returns the directory where the application is running from.
     * For JAR execution, this is the JAR's parent directory.
     * For IDE execution, this falls back to user.dir.
     *
     * @return the application directory
     */
    public static File getAppDirectory() {
        try {
            CodeSource codeSource = SecretsRepository.class.getProtectionDomain().getCodeSource();
            if (codeSource != null) {
                Path jarPath = Path.of(codeSource.getLocation().toURI());
                File jarFile = jarPath.toFile();
                if (jarFile.isFile()) {
                    return jarFile.getParentFile();
                } else {
                    // Running from classes directory (IDE)
                    return jarFile;
                }
            }
        } catch (URISyntaxException ex) {
            // Fall back to user.dir
        }
        return new File(System.getProperty("user.dir"));
    }
}
