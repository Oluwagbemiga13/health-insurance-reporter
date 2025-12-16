package cz.oluwagbemiga.eutax.tools;

import cz.oluwagbemiga.eutax.pojo.InsuranceCompany;

import java.io.IOException;
import java.nio.file.*;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Helper to locate or create insurer folders under a reports root and move files there.
 */
public class FileMover {

    private final Path root;

    public FileMover(Path root) {
        this.root = root;
    }

    /**
     * Normalize a folder/display name for comparison (same normalization logic used in InfoFromFiles).
     */
    private static String normalizeForComparison(String s) {
        if (s == null) return "";
        String trimmed = s.trim();
        if (trimmed.isEmpty()) return "";
        String noDiacritics = Normalizer.normalize(trimmed, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String replaced = noDiacritics.replaceAll("[_-]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
        replaced = replaced.replaceAll("^[^A-Za-z0-9]+", "");
        return replaced.toUpperCase(Locale.ROOT);
    }

    /**
     * Token-based containment match (display tokens appear as a contiguous token sequence inside parent).
     */
    private static boolean parentMatches(String normalizedParent, String normalizedDisplay) {
        if (normalizedParent.equals(normalizedDisplay)) return true;
        String[] parentTokens = normalizedParent.split(" ");
        String[] displayTokens = normalizedDisplay.split(" ");
        if (displayTokens.length == 0) return false;
        for (int i = 0; i <= parentTokens.length - displayTokens.length; i++) {
            boolean all = true;
            for (int j = 0; j < displayTokens.length; j++) {
                if (!parentTokens[i + j].equals(displayTokens[j])) {
                    all = false;
                    break;
                }
            }
            if (all) return true;
        }
        return false;
    }

    /**
     * Search the reports tree under root for a directory matching any displayName of the insurer.
     * Returns first match found.
     */
    public Optional<Path> findTargetFolder(InsuranceCompany insurer) throws IOException {
        if (!Files.isDirectory(root)) return Optional.empty();
        String[] normalizedDisplays = insurer.getDisplayNames().stream()
                .map(FileMover::normalizeForComparison)
                .toArray(String[]::new);

        try (Stream<Path> dirs = Files.walk(root)) {
            return dirs.filter(Files::isDirectory)
                    .filter(p -> p.getFileName() != null)
                    .filter(p -> {
                        String name = p.getFileName().toString();
                        String norm = normalizeForComparison(name);
                        for (String nd : normalizedDisplays) {
                            if (parentMatches(norm, nd)) return true;
                        }
                        return false;
                    })
                    .findFirst();
        }
    }

    /**
     * Ensure a target folder exists. If no matching folder found, create one at root using primary displayName.
     */
    public Path ensureOrCreateFolder(InsuranceCompany insurer) throws IOException {
        Optional<Path> found = findTargetFolder(insurer);
        if (found.isPresent()) return found.get();
        String createName = insurer.getDisplayNames().get(0);
        Path created = root.resolve(createName);
        return Files.createDirectories(created);
    }

    /**
     * Move a file to targetDir (creates targetDir if needed). Returns destination path.
     */
    public Path moveFileToFolder(Path file, Path targetDir) throws IOException {
        Files.createDirectories(targetDir);
        Path dest = targetDir.resolve(file.getFileName());
        return Files.move(file, dest, StandardCopyOption.REPLACE_EXISTING);
    }
}

