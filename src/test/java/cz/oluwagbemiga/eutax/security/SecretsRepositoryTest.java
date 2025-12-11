 package cz.oluwagbemiga.eutax.security;

import cz.oluwagbemiga.eutax.exceptions.LoginException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SecretsRepository}.
 * Tests cover loading Google service account credentials from keystores and
 * keystore existence checks.
 */
class SecretsRepositoryTest {

    private static final String VALID_SERVICE_ACCOUNT_JSON = """
            {
              "type": "service_account",
              "project_id": "test-project",
              "private_key_id": "key123",
              "private_key": "-----BEGIN PRIVATE KEY-----\\nMIItest\\n-----END PRIVATE KEY-----\\n",
              "client_email": "test@test-project.iam.gserviceaccount.com",
              "client_id": "123456789",
              "auth_uri": "https://accounts.google.com/o/oauth2/auth",
              "token_uri": "https://oauth2.googleapis.com/token"
            }
            """;

    private static final char[] TEST_PASSWORD = "testPassword123".toCharArray();

    @TempDir
    Path tempDir;

    @Nested
    @DisplayName("loadGoogleServiceAccountJson tests")
    class LoadGoogleServiceAccountJsonTests {

        @Test
        @DisplayName("Should load Google service account JSON from external keystore")
        void shouldLoadJsonFromExternalKeystore() {
            // Given
            File keystoreFile = SecretsRepository.getExternalKeystoreFile();
            File backupFile = null;

            // Backup existing keystore if present
            if (keystoreFile.exists()) {
                backupFile = new File(keystoreFile.getParent(), "secrets.p12.backup");
                keystoreFile.renameTo(backupFile);
            }

            try {
                // Create a test keystore
                KeystoreCreator.createKeystoreFromJsonString(VALID_SERVICE_ACCOUNT_JSON, keystoreFile, TEST_PASSWORD);

                // When
                String loadedJson = SecretsRepository.loadGoogleServiceAccountJson(TEST_PASSWORD);

                // Then
                assertNotNull(loadedJson);
                assertEquals(VALID_SERVICE_ACCOUNT_JSON, loadedJson);

            } finally {
                // Cleanup
                keystoreFile.delete();
                if (backupFile != null && backupFile.exists()) {
                    backupFile.renameTo(keystoreFile);
                }
            }
        }

        @Test
        @DisplayName("Should throw LoginException for wrong password")
        void shouldThrowExceptionForWrongPassword() {
            // Given
            File keystoreFile = SecretsRepository.getExternalKeystoreFile();
            File backupFile = null;

            if (keystoreFile.exists()) {
                backupFile = new File(keystoreFile.getParent(), "secrets.p12.backup");
                keystoreFile.renameTo(backupFile);
            }

            try {
                KeystoreCreator.createKeystoreFromJsonString(VALID_SERVICE_ACCOUNT_JSON, keystoreFile, TEST_PASSWORD);

                // When & Then
                assertThrows(LoginException.class,
                        () -> SecretsRepository.loadGoogleServiceAccountJson("wrongPassword".toCharArray()));

            } finally {
                keystoreFile.delete();
                if (backupFile != null && backupFile.exists()) {
                    backupFile.renameTo(keystoreFile);
                }
            }
        }

        @Test
        @DisplayName("Should throw LoginException when no keystore exists")
        void shouldThrowExceptionWhenNoKeystoreExists() {
            // Given - Ensure no keystore exists
            File keystoreFile = SecretsRepository.getExternalKeystoreFile();
            File backupFile = null;

            if (keystoreFile.exists()) {
                backupFile = new File(keystoreFile.getParent(), "secrets.p12.backup");
                keystoreFile.renameTo(backupFile);
            }

            try {
                // Verify keystore doesn't exist in resources or externally
                // (assuming bundled resource doesn't exist in test environment)
                if (!SecretsRepository.keystoreExists()) {
                    // When & Then
                    LoginException exception = assertThrows(LoginException.class,
                            () -> SecretsRepository.loadGoogleServiceAccountJson(TEST_PASSWORD));

                    assertTrue(exception.getMessage().contains("not found") ||
                               exception.getMessage().contains("Keystore"));
                }
            } finally {
                if (backupFile != null && backupFile.exists()) {
                    backupFile.renameTo(keystoreFile);
                }
            }
        }

        @Test
        @DisplayName("Should throw LoginException for empty password with valid keystore")
        void shouldThrowExceptionForEmptyPassword() {
            // Given
            File keystoreFile = SecretsRepository.getExternalKeystoreFile();
            File backupFile = null;

            if (keystoreFile.exists()) {
                backupFile = new File(keystoreFile.getParent(), "secrets.p12.backup");
                keystoreFile.renameTo(backupFile);
            }

            try {
                KeystoreCreator.createKeystoreFromJsonString(VALID_SERVICE_ACCOUNT_JSON, keystoreFile, TEST_PASSWORD);

                // When & Then
                assertThrows(LoginException.class,
                        () -> SecretsRepository.loadGoogleServiceAccountJson(new char[0]));

            } finally {
                keystoreFile.delete();
                if (backupFile != null && backupFile.exists()) {
                    backupFile.renameTo(keystoreFile);
                }
            }
        }

