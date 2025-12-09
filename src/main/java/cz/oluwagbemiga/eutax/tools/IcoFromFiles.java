package cz.oluwagbemiga.eutax.tools;

import cz.oluwagbemiga.eutax.exceptions.ReportException;
import cz.oluwagbemiga.eutax.pojo.ErrorReport;
import cz.oluwagbemiga.eutax.pojo.ParsedFileName;
import cz.oluwagbemiga.eutax.pojo.WalkerResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Reads client report files from a directory tree and extracts report metadata from the file name.
 */
@Slf4j
@RequiredArgsConstructor
public class IcoFromFiles {

    private static final Pattern ICO_PATTERN = Pattern.compile("(?<!\\d)(\\d{8})(?!\\d)");
    private static final Pattern MONTH_PATTERN = Pattern.compile("^(0[1-9]|1[0-2])$");
    private static final Pattern YEAR_MONTH_PATTERN = Pattern.compile("(?<!\\d)(\\d{4})-(0[1-9]|1[0-2])(?!\\d)");
    private static final DateTimeFormatter REPORT_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            .withResolverStyle(ResolverStyle.STRICT)
            .withLocale(Locale.ROOT);

    /**
     * Walks the supplied folder recursively and returns a {@link ParsedFileName} entry for every readable file.
     *
     * @param folderPath absolute or relative path to the folder that should be scanned
     * @return collected reports; empty list if the folder does not exist or an I/O error occurs
     */
    public WalkerResult readReports(String folderPath) {
        Path root = Paths.get(folderPath);
        if (!Files.isDirectory(root)) {
            log.warn("Provided path {} is not a directory", folderPath);
            return new WalkerResult(List.of(), List.of(new ErrorReport(folderPath, "Provided path is not a directory")));
        }

        List<ParsedFileName> parsedFileNames = new java.util.ArrayList<>();
        List<ErrorReport> errors = new java.util.ArrayList<>();
        try (Stream<Path> files = Files.walk(root)) {
            files
                    .filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(Path::toString))
                    .forEach(file -> processFile(file, parsedFileNames, errors));
        } catch (IOException e) {
            log.error("Unable to read files from {}", folderPath, e);
            errors.add(new ErrorReport(folderPath, "Unable to read files: " + e.getMessage()));
        }
        return new WalkerResult(List.copyOf(parsedFileNames), List.copyOf(errors));
    }

    private void processFile(Path file, List<ParsedFileName> parsedFileNames, List<ErrorReport> errors) {
        try {
            parsedFileNames.add(parse(file.getFileName().toString()));
        } catch (ReportException ex) {
            log.warn("{}", ex.getMessage());
            errors.add(new ErrorReport(file.getFileName().toString(), ex.getMessage()));
        } catch (Exception ex) {
            log.error("Unexpected error while processing {}", file, ex);
            errors.add(new ErrorReport(file.getFileName().toString(), "Unexpected error: " + ex.getMessage()));
        }
    }

    /**
     * @return the first 8-digit ICO found in the provided file name
     * @throws ReportException when no ICO is present in the name
     */
    private String extractIco(String source, String originalFileName) {
        Matcher matcher = ICO_PATTERN.matcher(source);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new ReportException("ICO not found in file name " + originalFileName);
    }

    private ParsedFileName parse(String fileName) throws ReportException {
        String baseName = stripExtension(fileName);
        String normalized = baseName.replaceAll("[\\s_]+", "-");
        List<String> tokens = Arrays.stream(normalized.split("-+"))
                .filter(token -> !token.isBlank())
                .toList();
        if (tokens.size() < 3) {
            throw new ReportException("Filename does not provide enough segments: " + fileName);
        }

        String ico = extractIco(baseName, fileName);

        YearMonthTokens yearMonth = findYearMonth(tokens, normalized)
                .orElseThrow(() -> new ReportException("Filename does not contain year-month information: " + fileName));

        LocalDate reportDate = parseDate(yearMonth.year(), yearMonth.month(), fileName);


        return new ParsedFileName(ico, reportDate);
    }


    private Optional<YearMonthTokens> findYearMonth(List<String> tokens, String normalizedName) {
        for (int i = 0; i < tokens.size() - 1; i++) {
            if (tokens.get(i).matches("\\d{4}") && MONTH_PATTERN.matcher(tokens.get(i + 1)).matches()) {
                return Optional.of(new YearMonthTokens(tokens.get(i), tokens.get(i + 1)));
            }
        }

        Matcher matcher = YEAR_MONTH_PATTERN.matcher(normalizedName);
        if (matcher.find()) {
            return Optional.of(new YearMonthTokens(matcher.group(1), matcher.group(2)));
        }
        return Optional.empty();
    }


    private String stripExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
    }

    private LocalDate parseDate(String year, String month, String fileName) {
        try {
            return LocalDate.parse(year + "-" + month + "-01");
        } catch (DateTimeParseException ex) {
            throw new ReportException("Invalid date segment in filename " + fileName);
        }
    }

    private record YearMonthTokens(String year, String month) {
    }
}
