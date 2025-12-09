package cz.oluwagbemiga.eutax.tools;

import cz.oluwagbemiga.eutax.pojo.Client;
import cz.oluwagbemiga.eutax.pojo.CzechMonth;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExcelReaderTest {


    @Test
    @SneakyThrows
    void testReadClients() {
        ExcelReader reader = new ExcelReader();
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
        ExcelReader reader = new ExcelReader();
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
        ExcelReader reader = new ExcelReader();
        assertThrows(FileNotFoundException.class, () -> {
            reader.readClients("nonexistent.xlsx", CzechMonth.LEDEN);
        });
    }
}