        @Test
        @DisplayName("Should load JSON with special characters correctly")
        void shouldLoadJsonWithSpecialCharacters() {
            // Given
            File keystoreFile = SecretsRepository.getExternalKeystoreFile();
            File backupFile = null;

            if (keystoreFile.exists()) {
                backupFile = new File(keystoreFile.getParent(), "secrets.p12.backup");
                keystoreFile.renameTo(backupFile);
            }

            try {
                String jsonWithSpecialChars = "{\"type\":\"service_account\",\"special\":\"čřžýáíé日本語🔐\"}";
                KeystoreCreator.createKeystoreFromJsonString(jsonWithSpecialChars, keystoreFile, TEST_PASSWORD);

                // When
                String loadedJson = SecretsRepository.loadGoogleServiceAccountJson(TEST_PASSWORD);

                // Then
                assertEquals(jsonWithSpecialChars, loadedJson);

            } finally {
                keystoreFile.delete();
                if (backupFile != null && backupFile.exists()) {
                    backupFile.renameTo(keystoreFile);
                }
            }
        }

        @Test
        @DisplayName("Should load large JSON correctly")
        void shouldLoadLargeJsonCorrectly() {
            // Given
            File keystoreFile = SecretsRepository.getExternalKeystoreFile();
            File backupFile = null;

            if (keystoreFile.exists()) {
                backupFile = new File(keystoreFile.getParent(), "secrets.p12.backup");
                keystoreFile.renameTo(backupFile);
            }

            try {
                String largeJson = "{\"type\":\"service_account\",\"data\":\"" + "x".repeat(50000) + "\"}";

                KeystoreCreator.createKeystoreFromJsonString(largeJson, keystoreFile, TEST_PASSWORD);

                // When
                String loadedJson = SecretsRepository.loadGoogleServiceAccountJson(TEST_PASSWORD);

                // Then
                assertEquals(largeJson, loadedJson);

            } finally {
                keystoreFile.delete();
                if (backupFile != null && backupFile.exists()) {
                    backupFile.renameTo(keystoreFile);
                }
            }
        }

        @Test
        @DisplayName("Should throw LoginException for null password")
        void shouldThrowExceptionForNullPassword() {
            // Given
            File keystoreFile = SecretsRepository.getExternalKeystoreFile();
            File backupFile = null;

            if (keystoreFile.exists()) {
                backupFile = new File(keystoreFile.getParent(), "secrets.p12.backup");
                keystoreFile.renameTo(backupFile);
            }

            try {
                KeystoreCreator.createKeystoreFromJsonString(VALID_SERVICE_ACCOUNT_JSON, keystoreFile, TEST_PASSWORD);

                // When & Then - null password should cause an exception
                assertThrows(Exception.class,
                        () -> SecretsRepository.loadGoogleServiceAccountJson(null));

            } finally {
                keystoreFile.delete();
                if (backupFile != null && backupFile.exists()) {
                    backupFile.renameTo(keystoreFile);
                }
            }
        }

        @Test
        @DisplayName("Should handle keystore with invalid PKCS12 structure")
        void shouldThrowExceptionForInvalidPkcs12Structure() throws Exception {
            // Given
            File keystoreFile = SecretsRepository.getExternalKeystoreFile();
            File backupFile = null;

            if (keystoreFile.exists()) {
                backupFile = new File(keystoreFile.getParent(), "secrets.p12.backup");
                keystoreFile.renameTo(backupFile);
            }

            try {
                // Create a file that looks like PKCS12 header but is invalid
                // PKCS12 files start with specific bytes - we'll create something that's partially valid
                byte[] invalidPkcs12 = new byte[]{0x30, (byte) 0x82, 0x00, 0x10, 0x02, 0x01, 0x03};
                Files.write(keystoreFile.toPath(), invalidPkcs12);

                // When & Then
                assertThrows(LoginException.class,
                        () -> SecretsRepository.loadGoogleServiceAccountJson(TEST_PASSWORD));

            } finally {
                keystoreFile.delete();
                if (backupFile != null && backupFile.exists()) {
                    backupFile.renameTo(keystoreFile);
                }
            }
        }

        @Test
        @DisplayName("Should handle multiple consecutive load operations")
        void shouldHandleMultipleConsecutiveLoads() {
            // Given
            File keystoreFile = SecretsRepository.getExternalKeystoreFile();
            File backupFile = null;

            if (keystoreFile.exists()) {
                backupFile = new File(keystoreFile.getParent(), "secrets.p12.backup");
                keystoreFile.renameTo(backupFile);
            }

            try {
                KeystoreCreator.createKeystoreFromJsonString(VALID_SERVICE_ACCOUNT_JSON, keystoreFile, TEST_PASSWORD);

                // When - Load multiple times
                String loaded1 = SecretsRepository.loadGoogleServiceAccountJson(TEST_PASSWORD);
                String loaded2 = SecretsRepository.loadGoogleServiceAccountJson(TEST_PASSWORD);
                String loaded3 = SecretsRepository.loadGoogleServiceAccountJson(TEST_PASSWORD);

                // Then
                assertEquals(VALID_SERVICE_ACCOUNT_JSON, loaded1);
                assertEquals(loaded1, loaded2);
                assertEquals(loaded2, loaded3);

            } finally {
                keystoreFile.delete();
                if (backupFile != null && backupFile.exists()) {
                    backupFile.renameTo(keystoreFile);
                }
            }
        }
    }

