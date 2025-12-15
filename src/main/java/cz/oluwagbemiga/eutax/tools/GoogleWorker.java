package cz.oluwagbemiga.eutax.tools;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.BatchUpdateValuesRequest;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import cz.oluwagbemiga.eutax.pojo.Client;
import cz.oluwagbemiga.eutax.pojo.CzechMonth;
import cz.oluwagbemiga.eutax.pojo.InsuranceCompany;
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


/**
 * Implementation of {@link SpreadsheetWorker} for reading and writing Google Sheets.
 * <p>
 * This class uses the Google Sheets API v4 to interact with Google Spreadsheets.
 * It requires a Google service account JSON credential to be configured via
 * {@link #setGoogleServiceAccountJson(String)} before use.
 * </p>
 * <p>
 * The spreadsheet is expected to contain sheets named after Czech months, with
 * client data organized in columns:
 * <ul>
 *     <li>Column A (index 0): Client name</li>
 *     <li>Column B (index 1): Client ICO (identification number)</li>
 *     <li>Column G (index 6): Report generated status (boolean)</li>
 * </ul>
 * </p>
 *
 * @see SpreadsheetWorker
 * @see ExcelWorker
 * @see GoogleSheetIdResolver
 */
@Slf4j
@NoArgsConstructor
public final class GoogleWorker implements SpreadsheetWorker {

    /** Application name used for Google API requests */
    private static final String APPLICATION_NAME = "Health Insurance Reporter";

    /** JSON factory for Google API serialization */
    private static final com.google.api.client.json.JsonFactory JSON_FACTORY =
            com.google.api.client.json.gson.GsonFactory.getDefaultInstance();

    /** OAuth scopes required for reading and writing spreadsheets */
    private static final Collection<String> SCOPES = List.of(SheetsScopes.SPREADSHEETS);

    /** Row number where client data begins (1-indexed, after header row) */
    private static final int DATA_START_ROW = 2;

    /**
     * The Google service account JSON credentials.
     * Can be provided as raw JSON, a file path, or Base64-encoded content.
     */
    @Setter
    private static volatile String googleServiceAccountJson;


    /**
     * Returns the current Google service account JSON credentials if available.
     *
     * @return an {@link Optional} containing the credentials JSON, or empty if not set
     */
    public static Optional<String> getGoogleServiceAccountJson() {
        return Optional.ofNullable(googleServiceAccountJson);
    }

    /**
     * Checks whether Google service account credentials have been configured.
     *
     * @return {@code true} if credentials are available and non-blank
     */
    public static boolean hasGoogleServiceAccountJson() {
        return googleServiceAccountJson != null && !googleServiceAccountJson.isBlank();
    }

    /**
     * Clears the stored Google service account credentials.
     */
    public static void clearGoogleServiceAccountJson() {
        log.debug("Clearing Google service account credentials");
        googleServiceAccountJson = null;
    }



