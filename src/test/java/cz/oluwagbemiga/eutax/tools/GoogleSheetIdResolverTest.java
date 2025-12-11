package cz.oluwagbemiga.eutax.tools;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class GoogleSheetIdResolverTest {

    private static final String TEST_SPREADSHEET_ID = "1aj2fuUzjk5L6wWxXkrfs73XluM3TxttbPW4CthEEu20";

    @Nested
    @DisplayName("resolve() method tests")
    class ResolveTests {

        @Test
        @DisplayName("should return empty string for null input")
        void resolve_nullInput_returnsEmptyString() {
            String result = GoogleSheetIdResolver.resolve(null);
            assertEquals("", result);
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   ", "\t", "\n"})
        @DisplayName("should return empty string for blank input")
        void resolve_blankInput_returnsEmptyString(String input) {
            String result = GoogleSheetIdResolver.resolve(input);
            assertEquals("", result);
        }

        @Test
        @DisplayName("should return direct spreadsheet ID unchanged")
        void resolve_directId_returnsSameId() {
            String result = GoogleSheetIdResolver.resolve(TEST_SPREADSHEET_ID);
            assertEquals(TEST_SPREADSHEET_ID, result);
        }

        @Test
        @DisplayName("should trim whitespace from direct ID")
        void resolve_directIdWithWhitespace_returnsTrimmedId() {
            String result = GoogleSheetIdResolver.resolve("  " + TEST_SPREADSHEET_ID + "  ");
            assertEquals(TEST_SPREADSHEET_ID, result);
        }
    }

    @Nested
    @DisplayName("URL extraction tests")
    class UrlExtractionTests {

        @Test
        @DisplayName("should extract ID from standard Google Sheets URL")
        void resolve_standardUrl_extractsId() {
            String url = "https://docs.google.com/spreadsheets/d/" + TEST_SPREADSHEET_ID + "/edit";
            String result = GoogleSheetIdResolver.resolve(url);
            assertEquals(TEST_SPREADSHEET_ID, result);
        }

        @Test
        @DisplayName("should extract ID from URL with query parameters")
        void resolve_urlWithQueryParams_extractsId() {
            String url = "https://docs.google.com/spreadsheets/d/" + TEST_SPREADSHEET_ID + "/edit?gid=123#gid=123";
            String result = GoogleSheetIdResolver.resolve(url);
            assertEquals(TEST_SPREADSHEET_ID, result);
        }

        @Test
        @DisplayName("should extract ID from URL with gid fragment")
        void resolve_urlWithFragment_extractsId() {
            String url = "https://docs.google.com/spreadsheets/d/" + TEST_SPREADSHEET_ID + "/edit#gid=1859744796";
            String result = GoogleSheetIdResolver.resolve(url);
            assertEquals(TEST_SPREADSHEET_ID, result);
        }

        @Test
        @DisplayName("should handle URL with different subdomain")
        void resolve_urlWithDifferentPath_extractsId() {
            String url = "https://docs.google.com/spreadsheets/d/" + TEST_SPREADSHEET_ID;
            String result = GoogleSheetIdResolver.resolve(url);
            assertEquals(TEST_SPREADSHEET_ID, result);
        }

        @Test
        @DisplayName("should handle ID with hyphens and underscores")
        void resolve_urlWithComplexId_extractsId() {
            String complexId = "abc-123_DEF-456";
            String url = "https://docs.google.com/spreadsheets/d/" + complexId + "/edit";
            String result = GoogleSheetIdResolver.resolve(url);
            assertEquals(complexId, result);
        }
    }

    @Nested
    @DisplayName(".gsheet file tests")
    class GSheetFileTests {

        @Test
        @DisplayName("should extract ID from real .gsheet file")
        void resolve_realGsheetFile_extractsId() {
            String result = GoogleSheetIdResolver.resolve("src/test/resources/pojistovny_google.gsheet");
            assertEquals(TEST_SPREADSHEET_ID, result);
        }

        @Test
        @DisplayName("should extract ID from .gsheet file content")
        void resolve_gsheetFile_extractsDocId(@TempDir Path tempDir) throws IOException {
            Path gsheetFile = tempDir.resolve("test.gsheet");
            String content = "{\"doc_id\":\"" + TEST_SPREADSHEET_ID + "\",\"email\":\"test@test.com\"}";
            Files.writeString(gsheetFile, content, StandardCharsets.UTF_8);

            String result = GoogleSheetIdResolver.resolve(gsheetFile.toString());
            assertEquals(TEST_SPREADSHEET_ID, result);
        }

        @Test
        @DisplayName("should return empty string for non-existent .gsheet file")
        void resolve_nonExistentGsheetFile_returnsEmptyString() {
            String result = GoogleSheetIdResolver.resolve("nonexistent.gsheet");
            assertEquals("", result);
        }

        @Test
        @DisplayName("should return empty string for .gsheet file without doc_id")
        void resolve_gsheetWithoutDocId_returnsEmptyString(@TempDir Path tempDir) throws IOException {
            Path gsheetFile = tempDir.resolve("invalid.gsheet");
            String content = "{\"email\":\"test@test.com\"}";
            Files.writeString(gsheetFile, content, StandardCharsets.UTF_8);

            String result = GoogleSheetIdResolver.resolve(gsheetFile.toString());
            assertEquals("", result);
        }

        @Test
        @DisplayName("should return empty string for .gsheet directory")
        void resolve_gsheetDirectory_returnsEmptyString(@TempDir Path tempDir) throws IOException {
            Path gsheetDir = tempDir.resolve("test.gsheet");
            Files.createDirectory(gsheetDir);

            String result = GoogleSheetIdResolver.resolve(gsheetDir.toString());
            assertEquals("", result);
        }
    }

    @Nested
    @DisplayName(".url shortcut file tests")
    class UrlShortcutFileTests {

        @Test
        @DisplayName("should extract ID from real .url file")
        void resolve_realUrlFile_extractsId() {
            String result = GoogleSheetIdResolver.resolve("src/test/resources/sheet_shortcut.url");
            assertEquals(TEST_SPREADSHEET_ID, result);
        }

        @Test
        @DisplayName("should extract ID from .url shortcut file")
        void resolve_urlShortcutFile_extractsId(@TempDir Path tempDir) throws IOException {
            Path urlFile = tempDir.resolve("test.url");
            String content = "[InternetShortcut]\nURL=https://docs.google.com/spreadsheets/d/" + TEST_SPREADSHEET_ID + "/edit";
            Files.writeString(urlFile, content, StandardCharsets.UTF_8);

            String result = GoogleSheetIdResolver.resolve(urlFile.toString());
            assertEquals(TEST_SPREADSHEET_ID, result);
        }

        @Test
        @DisplayName("should handle .url file with Windows headers")
        void resolve_urlFileWithHeaders_extractsId(@TempDir Path tempDir) throws IOException {
            Path urlFile = tempDir.resolve("test.url");
            String content = "[{000214A0-0000-0000-C000-000000000046}]\n" +
                    "Prop3=19,11\n" +
                    "[InternetShortcut]\n" +
                    "IDList=\n" +
                    "URL=https://docs.google.com/spreadsheets/d/" + TEST_SPREADSHEET_ID + "/edit";
            Files.writeString(urlFile, content, StandardCharsets.UTF_8);

            String result = GoogleSheetIdResolver.resolve(urlFile.toString());
            assertEquals(TEST_SPREADSHEET_ID, result);
        }

        @Test
        @DisplayName("should return empty string for non-existent .url file")
        void resolve_nonExistentUrlFile_returnsEmptyString() {
            String result = GoogleSheetIdResolver.resolve("nonexistent.url");
            assertEquals("", result);
        }

        @Test
        @DisplayName("should return empty string for .url file without URL entry")
        void resolve_urlFileWithoutUrl_returnsEmptyString(@TempDir Path tempDir) throws IOException {
            Path urlFile = tempDir.resolve("invalid.url");
            String content = "[InternetShortcut]\nSomething=else";
            Files.writeString(urlFile, content, StandardCharsets.UTF_8);

            String result = GoogleSheetIdResolver.resolve(urlFile.toString());
            assertEquals("", result);
        }

        @Test
        @DisplayName("should return empty string for .url file with non-Google URL")
        void resolve_urlFileWithNonGoogleUrl_returnsEmptyString(@TempDir Path tempDir) throws IOException {
            Path urlFile = tempDir.resolve("other.url");
            String content = "[InternetShortcut]\nURL=https://example.com/page";
            Files.writeString(urlFile, content, StandardCharsets.UTF_8);

            String result = GoogleSheetIdResolver.resolve(urlFile.toString());
            assertEquals("", result);
        }

        @Test
        @DisplayName("should return empty string for .url directory")
        void resolve_urlDirectory_returnsEmptyString(@TempDir Path tempDir) throws IOException {
            Path urlDir = tempDir.resolve("test.url");
            Files.createDirectory(urlDir);

            String result = GoogleSheetIdResolver.resolve(urlDir.toString());
            assertEquals("", result);
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("should handle file path that looks like URL but isn't")
        void resolve_filePathLikeUrl_returnsAsIs() {
            String result = GoogleSheetIdResolver.resolve("some-random-string");
            assertEquals("some-random-string", result);
        }

        @Test
        @DisplayName("should handle case-insensitive .gsheet extension")
        void resolve_upperCaseGsheetExtension_extractsId(@TempDir Path tempDir) throws IOException {
            Path gsheetFile = tempDir.resolve("test.GSHEET");
            String content = "{\"doc_id\":\"" + TEST_SPREADSHEET_ID + "\"}";
            Files.writeString(gsheetFile, content, StandardCharsets.UTF_8);

            String result = GoogleSheetIdResolver.resolve(gsheetFile.toString());
            assertEquals(TEST_SPREADSHEET_ID, result);
        }

        @Test
        @DisplayName("should handle case-insensitive .url extension")
        void resolve_upperCaseUrlExtension_extractsId(@TempDir Path tempDir) throws IOException {
            Path urlFile = tempDir.resolve("test.URL");
            String content = "[InternetShortcut]\nURL=https://docs.google.com/spreadsheets/d/" + TEST_SPREADSHEET_ID + "/edit";
            Files.writeString(urlFile, content, StandardCharsets.UTF_8);

            String result = GoogleSheetIdResolver.resolve(urlFile.toString());
            assertEquals(TEST_SPREADSHEET_ID, result);
        }
    }
}

