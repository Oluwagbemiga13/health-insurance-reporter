package cz.oluwagbemiga.eutax.tools;

import cz.oluwagbemiga.eutax.pojo.Client;
import cz.oluwagbemiga.eutax.pojo.CzechMonth;
import cz.oluwagbemiga.eutax.pojo.InsuranceCompany;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExcelWorkerTest {
    @Test
    @SneakyThrows
    void testReadClients() {
        ExcelWorker reader = new ExcelWorker();
        String filePath = "src/test/resources/Zdrav. pojišťovny.xlsx";

        // Test reading from a month sheet - try with different months
        for (CzechMonth month : CzechMonth.values()) {
            List<Client> clients = reader.readClients(filePath, month);

            if (!clients.isEmpty()) {
                System.out.println("\n=== Found clients in sheet: " + month.getCzechName() + " ===");
                System.out.println("Number of clients read: " + clients.size());

                // Assert the values
                assertEquals(3, clients.size(), "Should read exactly 3 clients");

                // Assert first client
                assertEquals("Klient A s.r.o", clients.get(0).name());
                assertEquals("00000001", clients.get(0).ico());
                assertTrue(clients.get(0).reportGenerated());

                // Assert second client
                assertEquals("Klient 2 s.r.o", clients.get(1).name());
                assertEquals("00000002", clients.get(1).ico());
                assertTrue(clients.get(1).reportGenerated());

                // Assert third client
                assertEquals("Další klient a.s.", clients.get(2).name());
                assertEquals("00000003", clients.get(2).ico());
                assertFalse(clients.get(2).reportGenerated());

                break;
            }
        }
    }

    @Test
    @SneakyThrows
    void testReadClientsUnor() {
        ExcelWorker reader = new ExcelWorker();
        String filePath = "src/test/resources/Zdrav. pojišťovny.xlsx";

        List<Client> clients = reader.readClients(filePath, CzechMonth.UNOR);

        System.out.println("\n=== Testing UNOR sheet ===");
        System.out.println("Number of clients read: " + clients.size());

        // Print out the clients for verification
        clients.forEach(client ->
                System.out.println("Client: " + client.name() +
                        ", ICO: " + client.ico() +
                        ", Report Generated: " + client.reportGenerated())
        );

        // Assert the values
        assertEquals(3, clients.size(), "Should read exactly 3 clients");

        // Assert first client
        assertEquals("Klient A s.r.o", clients.get(0).name());
        assertEquals("00000001", clients.get(0).ico());
        assertTrue(clients.get(0).reportGenerated());

        // Assert second client
        assertEquals("Klient 2 s.r.o", clients.get(1).name());
        assertEquals("00000002", clients.get(1).ico());
        assertTrue(clients.get(1).reportGenerated());

        // Assert third client
        assertEquals("Další klient a.s.", clients.get(2).name());
        assertEquals("00000003", clients.get(2).ico());
        assertTrue(clients.get(2).reportGenerated());
    }

    @Test
    @SneakyThrows
    void testFileNotFound() {
        ExcelWorker reader = new ExcelWorker();
        assertThrows(FileNotFoundException.class, () -> {
            reader.readClients("nonexistent.xlsx", CzechMonth.LEDEN);
        });
    }

    @Test
    @SneakyThrows
    void testUpdateReportGeneratedStatus(@TempDir Path tempDir) {
        // Copy the test file to temp directory so we don't modify the original
        Path originalFile = Path.of("src/test/resources/Zdrav. pojišťovny.xlsx");
        Path tempFile = tempDir.resolve("test_update.xlsx");
        Files.copy(originalFile, tempFile, StandardCopyOption.REPLACE_EXISTING);

        ExcelWorker excelWorker = new ExcelWorker();
        String filePath = tempFile.toString();

        // Read the original clients from UNOR sheet
        List<Client> originalClients = excelWorker.readClients(filePath, CzechMonth.UNOR);
        assertEquals(3, originalClients.size());

        // Create updated clients - note: method only sets values TO true, never to false
        // So clients with reportGenerated=false in the list will be ignored
        List<Client> updatedClients = List.of(
                new Client("Klient A s.r.o", "00000001", true, List.of()),   // Already true, should stay true
                new Client("Klient 2 s.r.o", "00000002", true, List.of()),   // Already true, should stay true
                new Client("Další klient a.s.", "00000003", true, List.of()) // Already true, should stay true
        );

        // Update the file
        excelWorker.updateReportGeneratedStatus(filePath, updatedClients, CzechMonth.UNOR);

        // Read the clients again and verify the values (all should remain true)
        List<Client> clientsAfterUpdate = excelWorker.readClients(filePath, CzechMonth.UNOR);
        assertEquals(3, clientsAfterUpdate.size());

        // Assert all values are true (method never sets to false)
        assertEquals("Klient A s.r.o", clientsAfterUpdate.get(0).name());
        assertEquals("00000001", clientsAfterUpdate.get(0).ico());
        assertTrue(clientsAfterUpdate.get(0).reportGenerated(), "First client should remain true");

        assertEquals("Klient 2 s.r.o", clientsAfterUpdate.get(1).name());
        assertEquals("00000002", clientsAfterUpdate.get(1).ico());
        assertTrue(clientsAfterUpdate.get(1).reportGenerated(), "Second client should remain true");

        assertEquals("Další klient a.s.", clientsAfterUpdate.get(2).name());
        assertEquals("00000003", clientsAfterUpdate.get(2).ico());
        assertTrue(clientsAfterUpdate.get(2).reportGenerated(), "Third client should remain true");
    }

    @Test
    @SneakyThrows
    void testUpdateReportGeneratedStatus_PartialUpdate(@TempDir Path tempDir) {
        // Copy the test file to temp directory
        Path originalFile = Path.of("src/test/resources/Zdrav. pojišťovny.xlsx");
        Path tempFile = tempDir.resolve("test_partial_update.xlsx");
        Files.copy(originalFile, tempFile, StandardCopyOption.REPLACE_EXISTING);

        ExcelWorker excelWorker = new ExcelWorker();
        String filePath = tempFile.toString();

        // Only update one client with reportGenerated=true
        // Note: method ignores clients with reportGenerated=false
        List<Client> partialUpdate = List.of(
                new Client("Klient A s.r.o", "00000001", true, List.of())
         );

        // Update the file
        excelWorker.updateReportGeneratedStatus(filePath, partialUpdate, CzechMonth.UNOR);

        // Read the clients and verify values
        List<Client> clientsAfterUpdate = excelWorker.readClients(filePath, CzechMonth.UNOR);
        assertEquals(3, clientsAfterUpdate.size());

        // First client should remain true (was already true)
        assertTrue(clientsAfterUpdate.get(0).reportGenerated(), "First client should remain true");

        // Other clients should remain unchanged (original values from UNOR sheet)
        assertTrue(clientsAfterUpdate.get(1).reportGenerated(), "Second client should remain unchanged");
        assertTrue(clientsAfterUpdate.get(2).reportGenerated(), "Third client should remain unchanged");
    }

    @Test
    @SneakyThrows
    void testUpdateReportGeneratedStatus_SetFalseToTrue(@TempDir Path tempDir) {
        // Copy the test file to temp directory
        Path originalFile = Path.of("src/test/resources/Zdrav. pojišťovny.xlsx");
        Path tempFile = tempDir.resolve("test_set_to_true.xlsx");
        Files.copy(originalFile, tempFile, StandardCopyOption.REPLACE_EXISTING);

        ExcelWorker excelWorker = new ExcelWorker();
        String filePath = tempFile.toString();

        // Read original values from LEDEN sheet (third client has reportGenerated=false)
        List<Client> originalClients = excelWorker.readClients(filePath, CzechMonth.LEDEN);
        assertEquals(3, originalClients.size());
        assertFalse(originalClients.get(2).reportGenerated(), "Third client should start as false");

        // Update third client to true
        List<Client> updateClients = List.of(
                new Client("Další klient a.s.", "00000003", true, List.of())
         );

        // Update the file
        excelWorker.updateReportGeneratedStatus(filePath, updateClients, CzechMonth.LEDEN);

        // Read the clients again and verify the third client is now true
        List<Client> clientsAfterUpdate = excelWorker.readClients(filePath, CzechMonth.LEDEN);
        assertEquals(3, clientsAfterUpdate.size());

        // First two should remain unchanged
        assertTrue(clientsAfterUpdate.get(0).reportGenerated(), "First client should remain true");
        assertTrue(clientsAfterUpdate.get(1).reportGenerated(), "Second client should remain true");
        // Third client should now be true
        assertTrue(clientsAfterUpdate.get(2).reportGenerated(), "Third client should be updated to true");
    }

    @Test
    void testUpdateReportGeneratedStatus_FileNotFound() {
        ExcelWorker excelWorker = new ExcelWorker();
        List<Client> clients = List.of(new Client("Test", "12345678", true, List.of()));

        assertThrows(FileNotFoundException.class, () -> {
            excelWorker.updateReportGeneratedStatus("nonexistent.xlsx", clients, CzechMonth.LEDEN);
        });
    }

    @Test
    @SneakyThrows
    void testUpdateReportGeneratedStatus_EmptyClientList(@TempDir Path tempDir) {
        // Copy the test file to temp directory
        Path originalFile = Path.of("src/test/resources/Zdrav. pojišťovny.xlsx");
        Path tempFile = tempDir.resolve("test_empty_list.xlsx");
        Files.copy(originalFile, tempFile, StandardCopyOption.REPLACE_EXISTING);

        ExcelWorker excelWorker = new ExcelWorker();
        String filePath = tempFile.toString();

        // Read original values
        List<Client> originalClients = excelWorker.readClients(filePath, CzechMonth.UNOR);

        // Update with empty list - nothing should change
        excelWorker.updateReportGeneratedStatus(filePath, List.of(), CzechMonth.UNOR);

        // Read again and verify nothing changed
        List<Client> clientsAfterUpdate = excelWorker.readClients(filePath, CzechMonth.UNOR);

        assertEquals(originalClients.size(), clientsAfterUpdate.size());
        for (int i = 0; i < originalClients.size(); i++) {
            assertEquals(originalClients.get(i).reportGenerated(), clientsAfterUpdate.get(i).reportGenerated());
        }
    }

    @Test
    @SneakyThrows
    void testInsuranceCompaniesParsing() {
        ExcelWorker reader = new ExcelWorker();
        String filePath = "src/test/resources/Zdrav. pojišťovny.xlsx";

        List<Client> clients = reader.readClients(filePath, CzechMonth.UNOR);
        assertEquals(3, clients.size(), "Should read 3 clients from UNOR sheet");

        // First client should contain ZPMV
        List<InsuranceCompany> firstIns = clients.get(0).insuranceCompanies();
        assertNotNull(firstIns);
        assertTrue(firstIns.contains(InsuranceCompany.ZPMV), "First client should have ZPMV in insuranceCompanies");

        // Other clients have no insurance companies in the fixture
        assertTrue(clients.get(1).insuranceCompanies().isEmpty(), "Second client should have no insurance companies");
        assertTrue(clients.get(2).insuranceCompanies().isEmpty(), "Third client should have no insurance companies");
    }
}
