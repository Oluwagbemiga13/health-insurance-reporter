package cz.oluwagbemiga.eutax.tools;

import cz.oluwagbemiga.eutax.pojo.Report;
import cz.oluwagbemiga.eutax.pojo.WalkerResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportFromFilesTest {

    @TempDir
    Path tempDir;

    @Test
    void readReportsParsesIcoAndNameFromFiles() throws IOException {
        writeFile("Mega_Company_sro_12345678.txt", "irrelevant content");
        writeFile("Another-Biz-87654321.txt", "any content");

        ReportFromFiles reader = new ReportFromFiles();
        WalkerResult result = reader.readReports(tempDir.toString());
        List<Report> reports = result.reports().stream()
                .sorted(Comparator.comparing(Report::fileName))
                .toList();

        assertEquals(2, reports.size());
        Report first = reports.get(0);
        assertEquals("Another-Biz-87654321.txt", first.fileName());
        assertEquals("Another Biz", first.clientName());
        assertEquals("87654321", first.clientIco());

        Report second = reports.get(1);
        assertEquals("Mega_Company_sro_12345678.txt", second.fileName());
        assertEquals("Mega Company sro", second.clientName());
        assertEquals("12345678", second.clientIco());
        assertTrue(result.errorReports().isEmpty());
    }

    @Test
    void readReportsSkipsFilesWithoutIco() throws IOException {
        writeFile("NoIcoFile.txt", "Only Text Without Digits");
        writeFile("Valid-Client-11223344.txt", "Valid Client 11223344 here");

        ReportFromFiles reader = new ReportFromFiles();
        WalkerResult result = reader.readReports(tempDir.toString());

        assertEquals(1, result.reports().size());
        Report report = result.reports().get(0);
        assertEquals("Valid-Client-11223344.txt", report.fileName());
        assertEquals("Valid Client", report.clientName());
        assertEquals("11223344", report.clientIco());

        assertEquals(1, result.errorReports().size());
        assertEquals("NoIcoFile.txt", result.errorReports().get(0).fileName());
        assertTrue(result.errorReports().get(0).errorMessage().contains("ICO not found"));
    }

    @Test
    void readReportsReturnsErrorWhenPathIsNotDirectory() throws IOException {
        Path regularFile = Files.createFile(tempDir.resolve("single-file.txt"));

        ReportFromFiles reader = new ReportFromFiles();
        WalkerResult result = reader.readReports(regularFile.toString());

        assertTrue(result.reports().isEmpty());
        assertEquals(1, result.errorReports().size());
        assertTrue(result.errorReports().get(0).errorMessage().contains("not a directory"));
    }

    @Test
    void readReportsHandlesNestedDirectoriesAndAggregatesErrors() throws IOException {
        writeFile("Top-Level-99990000.txt", "top");
        writeFile(tempDir.resolve("sub/DeepFirm_11112222_extra.txt"), "nested");
        writeFile("BrokenFile123.txt", "missing ico");

        ReportFromFiles reader = new ReportFromFiles();
        WalkerResult result = reader.readReports(tempDir.toString());

        List<Report> reports = result.reports().stream()
                .sorted(Comparator.comparing(Report::fileName))
                .toList();

        assertEquals(2, reports.size());
        assertEquals("DeepFirm_11112222_extra.txt", reports.get(0).fileName());
        assertEquals("DeepFirm extra", reports.get(0).clientName());
        assertEquals("11112222", reports.get(0).clientIco());
        assertEquals("Top-Level-99990000.txt", reports.get(1).fileName());
        assertEquals("Top Level", reports.get(1).clientName());
        assertEquals("99990000", reports.get(1).clientIco());

        assertEquals(1, result.errorReports().size());
        assertEquals("BrokenFile123.txt", result.errorReports().get(0).fileName());
    }

    private void writeFile(String filename, String content) throws IOException {
        Files.writeString(tempDir.resolve(filename), content);
    }

    private void writeFile(Path targetPath, String content) throws IOException {
        Files.createDirectories(targetPath.getParent());
        Files.writeString(targetPath, content);
    }
}
