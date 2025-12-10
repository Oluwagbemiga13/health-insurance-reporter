package cz.oluwagbemiga.eutax.tools;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for resolving Google Sheets spreadsheet IDs from various input formats.
 * <p>
 * This resolver supports multiple input formats:
 * <ul>
 *     <li><b>.gsheet files</b>: Google Drive shortcut files containing JSON with a doc_id field</li>
 *     <li><b>.url files</b>: Windows shortcut files containing a Google Sheets URL</li>
 *     <li><b>Direct URLs</b>: Full Google Sheets URLs from which the ID is extracted</li>
 *     <li><b>Direct IDs</b>: Raw spreadsheet IDs passed through unchanged</li>
 * </ul>
 * </p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * String spreadsheetId = GoogleSheetIdResolver.resolve("path/to/sheet.gsheet");
 * String spreadsheetId = GoogleSheetIdResolver.resolve("https://docs.google.com/spreadsheets/d/1abc123/edit");
 * }</pre>
 *
 * @see GoogleWorker
 */
@Slf4j
public final class GoogleSheetIdResolver {

    /** Pattern to extract spreadsheet ID from a Google Sheets URL */
    private static final Pattern URL_PATTERN = Pattern.compile("/d/([a-zA-Z0-9-_]+)");

    /** Pattern to extract doc_id from .gsheet file JSON content */
    private static final Pattern DOC_ID_PATTERN = Pattern.compile("\\\"doc_id\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");

    /** Pattern to extract URL from Windows .url shortcut files */
    private static final Pattern URL_LINE_PATTERN = Pattern.compile("(?im)^URL\\s*=\\s*(.+)$");

    /**
     * Private constructor to prevent instantiation of this utility class.
     *
     * @throws IllegalStateException always thrown to prevent instantiation
     */
    private GoogleSheetIdResolver() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Resolves a Google Sheets spreadsheet ID from various input formats.
     * <p>
     * The method automatically detects the input type based on file extension or content
     * and extracts the spreadsheet ID accordingly.
     * </p>
     *
     * @param rawInput the input string which can be a file path (.gsheet, .url),
     *                 a Google Sheets URL, or a direct spreadsheet ID
     * @return the resolved spreadsheet ID, or an empty string if resolution fails
     */
    public static String resolve(String rawInput) {
        log.debug("Resolving spreadsheet ID from input: {}", rawInput);
        if (rawInput == null) {
            return "";
        }
        String trimmed = rawInput.trim();
        if (trimmed.isEmpty()) {
            return "";
        }

        String lower = trimmed.toLowerCase();
        if (lower.endsWith(".gsheet")) {
            log.debug("Input identified as .gsheet file");
            return readFromGSheetFile(Path.of(trimmed));
        }
        if (lower.endsWith(".url")) {
            log.debug("Input identified as .url shortcut file");
            return readFromUrlShortcut(Path.of(trimmed));
        }

        String fromUrl = extractIdFromUrl(trimmed);
        if (!fromUrl.isEmpty()) {
            log.debug("Successfully extracted spreadsheet ID from URL: {}", fromUrl);
            return fromUrl;
        }

        log.debug("Input treated as direct spreadsheet ID: {}", trimmed);
        return trimmed;
    }

    /**
     * Reads and extracts the spreadsheet ID from a Google Drive .gsheet shortcut file.
     *
     * @param path the path to the .gsheet file
     * @return the extracted spreadsheet ID, or an empty string if extraction fails
     */
    private static String readFromGSheetFile(Path path) {
        try {
            if (!Files.isRegularFile(path)) {
                log.warn("Google Sheets shortcut {} does not exist", path);
                return "";
            }
            String content = Files.readString(path, StandardCharsets.UTF_8);
            Matcher matcher = DOC_ID_PATTERN.matcher(content);
            if (matcher.find()) {
                String docId = matcher.group(1);
                log.debug("Successfully extracted doc_id from .gsheet file: {}", docId);
                return docId;
            }
            log.warn("File {} does not contain doc_id field", path);
        } catch (IOException ex) {
            log.warn("Unable to read Google Sheet shortcut {}", path, ex);
        }
        return "";
    }

    /**
     * Reads and extracts the spreadsheet ID from a Windows .url shortcut file.
     *
     * @param path the path to the .url shortcut file
     * @return the extracted spreadsheet ID, or an empty string if extraction fails
     */
    private static String readFromUrlShortcut(Path path) {
        try {
            if (!Files.isRegularFile(path)) {
                log.warn("Shortcut {} does not exist", path);
                return "";
            }
            String content = Files.readString(path, StandardCharsets.UTF_8);
            Matcher matcher = URL_LINE_PATTERN.matcher(content);
            if (matcher.find()) {
                String extractedId = extractIdFromUrl(matcher.group(1).trim());
                if (!extractedId.isEmpty()) {
                    log.debug("Successfully extracted spreadsheet ID from .url shortcut: {}", extractedId);
                }
                return extractedId;
            }
            log.warn("Shortcut {} does not contain URL entry", path);
        } catch (IOException ex) {
            log.warn("Unable to read shortcut {}", path, ex);
        }
        return "";
    }

    /**
     * Extracts the spreadsheet ID from a Google Sheets URL.
     *
     * @param candidate the URL string to parse
     * @return the extracted spreadsheet ID, or an empty string if the URL is not a valid Google Sheets URL
     */
    private static String extractIdFromUrl(String candidate) {
        Matcher urlMatcher = URL_PATTERN.matcher(candidate);
        if (urlMatcher.find()) {
            return urlMatcher.group(1);
        }
        return "";
    }
}
