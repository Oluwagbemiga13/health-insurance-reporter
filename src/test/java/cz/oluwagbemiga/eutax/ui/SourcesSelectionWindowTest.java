package cz.oluwagbemiga.eutax.ui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SourcesSelectionWindowTest {

    private SourcesSelectionWindow window;

    @BeforeAll
    static void configureHeadless() {
        System.setProperty("java.awt.headless", "false");
    }

    @AfterEach
    void tearDown() {
        SwingTestUtils.dispose(window);
        window = null;
    }

    @Test
    void continueButtonEnabledWhenInputsValid() throws Exception {
        Path tempDir = Files.createTempDirectory("reports");
        Path spreadsheet = Files.createTempFile("clients", ".xlsx");

        window = SwingTestUtils.createOnEdt(SourcesSelectionWindow::new);

        JTextField folderField = SwingTestUtils.getField(window, "folderField", JTextField.class);
        JTextField spreadsheetField = SwingTestUtils.getField(window, "spreadsheetField", JTextField.class);
        JLabel errorLabel = SwingTestUtils.getField(window, "errorLabel", JLabel.class);
        JButton continueButton = SwingTestUtils.getField(window, "continueButton", JButton.class);

        assertNotNull(folderField);
        assertNotNull(spreadsheetField);
        assertNotNull(errorLabel);
        assertNotNull(continueButton);
        assertFalse(continueButton.isEnabled(), "Continue should be disabled until inputs valid");

        SwingTestUtils.runOnEdt(() -> {
            folderField.setText(tempDir.toString());
            spreadsheetField.setText(spreadsheet.toString());
        });

        SwingTestUtils.runOnEdt(() -> {
            try {
                var validateInputs = SourcesSelectionWindow.class.getDeclaredMethod("validateInputs");
                validateInputs.setAccessible(true);
                validateInputs.invoke(window);
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        });

        assertTrue(continueButton.isEnabled(), "Continue should enable with valid inputs");
        assertEquals(" ", errorLabel.getText(), "Error label should be cleared");
    }
}
