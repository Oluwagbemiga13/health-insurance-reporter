package cz.oluwagbemiga.eutax.tools;

import cz.oluwagbemiga.eutax.pojo.Client;
import cz.oluwagbemiga.eutax.pojo.CzechMonth;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

/**
 * Interface for reading and writing client data to spreadsheet files.
 * <p>
 * This abstraction allows the application to work with different spreadsheet formats
 * (e.g., local Excel files, Google Sheets) through a unified API.
 * </p>
 * <p>
 * Implementations are expected to:
 * <ul>
 *     <li>Read client data from sheets named after Czech month names</li>
 *     <li>Parse client name from column A, ICO from column B, and report status from column G</li>
 *     <li>Update the report generated status in column G based on ICO matching</li>
 * </ul>
 * </p>
 *
 * @see ExcelWorker
 * @see GoogleWorker
 * @see SpreadsheetWorkerFactory
 */
public interface SpreadsheetWorker {

    /**
     * Reads client information from the specified spreadsheet for the given month.
     * <p>
     * The implementation should look for a sheet named after the Czech month name
     * (e.g., "Leden" for January) and read client data starting from the second row
     * (assuming the first row contains headers).
     * </p>
     *
     * @param filePath the path to the spreadsheet file or identifier (implementation-specific)
     * @param month    the month determining which sheet to read from
     * @return a list of {@link Client} objects read from the spreadsheet; may be empty but never null
     * @throws FileNotFoundException if the specified spreadsheet or sheet does not exist
     */
    List<Client> readClients(String filePath, CzechMonth month) throws FileNotFoundException;

    /**
     * Updates the report generated status (column G) in the spreadsheet for clients whose
     * {@code reportGenerated} field is {@code true}.
     * <p>
     * The implementation should match clients by their ICO number and update only the
     * corresponding cells in column G. Clients with {@code reportGenerated = false} should
     * not affect existing values in the spreadsheet.
     * </p>
     *
     * @param filePath the path to the spreadsheet file or identifier (implementation-specific)
     * @param clients  the list of clients with potentially updated report status
     * @param month    the month determining which sheet to update
     * @throws IOException if an error occurs while reading or writing the spreadsheet
     */
    void updateReportGeneratedStatus(String filePath, List<Client> clients, CzechMonth month) throws IOException;

}
