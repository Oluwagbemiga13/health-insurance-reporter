package cz.oluwagbemiga.eutax.tools;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public final class GoogleSheetIdResolver {

    private static final Pattern URL_PATTERN = Pattern.compile("/d/([a-zA-Z0-9-_]+)");
    private static final Pattern DOC_ID_PATTERN = Pattern.compile("\\\"doc_id\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
    private static final Pattern URL_LINE_PATTERN = Pattern.compile("(?im)^URL\\s*=\\s*(.+)$");

    private GoogleSheetIdResolver() {
        throw new IllegalStateException("Utility class");
    }

    public static String resolve(String rawInput) {
        if (rawInput == null) {
            return "";
        }
        String trimmed = rawInput.trim();
        if (trimmed.isEmpty()) {
            return "";
        }

        String lower = trimmed.toLowerCase();
        if (lower.endsWith(".gsheet")) {
            return readFromGSheetFile(Path.of(trimmed));
        }
        if (lower.endsWith(".url")) {
            return readFromUrlShortcut(Path.of(trimmed));
        }

        String fromUrl = extractIdFromUrl(trimmed);
        if (!fromUrl.isEmpty()) {
            return fromUrl;
        }

        return trimmed;
    }

    private static String readFromGSheetFile(Path path) {
        try {
            if (!Files.isRegularFile(path)) {
                log.warn("Google Sheets shortcut {} does not exist", path);
                return "";
            }
            String content = Files.readString(path, StandardCharsets.UTF_8);
            Matcher matcher = DOC_ID_PATTERN.matcher(content);
            if (matcher.find()) {
                return matcher.group(1);
            }
            log.warn("File {} does not contain doc_id field", path);
        } catch (IOException ex) {
            log.warn("Unable to read Google Sheet shortcut {}", path, ex);
        }
        return "";
    }

    private static String readFromUrlShortcut(Path path) {
        try {
            if (!Files.isRegularFile(path)) {
                log.warn("Shortcut {} does not exist", path);
                return "";
            }
            String content = Files.readString(path, StandardCharsets.UTF_8);
            Matcher matcher = URL_LINE_PATTERN.matcher(content);
            if (matcher.find()) {
                return extractIdFromUrl(matcher.group(1).trim());
            }
            log.warn("Shortcut {} does not contain URL entry", path);
        } catch (IOException ex) {
            log.warn("Unable to read shortcut {}", path, ex);
        }
        return "";
    }

    private static String extractIdFromUrl(String candidate) {
        Matcher urlMatcher = URL_PATTERN.matcher(candidate);
        if (urlMatcher.find()) {
            return urlMatcher.group(1);
        }
        return "";
    }
}
