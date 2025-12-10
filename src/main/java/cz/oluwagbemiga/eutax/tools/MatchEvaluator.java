package cz.oluwagbemiga.eutax.tools;

import cz.oluwagbemiga.eutax.pojo.Client;
import cz.oluwagbemiga.eutax.pojo.CzechMonth;
import cz.oluwagbemiga.eutax.pojo.ParsedFileName;
import cz.oluwagbemiga.eutax.pojo.WalkerResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.FileNotFoundException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service class that evaluates which clients have matching report files and updates their report status.
 * <p>
 * This class acts as a bridge between the spreadsheet data (clients) and the file system (reports).
 * It matches clients from a spreadsheet with report files found in a directory by comparing
 * client ICO numbers with ICO numbers extracted from file names.
 * </p>
 * <p>
 * The matching process:
 * <ol>
 *     <li>Reads client information from the spreadsheet for the specified month</li>
 *     <li>Scans the reports folder for files and parses their names to extract ICO and date</li>
 *     <li>Filters reports to only include those matching the target year and month</li>
 *     <li>Updates each client's {@code reportGenerated} status based on whether a matching report exists</li>
 * </ol>
 * </p>
 *
 * @see SpreadsheetWorker
 * @see IcoFromFiles
 * @see Client
 */
@Slf4j
@RequiredArgsConstructor
public class MatchEvaluator {

    /** The spreadsheet worker used to read and write client data */
    private final SpreadsheetWorker spreadsheetWorker;

    /** The file parser used to extract metadata from report file names */
    private final IcoFromFiles icoFromFiles;

    /**
     * Reads clients from a spreadsheet and checks if they have corresponding report files for the current year.
     * <p>
     * Uses the current year from {@link LocalDate#now()} as the target year for matching reports.
     * </p>
     *
     * @param spreadsheetIdentifier path to the spreadsheet or Google Sheets identifier containing client information
     * @param reportsFolder         path to the folder containing report files
     * @param month                 the month to process (determines which sheet to read and which reports to match)
     * @return list of clients with updated {@code reportGenerated} status; {@code true} if a matching report exists
     * @throws FileNotFoundException if the spreadsheet doesn't exist
     * @see #evaluateMatches(String, String, CzechMonth, int) for specifying a custom year
     */
    public List<Client> evaluateMatches(String spreadsheetIdentifier, String reportsFolder, CzechMonth month)
            throws FileNotFoundException {
        log.debug("Evaluating matches for spreadsheet: {}, reports folder: {}, month: {}",
                spreadsheetIdentifier, reportsFolder, month.getCzechName());

        log.info("Starting match evaluation for month: {}", month.getCzechName());

        // Read clients from spreadsheet
        List<Client> clients = spreadsheetWorker.readClients(spreadsheetIdentifier, month);
        log.info("Read {} clients from spreadsheet", clients.size());

        // Read reports from file system
        WalkerResult walkerResult = icoFromFiles.readReports(reportsFolder);
        List<ParsedFileName> parsedFiles = walkerResult.parsedFileNames();
        log.info("Found {} parsed report files", parsedFiles.size());

        if (!walkerResult.errorReports().isEmpty()) {
            log.warn("Encountered {} errors while reading reports:", walkerResult.errorReports().size());
            walkerResult.errorReports().forEach(error ->
                    log.warn("  - {}: {}", error.fileName(), error.errorMessage())
            );
        }

        // Create a map of ICO -> LocalDate for the target month
        int targetYear = LocalDate.now().getYear();
        int targetMonth = month.getMonthNumber();

        Map<String, LocalDate> icoToReportDate = parsedFiles.stream()
                .filter(pf -> pf.date().getYear() == targetYear
                        && pf.date().getMonthValue() == targetMonth)
                .collect(Collectors.toMap(
                        ParsedFileName::ico,
                        ParsedFileName::date,
                        (existing, replacement) -> existing // Keep first occurrence if duplicates
                ));

        log.info("Found {} reports matching year {} and month {}",
                icoToReportDate.size(), targetYear, targetMonth);

        return updateClientsWithMatches(clients, icoToReportDate);
    }

    /**
     * Reads clients from a spreadsheet and checks if they have corresponding report files for a specific year.
     * <p>
     * This overloaded method allows processing historical data by specifying a custom year.
     * </p>
     *
     * @param spreadsheetIdentifier path to the spreadsheet or Google Sheets identifier containing client information
     * @param reportsFolder         path to the folder containing report files
     * @param month                 the month to process (determines which sheet to read and which reports to match)
     * @param year                  the year to match reports against (e.g., 2024 for historical processing)
     * @return list of clients with updated {@code reportGenerated} status; {@code true} if a matching report exists
     * @throws FileNotFoundException if the spreadsheet doesn't exist
     */
    public List<Client> evaluateMatches(String spreadsheetIdentifier, String reportsFolder,
                                        CzechMonth month, int year)
            throws FileNotFoundException {
        log.debug("Evaluating matches for spreadsheet: {}, reports folder: {}, month: {}, year: {}",
                spreadsheetIdentifier, reportsFolder, month.getCzechName(), year);

        log.info("Starting match evaluation for month: {} and year: {}",
                month.getCzechName(), year);

        List<Client> clients = spreadsheetWorker.readClients(spreadsheetIdentifier, month);
        log.info("Read {} clients from spreadsheet", clients.size());

        WalkerResult walkerResult = icoFromFiles.readReports(reportsFolder);
        List<ParsedFileName> parsedFiles = walkerResult.parsedFileNames();
        log.info("Found {} parsed report files", parsedFiles.size());

        if (!walkerResult.errorReports().isEmpty()) {
            log.warn("Encountered {} errors while reading reports:", walkerResult.errorReports().size());
            walkerResult.errorReports().forEach(error ->
                    log.warn("  - {}: {}", error.fileName(), error.errorMessage())
            );
        }

        Map<String, LocalDate> icoToReportDate = parsedFiles.stream()
                .filter(pf -> pf.date().getYear() == year
                        && pf.date().getMonthValue() == month.getMonthNumber())
                .collect(Collectors.toMap(
                        ParsedFileName::ico,
                        ParsedFileName::date,
                        (existing, replacement) -> existing
                ));

        log.info("Found {} reports matching year {} and month {}",
                icoToReportDate.size(), year, month.getMonthNumber());

        return updateClientsWithMatches(clients, icoToReportDate);
    }

    /**
     * Updates the {@code reportGenerated} status for each client based on whether their ICO
     * exists in the provided report date map.
     *
     * @param clients         the original list of clients from the spreadsheet
     * @param icoToReportDate a map of ICO numbers to their corresponding report dates
     * @return a new list of clients with updated {@code reportGenerated} status
     */
    private List<Client> updateClientsWithMatches(List<Client> clients, Map<String, LocalDate> icoToReportDate) {
        log.debug("Updating {} clients with {} available reports", clients.size(), icoToReportDate.size());
        List<Client> updatedClients = new ArrayList<>();
        for (Client client : clients) {
            boolean hasReport = icoToReportDate.containsKey(client.ico());
            updatedClients.add(new Client(
                    client.name(),
                    client.ico(),
                    hasReport
            ));

            log.debug("Client {}: ICO={}, Report={}",
                    client.name(), client.ico(), hasReport);
        }

        long withReports = updatedClients.stream()
                .filter(Client::reportGenerated)
                .count();
        log.info("Match evaluation complete: {}/{} clients have reports",
                withReports, updatedClients.size());

        return updatedClients;
    }
}
