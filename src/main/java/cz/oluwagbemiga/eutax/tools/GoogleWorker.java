package cz.oluwagbemiga.eutax.tools;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.BatchUpdateValuesRequest;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.common.collect.ImmutableList;
import cz.oluwagbemiga.eutax.pojo.Client;
import cz.oluwagbemiga.eutax.pojo.CzechMonth;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.*;

@Slf4j
@NoArgsConstructor
public final class GoogleWorker implements SpreadsheetWorker {

    private static final String APPLICATION_NAME = "Health Insurance Reporter";
    private static final com.google.api.client.json.JsonFactory JSON_FACTORY =
            com.google.api.client.json.gson.GsonFactory.getDefaultInstance();
    private static final Collection<String> SCOPES = List.of(SheetsScopes.SPREADSHEETS);
    private static final int DATA_START_ROW = 2;

    @Setter
    private static volatile String googleServiceAccountJson;


    public static Optional<String> getGoogleServiceAccountJson() {
        return Optional.ofNullable(googleServiceAccountJson);
    }

    public static boolean hasGoogleServiceAccountJson() {
        return googleServiceAccountJson != null && !googleServiceAccountJson.isBlank();
    }

    public static void clearGoogleServiceAccountJson() {
        googleServiceAccountJson = null;
    }

    @Override
    public List<Client> readClients(String filePath, CzechMonth month) throws FileNotFoundException {
        String spreadsheetId = requireSpreadsheetId(filePath);
        List<Client> clients = new ArrayList<>();
        if (!hasGoogleServiceAccountJson()) {
            log.error("Google service account JSON is not available");
            return clients;
        }

        String sheetName = month.getCzechName();
        String range = sheetRange(sheetName, "A2:G");

        try {
            Sheets sheets = createSheetsService();
            ValueRange response = sheets.spreadsheets()
                    .values()
                    .get(spreadsheetId, range)
                    .execute();

            List<List<Object>> rows = response.getValues();
            if (rows == null || rows.isEmpty()) {
                log.warn("Sheet '{}' is empty or missing in spreadsheet {}", sheetName, spreadsheetId);
                return clients;
            }

            for (List<Object> row : rows) {
                if (row == null || row.isEmpty()) {
                    continue;
                }

                String name = getCellValueAsString(row, 0).trim();
                if (name.isEmpty()) {
                    continue;
                }
                String ico = getCellValueAsString(row, 1).trim();
                boolean reportGenerated = getCellValueAsBoolean(row, 6);
                clients.add(new Client(name, ico, reportGenerated));
                log.debug("Read client from Google Sheets: {} ({})", name, ico);
            }

            log.info("Successfully read {} clients from sheet '{}'", clients.size(), sheetName);
        } catch (com.google.api.client.googleapis.json.GoogleJsonResponseException ex) {
            if (ex.getStatusCode() == 404) {
                log.error("Spreadsheet {} or sheet '{}' not found", spreadsheetId, sheetName);
                throw new FileNotFoundException("Spreadsheet or sheet not found: " + spreadsheetId);
            }
            log.error("Google API error while reading spreadsheet {}", spreadsheetId, ex);
        } catch (IOException ex) {
            log.error("I/O error while reading spreadsheet {}", spreadsheetId, ex);
        }

        return clients;
    }

