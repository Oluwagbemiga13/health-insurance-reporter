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
 * Evaluates which clients have matching report files and updates their reportGenerated status.
 */
@Slf4j
@RequiredArgsConstructor
public class MatchEvaluator {

    private final SpreadsheetWorker spreadsheetWorker;
    private final IcoFromFiles icoFromFiles;

    /**
     * Reads clients from spreadsheet and checks if they have corresponding report files.
     *
     * @param spreadsheetIdentifier path to the spreadsheet containing client information
     * @param reportsFolder         path to the folder containing report files
     * @param month                 the month to process
     * @return list of clients with updated reportGenerated status
     * @throws FileNotFoundException if the spreadsheet doesn't exist
     */
    public List<Client> evaluateMatches(String spreadsheetIdentifier, String reportsFolder, CzechMonth month)
            throws FileNotFoundException {

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
     * Overloaded method that accepts year parameter for historical data processing.
     */
    public List<Client> evaluateMatches(String spreadsheetIdentifier, String reportsFolder,
                                        CzechMonth month, int year)
            throws FileNotFoundException {

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

    private List<Client> updateClientsWithMatches(List<Client> clients, Map<String, LocalDate> icoToReportDate) {
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