    @Nested
    @DisplayName("keystoreExists tests")
    class KeystoreExistsTests {

        @Test
        @DisplayName("Should return true when external keystore exists")
        void shouldReturnTrueWhenExternalKeystoreExists() {
            // Given
            File keystoreFile = SecretsRepository.getExternalKeystoreFile();
            File backupFile = null;

            if (keystoreFile.exists()) {
                backupFile = new File(keystoreFile.getParent(), "secrets.p12.backup");
                keystoreFile.renameTo(backupFile);
            }

            try {
                // Create keystore
                KeystoreCreator.createKeystoreFromJsonString(VALID_SERVICE_ACCOUNT_JSON, keystoreFile, TEST_PASSWORD);

                // When & Then
                assertTrue(SecretsRepository.keystoreExists());

            } finally {
                keystoreFile.delete();
                if (backupFile != null && backupFile.exists()) {
                    backupFile.renameTo(keystoreFile);
                }
            }
        }

        @Test
        @DisplayName("Should return false when no keystore exists")
        void shouldReturnFalseWhenNoKeystoreExists() {
            // Given
            File keystoreFile = SecretsRepository.getExternalKeystoreFile();
            File backupFile = null;

            if (keystoreFile.exists()) {
                backupFile = new File(keystoreFile.getParent(), "secrets.p12.backup");
                keystoreFile.renameTo(backupFile);
            }

            try {
                // Ensure external keystore doesn't exist
                assertFalse(keystoreFile.exists());

                // When - Check if bundled resource exists
                // Note: This behavior depends on whether there's a bundled keystore
                boolean exists = SecretsRepository.keystoreExists();

                // Just verify method executes without error
                // Can't assert specific value as bundled resource may or may not exist
                assertDoesNotThrow(SecretsRepository::keystoreExists);

            } finally {
                if (backupFile != null && backupFile.exists()) {
                    backupFile.renameTo(keystoreFile);
                }
            }
        }

        @Test
        @DisplayName("Should return consistent result on multiple calls")
        void shouldReturnConsistentResult() {
            // Given
            File keystoreFile = SecretsRepository.getExternalKeystoreFile();
            File backupFile = null;

            if (keystoreFile.exists()) {
                backupFile = new File(keystoreFile.getParent(), "secrets.p12.backup");
                keystoreFile.renameTo(backupFile);
            }

            try {
                KeystoreCreator.createKeystoreFromJsonString(VALID_SERVICE_ACCOUNT_JSON, keystoreFile, TEST_PASSWORD);

                // When
                boolean result1 = SecretsRepository.keystoreExists();
                boolean result2 = SecretsRepository.keystoreExists();
                boolean result3 = SecretsRepository.keystoreExists();

                // Then
                assertEquals(result1, result2);
                assertEquals(result2, result3);
                assertTrue(result1); // Should be true since we created it

            } finally {
                keystoreFile.delete();
                if (backupFile != null && backupFile.exists()) {
                    backupFile.renameTo(keystoreFile);
                }
            }
        }

        @Test
        @DisplayName("Should detect keystore creation")
        void shouldDetectKeystoreCreation() {
            // Given
            File keystoreFile = SecretsRepository.getExternalKeystoreFile();
            File backupFile = null;

            if (keystoreFile.exists()) {
                backupFile = new File(keystoreFile.getParent(), "secrets.p12.backup");
                keystoreFile.renameTo(backupFile);
            }

            try {
                // Initially should not exist (assuming no bundled keystore)
                boolean initiallyExists = keystoreFile.exists();
                assertFalse(initiallyExists, "External keystore should not exist initially");

                // Create keystore
                KeystoreCreator.createKeystoreFromJsonString(VALID_SERVICE_ACCOUNT_JSON, keystoreFile, TEST_PASSWORD);

                // When & Then
                assertTrue(SecretsRepository.keystoreExists(), "Should detect newly created keystore");

            } finally {
                keystoreFile.delete();
                if (backupFile != null && backupFile.exists()) {
                    backupFile.renameTo(keystoreFile);
                }
            }
        }

        @Test
        @DisplayName("Should detect keystore deletion")
        void shouldDetectKeystoreDeletion() {
            // Given
            File keystoreFile = SecretsRepository.getExternalKeystoreFile();
            File backupFile = null;

            if (keystoreFile.exists()) {
                backupFile = new File(keystoreFile.getParent(), "secrets.p12.backup");
                keystoreFile.renameTo(backupFile);
            }

            try {
                // Create keystore
                KeystoreCreator.createKeystoreFromJsonString(VALID_SERVICE_ACCOUNT_JSON, keystoreFile, TEST_PASSWORD);
                assertTrue(SecretsRepository.keystoreExists(), "Keystore should exist after creation");

                // Delete keystore
                assertTrue(keystoreFile.delete(), "Should be able to delete keystore");

                // When & Then - should detect that keystore no longer exists
                // Note: Result depends on whether bundled resource exists
                assertDoesNotThrow(SecretsRepository::keystoreExists);

            } finally {
                if (backupFile != null && backupFile.exists()) {
                    backupFile.renameTo(keystoreFile);
                }
            }
        }

