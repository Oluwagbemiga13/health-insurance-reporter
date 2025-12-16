package cz.oluwagbemiga.eutax.tools;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateValuesRequest;
import com.google.api.services.sheets.v4.model.ValueRange;
import cz.oluwagbemiga.eutax.pojo.Client;
import cz.oluwagbemiga.eutax.pojo.CzechMonth;
import cz.oluwagbemiga.eutax.pojo.InsuranceCompany;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class GoogleWorkerTest {

    @AfterEach
    void tearDown() {
        GoogleWorker.clearGoogleServiceAccountJson();
        GoogleWorker.clearSheetsServiceProvider();
    }

    // Utility to call private static methods via reflection
    private static Object callPrivateStatic(String methodName, Class<?>[] paramTypes, Object... args) throws Throwable {
        Method m = GoogleWorker.class.getDeclaredMethod(methodName, paramTypes);
        m.setAccessible(true);
        try {
            return m.invoke(null, args);
        } catch (InvocationTargetException ex) {
            throw ex.getTargetException();
        }
    }

    @Test
    void resolveCredentialStream_rawJson_returnsStream() throws Throwable {
        String json = "{\"type\":\"service_account\",\"project_id\":\"x\"}";
        GoogleWorker.setGoogleServiceAccountJson(json);
        ByteArrayInputStream stream = (ByteArrayInputStream) callPrivateStatic("resolveCredentialStream", new Class<?>[0]);
        byte[] bytes = stream.readAllBytes();
        assertEquals(json, new String(bytes, StandardCharsets.UTF_8));
    }

    @Test
    void resolveCredentialStream_filePath_readsFile() throws Throwable {
        File temp = File.createTempFile("gworker-test", ".json");
        temp.deleteOnExit();
        String content = "{\"a\":1}";
        java.nio.file.Files.writeString(temp.toPath(), content, StandardCharsets.UTF_8);
        GoogleWorker.setGoogleServiceAccountJson(temp.getAbsolutePath());
        ByteArrayInputStream stream = (ByteArrayInputStream) callPrivateStatic("resolveCredentialStream", new Class<?>[0]);
        byte[] bytes = stream.readAllBytes();
        assertEquals(content, new String(bytes, StandardCharsets.UTF_8));
    }

    @Test
    void resolveCredentialStream_base64_decodes() throws Throwable {
        String json = "{\"x\":2}";
        String b64 = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        GoogleWorker.setGoogleServiceAccountJson(b64);
        ByteArrayInputStream stream = (ByteArrayInputStream) callPrivateStatic("resolveCredentialStream", new Class<?>[0]);
        byte[] bytes = stream.readAllBytes();
        assertEquals(json, new String(bytes, StandardCharsets.UTF_8));
    }

    @Test
    void resolveCredentialStream_invalid_throwsIOException() {
        GoogleWorker.setGoogleServiceAccountJson("not-a-valid-secret");
        Throwable ex = assertThrows(IOException.class, () -> {
            try {
                callPrivateStatic("resolveCredentialStream", new Class<?>[0]);
            } catch (Throwable t) {
                if (t instanceof IOException) throw t;
                throw new RuntimeException(t);
            }
        });
        assertTrue(ex.getMessage().toLowerCase().contains("invalid"));
    }

    @Test
    void getCellValueAsString_variousCases() throws Throwable {
        // null row
        assertEquals("", callPrivateStatic("getCellValueAsString", new Class<?>[]{List.class, int.class}, null, 0));

        // out of bounds
        List<Object> one = List.of("a");
        assertEquals("", callPrivateStatic("getCellValueAsString", new Class<?>[]{List.class, int.class}, one, 5));

        // null cell
        List<Object> nullCell = Arrays.asList((Object) null);
        assertEquals("", callPrivateStatic("getCellValueAsString", new Class<?>[]{List.class, int.class}, nullCell, 0));

        // whole number
        List<Object> whole = List.of(123.0);
        assertEquals("123", callPrivateStatic("getCellValueAsString", new Class<?>[]{List.class, int.class}, whole, 0));

        // fractional
        List<Object> frac = List.of(123.45);
        assertEquals("123.45", callPrivateStatic("getCellValueAsString", new Class<?>[]{List.class, int.class}, frac, 0));

        // string
        List<Object> str = List.of("abc");
        assertEquals("abc", callPrivateStatic("getCellValueAsString", new Class<?>[]{List.class, int.class}, str, 0));
    }

    @Test
    void getCellValueAsBoolean_variousCases() throws Throwable {
        // null row or out of bounds
        assertEquals(false, callPrivateStatic("getCellValueAsBoolean", new Class<?>[]{List.class, int.class}, null, 0));
        List<Object> one = List.of("a");
        assertEquals(false, callPrivateStatic("getCellValueAsBoolean", new Class<?>[]{List.class, int.class}, one, 5));

        // boolean true
        List<Object> boolTrue = List.of(Boolean.TRUE);
        assertEquals(true, callPrivateStatic("getCellValueAsBoolean", new Class<?>[]{List.class, int.class}, boolTrue, 0));

        // numeric zero/ non-zero
        List<Object> zero = List.of(0);
        assertEquals(false, callPrivateStatic("getCellValueAsBoolean", new Class<?>[]{List.class, int.class}, zero, 0));
        List<Object> two = List.of(2);
        assertEquals(true, callPrivateStatic("getCellValueAsBoolean", new Class<?>[]{List.class, int.class}, two, 0));

        // string variants
        List<Object> t = List.of("true");
        assertEquals(true, callPrivateStatic("getCellValueAsBoolean", new Class<?>[]{List.class, int.class}, t, 0));
        List<Object> y = List.of("YES");
        assertEquals(true, callPrivateStatic("getCellValueAsBoolean", new Class<?>[]{List.class, int.class}, y, 0));
        List<Object> ano = List.of("AnO");
        assertEquals(true, callPrivateStatic("getCellValueAsBoolean", new Class<?>[]{List.class, int.class}, ano, 0));
        List<Object> oneStr = List.of("1");
        assertEquals(true, callPrivateStatic("getCellValueAsBoolean", new Class<?>[]{List.class, int.class}, oneStr, 0));

        // other strings
        List<Object> no = List.of("no");
        assertEquals(false, callPrivateStatic("getCellValueAsBoolean", new Class<?>[]{List.class, int.class}, no, 0));
    }

    @Test
    void rangeHelpers_escapeAndFormat() throws Throwable {
        assertEquals("O''Brian", callPrivateStatic("escapeSheetName", new Class<?>[]{String.class}, "O'Brian"));
        assertEquals("'Sheet'!A2:G", callPrivateStatic("sheetRange", new Class<?>[]{String.class, String.class}, "Sheet", "A2:G"));
        assertEquals("'My''Name'!G5", callPrivateStatic("singleCellRange", new Class<?>[]{String.class, String.class, int.class}, "My'Name", "G", 5));
    }

    @Test
    void readClients_withMockedSheets_parsesClientsCorrectly() throws Exception {
        // prepare mocked Sheets chain
        Sheets mockSheets = mock(Sheets.class);
        Sheets.Spreadsheets mockSpreadsheets = mock(Sheets.Spreadsheets.class);
        Sheets.Spreadsheets.Values mockValues = mock(Sheets.Spreadsheets.Values.class);
        Sheets.Spreadsheets.Values.Get mockGet = mock(Sheets.Spreadsheets.Values.Get.class);

        when(mockSheets.spreadsheets()).thenReturn(mockSpreadsheets);
        when(mockSpreadsheets.values()).thenReturn(mockValues);
        when(mockValues.get(anyString(), anyString())).thenReturn(mockGet);

        // Build value rows: Alice with ICO and insulin columns; Bob with empty ICO -> stops reading
        List<Object> aliceRow = new ArrayList<>(Collections.nCopies(17, ""));
        aliceRow.set(0, "Alice");
        aliceRow.set(1, "11111111");
        aliceRow.set(6, true); // column G
        // set insurance company K..Q indices (K is column 11 -> index 10). Use enum to mark one.
        InsuranceCompany some = InsuranceCompany.values()[0];
        int idx = some.getColumnIndex() - 1;
        aliceRow.set(idx, "X");

        List<Object> bobRow = new ArrayList<>(Collections.nCopies(17, ""));
        bobRow.set(0, "Bob");
        bobRow.set(1, ""); // empty ICO triggers stop

        ValueRange vr = new ValueRange();
        vr.setValues(List.of(aliceRow, bobRow));
        when(mockGet.execute()).thenReturn(vr);

        GoogleWorker.setGoogleServiceAccountJson("{\"a\":1}");
        GoogleWorker.setSheetsServiceProvider(() -> mockSheets);

        GoogleWorker gw = new GoogleWorker();
        List<Client> clients = gw.readClients("test-id", CzechMonth.LEDEN);
        assertEquals(1, clients.size());
        Client c = clients.get(0);
        assertEquals("Alice", c.name());
        assertEquals("11111111", c.ico());
        assertTrue(c.reportGenerated());
        assertTrue(c.insuranceCompanies().contains(some));
    }

    @Test
    void readClients_skipsRowsWithNE() throws Exception {
        Sheets mockSheets = mock(Sheets.class);
        Sheets.Spreadsheets mockSpreadsheets = mock(Sheets.Spreadsheets.class);
        Sheets.Spreadsheets.Values mockValues = mock(Sheets.Spreadsheets.Values.class);
        Sheets.Spreadsheets.Values.Get mockGet = mock(Sheets.Spreadsheets.Values.Get.class);

        when(mockSheets.spreadsheets()).thenReturn(mockSpreadsheets);
        when(mockSpreadsheets.values()).thenReturn(mockValues);
        when(mockValues.get(anyString(), anyString())).thenReturn(mockGet);

        List<Object> rowNe = new ArrayList<>(Collections.nCopies(17, ""));
        rowNe.set(0, "Carol");
        rowNe.set(1, "22222222");
        rowNe.set(6, "NE");
        ValueRange vr = new ValueRange();
        vr.setValues(List.of(rowNe));
        when(mockGet.execute()).thenReturn(vr);

        GoogleWorker.setGoogleServiceAccountJson("{\"a\":1}");
        GoogleWorker.setSheetsServiceProvider(() -> mockSheets);

        GoogleWorker gw = new GoogleWorker();
        List<Client> clients = gw.readClients("id", CzechMonth.LEDEN);
        assertTrue(clients.isEmpty());
    }

    @Test
    void updateReportGeneratedStatus_preparesBatchUpdates_correctRangeAndValues() throws Exception {
        Sheets mockSheets = mock(Sheets.class);
        Sheets.Spreadsheets mockSpreadsheets = mock(Sheets.Spreadsheets.class);
        Sheets.Spreadsheets.Values mockValues = mock(Sheets.Spreadsheets.Values.class);
        Sheets.Spreadsheets.Values.Get mockGet = mock(Sheets.Spreadsheets.Values.Get.class);
        Sheets.Spreadsheets.Values.BatchUpdate mockBatchUpdate = mock(Sheets.Spreadsheets.Values.BatchUpdate.class);

        when(mockSheets.spreadsheets()).thenReturn(mockSpreadsheets);
        when(mockSpreadsheets.values()).thenReturn(mockValues);
        when(mockValues.get(anyString(), anyString())).thenReturn(mockGet);
        when(mockValues.batchUpdate(anyString(), any(BatchUpdateValuesRequest.class))).thenReturn(mockBatchUpdate);
        when(mockBatchUpdate.execute()).thenReturn(null);

        // existing rows: row1 ICO 11111111 previous false -> should be set true if in clients
        List<Object> row1 = new ArrayList<>(Collections.nCopies(7, ""));
        row1.set(0, "Alice");
        row1.set(1, "11111111");
        row1.set(6, false);

        // row2 ICO 33333333 previous true -> if not in clients -> reset to false
        List<Object> row2 = new ArrayList<>(Collections.nCopies(7, ""));
        row2.set(0, "Eve");
        row2.set(1, "33333333");
        row2.set(6, true);

        // row3 NE -> ignored
        List<Object> row3 = new ArrayList<>(Collections.nCopies(7, ""));
        row3.set(0, "Dave");
        row3.set(1, "44444444");
        row3.set(6, "NE");

        ValueRange vr = new ValueRange();
        vr.setValues(List.of(row1, row2, row3));
        when(mockGet.execute()).thenReturn(vr);

        // clients: set 11111111 true; 33333333 false (not present in list)
        Client toSet = new Client("Alice", "11111111", true, List.of());
        Client leave = new Client("Eve", "33333333", false, List.of());
        List<Client> clients = List.of(toSet);

        GoogleWorker.setGoogleServiceAccountJson("{\"a\":1}");
        GoogleWorker.setSheetsServiceProvider(() -> mockSheets);

        GoogleWorker gw = new GoogleWorker();
        gw.updateReportGeneratedStatus("id", clients, CzechMonth.LEDEN);

        ArgumentCaptor<BatchUpdateValuesRequest> captor = ArgumentCaptor.forClass(BatchUpdateValuesRequest.class);
        verify(mockValues).batchUpdate(eq("id"), captor.capture());
        BatchUpdateValuesRequest req = captor.getValue();
        assertEquals("USER_ENTERED", req.getValueInputOption());
        List<ValueRange> data = req.getData();
        // Should have updates for row1 (set true) and row2 (reset false) -> but row2 desired false and previous true => reset
        // However since clients contained only 11111111, desired for 33333333 is false and previous true -> an update expected
        assertEquals(2, data.size());
        // find ranges and values
        Map<String, Object> map = new HashMap<>();
        for (ValueRange vrx : data) {
            map.put(vrx.getRange(), vrx.getValues().get(0).get(0));
        }
        // Row numbers start at DATA_START_ROW = 2
        assertTrue(map.containsKey("'Leden'!G2"));
        assertTrue(map.containsKey("'Leden'!G3"));
        assertEquals(Boolean.TRUE, map.get("'Leden'!G2"));
        assertEquals(Boolean.FALSE, map.get("'Leden'!G3"));
    }

    @Test
    void updateReportGeneratedStatus_noClients_noApiCall() throws Exception {
        Sheets mockSheets = mock(Sheets.class);
        Sheets.Spreadsheets mockSpreadsheets = mock(Sheets.Spreadsheets.class);
        Sheets.Spreadsheets.Values mockValues = mock(Sheets.Spreadsheets.Values.class);
        when(mockSheets.spreadsheets()).thenReturn(mockSpreadsheets);
        when(mockSpreadsheets.values()).thenReturn(mockValues);

        GoogleWorker.setGoogleServiceAccountJson("{\"a\":1}");
        GoogleWorker.setSheetsServiceProvider(() -> mockSheets);

        GoogleWorker gw = new GoogleWorker();
        gw.updateReportGeneratedStatus("id", Collections.emptyList(), CzechMonth.LEDEN);
        verify(mockValues, never()).batchUpdate(anyString(), any(BatchUpdateValuesRequest.class));
    }
}