    /**
     * {@inheritDoc}
     * <p>
     * Reads client information from the specified Google Sheets spreadsheet for the given month.
     * The spreadsheet ID can be provided as a direct ID, URL, or file path to a .gsheet/.url shortcut.
     * </p>
     *
     * @param filePath the spreadsheet identifier (ID, URL, or path to .gsheet/.url file)
     * @param month    the month determining which sheet to read from
     * @return a list of {@link Client} objects read from the spreadsheet; empty list if sheet not found
     * @throws FileNotFoundException if the spreadsheet or sheet does not exist
     */
    @Override
    public List<Client> readClients(String filePath, CzechMonth month) throws FileNotFoundException {
        log.debug("Attempting to read clients from Google Sheets: {} for month: {}", filePath, month.getCzechName());
        String spreadsheetId = requireSpreadsheetId(filePath);
        List<Client> clients = new ArrayList<>();
        if (!hasGoogleServiceAccountJson()) {
            log.error("Google service account JSON is not available");
            return clients;
        }

        String sheetName = month.getCzechName();
        // Read up to column Q so we can inspect insurance company columns (K..Q)
        String range = sheetRange(sheetName, "A2:Q");

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

                // If ICO is empty, stop reading further rows (do not include this row)
                if (ico.isEmpty()) {
                    log.debug("Stopping read at Google Sheets row for '{}' because ICO is empty", name);
                    break;
                }

                // Column G (index 6) raw check for explicit "NE" to skip this client entirely
                String reportGeneratedRaw = getCellValueAsString(row, 6);
                if (reportGeneratedRaw != null && "NE".equalsIgnoreCase(reportGeneratedRaw.trim())) {
                    log.debug("Skipping Google Sheets row for '{}' because column G is 'NE'", name);
                    continue;
                }

                boolean reportGenerated = getCellValueAsBoolean(row, 6);

                // Collect insurance companies from columns K..Q using enum mapping
                List<InsuranceCompany> insuranceCompanies = new ArrayList<>();
                for (InsuranceCompany company : InsuranceCompany.values()) {
                    int colIndexZeroBased = company.getColumnIndex() - 1; // enum stores 1-based index
                    String insValue = getCellValueAsString(row, colIndexZeroBased);
                    if (insValue != null && !insValue.trim().isEmpty()) {
                        insuranceCompanies.add(company);
                    }
                }

                clients.add(new Client(name, ico, reportGenerated, insuranceCompanies));
                log.debug("Read client from Google Sheets: {} ({}), ins: {}", name, ico, insuranceCompanies);
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

    /**
     * {@inheritDoc}
     * <p>
     * Updates the report generated status in the Google Sheets spreadsheet for clients
     * whose {@code reportGenerated} field is {@code true}. Matches clients by their ICO
     * and updates column G in the corresponding rows.
     * </p>
     *
     * @param filePath the spreadsheet identifier (ID, URL, or path to .gsheet/.url file)
     * @param clients  the list of clients with updated report status
     * @param month    the month determining which sheet to update
     * @throws IOException           if an error occurs while communicating with Google Sheets API
     * @throws IllegalStateException if Google service account credentials are not configured
     */
    @Override
    public void updateReportGeneratedStatus(String filePath, List<Client> clients, CzechMonth month) throws IOException {
        log.debug("Updating report status for {} clients in Google Sheets: {} for month: {}",
                clients != null ? clients.size() : 0, filePath, month.getCzechName());
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

    /**
     * Creates and configures a Google Sheets API service instance.
     * <p>
     * Uses the configured service account credentials to authenticate.
     * </p>
     *
     * @return a configured {@link Sheets} service instance
     * @throws IOException if credential initialization or transport setup fails
     */
    private static Sheets createSheetsService() throws IOException {
        log.debug("Creating Google Sheets API service");
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

    /**
     * Resolves the service account credential stream from the configured credentials.
     * <p>
     * The credentials can be provided in three formats:
     * <ul>
     *     <li>Raw JSON string starting with '{' or '['</li>
     *     <li>File path to a JSON credentials file</li>
     *     <li>Base64-encoded JSON string</li>
     * </ul>
     * </p>
     *
     * @return a {@link ByteArrayInputStream} containing the credential JSON
     * @throws IOException if credentials are missing or in an invalid format
     */
    private static ByteArrayInputStream resolveCredentialStream() throws IOException {
        log.debug("Resolving Google service account credentials");
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

    /**
     * Validates and returns the spreadsheet ID, throwing an exception if empty.
     *
     * @param filePath the spreadsheet identifier to validate
     * @return the trimmed spreadsheet identifier
     * @throws FileNotFoundException if the identifier is null or blank
     */
    private static String requireSpreadsheetId(String filePath) throws FileNotFoundException {
        if (filePath == null || filePath.isBlank()) {
            log.error("Spreadsheet identifier is empty or null");
            throw new FileNotFoundException("Spreadsheet identifier must not be empty");
        }
        return filePath.trim();
    }

    /**
     * Constructs an A1 notation range reference for a Google Sheets API request.
     *
     * @param sheetName   the name of the sheet
     * @param coordinates the cell range coordinates (e.g., "A2:G")
     * @return the formatted range string (e.g., "'SheetName'!A2:G")
     */
    private static String sheetRange(String sheetName, String coordinates) {
        return "'" + escapeSheetName(sheetName) + "'!" + coordinates;
    }

    /**
     * Constructs an A1 notation reference for a single cell.
     *
     * @param sheetName the name of the sheet
     * @param column    the column letter (e.g., "G")
     * @param row       the row number (1-indexed)
     * @return the formatted single cell reference
     */
    private static String singleCellRange(String sheetName, String column, int row) {
        return sheetRange(sheetName, column + row);
    }

    /**
     * Escapes single quotes in sheet names for use in A1 notation.
     *
     * @param sheetName the sheet name to escape
     * @return the escaped sheet name
     */
    private static String escapeSheetName(String sheetName) {
        return sheetName.replace("'", "''");
    }

    /**
     * Extracts a string value from a row at the specified column index.
     * <p>
     * Handles Number values by formatting whole numbers without decimal points.
     * </p>
     *
     * @param row   the row data as a list of objects
     * @param index the column index (0-based)
     * @return the cell value as a string; empty string if null or out of bounds
     */
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

    /**
     * Extracts a boolean value from a row at the specified column index.
     * <p>
     * Interprets the following as {@code true}:
     * <ul>
     *     <li>Boolean value {@code true}</li>
     *     <li>Non-zero numeric values</li>
     *     <li>String values "true", "yes", "ano", or "1" (case-insensitive)</li>
     * </ul>
     * </p>
     *
     * @param row   the row data as a list of objects
     * @param index the column index (0-based)
     * @return {@code true} if the cell represents a truthy value; {@code false} otherwise
     */
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