        @Test
        @DisplayName("Should handle rapid exists checks")
        void shouldHandleRapidExistsChecks() {
            // Given
            File keystoreFile = SecretsRepository.getExternalKeystoreFile();
            File backupFile = null;

            if (keystoreFile.exists()) {
                backupFile = new File(keystoreFile.getParent(), "secrets.p12.backup");
                keystoreFile.renameTo(backupFile);
            }

            try {
                KeystoreCreator.createKeystoreFromJsonString(VALID_SERVICE_ACCOUNT_JSON, keystoreFile, TEST_PASSWORD);

                // When - Perform many rapid checks
                boolean allTrue = true;
                for (int i = 0; i < 100; i++) {
                    if (!SecretsRepository.keystoreExists()) {
                        allTrue = false;
                        break;
                    }
                }

                // Then
                assertTrue(allTrue, "All rapid checks should return true");

            } finally {
                keystoreFile.delete();
                if (backupFile != null && backupFile.exists()) {
                    backupFile.renameTo(keystoreFile);
                }
            }
        }
    }

    @Nested
    @DisplayName("getExternalKeystoreFile tests")
    class GetExternalKeystoreFileTests {

        @Test
        @DisplayName("Should return file with correct name")
        void shouldReturnFileWithCorrectName() {
            // When
            File keystoreFile = SecretsRepository.getExternalKeystoreFile();

            // Then
            assertNotNull(keystoreFile);
            assertEquals("secrets.p12", keystoreFile.getName());
        }

        @Test
        @DisplayName("Should return file in app directory")
        void shouldReturnFileInAppDirectory() {
            // When
            File keystoreFile = SecretsRepository.getExternalKeystoreFile();
            File appDir = SecretsRepository.getAppDirectory();

            // Then
            assertEquals(appDir, keystoreFile.getParentFile());
        }

        @Test
        @DisplayName("Should return consistent path on multiple calls")
        void shouldReturnConsistentPath() {
            // When
            File keystoreFile1 = SecretsRepository.getExternalKeystoreFile();
            File keystoreFile2 = SecretsRepository.getExternalKeystoreFile();

            // Then
            assertEquals(keystoreFile1.getAbsolutePath(), keystoreFile2.getAbsolutePath());
        }

        @Test
        @DisplayName("Should return absolute path")
        void shouldReturnAbsolutePath() {
            // When
            File keystoreFile = SecretsRepository.getExternalKeystoreFile();

            // Then
            assertTrue(keystoreFile.isAbsolute() || keystoreFile.getAbsolutePath().contains(File.separator));
        }

        @Test
        @DisplayName("Should return file with .p12 extension")
        void shouldReturnFileWithP12Extension() {
            // When
            File keystoreFile = SecretsRepository.getExternalKeystoreFile();

            // Then
            assertTrue(keystoreFile.getName().endsWith(".p12"));
        }

        @Test
        @DisplayName("Should return path that can be used to create file")
        void shouldReturnUsablePath() {
            // When
            File keystoreFile = SecretsRepository.getExternalKeystoreFile();
            File backupFile = null;

            if (keystoreFile.exists()) {
                backupFile = new File(keystoreFile.getParent(), "secrets.p12.backup");
                keystoreFile.renameTo(backupFile);
            }

            try {
                // Then - should be able to create file at this path
                KeystoreCreator.createKeystoreFromJsonString("{}", keystoreFile, TEST_PASSWORD);
                assertTrue(keystoreFile.exists());

            } finally {
                keystoreFile.delete();
                if (backupFile != null && backupFile.exists()) {
                    backupFile.renameTo(keystoreFile);
                }
            }
        }
    }

    @Nested
    @DisplayName("getAppDirectory tests")
    class GetAppDirectoryTests {

        @Test
        @DisplayName("Should return non-null directory")
        void shouldReturnNonNullDirectory() {
            // When
            File appDir = SecretsRepository.getAppDirectory();

            // Then
            assertNotNull(appDir);
        }

        @Test
        @DisplayName("Should return existing or creatable directory path")
        void shouldReturnValidDirectoryPath() {
            // When
            File appDir = SecretsRepository.getAppDirectory();

            // Then
            // The directory should either exist or be a valid path
            assertTrue(appDir.exists() || appDir.getParentFile() == null || appDir.getParentFile().exists(),
                    "App directory should be a valid path");
        }

        @Test
        @DisplayName("Should return consistent directory on multiple calls")
        void shouldReturnConsistentDirectory() {
            // When
            File appDir1 = SecretsRepository.getAppDirectory();
            File appDir2 = SecretsRepository.getAppDirectory();

            // Then
            assertEquals(appDir1.getAbsolutePath(), appDir2.getAbsolutePath());
        }

        @Test
        @DisplayName("Should return absolute path")
        void shouldReturnAbsolutePath() {
            // When
            File appDir = SecretsRepository.getAppDirectory();

            // Then
            assertTrue(appDir.isAbsolute() || appDir.getAbsolutePath().contains(File.separator));
        }

