package cz.oluwagbemiga.eutax.ui;

import cz.oluwagbemiga.eutax.pojo.CzechMonth;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.prefs.Preferences;

public class SourcesSelectionWindow extends JFrame {

    private static final String[] SUPPORTED_EXTENSIONS = {".xlsx", ".xls", ".ods"};
    private static final String PREF_FOLDER_PATH = "folderPath";
    private static final String PREF_SPREADSHEET_PATH = "spreadsheetPath";
    private static final String PREF_SELECTED_MONTH = "selectedMonth";

    private final Preferences prefs = Preferences.userNodeForPackage(SourcesSelectionWindow.class);
    private final JTextField folderField = new JTextField();
    private final JTextField spreadsheetField = new JTextField();
    private final JComboBox<CzechMonth> monthComboBox = new JComboBox<>(CzechMonth.values());
    private final JLabel errorLabel = new JLabel(" ");
    private final JButton continueButton = new JButton("Pokračovat");

    public SourcesSelectionWindow() {
        setTitle("Výběr zdrojů");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        AppIconProvider.apply(this);
        loadSavedPreferences();
        buildUi();
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void loadSavedPreferences() {
        String savedFolder = prefs.get(PREF_FOLDER_PATH, "");
        String savedSpreadsheet = prefs.get(PREF_SPREADSHEET_PATH, "");
        int savedMonth = prefs.getInt(PREF_SELECTED_MONTH, java.time.LocalDate.now().getMonthValue() - 1);

        if (!savedFolder.isEmpty()) {
            folderField.setText(savedFolder);
        }
        if (!savedSpreadsheet.isEmpty()) {
            spreadsheetField.setText(savedSpreadsheet);
        }
        monthComboBox.setSelectedIndex(Math.max(0, Math.min(savedMonth, 11)));
    }

    private void savePreferences() {
        prefs.put(PREF_FOLDER_PATH, folderField.getText());
        prefs.put(PREF_SPREADSHEET_PATH, spreadsheetField.getText());
        prefs.putInt(PREF_SELECTED_MONTH, monthComboBox.getSelectedIndex());
    }

    private void buildUi() {
        JPanel content = new JPanel(new BorderLayout());
        content.setBorder(new EmptyBorder(24, 24, 24, 24));
        content.add(buildForm(), BorderLayout.CENTER);
        content.add(buildBottomPanel(), BorderLayout.SOUTH);
        setContentPane(content);
        setPreferredSize(new Dimension(600, 380));
    }

    private JPanel buildForm() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 8, 8);

        panel.add(new JLabel("Složka s reporty"), gbc);
        gbc.gridy++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        panel.add(createPickerPanel(folderField, this::pickFolder), gbc);

        gbc.gridy++;
        gbc.insets = new Insets(16, 0, 8, 8);
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        panel.add(new JLabel("Tabulka klientů"), gbc);

        gbc.gridy++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        panel.add(createPickerPanel(spreadsheetField, this::pickSpreadsheet), gbc);

        gbc.gridy++;
        gbc.insets = new Insets(16, 0, 8, 8);
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        panel.add(new JLabel("Měsíc"), gbc);

        gbc.gridy++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        monthComboBox.addActionListener(e -> savePreferences());
        panel.add(monthComboBox, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(16, 0, 0, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        panel.add(errorLabel, gbc);
        errorLabel.setForeground(new Color(176, 0, 32));

        // Validate after UI is built with saved values
        SwingUtilities.invokeLater(this::validateInputs);

        return panel;
    }

    private JPanel createPickerPanel(JTextField field, Runnable picker) {
        JPanel panel = new JPanel(new BorderLayout(8, 0));
        field.setEditable(false);
        panel.add(field, BorderLayout.CENTER);
        JButton browse = new JButton("Vybrat...");
        browse.addActionListener(e -> picker.run());
        panel.add(browse, BorderLayout.EAST);
        return panel;
    }

    private JPanel buildBottomPanel() {
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.setBackground(Color.WHITE);

        JButton cancel = new JButton("Zpět");
        cancel.addActionListener(e -> {
            dispose();
            SwingUtilities.invokeLater(StartWindow::new);
        });
        bottom.add(cancel);

        continueButton.setEnabled(false);
        continueButton.addActionListener(e -> {
            dispose();
            String folder = folderField.getText();
            String spreadsheet = spreadsheetField.getText();
            CzechMonth month = (CzechMonth) monthComboBox.getSelectedItem();
            SwingUtilities.invokeLater(() -> new ResultsWindow(folder, spreadsheet, month));
        });
        bottom.add(continueButton);
        return bottom;
    }

    private void pickFolder() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Vyberte složku s reporty");
        // Start from previously selected folder if it exists
        String currentFolder = folderField.getText();
        if (!currentFolder.isEmpty() && Files.isDirectory(Path.of(currentFolder))) {
            chooser.setCurrentDirectory(Path.of(currentFolder).toFile());
        }
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            Path selected = chooser.getSelectedFile().toPath();
            folderField.setText(selected.toString());
            savePreferences();
            validateInputs();
        }
    }

    private void pickSpreadsheet() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setDialogTitle("Vyberte tabulku klientů");
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.addChoosableFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Excel nebo LibreOffice", "xlsx", "xls", "ods"));
        // Start from previously selected file's directory if it exists
        String currentSpreadsheet = spreadsheetField.getText();
        if (!currentSpreadsheet.isEmpty()) {
            Path currentPath = Path.of(currentSpreadsheet);
            if (Files.isRegularFile(currentPath)) {
                chooser.setSelectedFile(currentPath.toFile());
            } else if (currentPath.getParent() != null && Files.isDirectory(currentPath.getParent())) {
                chooser.setCurrentDirectory(currentPath.getParent().toFile());
            }
        }
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            Path selected = chooser.getSelectedFile().toPath();
            spreadsheetField.setText(selected.toString());
            savePreferences();
            validateInputs();
        }
    }

    private void validateInputs() {
        String folderPath = folderField.getText();
        String filePath = spreadsheetField.getText();

        if (folderPath.isBlank()) {
            setError("Vyberte složku s reporty.");
            continueButton.setEnabled(false);
            return;
        }
        Path folder = Path.of(folderPath);
        if (!Files.isDirectory(folder)) {
            setError("Vybraná složka s reporty není platná.");
            continueButton.setEnabled(false);
            return;
        }

        if (filePath.isBlank()) {
            setError("Vyberte tabulku klientů.");
            continueButton.setEnabled(false);
            return;
        }
        Path spreadsheet = Path.of(filePath);
        if (!Files.isRegularFile(spreadsheet) || !hasSupportedExtension(spreadsheet)) {
            setError("Soubor musí být Excel nebo LibreOffice tabulka.");
            continueButton.setEnabled(false);
            return;
        }

        clearError();
        continueButton.setEnabled(true);
    }

    private boolean hasSupportedExtension(Path path) {
        String lower = path.getFileName().toString().toLowerCase();
        for (String ext : SUPPORTED_EXTENSIONS) {
            if (lower.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    private void setError(String message) {
        errorLabel.setText(message);
    }

    private void clearError() {
        errorLabel.setText(" ");
    }
}
