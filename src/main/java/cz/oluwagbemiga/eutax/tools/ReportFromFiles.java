package cz.oluwagbemiga.eutax.tools;

import cz.oluwagbemiga.eutax.exceptions.ReportException;
import cz.oluwagbemiga.eutax.pojo.ErrorReport;
import cz.oluwagbemiga.eutax.pojo.Report;
import cz.oluwagbemiga.eutax.pojo.WalkerResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Reads client report files from a directory tree and extracts the customer name and ICO from each file name.
 */
@Slf4j
@RequiredArgsConstructor
public class ReportFromFiles {

    private static final Pattern ICO_PATTERN = Pattern.compile("(?<!\\d)(\\d{8})(?!\\d)");

    /**
     * Walks the supplied folder recursively and returns a {@link Report} entry for every readable file.
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

        List<Report> reports = new java.util.ArrayList<>();
        List<ErrorReport> errors = new java.util.ArrayList<>();
        try (Stream<Path> files = Files.walk(root)) {
            files
                    .filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(Path::toString))
                    .forEach(file -> processFile(file, reports, errors));
        } catch (IOException e) {
            log.error("Unable to read files from {}", folderPath, e);
            errors.add(new ErrorReport(folderPath, "Unable to read files: " + e.getMessage()));
        }
        return new WalkerResult(List.copyOf(reports), List.copyOf(errors));
    }

    private void processFile(Path file, List<Report> reports, List<ErrorReport> errors) {
        try {
            reports.add(toReport(file));
        } catch (ReportException ex) {
            log.warn("{}", ex.getMessage());
            errors.add(new ErrorReport(file.getFileName().toString(), ex.getMessage()));
        } catch (Exception ex) {
            log.error("Unexpected error while processing {}", file, ex);
            errors.add(new ErrorReport(file.getFileName().toString(), "Unexpected error: " + ex.getMessage()));
        }
    }

    private Report toReport(Path file) {
        String fileName = file.getFileName().toString();
        String baseName = stripExtension(fileName);
        String ico = extractIco(baseName, fileName);
        String name = extractName(baseName, ico);
        return new Report(fileName, name, ico);
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

    /**
     * Derives the client name by stripping the ICO (and extension) from the file name and trimming separators.
     */
    private String extractName(String source, String ico) {
        String withoutIco = source.replace(ico, "");
        String normalized = withoutIco.replace('_', ' ').replace('-', ' ');
        return normalized.trim().replaceAll("\\s+", " ");
    }

    private String stripExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
    }
}
