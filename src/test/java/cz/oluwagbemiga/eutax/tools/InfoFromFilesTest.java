package cz.oluwagbemiga.eutax.tools;

import cz.oluwagbemiga.eutax.pojo.WalkerResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class InfoFromFilesTest {

    private static final String REPORT_DIR = Path.of("src", "test", "resources", "report_dir")
            .toAbsolutePath()
            .toString();

    private final InfoFromFiles infoFromFiles = new InfoFromFiles();

    @Nested
    @DisplayName("readReports() basic functionality")
    class ReadReportsBasicTests {

        @Test
        @DisplayName("should parse valid files and traverse directory tree")
        void readReportsParsesValidFilesAndTraversesDirectoryTree() {
            WalkerResult result = infoFromFiles.readReports(REPORT_DIR);

            assertEquals(3, result.parsedFileNames().size());
            assertEquals(2, result.errorReports().size());

            assertTrue(result.parsedFileNames().stream().anyMatch(parsed ->
                    parsed.ico().equals("00000001") && parsed.date().equals(LocalDate.of(2025, 1, 1))));
            assertTrue(result.parsedFileNames().stream().anyMatch(parsed ->
                    parsed.ico().equals("00000002") && parsed.date().equals(LocalDate.of(2025, 2, 1))));
            assertTrue(result.parsedFileNames().stream().anyMatch(parsed ->
                    parsed.ico().equals("00000003") && parsed.date().equals(LocalDate.of(2025, 3, 1))));
        }

        @Test
        @DisplayName("should collect errors for invalid files")
        void readReportsCollectsErrorsForInvalidFiles() {
            WalkerResult result = infoFromFiles.readReports(REPORT_DIR);

            assertEquals(2, result.errorReports().size());
            assertTrue(result.errorReports().stream().anyMatch(error ->
                    error.fileName().equals("PPPZ-00000001-VZP.pdf") &&
                            error.errorMessage().contains("Filename does not contain year-month information")));
        }
    }

    @Nested
    @DisplayName("readReports() with non-existent or invalid paths")
    class ReadReportsInvalidPathTests {

        @Test
        @DisplayName("should return error for non-existent directory")
        void readReports_nonExistentDirectory_returnsError() {
            WalkerResult result = infoFromFiles.readReports("/non/existent/path");

            assertTrue(result.parsedFileNames().isEmpty());
            assertEquals(1, result.errorReports().size());
            assertTrue(result.errorReports().get(0).errorMessage().contains("is not a directory"));
        }

        @Test
        @DisplayName("should return error when path is a file not a directory")
        void readReports_pathIsFile_returnsError(@TempDir Path tempDir) throws IOException {
            Path file = tempDir.resolve("test.txt");
            Files.createFile(file);

            WalkerResult result = infoFromFiles.readReports(file.toString());

            assertTrue(result.parsedFileNames().isEmpty());
            assertEquals(1, result.errorReports().size());
            assertTrue(result.errorReports().get(0).errorMessage().contains("is not a directory"));
        }

        @Test
        @DisplayName("should return empty result for empty directory")
        void readReports_emptyDirectory_returnsEmptyResult(@TempDir Path tempDir) {
            WalkerResult result = infoFromFiles.readReports(tempDir.toString());

            assertTrue(result.parsedFileNames().isEmpty());
            assertTrue(result.errorReports().isEmpty());
        }
    }

    @Nested
    @DisplayName("File name parsing tests")
    class FileNameParsingTests {

        @Test
        @DisplayName("should parse file with underscore separators")
        void readReports_underscoreSeparators_parsesCorrectly(@TempDir Path tempDir) throws IOException {
            Path file = tempDir.resolve("12345678_VZP_2025_11.pdf");
            Files.createFile(file);

            WalkerResult result = infoFromFiles.readReports(tempDir.toString());

            assertEquals(1, result.parsedFileNames().size());
            assertEquals("12345678", result.parsedFileNames().get(0).ico());
            assertEquals(LocalDate.of(2025, 11, 1), result.parsedFileNames().get(0).date());
        }

        @Test
        @DisplayName("should parse file with hyphen separators")
        void readReports_hyphenSeparators_parsesCorrectly(@TempDir Path tempDir) throws IOException {
            Path file = tempDir.resolve("PPPZ-12345678-2025-06.pdf");
            Files.createFile(file);

            WalkerResult result = infoFromFiles.readReports(tempDir.toString());

            assertEquals(1, result.parsedFileNames().size());
            assertEquals("12345678", result.parsedFileNames().get(0).ico());
            assertEquals(LocalDate.of(2025, 6, 1), result.parsedFileNames().get(0).date());
        }

        @Test
        @DisplayName("should parse file with space separators")
        void readReports_spaceSeparators_parsesCorrectly(@TempDir Path tempDir) throws IOException {
            Path file = tempDir.resolve("Report 12345678 2025 07.pdf");
            Files.createFile(file);

            WalkerResult result = infoFromFiles.readReports(tempDir.toString());

            assertEquals(1, result.parsedFileNames().size());
            assertEquals("12345678", result.parsedFileNames().get(0).ico());
            assertEquals(LocalDate.of(2025, 7, 1), result.parsedFileNames().get(0).date());
        }

        @Test
        @DisplayName("should parse file with mixed separators")
        void readReports_mixedSeparators_parsesCorrectly(@TempDir Path tempDir) throws IOException {
            Path file = tempDir.resolve("12345678_VZP-2025_08.pdf");
            Files.createFile(file);

            WalkerResult result = infoFromFiles.readReports(tempDir.toString());

            assertEquals(1, result.parsedFileNames().size());
            assertEquals("12345678", result.parsedFileNames().get(0).ico());
            assertEquals(LocalDate.of(2025, 8, 1), result.parsedFileNames().get(0).date());
        }

        @Test
        @DisplayName("should parse file without extension")
        void readReports_noExtension_parsesCorrectly(@TempDir Path tempDir) throws IOException {
            Path file = tempDir.resolve("12345678_VZP_2025_09");
            Files.createFile(file);

            WalkerResult result = infoFromFiles.readReports(tempDir.toString());

            assertEquals(1, result.parsedFileNames().size());
            assertEquals("12345678", result.parsedFileNames().get(0).ico());
            assertEquals(LocalDate.of(2025, 9, 1), result.parsedFileNames().get(0).date());
        }

        @ParameterizedTest
        @ValueSource(strings = {"01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12"})
        @DisplayName("should parse all valid months")
        void readReports_validMonths_parsesCorrectly(String month, @TempDir Path tempDir) throws IOException {
            Path file = tempDir.resolve("12345678_VZP_2025_" + month + ".pdf");
            Files.createFile(file);

            WalkerResult result = infoFromFiles.readReports(tempDir.toString());

            assertEquals(1, result.parsedFileNames().size());
            assertEquals(Integer.parseInt(month), result.parsedFileNames().get(0).date().getMonthValue());
        }
    }

    @Nested
    @DisplayName("Error handling tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("should report error for file without ICO")
        void readReports_noIco_reportsError(@TempDir Path tempDir) throws IOException {
            Path file = tempDir.resolve("VZP_2025_01.pdf");
            Files.createFile(file);

            WalkerResult result = infoFromFiles.readReports(tempDir.toString());

            assertTrue(result.parsedFileNames().isEmpty());
            assertEquals(1, result.errorReports().size());
            assertTrue(result.errorReports().get(0).errorMessage().contains("ICO not found"));
        }

        @Test
        @DisplayName("should report error for file without year-month")
        void readReports_noYearMonth_reportsError(@TempDir Path tempDir) throws IOException {
            Path file = tempDir.resolve("PPPZ-12345678-VZP.pdf");
            Files.createFile(file);

            WalkerResult result = infoFromFiles.readReports(tempDir.toString());

            assertTrue(result.parsedFileNames().isEmpty());
            assertEquals(1, result.errorReports().size());
            assertTrue(result.errorReports().get(0).errorMessage().contains("year-month information"));
        }

        @Test
        @DisplayName("should report error for invalid month (13)")
        void readReports_invalidMonth_reportsError(@TempDir Path tempDir) throws IOException {
            Path file = tempDir.resolve("PPPZ-12345678-2025-13.pdf");
            Files.createFile(file);

            WalkerResult result = infoFromFiles.readReports(tempDir.toString());

            assertTrue(result.parsedFileNames().isEmpty());
            assertEquals(1, result.errorReports().size());
        }

        @Test
        @DisplayName("should report error for file with too few segments")
        void readReports_tooFewSegments_reportsError(@TempDir Path tempDir) throws IOException {
            Path file = tempDir.resolve("12345678-01.pdf");
            Files.createFile(file);

            WalkerResult result = infoFromFiles.readReports(tempDir.toString());

            assertTrue(result.parsedFileNames().isEmpty());
            assertEquals(1, result.errorReports().size());
            assertTrue(result.errorReports().get(0).errorMessage().contains("not provide enough segments"));
        }

        @Test
        @DisplayName("should report error for 7-digit ICO (not 8)")
        void readReports_shortIco_reportsError(@TempDir Path tempDir) throws IOException {
            Path file = tempDir.resolve("1234567_VZP_2025_01.pdf");
            Files.createFile(file);

            WalkerResult result = infoFromFiles.readReports(tempDir.toString());

            assertTrue(result.parsedFileNames().isEmpty());
            assertEquals(1, result.errorReports().size());
            assertTrue(result.errorReports().get(0).errorMessage().contains("ICO not found"));
        }

        @Test
        @DisplayName("should report error for 9-digit ICO (not 8)")
        void readReports_longIco_reportsError(@TempDir Path tempDir) throws IOException {
            Path file = tempDir.resolve("123456789_VZP_2025_01.pdf");
            Files.createFile(file);

            WalkerResult result = infoFromFiles.readReports(tempDir.toString());

            assertTrue(result.parsedFileNames().isEmpty());
            assertEquals(1, result.errorReports().size());
            assertTrue(result.errorReports().get(0).errorMessage().contains("ICO not found"));
        }
    }

    @Nested
    @DisplayName("Subdirectory handling tests")
    class SubdirectoryTests {

        @Test
        @DisplayName("should traverse subdirectories")
        void readReports_withSubdirectories_traversesAll(@TempDir Path tempDir) throws IOException {
            Path subDir = tempDir.resolve("subdir");
            Files.createDirectory(subDir);

            Path file1 = tempDir.resolve("12345678_VZP_2025_01.pdf");
            Path file2 = subDir.resolve("87654321_VZP_2025_02.pdf");
            Files.createFile(file1);
            Files.createFile(file2);

            WalkerResult result = infoFromFiles.readReports(tempDir.toString());

            assertEquals(2, result.parsedFileNames().size());
            assertTrue(result.parsedFileNames().stream().anyMatch(p -> p.ico().equals("12345678")));
            assertTrue(result.parsedFileNames().stream().anyMatch(p -> p.ico().equals("87654321")));
        }

        @Test
        @DisplayName("should traverse deeply nested subdirectories")
        void readReports_deeplyNested_traversesAll(@TempDir Path tempDir) throws IOException {
            Path level1 = tempDir.resolve("level1");
            Path level2 = level1.resolve("level2");
            Path level3 = level2.resolve("level3");
            Files.createDirectories(level3);

            Path file = level3.resolve("12345678_VZP_2025_01.pdf");
            Files.createFile(file);

            WalkerResult result = infoFromFiles.readReports(tempDir.toString());

            assertEquals(1, result.parsedFileNames().size());
            assertEquals("12345678", result.parsedFileNames().get(0).ico());
        }
    }

    @Nested
    @DisplayName("Edge case tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("should handle file with ICO containing leading zeros")
        void readReports_icoWithLeadingZeros_preservesZeros(@TempDir Path tempDir) throws IOException {
            Path file = tempDir.resolve("00000001_VZP_2025_01.pdf");
            Files.createFile(file);

            WalkerResult result = infoFromFiles.readReports(tempDir.toString());

            assertEquals(1, result.parsedFileNames().size());
            assertEquals("00000001", result.parsedFileNames().get(0).ico());
        }

        @Test
        @DisplayName("should handle file with multiple possible ICO patterns")
        void readReports_multipleIcoPatterns_usesFirst(@TempDir Path tempDir) throws IOException {
            // Contains multiple 8-digit sequences but should pick the first valid one
            Path file = tempDir.resolve("12345678_99999999_2025_01.pdf");
            Files.createFile(file);

            WalkerResult result = infoFromFiles.readReports(tempDir.toString());

            assertEquals(1, result.parsedFileNames().size());
            assertEquals("12345678", result.parsedFileNames().get(0).ico());
        }

        @Test
        @DisplayName("should parse combined YYYY-MM format in filename")
        void readReports_combinedYearMonthFormat_parsesCorrectly(@TempDir Path tempDir) throws IOException {
            Path file = tempDir.resolve("Report-12345678-2025-11.pdf");
            Files.createFile(file);

            WalkerResult result = infoFromFiles.readReports(tempDir.toString());

            assertEquals(1, result.parsedFileNames().size());
            assertEquals("12345678", result.parsedFileNames().get(0).ico());
            assertEquals(LocalDate.of(2025, 11, 1), result.parsedFileNames().get(0).date());
        }

        @Test
        @DisplayName("should handle year at end of 20th century")
        void readReports_year1999_parsesCorrectly(@TempDir Path tempDir) throws IOException {
            Path file = tempDir.resolve("12345678_VZP_1999_12.pdf");
            Files.createFile(file);

            WalkerResult result = infoFromFiles.readReports(tempDir.toString());

            assertEquals(1, result.parsedFileNames().size());
            assertEquals(LocalDate.of(1999, 12, 1), result.parsedFileNames().get(0).date());
        }

        @Test
        @DisplayName("should handle future year")
        void readReports_futureYear_parsesCorrectly(@TempDir Path tempDir) throws IOException {
            Path file = tempDir.resolve("12345678_VZP_2030_01.pdf");
            Files.createFile(file);

            WalkerResult result = infoFromFiles.readReports(tempDir.toString());

            assertEquals(1, result.parsedFileNames().size());
            assertEquals(LocalDate.of(2030, 1, 1), result.parsedFileNames().get(0).date());
        }

        @Test
        @DisplayName("should sort results by file path")
        void readReports_multipleFiles_sortsByPath(@TempDir Path tempDir) throws IOException {
            Path fileC = tempDir.resolve("c_12345678_2025_03.pdf");
            Path fileA = tempDir.resolve("a_87654321_2025_01.pdf");
            Path fileB = tempDir.resolve("b_11111111_2025_02.pdf");
            Files.createFile(fileC);
            Files.createFile(fileA);
            Files.createFile(fileB);

            WalkerResult result = infoFromFiles.readReports(tempDir.toString());

            assertEquals(3, result.parsedFileNames().size());
            // Should be sorted alphabetically by file path
            assertEquals("87654321", result.parsedFileNames().get(0).ico()); // a_
            assertEquals("11111111", result.parsedFileNames().get(1).ico()); // b_
            assertEquals("12345678", result.parsedFileNames().get(2).ico()); // c_
        }
    }
}
