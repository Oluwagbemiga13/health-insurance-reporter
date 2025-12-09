package cz.oluwagbemiga.eutax.tools;

import cz.oluwagbemiga.eutax.pojo.Client;
import cz.oluwagbemiga.eutax.pojo.CzechMonth;
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

        // Create updated clients with changed reportGenerated values
        List<Client> updatedClients = List.of(
                new Client("Klient A s.r.o", "00000001", false),  // Changed from true to false
                new Client("Klient 2 s.r.o", "00000002", false),  // Changed from true to false
                new Client("Další klient a.s.", "00000003", true) // Changed from true to true (or false to true)
        );

        // Update the file
        excelWorker.updateReportGeneratedStatus(filePath, updatedClients, CzechMonth.UNOR);

        // Read the clients again and verify the changes
        List<Client> clientsAfterUpdate = excelWorker.readClients(filePath, CzechMonth.UNOR);
        assertEquals(3, clientsAfterUpdate.size());

        // Assert the updated values
        assertEquals("Klient A s.r.o", clientsAfterUpdate.get(0).name());
        assertEquals("00000001", clientsAfterUpdate.get(0).ico());
        assertFalse(clientsAfterUpdate.get(0).reportGenerated(), "First client should be updated to false");

        assertEquals("Klient 2 s.r.o", clientsAfterUpdate.get(1).name());
        assertEquals("00000002", clientsAfterUpdate.get(1).ico());
        assertFalse(clientsAfterUpdate.get(1).reportGenerated(), "Second client should be updated to false");

        assertEquals("Další klient a.s.", clientsAfterUpdate.get(2).name());
        assertEquals("00000003", clientsAfterUpdate.get(2).ico());
        assertTrue(clientsAfterUpdate.get(2).reportGenerated(), "Third client should be updated to true");
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

        // Only update one client - others should remain unchanged
        List<Client> partialUpdate = List.of(
                new Client("Klient A s.r.o", "00000001", false)
        );

        // Update the file
        excelWorker.updateReportGeneratedStatus(filePath, partialUpdate, CzechMonth.UNOR);

        // Read the clients and verify only the specified client was updated
        List<Client> clientsAfterUpdate = excelWorker.readClients(filePath, CzechMonth.UNOR);
        assertEquals(3, clientsAfterUpdate.size());

        // First client should be updated
        assertFalse(clientsAfterUpdate.get(0).reportGenerated(), "First client should be updated to false");

        // Other clients should remain unchanged (original values from UNOR sheet)
        assertTrue(clientsAfterUpdate.get(1).reportGenerated(), "Second client should remain unchanged");
        assertTrue(clientsAfterUpdate.get(2).reportGenerated(), "Third client should remain unchanged");
    }

    @Test
    void testUpdateReportGeneratedStatus_FileNotFound() {
        ExcelWorker excelWorker = new ExcelWorker();
        List<Client> clients = List.of(new Client("Test", "12345678", true));

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
}