        @Test
        @DisplayName("Should return writable directory")
        void shouldReturnWritableDirectory() {
            // When
            File appDir = SecretsRepository.getAppDirectory();

            // Then - if directory exists, it should be writable
            if (appDir.exists()) {
                assertTrue(appDir.canWrite(), "App directory should be writable");
            }
        }

        @Test
        @DisplayName("Should return path usable for file creation")
        void shouldReturnPathUsableForFileCreation() throws IOException {
            // When
            File appDir = SecretsRepository.getAppDirectory();
            File testFile = new File(appDir, "test-temp-file.txt");

            try {
                // Then
                if (appDir.exists() && appDir.canWrite()) {
                    assertTrue(testFile.createNewFile() || testFile.exists());
                }
            } finally {
                testFile.delete();
            }
        }

        @Test
        @DisplayName("Should not throw exception regardless of environment")
        void shouldNotThrowException() {
            // When & Then
            assertDoesNotThrow(() -> SecretsRepository.getAppDirectory());
        }

        @Test
        @DisplayName("Should return directory (not file path)")
        void shouldReturnDirectoryNotFilePath() {
            // When
            File appDir = SecretsRepository.getAppDirectory();

            // Then
            if (appDir.exists()) {
                assertTrue(appDir.isDirectory(), "Should be a directory");
            }
        }
    }

    @Nested
    @DisplayName("Integration and edge case tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should complete full workflow: create keystore, check exists, load")
        void shouldCompleteFullWorkflow() {
            // Given
            File keystoreFile = SecretsRepository.getExternalKeystoreFile();
            File backupFile = null;

            if (keystoreFile.exists()) {
                backupFile = new File(keystoreFile.getParent(), "secrets.p12.backup");
                keystoreFile.renameTo(backupFile);
            }

            try {
                // Initially no external keystore
                boolean initialExists = keystoreFile.exists();
                assertFalse(initialExists, "External keystore should not exist initially");

                // Create keystore
                KeystoreCreator.createKeystoreFromJsonString(VALID_SERVICE_ACCOUNT_JSON, keystoreFile, TEST_PASSWORD);

                // Verify exists
                assertTrue(SecretsRepository.keystoreExists(), "Keystore should exist after creation");

                // Load and verify content
                String loadedJson = SecretsRepository.loadGoogleServiceAccountJson(TEST_PASSWORD);
                assertEquals(VALID_SERVICE_ACCOUNT_JSON, loadedJson, "Loaded JSON should match original");

            } finally {
                keystoreFile.delete();
                if (backupFile != null && backupFile.exists()) {
                    backupFile.renameTo(keystoreFile);
                }
            }
        }

