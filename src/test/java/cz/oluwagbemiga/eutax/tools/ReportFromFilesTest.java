package cz.oluwagbemiga.eutax.tools;

import cz.oluwagbemiga.eutax.pojo.WalkerResult;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ReportFromFilesTest {

    private static final String REPORT_DIR = Path.of("src", "test", "resources", "report_dir")
            .toAbsolutePath()
            .toString();

    private final ReportFromFiles reportFromFiles = new ReportFromFiles();

    @Test
    void readReportsParsesValidFilesAndTraversesDirectoryTree() {
        WalkerResult result = reportFromFiles.readReports(REPORT_DIR);

        assertEquals(3, result.parsedFileNames().size());
        assertEquals(2,result.errorReports().size() );

        assertTrue(result.parsedFileNames().stream().anyMatch(parsed ->
                parsed.ico().equals("00000001") && parsed.date().equals(LocalDate.of(2025, 1, 1))));
        assertTrue(result.parsedFileNames().stream().anyMatch(parsed ->
                parsed.ico().equals("00000002") && parsed.date().equals(LocalDate.of(2025, 2, 1))));
        assertTrue(result.parsedFileNames().stream().anyMatch(parsed ->
                parsed.ico().equals("00000003") && parsed.date().equals(LocalDate.of(2025, 3, 1))));

    }

    @Test
    void readReportsCollectsErrorsForInvalidFiles() {
        WalkerResult result = reportFromFiles.readReports(REPORT_DIR);

        assertEquals(2, result.errorReports().size());
        assertTrue(result.errorReports().stream().anyMatch(error ->
                error.fileName().equals("PPPZ-00000001-VZP.pdf") &&
                        error.errorMessage().contains("Filename does not contain year-month information")));
        assertTrue(result.errorReports().stream().anyMatch(error ->
                error.fileName().equals("PPPZ-00000001-VZP.pdf") &&
                        error.errorMessage().contains("Filename does not contain year-month information")));
    }
}
