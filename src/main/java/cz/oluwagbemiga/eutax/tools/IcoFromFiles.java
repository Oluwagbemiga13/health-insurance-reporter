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
 * Utility class for reading client report files from a directory tree and extracting report metadata from file names.
 * <p>
 * This class parses file names to extract:
 * <ul>
 *     <li><b>ICO</b>: The client's 8-digit identification number</li>
 *     <li><b>Report date</b>: The year and month derived from the file name (set to the 1st of the month)</li>
 * </ul>
 * </p>
 * <p>
 * File names are expected to follow a pattern containing:
 * <ul>
 *     <li>An 8-digit ICO number (e.g., "12345678")</li>
 *     <li>A year-month segment in format "YYYY-MM" or as separate tokens "YYYY" and "MM"</li>
 * </ul>
 * Segments can be separated by hyphens, underscores, or spaces.
 * </p>
 *
 * <p>Example valid file names:</p>
 * <pre>
 * 10751416_VZP_2025_11.pdf       → ICO: 10751416, Date: 2025-11-01
 * PPPZ-02604477-2025-11.pdf      → ICO: 02604477, Date: 2025-11-01
 * </pre>
 *
 * @see ParsedFileName
 * @see WalkerResult
 */
@Slf4j
@RequiredArgsConstructor
public class IcoFromFiles {

    /** Pattern to match 8-digit ICO numbers (not preceded or followed by other digits) */
    private static final Pattern ICO_PATTERN = Pattern.compile("(?<!\\d)(\\d{8})(?!\\d)");

    /** Pattern to match month numbers (01-12) */
    private static final Pattern MONTH_PATTERN = Pattern.compile("^(0[1-9]|1[0-2])$");

    /** Pattern to match combined year-month format (YYYY-MM) */
    private static final Pattern YEAR_MONTH_PATTERN = Pattern.compile("(?<!\\d)(\\d{4})-(0[1-9]|1[0-2])(?!\\d)");

    /** Date formatter for parsing extracted date components */
    private static final DateTimeFormatter REPORT_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            .withResolverStyle(ResolverStyle.STRICT)
            .withLocale(Locale.ROOT);

    /**
     * Walks the supplied folder recursively and returns a {@link ParsedFileName} entry for every readable file.
     * <p>
     * Files that cannot be parsed (missing ICO, invalid date format, etc.) are collected as errors
     * in the returned {@link WalkerResult}.
     * </p>
     *
     * @param folderPath absolute or relative path to the folder that should be scanned
     * @return a {@link WalkerResult} containing successfully parsed files and any errors encountered;
     *         empty lists if the folder does not exist or an I/O error occurs
     */
    public WalkerResult readReports(String folderPath) {
        log.debug("Starting to read reports from folder: {}", folderPath);
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
        log.debug("Completed reading reports: {} successful, {} errors", parsedFileNames.size(), errors.size());
        return new WalkerResult(List.copyOf(parsedFileNames), List.copyOf(errors));
    }

    /**
     * Processes a single file and adds the parsed result to the appropriate list.
     *
     * @param file            the file path to process
     * @param parsedFileNames the list to add successfully parsed files to
     * @param errors          the list to add parsing errors to
     */
    private void processFile(Path file, List<ParsedFileName> parsedFileNames, List<ErrorReport> errors) {
        log.trace("Processing file: {}", file.getFileName());
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
     * Extracts the 8-digit ICO number from a source string.
     *
     * @param source           the string to search for an ICO
     * @param originalFileName the original file name (used for error messages)
     * @return the first 8-digit ICO found in the source
     * @throws ReportException if no ICO is present in the source string
     */
    private String extractIco(String source, String originalFileName) {
        Matcher matcher = ICO_PATTERN.matcher(source);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new ReportException("ICO not found in file name " + originalFileName);
    }

    /**
     * Parses a file name to extract ICO and report date information.
     * <p>
     * The file name is expected to contain:
     * <ul>
     *     <li>At least 3 segments when split by hyphens, underscores, or spaces</li>
     *     <li>An 8-digit ICO number</li>
     *     <li>Year and month information (either as separate tokens or combined YYYY-MM format)</li>
     * </ul>
     * </p>
     *
     * @param fileName the file name to parse (with or without extension)
     * @return a {@link ParsedFileName} containing the extracted ICO and date
     * @throws ReportException if the file name cannot be parsed (missing ICO, invalid date, etc.)
     */
    private ParsedFileName parse(String fileName) throws ReportException {
        log.trace("Parsing file name: {}", fileName);
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


    /**
     * Attempts to find year and month tokens in the parsed file name segments.
     * <p>
     * First tries to find adjacent tokens where the first is a 4-digit year and the second is a 2-digit month.
     * Falls back to searching for a combined YYYY-MM pattern in the normalized name.
     * </p>
     *
     * @param tokens         the tokenized segments from the file name
     * @param normalizedName the normalized file name string for fallback pattern matching
     * @return an {@link Optional} containing the year and month if found; empty otherwise
     */
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


    /**
     * Removes the file extension from a file name.
     *
     * @param fileName the file name to process
     * @return the file name without its extension; original name if no extension present
     */
    private String stripExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
    }

    /**
     * Parses year and month strings into a {@link LocalDate} set to the 1st of the month.
     *
     * @param year     the 4-digit year string
     * @param month    the 2-digit month string (01-12)
     * @param fileName the original file name (used for error messages)
     * @return a {@link LocalDate} representing the first day of the specified month
     * @throws ReportException if the date cannot be parsed
     */
    private LocalDate parseDate(String year, String month, String fileName) {
        try {
            return LocalDate.parse(year + "-" + month + "-01");
        } catch (DateTimeParseException ex) {
            throw new ReportException("Invalid date segment in filename " + fileName);
        }
    }

    /**
     * Internal record to hold parsed year and month tokens.
     *
     * @param year  the 4-digit year string
     * @param month the 2-digit month string (01-12)
     */
    private record YearMonthTokens(String year, String month) {
    }
}
