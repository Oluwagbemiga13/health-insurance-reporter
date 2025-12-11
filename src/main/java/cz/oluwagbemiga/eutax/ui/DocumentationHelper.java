package cz.oluwagbemiga.eutax.ui;

import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Helper class for opening HTML documentation files in the browser.
 * Extracts all documentation files to the same temp directory to preserve relative links.
 */
@Slf4j
public class DocumentationHelper {

    private static final String[] DOC_FILES = {"readme.html", "nastaveni-google-sheets.html"};
    public static final String ERROR = "Chyba";
    private static Path docsDirectory = null;

    private DocumentationHelper() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Opens the specified documentation file in the default browser.
     *
     * @param fileName the name of the HTML file to open (e.g., "readme.html")
     * @param parent   the parent component for error dialogs
     */
    public static void openDocumentation(String fileName, Component parent) {
        try {
            ensureDocsExtracted();

            if (docsDirectory == null) {
                JOptionPane.showMessageDialog(parent,
                        "Dokumentace nebyla nalezena.",
                        ERROR,
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            File docFile = docsDirectory.resolve(fileName).toFile();
            if (!docFile.exists()) {
                JOptionPane.showMessageDialog(parent,
                        "Dokumentace nebyla nalezena: " + fileName,
                        ERROR,
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(docFile.toURI());
            } else {
                JOptionPane.showMessageDialog(parent,
                        "Nepodařilo se otevřít prohlížeč.",
                        "Upozornění",
                        JOptionPane.WARNING_MESSAGE);
            }
        } catch (Exception ex) {
            log.error("Failed to open documentation: {}", fileName, ex);
            JOptionPane.showMessageDialog(parent,
                    "Nepodařilo se otevřít dokumentaci: " + ex.getMessage(),
                    ERROR,
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Ensures all documentation files are extracted to the temp directory.
     */
    private static synchronized void ensureDocsExtracted() {
        if (docsDirectory != null && Files.exists(docsDirectory)) {
            return;
        }

        try {
            // Create temp directory for docs
            docsDirectory = Files.createTempDirectory("health-insurance-reporter-docs");
            docsDirectory.toFile().deleteOnExit();

            // Extract all documentation files
            for (String docFile : DOC_FILES) {
                try (InputStream is = DocumentationHelper.class.getResourceAsStream("/" + docFile)) {
                    if (is != null) {
                        Path targetFile = docsDirectory.resolve(docFile);
                        Files.copy(is, targetFile, StandardCopyOption.REPLACE_EXISTING);
                        targetFile.toFile().deleteOnExit();
                    } else {
                        log.warn("Documentation file not found in resources: {}", docFile);
                    }
                }
            }
        } catch (Exception ex) {
            log.error("Failed to extract documentation files", ex);
            docsDirectory = null;
        }
    }
}

