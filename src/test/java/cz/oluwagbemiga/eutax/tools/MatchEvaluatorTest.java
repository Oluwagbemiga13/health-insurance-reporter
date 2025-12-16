package cz.oluwagbemiga.eutax.tools;

import cz.oluwagbemiga.eutax.pojo.Client;
import cz.oluwagbemiga.eutax.pojo.CzechMonth;
import cz.oluwagbemiga.eutax.pojo.ErrorReport;
import cz.oluwagbemiga.eutax.pojo.InsuranceCompany;
import cz.oluwagbemiga.eutax.pojo.ParsedFileName;
import cz.oluwagbemiga.eutax.pojo.WalkerResult;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MatchEvaluatorTest {

    static class SpreadsheetWorkerStub implements SpreadsheetWorker {
        private final List<Client> clients;

        SpreadsheetWorkerStub(List<Client> clients) {
            this.clients = clients;
        }

        @Override
        public List<Client> readClients(String filePath, CzechMonth month) throws FileNotFoundException {
            return clients;
        }

        @Override
        public void updateReportGeneratedStatus(String filePath, List<Client> clients, CzechMonth month) {
            // no-op for tests
        }
    }

    static class InfoFromFilesStub extends InfoFromFiles {
        private final WalkerResult result;

        InfoFromFilesStub(WalkerResult result) {
            this.result = result;
        }

        @Override
        public WalkerResult readReports(String folderPath) {
            return result;
        }
    }

    @Test
    void evaluateMatches_filtersByYearAndMonth_andSetsFlags() throws Exception {
        // Given
        List<Client> clients = List.of(
                new Client("Klient A s.r.o", "00000001", false, List.of()),
                new Client("Klient B s.r.o", "00000002", false, List.of()),
                new Client("Klient C a.s.", "00000003", false, List.of())
        );
        int year = 2025;
        CzechMonth month = CzechMonth.LEDEN; // January

        List<ParsedFileName> parsed = List.of(
                new ParsedFileName("00000001", LocalDate.of(year, month.getMonthNumber(), 15), InsuranceCompany.CPZP, ""), // match
                new ParsedFileName("00000002", LocalDate.of(year, month.getMonthNumber(), 20), InsuranceCompany.CPZP, ""), // match
                new ParsedFileName("00000003", LocalDate.of(year, month.getMonthNumber() == 12 ? 1 : month.getMonthNumber() + 1, 1), InsuranceCompany.CPZP, ""), // different month
                new ParsedFileName("00000001", LocalDate.of(year, month.getMonthNumber(), 16), InsuranceCompany.CPZP, "") // duplicate ICO same month
        );
        WalkerResult walkerResult = new WalkerResult(parsed, List.of());

        MatchEvaluator evaluator = new MatchEvaluator(
                new SpreadsheetWorkerStub(clients),
                new InfoFromFilesStub(walkerResult)
        );

        // When
        List<Client> updated = evaluator.evaluateMatches("ignored", "ignoredDir", month, year);

        // Then
        assertEquals(3, updated.size());
        // to preserve input order
        assertEquals("00000001", updated.get(0).ico());
        assertTrue(updated.get(0).reportGenerated(), "ICO 00000001 should be marked as having report");
        assertEquals("00000002", updated.get(1).ico());
        assertTrue(updated.get(1).reportGenerated(), "ICO 00000002 should be marked as having report");
        assertEquals("00000003", updated.get(2).ico());
        assertFalse(updated.get(2).reportGenerated(), "ICO 00000003 has a report but in different month");
    }

    @Test
    void evaluateMatches_noReports_marksAllFalse() throws Exception {
        List<Client> clients = List.of(
                new Client("Klient A s.r.o", "00000001", true, List.of()), // input irrelevant, should be recalculated
                new Client("Klient B s.r.o", "00000002", true, List.of())
        );
        WalkerResult empty = new WalkerResult(List.of(), List.of());
        MatchEvaluator evaluator = new MatchEvaluator(new SpreadsheetWorkerStub(clients), new InfoFromFilesStub(empty));

        List<Client> updated = evaluator.evaluateMatches("ignored", "ignoredDir", CzechMonth.UNOR, 2024);

        assertEquals(2, updated.size());
        assertFalse(updated.get(0).reportGenerated());
        assertFalse(updated.get(1).reportGenerated());
    }

    @Test
    void evaluateMatches_filtersOutDifferentYear() throws Exception {
        List<Client> clients = List.of(new Client("Klient A s.r.o", "00000001", false, List.of()));
        int year = 2025;
        CzechMonth month = CzechMonth.BREZEN;
        List<ParsedFileName> parsed = new ArrayList<>();
        parsed.add(new ParsedFileName("00000001", LocalDate.of(year - 1, month.getMonthNumber(), 10), InsuranceCompany.CPZP, "")); // different year
        parsed.add(new ParsedFileName("00000001", LocalDate.of(year, month.getMonthNumber(), 10), InsuranceCompany.CPZP, "")); // matching
        WalkerResult wr = new WalkerResult(parsed, List.of());

        MatchEvaluator evaluator = new MatchEvaluator(new SpreadsheetWorkerStub(clients), new InfoFromFilesStub(wr));
        List<Client> updated = evaluator.evaluateMatches("ignored", "ignoredDir", month, year);

        assertEquals(1, updated.size());
        assertTrue(updated.get(0).reportGenerated());
    }

    @Test
    void evaluateMatches_currentYearOverload_usesNowYear() throws Exception {
        int currentYear = LocalDate.now().getYear();
        CzechMonth targetMonth = CzechMonth.LISTOPAD; // arbitrary month
        List<Client> clients = List.of(
                new Client("One", "11111111", false, List.of()),
                new Client("Two", "22222222", false, List.of())
        );

        List<ParsedFileName> parsed = List.of(
                new ParsedFileName("11111111", LocalDate.of(currentYear, targetMonth.getMonthNumber(), 5), InsuranceCompany.CPZP, ""), // match
                new ParsedFileName("22222222", LocalDate.of(currentYear - 1, targetMonth.getMonthNumber(), 5), InsuranceCompany.CPZP, "") // different year -> no match
        );

        MatchEvaluator evaluator = new MatchEvaluator(
                new SpreadsheetWorkerStub(clients),
                new InfoFromFilesStub(new WalkerResult(parsed, List.of()))
        );

        List<Client> updated = evaluator.evaluateMatches("ignored", "ignoredDir", targetMonth);
        assertEquals(2, updated.size());
        assertTrue(updated.get(0).reportGenerated());
        assertFalse(updated.get(1).reportGenerated());
    }

    // New tests start here

    @Test
    void evaluateMatches_trimsIcoKeys_matchesWhenWhitespace() throws Exception {
        int year = 2025;
        CzechMonth month = CzechMonth.DUBEN;

        List<Client> clients = List.of(new Client("Trim Client", " 33333333 ", false, List.of()));
        List<ParsedFileName> parsed = List.of(new ParsedFileName("33333333", LocalDate.of(year, month.getMonthNumber(), 10), InsuranceCompany.VZP, ""));
        WalkerResult wr = new WalkerResult(parsed, List.of());

        MatchEvaluator evaluator = new MatchEvaluator(new SpreadsheetWorkerStub(clients), new InfoFromFilesStub(wr));
        List<Client> updated = evaluator.evaluateMatches("ignored", "ignoredDir", month, year);

        assertEquals(1, updated.size());
        assertTrue(updated.get(0).reportGenerated(), "Whitespace around ICO should be trimmed and match");
    }

    @Test
    void evaluateMatches_handlesMultipleRequiredInsurers_successAndPartialFail() throws Exception {
        int year = 2024;
        CzechMonth month = CzechMonth.KVETEN;

        Client successClient = new Client("Multi OK", "77777777", false, List.of(InsuranceCompany.VZP, InsuranceCompany.CPZP));
        Client failClient = new Client("Multi Partial", "88888888", false, List.of(InsuranceCompany.VZP, InsuranceCompany.CPZP));

        List<Client> clients = List.of(successClient, failClient);

        List<ParsedFileName> parsed = List.of(
                // for successClient both insurers present
                new ParsedFileName("77777777", LocalDate.of(year, month.getMonthNumber(), 1), InsuranceCompany.VZP, ""),
                new ParsedFileName("77777777", LocalDate.of(year, month.getMonthNumber(), 2), InsuranceCompany.CPZP, ""),
                // for failClient only one insurer present
                new ParsedFileName("88888888", LocalDate.of(year, month.getMonthNumber(), 3), InsuranceCompany.VZP, "")
        );

        WalkerResult wr = new WalkerResult(parsed, List.of());
        MatchEvaluator evaluator = new MatchEvaluator(new SpreadsheetWorkerStub(clients), new InfoFromFilesStub(wr));

        List<Client> updated = evaluator.evaluateMatches("ignored", "ignoredDir", month, year);
        assertEquals(2, updated.size());
        assertTrue(updated.get(0).reportGenerated(), "Client with all required insurers should be marked as having report");
        assertFalse(updated.get(1).reportGenerated(), "Client missing one insurer should not be marked as having report");
    }

    @Test
    void evaluateMatches_ignoresInvalidDirectories() throws Exception {
        int year = 2023;
        CzechMonth month = CzechMonth.CERVEN;

        Client client = new Client("Invalid Dir", "44444444", false, List.of());
        List<Client> clients = List.of(client);

        List<ParsedFileName> parsed = List.of(
                new ParsedFileName("44444444", LocalDate.of(year, month.getMonthNumber(), 5), InsuranceCompany.VZP, "", true, "badDir")
        );

        WalkerResult wr = new WalkerResult(parsed, List.of());
        MatchEvaluator evaluator = new MatchEvaluator(new SpreadsheetWorkerStub(clients), new InfoFromFilesStub(wr));

        List<Client> updated = evaluator.evaluateMatches("ignored", "ignoredDir", month, year);
        assertEquals(1, updated.size());
        assertFalse(updated.get(0).reportGenerated(), "Files from invalid directories must be excluded from matching");
    }

    @Test
    void evaluateMatches_handlesErrorReports_butStillMatches() throws Exception {
        int year = 2022;
        CzechMonth month = CzechMonth.SRPEN; // August

        Client client = new Client("Err Client", "55555555", false, List.of());
        List<Client> clients = List.of(client);

        List<ParsedFileName> parsed = List.of(
                new ParsedFileName("55555555", LocalDate.of(year, month.getMonthNumber(), 12), InsuranceCompany.ZPMV, "")
        );

        List<ErrorReport> errors = List.of(new ErrorReport("badfile.pdf", "parse error"));
        WalkerResult wr = new WalkerResult(parsed, errors);

        MatchEvaluator evaluator = new MatchEvaluator(new SpreadsheetWorkerStub(clients), new InfoFromFilesStub(wr));
        List<Client> updated = evaluator.evaluateMatches("ignored", "ignoredDir", month, year);

        assertEquals(1, updated.size());
        assertTrue(updated.get(0).reportGenerated(), "Presence of error reports should not prevent valid parsed files from matching");
    }

    @Test
    void evaluateMatches_nullOrBlankIco_noMatch() throws Exception {
        int year = 2025;
        CzechMonth month = CzechMonth.ZARI;

        Client nullIco = new Client("Null ICO", null, false, List.of());
        Client blankIco = new Client("Blank ICO", "   ", false, List.of());
        Client normal = new Client("Normal", "99999999", false, List.of());

        List<Client> clients = List.of(nullIco, blankIco, normal);

        List<ParsedFileName> parsed = List.of(new ParsedFileName("99999999", LocalDate.of(year, month.getMonthNumber(), 10), InsuranceCompany.VOZP, ""));
        WalkerResult wr = new WalkerResult(parsed, List.of(new ErrorReport("somefile", "parse error")));

        MatchEvaluator evaluator = new MatchEvaluator(new SpreadsheetWorkerStub(clients), new InfoFromFilesStub(wr));
        List<Client> updated = evaluator.evaluateMatches("ignored", "ignoredDir", month, year);

        assertEquals(3, updated.size());
        assertFalse(updated.get(0).reportGenerated(), "Client with null ICO should not match");
        assertFalse(updated.get(1).reportGenerated(), "Client with blank ICO should not match");
        assertTrue(updated.get(2).reportGenerated(), "Normal client should match when parsed file exists");
    }

}