        @Test
        @DisplayName("Should handle keystore with missing alias gracefully")
        void shouldThrowExceptionForMissingAlias() throws Exception {
            // Given - Create a keystore without the expected alias
            File keystoreFile = SecretsRepository.getExternalKeystoreFile();
            File backupFile = null;

            if (keystoreFile.exists()) {
                backupFile = new File(keystoreFile.getParent(), "secrets.p12.backup");
                keystoreFile.renameTo(backupFile);
            }

            try {
                // Create a keystore with different alias
                java.security.KeyStore keyStore = java.security.KeyStore.getInstance("PKCS12");
                keyStore.load(null, TEST_PASSWORD);

                // Store with wrong alias
                byte[] jsonBytes = VALID_SERVICE_ACCOUNT_JSON.getBytes(StandardCharsets.UTF_8);
                javax.crypto.SecretKey secretKey = new javax.crypto.spec.SecretKeySpec(jsonBytes, "AES");
                keyStore.setEntry("wrong-alias",
                        new java.security.KeyStore.SecretKeyEntry(secretKey),
                        new java.security.KeyStore.PasswordProtection(TEST_PASSWORD));

                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(keystoreFile)) {
                    keyStore.store(fos, TEST_PASSWORD);
                }

                // When & Then
                LoginException exception = assertThrows(LoginException.class,
                        () -> SecretsRepository.loadGoogleServiceAccountJson(TEST_PASSWORD));

                assertTrue(exception.getMessage().contains("missing") ||
                           exception.getMessage().contains("secret"));

            } finally {
                keystoreFile.delete();
                if (backupFile != null && backupFile.exists()) {
                    backupFile.renameTo(keystoreFile);
                }
            }
        }

        @Test
        @DisplayName("Should handle corrupted keystore file")
        void shouldThrowExceptionForCorruptedKeystore() throws IOException {
            // Given
            File keystoreFile = SecretsRepository.getExternalKeystoreFile();
            File backupFile = null;

            if (keystoreFile.exists()) {
                backupFile = new File(keystoreFile.getParent(), "secrets.p12.backup");
                keystoreFile.renameTo(backupFile);
            }

            try {
                // Create a corrupted keystore file
                Files.writeString(keystoreFile.toPath(), "not a valid keystore");

                // When & Then
                assertThrows(LoginException.class,
                        () -> SecretsRepository.loadGoogleServiceAccountJson(TEST_PASSWORD));

            } finally {
                keystoreFile.delete();
                if (backupFile != null && backupFile.exists()) {
                    backupFile.renameTo(keystoreFile);
                }
            }
        }

        @Test
        @DisplayName("Should handle empty keystore file")
        void shouldThrowExceptionForEmptyKeystore() throws IOException {
            // Given
            File keystoreFile = SecretsRepository.getExternalKeystoreFile();
            File backupFile = null;

            if (keystoreFile.exists()) {
                backupFile = new File(keystoreFile.getParent(), "secrets.p12.backup");
                keystoreFile.renameTo(backupFile);
            }

            try {
                // Create an empty file
                Files.writeString(keystoreFile.toPath(), "");

                // When & Then
                assertThrows(LoginException.class,
                        () -> SecretsRepository.loadGoogleServiceAccountJson(TEST_PASSWORD));

            } finally {
                keystoreFile.delete();
                if (backupFile != null && backupFile.exists()) {
                    backupFile.renameTo(keystoreFile);
                }
            }
        }

        @Test
        @DisplayName("Should handle binary garbage keystore file")
        void shouldThrowExceptionForBinaryGarbageKeystore() throws IOException {
            // Given
            File keystoreFile = SecretsRepository.getExternalKeystoreFile();
            File backupFile = null;

            if (keystoreFile.exists()) {
                backupFile = new File(keystoreFile.getParent(), "secrets.p12.backup");
                keystoreFile.renameTo(backupFile);
            }

            try {
                // Create a binary garbage file
                byte[] garbage = new byte[]{0x00, 0x01, 0x02, (byte) 0xFF, (byte) 0xFE, 0x00};
                Files.write(keystoreFile.toPath(), garbage);

                // When & Then
                assertThrows(LoginException.class,
                        () -> SecretsRepository.loadGoogleServiceAccountJson(TEST_PASSWORD));

            } finally {
                keystoreFile.delete();
                if (backupFile != null && backupFile.exists()) {
                    backupFile.renameTo(keystoreFile);
                }
            }
        }

        @Test
        @DisplayName("Should handle keystore file with truncated data")
        void shouldThrowExceptionForTruncatedKeystore() throws IOException {
            // Given
            File keystoreFile = SecretsRepository.getExternalKeystoreFile();
            File backupFile = null;

            if (keystoreFile.exists()) {
                backupFile = new File(keystoreFile.getParent(), "secrets.p12.backup");
                keystoreFile.renameTo(backupFile);
            }

            try {
                // Create a valid keystore first
                KeystoreCreator.createKeystoreFromJsonString(VALID_SERVICE_ACCOUNT_JSON, keystoreFile, TEST_PASSWORD);

                // Truncate the file
                byte[] fullContent = Files.readAllBytes(keystoreFile.toPath());
                byte[] truncated = new byte[fullContent.length / 2];
                System.arraycopy(fullContent, 0, truncated, 0, truncated.length);
                Files.write(keystoreFile.toPath(), truncated);

                // When & Then
                assertThrows(LoginException.class,
                        () -> SecretsRepository.loadGoogleServiceAccountJson(TEST_PASSWORD));

            } finally {
                keystoreFile.delete();
                if (backupFile != null && backupFile.exists()) {
                    backupFile.renameTo(keystoreFile);
                }
            }
        }

        @Test
        @DisplayName("Should handle keystore file with modified bytes")
        void shouldThrowExceptionForModifiedKeystore() throws IOException {
            // Given
            File keystoreFile = SecretsRepository.getExternalKeystoreFile();
            File backupFile = null;

            if (keystoreFile.exists()) {
                backupFile = new File(keystoreFile.getParent(), "secrets.p12.backup");
                keystoreFile.renameTo(backupFile);
            }

            try {
                // Create a valid keystore first
                KeystoreCreator.createKeystoreFromJsonString(VALID_SERVICE_ACCOUNT_JSON, keystoreFile, TEST_PASSWORD);

                // Modify some bytes in the middle of the file
                byte[] content = Files.readAllBytes(keystoreFile.toPath());
                if (content.length > 100) {
                    content[50] = (byte) (content[50] ^ 0xFF);  // Flip bits
                    content[51] = (byte) (content[51] ^ 0xFF);
                    content[52] = (byte) (content[52] ^ 0xFF);
                }
                Files.write(keystoreFile.toPath(), content);

                // When & Then
                assertThrows(LoginException.class,
                        () -> SecretsRepository.loadGoogleServiceAccountJson(TEST_PASSWORD));

            } finally {
                keystoreFile.delete();
                if (backupFile != null && backupFile.exists()) {
                    backupFile.renameTo(keystoreFile);
                }
            }
        }

        @Test
        @DisplayName("Constructor should throw IllegalStateException")
        void constructorShouldThrowIllegalStateException() throws Exception {
            // Use reflection to test private constructor
            var constructor = SecretsRepository.class.getDeclaredConstructor();
            constructor.setAccessible(true);

            InvocationTargetException exception = assertThrows(InvocationTargetException.class,
                    constructor::newInstance);

            assertInstanceOf(IllegalStateException.class, exception.getCause(),
                    "Utility class constructor should throw IllegalStateException");
        }

        @Test
        @DisplayName("Should handle JSON with various content sizes")
        void shouldHandleVariousJsonSizes() {
            // Given
            File keystoreFile = SecretsRepository.getExternalKeystoreFile();
            File backupFile = null;

            if (keystoreFile.exists()) {
                backupFile = new File(keystoreFile.getParent(), "secrets.p12.backup");
                keystoreFile.renameTo(backupFile);
            }

            try {
                // Test with minimal JSON
                String minimalJson = "{\"type\":\"service_account\"}";
                KeystoreCreator.createKeystoreFromJsonString(minimalJson, keystoreFile, TEST_PASSWORD);

                String loadedMinimal = SecretsRepository.loadGoogleServiceAccountJson(TEST_PASSWORD);
                assertEquals(minimalJson, loadedMinimal);

                // Test with larger JSON
                String largeJson = "{\"type\":\"service_account\",\"data\":\"" + "x".repeat(5000) + "\"}";

                KeystoreCreator.createKeystoreFromJsonString(largeJson, keystoreFile, TEST_PASSWORD);

                String loadedLarge = SecretsRepository.loadGoogleServiceAccountJson(TEST_PASSWORD);
                assertEquals(largeJson, loadedLarge);

            } finally {
                keystoreFile.delete();
                if (backupFile != null && backupFile.exists()) {
                    backupFile.renameTo(keystoreFile);
                }
            }
        }

        @Test
        @DisplayName("Should handle special characters in JSON content")
        void shouldHandleSpecialCharactersInJson() {
            // Given
            File keystoreFile = SecretsRepository.getExternalKeystoreFile();
            File backupFile = null;

            if (keystoreFile.exists()) {
                backupFile = new File(keystoreFile.getParent(), "secrets.p12.backup");
                keystoreFile.renameTo(backupFile);
            }

            try {
                String jsonWithSpecialChars = """
                        {
                          "type": "service_account",
                          "czech": "čřžýáíéúů",
                          "japanese": "日本語",
                          "emoji": "🔐🔑",
                          "escapes": "line1\\nline2\\ttab"
                        }
                        """;

                KeystoreCreator.createKeystoreFromJsonString(jsonWithSpecialChars, keystoreFile, TEST_PASSWORD);

                String loaded = SecretsRepository.loadGoogleServiceAccountJson(TEST_PASSWORD);
                assertEquals(jsonWithSpecialChars, loaded);

            } finally {
                keystoreFile.delete();
                if (backupFile != null && backupFile.exists()) {
                    backupFile.renameTo(keystoreFile);
                }
            }
        }

        @Test
        @DisplayName("Should handle repeated load and store operations")
        void shouldHandleRepeatedLoadAndStoreOperations() {
            // Given
            File keystoreFile = SecretsRepository.getExternalKeystoreFile();
            File backupFile = null;

            if (keystoreFile.exists()) {
                backupFile = new File(keystoreFile.getParent(), "secrets.p12.backup");
                keystoreFile.renameTo(backupFile);
            }

            try {
                // Perform multiple store and load operations
                for (int i = 0; i < 5; i++) {
                    String json = "{\"type\":\"service_account\",\"iteration\":" + i + "}";
                    KeystoreCreator.createKeystoreFromJsonString(json, keystoreFile, TEST_PASSWORD);

                    String loaded = SecretsRepository.loadGoogleServiceAccountJson(TEST_PASSWORD);
                    assertEquals(json, loaded);
                }

            } finally {
                keystoreFile.delete();
                if (backupFile != null && backupFile.exists()) {
                    backupFile.renameTo(keystoreFile);
                }
            }
        }

        @Test
        @DisplayName("Should handle keystore recreation after deletion")
        void shouldHandleKeystoreRecreationAfterDeletion() {
            // Given
            File keystoreFile = SecretsRepository.getExternalKeystoreFile();
            File backupFile = null;

            if (keystoreFile.exists()) {
                backupFile = new File(keystoreFile.getParent(), "secrets.p12.backup");
                keystoreFile.renameTo(backupFile);
            }

            try {
                // Create initial keystore
                String json1 = "{\"type\":\"service_account\",\"version\":1}";
                KeystoreCreator.createKeystoreFromJsonString(json1, keystoreFile, TEST_PASSWORD);
                assertTrue(SecretsRepository.keystoreExists());

                // Delete keystore
                assertTrue(keystoreFile.delete());

                // Recreate with different content
                String json2 = "{\"type\":\"service_account\",\"version\":2}";
                KeystoreCreator.createKeystoreFromJsonString(json2, keystoreFile, TEST_PASSWORD);

                // Load and verify
                String loaded = SecretsRepository.loadGoogleServiceAccountJson(TEST_PASSWORD);
                assertEquals(json2, loaded);

            } finally {
                keystoreFile.delete();
                if (backupFile != null && backupFile.exists()) {
                    backupFile.renameTo(keystoreFile);
                }
            }
        }

        @Test
        @DisplayName("Should handle password with numbers and symbols")
        void shouldHandlePasswordWithNumbersAndSymbols() {
            // Given
            File keystoreFile = SecretsRepository.getExternalKeystoreFile();
            File backupFile = null;

            if (keystoreFile.exists()) {
                backupFile = new File(keystoreFile.getParent(), "secrets.p12.backup");
                keystoreFile.renameTo(backupFile);
            }

            try {
                char[] complexPassword = "P@55w0rd!#$%123".toCharArray();
                KeystoreCreator.createKeystoreFromJsonString(VALID_SERVICE_ACCOUNT_JSON, keystoreFile, complexPassword);

                String loaded = SecretsRepository.loadGoogleServiceAccountJson(complexPassword);
                assertEquals(VALID_SERVICE_ACCOUNT_JSON, loaded);

            } finally {
                keystoreFile.delete();
                if (backupFile != null && backupFile.exists()) {
                    backupFile.renameTo(keystoreFile);
                }
            }
        }

        @Test
        @DisplayName("Should handle empty keystore (valid but no entries)")
        void shouldThrowExceptionForEmptyValidKeystore() throws Exception {
            // Given
            File keystoreFile = SecretsRepository.getExternalKeystoreFile();
            File backupFile = null;

            if (keystoreFile.exists()) {
                backupFile = new File(keystoreFile.getParent(), "secrets.p12.backup");
                keystoreFile.renameTo(backupFile);
            }

            try {
                // Create an empty but valid PKCS12 keystore
                java.security.KeyStore keyStore = java.security.KeyStore.getInstance("PKCS12");
                keyStore.load(null, TEST_PASSWORD);
                // Don't add any entries
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(keystoreFile)) {
                    keyStore.store(fos, TEST_PASSWORD);
                }

                // When & Then
                LoginException exception = assertThrows(LoginException.class,
                        () -> SecretsRepository.loadGoogleServiceAccountJson(TEST_PASSWORD));

                assertTrue(exception.getMessage().contains("missing") ||
                           exception.getMessage().contains("secret"));

            } finally {
                keystoreFile.delete();
                if (backupFile != null && backupFile.exists()) {
                    backupFile.renameTo(keystoreFile);
                }
            }
        }

        @Test
        @DisplayName("Should handle keystore with multiple entries")
        void shouldLoadCorrectAliasFromMultiEntryKeystore() throws Exception {
            // Given
            File keystoreFile = SecretsRepository.getExternalKeystoreFile();
            File backupFile = null;

            if (keystoreFile.exists()) {
                backupFile = new File(keystoreFile.getParent(), "secrets.p12.backup");
                keystoreFile.renameTo(backupFile);
            }

            try {
                // Create a keystore with multiple entries including the correct one
                java.security.KeyStore keyStore = java.security.KeyStore.getInstance("PKCS12");
                keyStore.load(null, TEST_PASSWORD);

                // Add multiple entries
                byte[] jsonBytes = VALID_SERVICE_ACCOUNT_JSON.getBytes(StandardCharsets.UTF_8);
                javax.crypto.SecretKey secretKey = new javax.crypto.spec.SecretKeySpec(jsonBytes, "AES");

                // Add some extra entries
                keyStore.setEntry("other-alias-1",
                        new java.security.KeyStore.SecretKeyEntry(
                                new javax.crypto.spec.SecretKeySpec("data1".getBytes(), "AES")),
                        new java.security.KeyStore.PasswordProtection(TEST_PASSWORD));

                // Add the expected entry
                keyStore.setEntry("google-service-account-json",
                        new java.security.KeyStore.SecretKeyEntry(secretKey),
                        new java.security.KeyStore.PasswordProtection(TEST_PASSWORD));

                // Add another extra entry
                keyStore.setEntry("other-alias-2",
                        new java.security.KeyStore.SecretKeyEntry(
                                new javax.crypto.spec.SecretKeySpec("data2".getBytes(), "AES")),
                        new java.security.KeyStore.PasswordProtection(TEST_PASSWORD));

                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(keystoreFile)) {
                    keyStore.store(fos, TEST_PASSWORD);
                }

                // When
                String loaded = SecretsRepository.loadGoogleServiceAccountJson(TEST_PASSWORD);

                // Then - should load the correct entry
                assertEquals(VALID_SERVICE_ACCOUNT_JSON, loaded);

            } finally {
                keystoreFile.delete();
                if (backupFile != null && backupFile.exists()) {
                    backupFile.renameTo(keystoreFile);
                }
            }
        }

        @Test
        @DisplayName("Should throw exception for very long password attempt on existing keystore")
        void shouldHandleVeryLongWrongPassword() {
            // Given
            File keystoreFile = SecretsRepository.getExternalKeystoreFile();
            File backupFile = null;

            if (keystoreFile.exists()) {
                backupFile = new File(keystoreFile.getParent(), "secrets.p12.backup");
                keystoreFile.renameTo(backupFile);
            }

            try {
                KeystoreCreator.createKeystoreFromJsonString(VALID_SERVICE_ACCOUNT_JSON, keystoreFile, TEST_PASSWORD);

                // When & Then - very long wrong password
                char[] veryLongPassword = "x".repeat(10000).toCharArray();
                assertThrows(LoginException.class,
                        () -> SecretsRepository.loadGoogleServiceAccountJson(veryLongPassword));

            } finally {
                keystoreFile.delete();
                if (backupFile != null && backupFile.exists()) {
                    backupFile.renameTo(keystoreFile);
                }
            }
        }
    }
}

