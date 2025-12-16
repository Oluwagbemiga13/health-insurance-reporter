package cz.oluwagbemiga.eutax.tools;

import cz.oluwagbemiga.eutax.pojo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.FileNotFoundException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
 * @see InfoFromFiles
 * @see Client
 */
@Slf4j
@RequiredArgsConstructor
public class MatchEvaluator {

    /** The spreadsheet worker used to read and write client data */
    private final SpreadsheetWorker spreadsheetWorker;

    /** The file parser used to extract metadata from report file names */
    private final InfoFromFiles infoFromFiles;

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
        WalkerResult walkerResult = infoFromFiles.readReports(reportsFolder);
        List<ParsedFileName> parsedFiles = walkerResult.parsedFileNames();
        log.info("Found {} parsed report files", parsedFiles.size());

        if (!walkerResult.errorReports().isEmpty()) {
            log.warn("Encountered {} errors while reading reports:", walkerResult.errorReports().size());
            walkerResult.errorReports().forEach(error ->
                    log.warn("  - {}: {}", error.fileName(), error.errorMessage())
            );
        }

        // Create a map of ICO -> Set<InsuranceCompany> for the target month
        int targetYear = LocalDate.now().getYear();
        int targetMonth = month.getMonthNumber();

        // Exclude files with invalid directories from matching
        long invalidCount = parsedFiles.stream().filter(ParsedFileName::invalidDirectory).count();
        if (invalidCount > 0) {
            log.info("Excluding {} files with invalid parent directories from matching", invalidCount);
        }

        Map<String, Set<InsuranceCompany>> icoToInsurers = parsedFiles.stream()
                .filter(pf -> !pf.invalidDirectory())
                .filter(pf -> pf.date().getYear() == targetYear
                        && pf.date().getMonthValue() == targetMonth)
                .collect(Collectors.groupingBy(
                        ParsedFileName::ico,
                        Collectors.mapping(ParsedFileName::insuranceCompany, Collectors.toSet())
                ));

        log.info("Found {} reports (by ICO) matching year {} and month {}",
                icoToInsurers.size(), targetYear, targetMonth);

        return updateClientsWithMatches(clients, icoToInsurers);
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

        WalkerResult walkerResult = infoFromFiles.readReports(reportsFolder);
        List<ParsedFileName> parsedFiles = walkerResult.parsedFileNames();
        log.info("Found {} parsed report files", parsedFiles.size());

        if (!walkerResult.errorReports().isEmpty()) {
            log.warn("Encountered {} errors while reading reports:", walkerResult.errorReports().size());
            walkerResult.errorReports().forEach(error ->
                    log.warn("  - {}: {}", error.fileName(), error.errorMessage())
            );
        }

        Map<String, Set<InsuranceCompany>> icoToInsurers = parsedFiles.stream()
                .filter(pf -> !pf.invalidDirectory())
                .filter(pf -> pf.date().getYear() == year
                        && pf.date().getMonthValue() == month.getMonthNumber())
                .collect(Collectors.groupingBy(
                        ParsedFileName::ico,
                        Collectors.mapping(ParsedFileName::insuranceCompany, Collectors.toSet())
                ));

        log.info("Found {} reports (by ICO) matching year {} and month {}",
                icoToInsurers.size(), year, month.getMonthNumber());

        return updateClientsWithMatches(clients, icoToInsurers);
    }

    /**
     * Updates the {@code reportGenerated} status for each client based on whether all their required
     * insurance companies exist in the provided report insurer map for their ICO.
     *
     * @param clients       the original list of clients from the spreadsheet
     * @param icoToInsurers a map of ICO numbers to the set of insurers available for that ICO in the target month
     * @return a new list of clients with updated {@code reportGenerated} status
     */
    private List<Client> updateClientsWithMatches(List<Client> clients, Map<String, Set<InsuranceCompany>> icoToInsurers) {
        log.debug("Updating {} clients with {} available report-insurer entries", clients.size(), icoToInsurers.size());
        List<Client> updatedClients = new ArrayList<>();

        // Normalize the map keys (trim) to avoid missing matches due to whitespace
        Map<String, Set<InsuranceCompany>> normalizedMap = icoToInsurers.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey() == null ? null : e.getKey().trim(), Map.Entry::getValue));

        for (Client client : clients) {
            boolean hasReport = false;
            String clientIcoKey = client.ico() == null ? null : client.ico().trim();
            Set<InsuranceCompany> available = normalizedMap.get(clientIcoKey);

            if (available != null) {
                // If client has no specific required insurers, any available insurer for the ICO counts as a match
                if (client.insuranceCompanies() == null || client.insuranceCompanies().isEmpty()) {
                    hasReport = !available.isEmpty();
                } else {
                    hasReport = available.containsAll(client.insuranceCompanies());
                }
            }

            updatedClients.add(new Client(
                    client.name(),
                    client.ico(),
                    hasReport,
                    client.insuranceCompanies()
            ));

            log.debug("Client {}: ICO={}, Report={}, RequiredInsurers={} AvailableInsurers={} ",
                    client.name(), client.ico(), hasReport, client.insuranceCompanies(), available);
        }

        long withReports = updatedClients.stream()
                .filter(Client::reportGenerated)
                .count();
        log.info("Match evaluation complete: {}/{} clients have reports",
                withReports, updatedClients.size());

        return updatedClients;
    }
}
