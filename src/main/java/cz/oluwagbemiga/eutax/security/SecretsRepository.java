package cz.oluwagbemiga.eutax.security;

import cz.oluwagbemiga.eutax.exceptions.LoginException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;

/**
 * Loads sensitive data from the packaged PKCS#12 keystore.
 */
public final class SecretsRepository {

    private static final String KEYSTORE_RESOURCE = "/secrets.p12";
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
        try (InputStream stream = SecretsRepository.class.getResourceAsStream(KEYSTORE_RESOURCE)) {
            if (stream == null) {
                throw new LoginException("Keystore secrets.p12 was not found in resources");
            }
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(stream, password);
            return keyStore;
        } catch (IOException ex) {
            throw new LoginException("Unable to read application keystore", ex);
        } catch (KeyStoreException | NoSuchAlgorithmException ex) {
            throw new LoginException("Keystore initialization failed", ex);
        } catch (Exception ex) {
            throw new LoginException("Unexpected keystore error", ex);
        }
    }
}
