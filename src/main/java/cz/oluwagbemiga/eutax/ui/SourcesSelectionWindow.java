package cz.oluwagbemiga.eutax.ui;

import cz.oluwagbemiga.eutax.pojo.CzechMonth;
import cz.oluwagbemiga.eutax.pojo.SpreadsheetSource;
import cz.oluwagbemiga.eutax.pojo.SpreadsheetSource.SpreadsheetSourceType;
import cz.oluwagbemiga.eutax.tools.GoogleSheetIdResolver;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.prefs.Preferences;

public class SourcesSelectionWindow extends JFrame {

    private static final String[] SUPPORTED_EXCEL_EXTENSIONS = {".xlsx"};
    private static final String PREF_FOLDER_PATH = "folderPath";
    private static final String PREF_SPREADSHEET_PATH = "spreadsheetPath";
    private static final String PREF_SELECTED_MONTH = "selectedMonth";

    private final Preferences prefs = Preferences.userNodeForPackage(SourcesSelectionWindow.class);
    private final JTextField folderField = new JTextField();
    private final JTextField spreadsheetField = new JTextField();
    private final JComboBox<CzechMonth> monthComboBox = new JComboBox<>(CzechMonth.values());
    private final JLabel errorLabel = new JLabel(" ");
    private final JButton continueButton = new JButton("Pokračovat");
    private SpreadsheetSource spreadsheetSource;

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
        folderField.setText(prefs.get(PREF_FOLDER_PATH, ""));
        spreadsheetField.setText(prefs.get(PREF_SPREADSHEET_PATH, ""));
        int savedMonth = prefs.getInt(PREF_SELECTED_MONTH, java.time.LocalDate.now().getMonthValue() - 1);
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
        setPreferredSize(new Dimension(650, 430));
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
        panel.add(new JLabel("Zdroj tabulky"), gbc);

        gbc.gridy++;
        panel.add(buildSourceSelector(), gbc);

        gbc.gridy++;
        gbc.insets = new Insets(16, 0, 8, 8);
        panel.add(new JLabel("Měsíc"), gbc);

        gbc.gridy++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        monthComboBox.addActionListener(e -> {
            savePreferences();
            validateInputs();
        });
        panel.add(monthComboBox, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(16, 0, 0, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        panel.add(errorLabel, gbc);
        errorLabel.setForeground(new Color(176, 0, 32));

        SwingUtilities.invokeLater(this::validateInputs);
        return panel;
    }

    private JPanel buildSourceSelector() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(8, 16, 8, 16));
        panel.add(new JLabel("Soubor se seznamem klientů"), BorderLayout.NORTH);
        panel.add(createPickerPanel(spreadsheetField, this::pickSpreadsheet), BorderLayout.CENTER);
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
            SpreadsheetSource source = buildSource();
            CzechMonth month = (CzechMonth) monthComboBox.getSelectedItem();
            SwingUtilities.invokeLater(() -> new ResultsWindow(folder, source, month));
        });
        bottom.add(continueButton);
        return bottom;
    }

    private void pickFolder() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Vyberte složku s reporty");
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
        chooser.setDialogTitle("Vyberte Excel nebo Google Sheets soubor");
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.addChoosableFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Excel (.xlsx) nebo Google Sheets (.gsheet, .url)", "xlsx", "gsheet", "url"));
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

        String spreadsheetPath = spreadsheetField.getText();
        if (spreadsheetPath.isBlank()) {
            setError("Vyberte soubor se seznamem klientů.");
            continueButton.setEnabled(false);
            return;
        }

        boolean googleSelection = isGoogleSelection(spreadsheetPath);
        Path spreadsheet = Path.of(spreadsheetPath);
        if (!Files.isRegularFile(spreadsheet)) {
            setError("Vybraný soubor neexistuje.");
            continueButton.setEnabled(false);
            return;
        }

        if (googleSelection) {
            String resolved = GoogleSheetIdResolver.resolve(spreadsheetPath);
            if (resolved.isBlank()) {
                setError("Vyberte platný Google Sheets zástupce (.gsheet nebo .url).");
                continueButton.setEnabled(false);
                return;
            }
            spreadsheetSource = new SpreadsheetSource(SpreadsheetSourceType.GOOGLE, resolved);
        } else {
            if (!hasSupportedExcelExtension(spreadsheet)) {
                setError("Podporovaný je pouze Excel (.xlsx).");
                continueButton.setEnabled(false);
                return;
            }
            spreadsheetSource = new SpreadsheetSource(SpreadsheetSourceType.EXCEL, spreadsheetPath);
        }

        clearError();
        continueButton.setEnabled(true);
    }

    private SpreadsheetSource buildSource() {
        if (spreadsheetSource != null) {
            return spreadsheetSource;
        }
        String candidate = spreadsheetField.getText();
        if (isGoogleSelection(candidate)) {
            return new SpreadsheetSource(SpreadsheetSourceType.GOOGLE, GoogleSheetIdResolver.resolve(candidate));
        }
        return new SpreadsheetSource(SpreadsheetSourceType.EXCEL, candidate);
    }

    private boolean hasSupportedExcelExtension(Path path) {
        String lower = path.getFileName().toString().toLowerCase();
        for (String ext : SUPPORTED_EXCEL_EXTENSIONS) {
            if (lower.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    private boolean isGoogleSelection(String selection) {
        String lower = selection.toLowerCase();
        return lower.endsWith(".gsheet") || lower.endsWith(".url");
    }

    private void setError(String message) {
        errorLabel.setText(message);
    }

    private void clearError() {
        errorLabel.setText(" ");
    }
}
