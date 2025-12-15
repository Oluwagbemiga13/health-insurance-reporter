package cz.oluwagbemiga.eutax.tools;

import cz.oluwagbemiga.eutax.pojo.Client;
import cz.oluwagbemiga.eutax.pojo.CzechMonth;
import cz.oluwagbemiga.eutax.pojo.InsuranceCompany;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of {@link SpreadsheetWorker} for reading and writing Excel (.xlsx) files.
 * <p>
 * This class uses Apache POI to interact with Excel workbooks. It expects each workbook
 * to contain sheets named after Czech months, with client data organized in columns:
 * <ul>
 *     <li>Column A (index 0): Client name</li>
 *     <li>Column B (index 1): Client ICO (identification number)</li>
 *     <li>Column G (index 6): Report generated status (boolean)</li>
 * </ul>
 * </p>
 *
 * @see SpreadsheetWorker
 * @see GoogleWorker
 */
@Slf4j
public class ExcelWorker implements SpreadsheetWorker {

    /**
     * Reads client information from the specified Excel file for the given month.
     * <p>
     * The method looks for a sheet named after the Czech month name and reads
     * client data starting from row 1 (skipping the header row at row 0).
     * </p>
     *
     * @param filePath the absolute or relative path to the Excel file (.xlsx)
     * @param month    the month determining which sheet to read from
     * @return a list of {@link Client} objects read from the spreadsheet; empty list if sheet not found
     * @throws FileNotFoundException if the specified file does not exist
     */
    @Override
    public List<Client> readClients(String filePath, CzechMonth month) throws FileNotFoundException {
        log.debug("Attempting to read clients from Excel file: {} for month: {}", filePath, month.getCzechName());
        File file = new File(filePath);
        List<Client> clients = new ArrayList<>();

        if (!file.exists()) {
            log.error("File does not exist: {}", filePath);
            throw new FileNotFoundException("File not found: " + filePath);
        }

        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = new XSSFWorkbook(fis)) {

            // Find the sheet with the month name
            Sheet sheet = workbook.getSheet(month.getCzechName());
            if (sheet == null) {
                log.error("Sheet '{}' not found in file: {}", month.getCzechName(), filePath);
                return clients;
            }

            log.info("Reading sheet '{}' from file: {}", month.getCzechName(), filePath);

            // Skip the header row (row 0) and start reading from row 1
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    // empty row — skip processing for this iteration without using continue/break
                } else {
                    // Column A (index 0): Name
                    Cell nameCell = row.getCell(0);
                    String name = getCellValueAsString(nameCell);

                    // Column B (index 1): ICO
                    Cell icoCell = row.getCell(1);
                    String ico = getCellValueAsString(icoCell);

                    // If ICO is empty, stop reading further rows (do not include this row)
                    if (ico == null || ico.trim().isEmpty()) {
                        log.debug("Stopping read at row {} because ICO is empty", i);
                        break;
                    }

                    // Column G (index 6): Report Generated (we need raw string to detect "NE")
                    Cell reportGeneratedCell = row.getCell(6);
                    String reportGeneratedRaw = getCellValueAsString(reportGeneratedCell);

                    boolean reportGenerated = getCellValueAsBoolean(reportGeneratedCell);

                    // Collect insurance companies from columns K..Q using the enum mapping
                    List<InsuranceCompany> insuranceCompanies = new ArrayList<>();
                    for (InsuranceCompany company : InsuranceCompany.values()) {
                        int colIndexZeroBased = company.getColumnIndex() - 1; // enum stores 1-based index
                        Cell insCell = row.getCell(colIndexZeroBased);
                        String insValue = getCellValueAsString(insCell);
                        if (insValue != null && !insValue.trim().isEmpty()) {
                            insuranceCompanies.add(company);
                        }
                    }

                    // Only add if name is not empty and column G isn't explicitly "NE"
                    if (name != null && !name.trim().isEmpty() && !(reportGeneratedRaw != null && "NE".equalsIgnoreCase(reportGeneratedRaw.trim()))) {
                        Client client = new Client(name.trim(), ico.trim(), reportGenerated, insuranceCompanies);
                        clients.add(client);
                        log.debug("Read client: {}", client);
                    }
                }
            }

            log.info("Successfully read {} clients from sheet '{}'", clients.size(), month.getCzechName());

        } catch (IOException e) {
            log.error("Error reading Excel file: {}", filePath, e);
        }

        return clients;
    }

    /**
     * Extracts the string value from an Excel cell, handling various cell types.
     * <p>
     * Supports STRING, NUMERIC (including dates), BOOLEAN, FORMULA, and BLANK cell types.
     * Numeric values that are whole numbers are returned without decimal points.
     * </p>
     *
     * @param cell the Excel cell to read from; may be {@code null}
     * @return the cell value as a string; empty string if cell is {@code null} or blank
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getDateCellValue().toString();
                } else {
                    // Convert numeric to string, removing decimal point if it's a whole number
                    double numericValue = cell.getNumericCellValue();
                    if (numericValue == Math.floor(numericValue)) {
                        yield String.valueOf((long) numericValue);
                    } else {
                        yield String.valueOf(numericValue);
                    }
                }
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                // Use the cached formula result type to safely extract the result
                var cachedType = cell.getCachedFormulaResultType();
                yield switch (cachedType) {
                    case STRING -> cell.getStringCellValue();
                    case NUMERIC -> {
                        if (DateUtil.isCellDateFormatted(cell)) {
                            yield cell.getDateCellValue().toString();
                        } else {
                            double numericValue = cell.getNumericCellValue();
                            if (numericValue == Math.floor(numericValue)) {
                                yield String.valueOf((long) numericValue);
                            } else {
                                yield String.valueOf(numericValue);
                            }
                        }
                    }
                    case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
                    default -> "";
                };
            }
            case BLANK -> "";
            default -> "";
        };
    }

    /**
     * Extracts a boolean value from an Excel cell, handling various cell types.
     * <p>
     * Interprets the following as {@code true}:
     * <ul>
     *     <li>Boolean cell with value {@code true}</li>
     *     <li>String cell containing "true", "yes", "ano", or "1" (case-insensitive)</li>
     *     <li>Numeric cell with non-zero value</li>
     *     <li>Formula cells are evaluated and interpreted using the above rules</li>
     * </ul>
     * </p>
     *
     * @param cell the Excel cell to read from; may be {@code null}
     * @return {@code true} if the cell represents a truthy value; {@code false} otherwise
     */
    private boolean getCellValueAsBoolean(Cell cell) {
        if (cell == null) {
            return false;
        }

        return switch (cell.getCellType()) {
            case BOOLEAN -> cell.getBooleanCellValue();
            case STRING -> {
                String value = cell.getStringCellValue().trim().toLowerCase();
                yield value.equals("true") || value.equals("yes") || value.equals("ano") || value.equals("1");
            }
            case NUMERIC -> cell.getNumericCellValue() != 0;
            case FORMULA -> {
                // Use the cached formula result type to evaluate the formula safely
                var cachedType = cell.getCachedFormulaResultType();
                yield switch (cachedType) {
                    case BOOLEAN -> cell.getBooleanCellValue();
                    case NUMERIC -> cell.getNumericCellValue() != 0;
                    case STRING -> {
                        String value = cell.getStringCellValue().trim().toLowerCase();
                        yield value.equals("true") || value.equals("yes") || value.equals("ano") || value.equals("1");
                    }
                    default -> false;
                };
            }
            case BLANK -> false;
            default -> false;
        };
    }

    /**
     * Updates the reportGenerated column (column G) in the Excel file for the given month.
     * Matches clients by ICO and updates only the column G values.
     *
     * @param filePath the path to the Excel file
     * @param clients  the list of clients with updated reportGenerated values
     * @param month    the month (sheet) to update
     * @throws FileNotFoundException if the file does not exist
     * @throws IOException           if an error occurs while reading or writing the file
     */
    @Override
    public void updateReportGeneratedStatus(String filePath, List<Client> clients, CzechMonth month) throws IOException {
        log.debug("Updating report status for {} clients in file: {} for month: {}",
                clients != null ? clients.size() : 0, filePath, month.getCzechName());
        File file = new File(filePath);

        if (!file.exists()) {
            log.error("File does not exist: {}", filePath);
            throw new FileNotFoundException("File not found: " + filePath);
        }

        // Create a set for quick lookup by ICO - include ICOs of clients with reportGenerated = true
        Set<String> icoSet = (clients == null ? List.<Client>of() : clients).stream()
                .filter(Client::reportGenerated)
                .map(Client::ico)
                .filter(s -> s != null && !s.trim().isEmpty())
                .map(String::trim)
                .collect(Collectors.toSet());

        try (FileInputStream fis = new FileInputStream(file);
             XSSFWorkbook workbook = new XSSFWorkbook(fis)) {

            // Find the sheet with the month name
            Sheet sheet = workbook.getSheet(month.getCzechName());
            if (sheet == null) {
                log.error("Sheet '{}' not found in file: {}", month.getCzechName(), filePath);
                throw new IOException("Sheet '" + month.getCzechName() + "' not found in file: " + filePath);
            }

            log.info("Updating sheet '{}' in file: {}", month.getCzechName(), filePath);

            int updatedCount = 0;

            // Skip the header row (row 0) and start updating from row 1
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }

                // Column B (index 1): ICO
                Cell icoCell = row.getCell(1);
                String ico = getCellValueAsString(icoCell);

                if (ico != null && !ico.trim().isEmpty() && icoSet.contains(ico.trim())) {
                     // Column G (index 6): Report Generated
                     Cell reportGeneratedCell = row.getCell(6);

                     if (reportGeneratedCell == null) {
                         reportGeneratedCell = row.createCell(6);
                     }

                     reportGeneratedCell.setCellValue(true);
                     updatedCount++;
                     log.debug("Updated ICO '{}' reportGenerated to: true", ico.trim());
                 }
             }

            if (updatedCount > 0) {
                log.info("Successfully updated {} clients in sheet '{}'", updatedCount, month.getCzechName());

                // Write the changes back to the file
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    workbook.write(fos);
                }
            } else {
                log.info("No clients needed updating in sheet '{}" , month.getCzechName());
            }

        } catch (IOException e) {
            log.error("Error updating Excel file: {}", filePath, e);
            throw e;
        }
    }
}
