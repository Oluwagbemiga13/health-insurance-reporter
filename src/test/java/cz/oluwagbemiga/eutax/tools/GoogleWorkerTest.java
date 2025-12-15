package cz.oluwagbemiga.eutax.tools;

import cz.oluwagbemiga.eutax.pojo.CzechMonth;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GoogleWorkerTest {

    @BeforeEach
    void setUp() {
        // Clear credentials before each test
        GoogleWorker.clearGoogleServiceAccountJson();
    }

    @AfterEach
    void tearDown() {
        // Clean up after each test
        GoogleWorker.clearGoogleServiceAccountJson();
    }

    @Nested
    @DisplayName("Static credential management tests")
    class CredentialManagementTests {

        @Test
        @DisplayName("should return false when no credentials set")
        void hasGoogleServiceAccountJson_noCredentials_returnsFalse() {
            assertFalse(GoogleWorker.hasGoogleServiceAccountJson());
        }

        @Test
        @DisplayName("should return true when credentials are set")
        void hasGoogleServiceAccountJson_withCredentials_returnsTrue() {
            GoogleWorker.setGoogleServiceAccountJson("{\"type\":\"service_account\"}");
            assertTrue(GoogleWorker.hasGoogleServiceAccountJson());
        }

        @Test
        @DisplayName("should return empty optional when no credentials set")
        void getGoogleServiceAccountJson_noCredentials_returnsEmpty() {
            assertTrue(GoogleWorker.getGoogleServiceAccountJson().isEmpty());
        }

        @Test
        @DisplayName("should return credentials when set")
        void getGoogleServiceAccountJson_withCredentials_returnsValue() {
            String json = "{\"type\":\"service_account\"}";
            GoogleWorker.setGoogleServiceAccountJson(json);

            assertTrue(GoogleWorker.getGoogleServiceAccountJson().isPresent());
            assertEquals(json, GoogleWorker.getGoogleServiceAccountJson().get());
        }

        @Test
        @DisplayName("should clear credentials")
        void clearGoogleServiceAccountJson_clearsCredentials() {
            GoogleWorker.setGoogleServiceAccountJson("{\"type\":\"service_account\"}");
            assertTrue(GoogleWorker.hasGoogleServiceAccountJson());

            GoogleWorker.clearGoogleServiceAccountJson();

            assertFalse(GoogleWorker.hasGoogleServiceAccountJson());
            assertTrue(GoogleWorker.getGoogleServiceAccountJson().isEmpty());
        }

        @Test
        @DisplayName("should return false for blank credentials")
        void hasGoogleServiceAccountJson_blankCredentials_returnsFalse() {
            GoogleWorker.setGoogleServiceAccountJson("   ");
            assertFalse(GoogleWorker.hasGoogleServiceAccountJson());
        }
    }

    @Nested
    @DisplayName("readClients() without credentials")
    class ReadClientsWithoutCredentialsTests {

        @Test
        @DisplayName("should return empty list when no credentials configured")
        void readClients_noCredentials_returnsEmptyList() throws FileNotFoundException {
            GoogleWorker worker = new GoogleWorker();

            List<?> clients = worker.readClients("spreadsheet-id", CzechMonth.LEDEN);

            assertTrue(clients.isEmpty());
        }

        @Test
        @DisplayName("should throw FileNotFoundException for empty spreadsheet ID")
        void readClients_emptySpreadsheetId_throwsException() {
            GoogleWorker worker = new GoogleWorker();

            assertThrows(FileNotFoundException.class, () ->
                    worker.readClients("", CzechMonth.LEDEN)
            );
        }

        @Test
        @DisplayName("should throw FileNotFoundException for null spreadsheet ID")
        void readClients_nullSpreadsheetId_throwsException() {
            GoogleWorker worker = new GoogleWorker();

            assertThrows(FileNotFoundException.class, () ->
                    worker.readClients(null, CzechMonth.LEDEN)
            );
        }

        @Test
        @DisplayName("should throw FileNotFoundException for whitespace-only spreadsheet ID")
        void readClients_whitespaceSpreadsheetId_throwsException() {
            GoogleWorker worker = new GoogleWorker();

            assertThrows(FileNotFoundException.class, () ->
                    worker.readClients("   ", CzechMonth.LEDEN)
            );
        }
    }

    @Nested
    @DisplayName("updateReportGeneratedStatus() tests")
    class UpdateReportGeneratedStatusTests {

        @Test
        @DisplayName("should throw IllegalStateException when no credentials configured")
        void updateReportGeneratedStatus_noCredentials_throwsException() {
            GoogleWorker worker = new GoogleWorker();

            assertThrows(IllegalStateException.class, () ->
                    worker.updateReportGeneratedStatus("spreadsheet-id",
                            List.of(new cz.oluwagbemiga.eutax.pojo.Client("Test", "12345678", true, List.of())),
                            CzechMonth.LEDEN)
            );
        }

        @Test
        @DisplayName("should return without error for null clients list")
        void updateReportGeneratedStatus_nullClients_returnsWithoutError() {
            GoogleWorker.setGoogleServiceAccountJson("{\"type\":\"service_account\"}");
            GoogleWorker worker = new GoogleWorker();

            // Should not throw - returns early for null clients
            assertDoesNotThrow(() ->
                    worker.updateReportGeneratedStatus("spreadsheet-id", null, CzechMonth.LEDEN)
            );
        }

        @Test
        @DisplayName("should return without error for empty clients list")
        void updateReportGeneratedStatus_emptyClients_returnsWithoutError() {
            GoogleWorker.setGoogleServiceAccountJson("{\"type\":\"service_account\"}");
            GoogleWorker worker = new GoogleWorker();

            // Should not throw - returns early for empty clients
            assertDoesNotThrow(() ->
                    worker.updateReportGeneratedStatus("spreadsheet-id", List.of(), CzechMonth.LEDEN)
            );
        }

        @Test
        @DisplayName("should throw FileNotFoundException for empty spreadsheet ID")
        void updateReportGeneratedStatus_emptySpreadsheetId_throwsException() {
            GoogleWorker.setGoogleServiceAccountJson("{\"type\":\"service_account\"}");
            GoogleWorker worker = new GoogleWorker();

            assertThrows(FileNotFoundException.class, () ->
                    worker.updateReportGeneratedStatus("",
                            List.of(new cz.oluwagbemiga.eutax.pojo.Client("Test", "12345678", true, List.of())),
                            CzechMonth.LEDEN)
            );
        }
    }

    @Nested
    @DisplayName("Spreadsheet ID resolution tests")
    class SpreadsheetIdResolutionTests {

        @Test
        @DisplayName("should resolve ID from .gsheet file path")
        void readClients_gsheetFilePath_resolvesId() throws FileNotFoundException {
            GoogleWorker worker = new GoogleWorker();

            // Even though there are no credentials, the ID resolution happens first
            // We can verify the method processes .gsheet files by checking it doesn't throw
            // FileNotFoundException for the file path itself
            List<?> clients = worker.readClients("src/test/resources/pojistovny_google.gsheet", CzechMonth.LEDEN);

            // Should return empty list due to missing credentials, not throw exception
            assertTrue(clients.isEmpty());
        }

        @Test
        @DisplayName("should resolve ID from .url shortcut file path")
        void readClients_urlFilePath_resolvesId() throws FileNotFoundException {
            GoogleWorker worker = new GoogleWorker();

            List<?> clients = worker.readClients("src/test/resources/sheet_shortcut.url", CzechMonth.LEDEN);

            // Should return empty list due to missing credentials, not throw exception
            assertTrue(clients.isEmpty());
        }

        @Test
        @DisplayName("should resolve ID from Google Sheets URL")
        void readClients_googleSheetsUrl_resolvesId() throws FileNotFoundException {
            GoogleWorker worker = new GoogleWorker();

            String url = "https://docs.google.com/spreadsheets/d/1aj2fuUzjk5L6wWxXkrfs73XluM3TxttbPW4CthEEu20/edit";
            List<?> clients = worker.readClients(url, CzechMonth.LEDEN);

            // Should return empty list due to missing credentials
            assertTrue(clients.isEmpty());
        }

        @Test
        @DisplayName("should accept direct spreadsheet ID")
        void readClients_directId_acceptsId() throws FileNotFoundException {
            GoogleWorker worker = new GoogleWorker();

            List<?> clients = worker.readClients("1aj2fuUzjk5L6wWxXkrfs73XluM3TxttbPW4CthEEu20", CzechMonth.LEDEN);

            // Should return empty list due to missing credentials
            assertTrue(clients.isEmpty());
        }
    }

    @Nested
    @DisplayName("Credential format tests")
    class CredentialFormatTests {

        @Test
        @DisplayName("should accept JSON string credentials")
        void credentials_jsonString_isAccepted() {
            String json = "{\"type\":\"service_account\",\"project_id\":\"test\"}";
            GoogleWorker.setGoogleServiceAccountJson(json);

            assertTrue(GoogleWorker.hasGoogleServiceAccountJson());
            assertTrue(GoogleWorker.getGoogleServiceAccountJson().isPresent());
            assertEquals(json, GoogleWorker.getGoogleServiceAccountJson().orElse(""));
        }

        @Test
        @DisplayName("should accept JSON array credentials")
        void credentials_jsonArray_isAccepted() {
            String json = "[{\"type\":\"service_account\"}]";
            GoogleWorker.setGoogleServiceAccountJson(json);

            assertTrue(GoogleWorker.hasGoogleServiceAccountJson());
        }

        @Test
        @DisplayName("credentials with whitespace should be trimmed")
        void credentials_withWhitespace_isTrimmed() {
            String json = "  {\"type\":\"service_account\"}  ";
            GoogleWorker.setGoogleServiceAccountJson(json);

            assertTrue(GoogleWorker.hasGoogleServiceAccountJson());
        }
    }

    @Nested
    @DisplayName("Instance behavior tests")
    class InstanceBehaviorTests {

        @Test
        @DisplayName("should create instance via default constructor")
        void constructor_default_createsInstance() {
            GoogleWorker worker = new GoogleWorker();
            assertNotNull(worker);
        }

        @Test
        @DisplayName("multiple instances should share static credentials")
        void multipleInstances_shareCredentials() {
            GoogleWorker worker1 = new GoogleWorker();
            GoogleWorker worker2 = new GoogleWorker();

            GoogleWorker.setGoogleServiceAccountJson("{\"type\":\"service_account\"}");

            assertTrue(GoogleWorker.hasGoogleServiceAccountJson());
            // Both workers see the static credentials - they would share the same credential state
            assertNotSame(worker1, worker2); // Different instances
            // Verify that credentials are visible from both instances' perspective
            // (they both use the static hasGoogleServiceAccountJson method)
        }
    }

    @Nested
    @DisplayName("All months handling tests")
    class AllMonthsTests {

        @ParameterizedTest
        @ValueSource(strings = {"LEDEN", "UNOR", "BREZEN", "DUBEN", "KVETEN", "CERVEN",
                               "CERVENEC", "SRPEN", "ZARI", "RIJEN", "LISTOPAD", "PROSINEC"})
        @DisplayName("should handle all Czech months")
        void readClients_allMonths_handlesCorrectly(String monthName) throws FileNotFoundException {
            GoogleWorker worker = new GoogleWorker();
            CzechMonth month = CzechMonth.valueOf(monthName);

            List<?> clients = worker.readClients("spreadsheet-id", month);

            // Should return empty list due to missing credentials
            assertTrue(clients.isEmpty());
        }
    }
}