    @Override
    public void updateReportGeneratedStatus(String filePath, List<Client> clients, CzechMonth month) throws IOException {
        String spreadsheetId = requireSpreadsheetId(filePath);
        if (clients == null || clients.isEmpty()) {
            log.info("No clients supplied for Google Sheets update");
            return;
        }
        if (!hasGoogleServiceAccountJson()) {
            throw new IllegalStateException("Google service account JSON is not available");
        }

        List<String> icoList = clients.stream()
                .filter(Client::reportGenerated)
                .map(Client::ico)
                .filter(ico -> ico != null && !ico.trim().isEmpty())
                .map(String::trim)
                .distinct()
                .toList();

        if (icoList.isEmpty()) {
            log.info("No reportGenerated clients to update for sheet '{}'", month.getCzechName());
            return;
        }

        Sheets sheets = createSheetsService();
        String sheetName = month.getCzechName();
        String range = sheetRange(sheetName, "A2:G");

        ValueRange response;
        try {
            response = sheets.spreadsheets().values().get(spreadsheetId, range).execute();
        } catch (com.google.api.client.googleapis.json.GoogleJsonResponseException ex) {
            if (ex.getStatusCode() == 404) {
                throw new FileNotFoundException("Spreadsheet or sheet not found: " + spreadsheetId);
            }
            throw ex;
        }

        List<List<Object>> rows = response.getValues();
        if (rows == null || rows.isEmpty()) {
            log.warn("Sheet '{}' is empty, nothing to update", sheetName);
            return;
        }

        Set<String> icoLookup = Set.copyOf(icoList);
        List<ValueRange> updates = new ArrayList<>();
        int rowNumber = DATA_START_ROW;
        for (List<Object> row : rows) {
            String ico = getCellValueAsString(row, 1).trim();
            if (!ico.isEmpty() && icoLookup.contains(ico)) {
                updates.add(
                        new ValueRange()
                                .setRange(singleCellRange(sheetName, "G", rowNumber))
                                .setValues(List.of(List.of(true)))
                );
                log.debug("Prepared Google Sheets update for ICO {} at row {}", ico, rowNumber);
            }
            rowNumber++;
        }

        if (updates.isEmpty()) {
            log.info("No matching ICO entries found in sheet '{}'", sheetName);
            return;
        }

        BatchUpdateValuesRequest request =
                new BatchUpdateValuesRequest()
                        .setValueInputOption("USER_ENTERED")
                        .setData(updates);

        sheets.spreadsheets().values().batchUpdate(spreadsheetId, request).execute();
        log.info("Updated {} clients in sheet '{}'", updates.size(), sheetName);
    }

    private static Sheets createSheetsService() throws IOException {
        try {
            com.google.api.client.http.HttpTransport transport =
                    com.google.api.client.googleapis.javanet.GoogleNetHttpTransport.newTrustedTransport();
            GoogleCredentials credentials = GoogleCredentials.fromStream(resolveCredentialStream())
                    .createScoped(SCOPES);
            return new Sheets.Builder(transport, JSON_FACTORY, new HttpCredentialsAdapter(credentials))
                    .setApplicationName(APPLICATION_NAME)
                    .build();
        } catch (GeneralSecurityException ex) {
            throw new IOException("Unable to initialize Google Sheets transport", ex);
        }
    }

    private static ByteArrayInputStream resolveCredentialStream() throws IOException {
        if (!hasGoogleServiceAccountJson()) {
            throw new IOException("Google service account credentials missing");
        }
        String candidate = googleServiceAccountJson.trim();
        if (candidate.startsWith("{") || candidate.startsWith("[")) {
            return new ByteArrayInputStream(candidate.getBytes(StandardCharsets.UTF_8));
        }
        Path potentialFile = Path.of(candidate);
        if (Files.isRegularFile(potentialFile)) {
            byte[] bytes = Files.readAllBytes(potentialFile);
            return new ByteArrayInputStream(bytes);
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(candidate);
            return new ByteArrayInputStream(decoded);
        } catch (IllegalArgumentException ex) {
            log.error("Provided Google service account secret is not valid JSON, file path, or Base64");
            throw new IOException("Invalid Google service account secret format", ex);
        }
    }

    private static String requireSpreadsheetId(String filePath) throws FileNotFoundException {
        if (filePath == null || filePath.isBlank()) {
            throw new FileNotFoundException("Spreadsheet identifier must not be empty");
        }
        return filePath.trim();
    }

    private static String sheetRange(String sheetName, String coordinates) {
        return "'" + escapeSheetName(sheetName) + "'!" + coordinates;
    }

    private static String singleCellRange(String sheetName, String column, int row) {
        return sheetRange(sheetName, column + row);
    }

    private static String escapeSheetName(String sheetName) {
        return sheetName.replace("'", "''");
    }

    private static String getCellValueAsString(List<Object> row, int index) {
        if (row == null || index >= row.size()) {
            return "";
        }
        Object value = row.get(index);
        if (value == null) {
            return "";
        }
        if (value instanceof Number number) {
            double numeric = number.doubleValue();
            if (numeric == Math.rint(numeric)) {
                return Long.toString((long) numeric);
            }
            return Double.toString(numeric);
        }
        return value.toString();
    }

    private static boolean getCellValueAsBoolean(List<Object> row, int index) {
        if (row == null || index >= row.size()) {
            return false;
        }
        Object value = row.get(index);
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.doubleValue() != 0;
        }
        String text = value == null ? "" : value.toString().trim().toLowerCase(Locale.ROOT);
        return text.equals("true") || text.equals("yes") || text.equals("ano") || text.equals("1");
    }
}
