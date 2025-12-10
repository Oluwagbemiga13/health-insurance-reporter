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
    private final JTextField folderField = UiTheme.createTextField();
    private final JTextField spreadsheetField = UiTheme.createTextField();
    private final JComboBox<CzechMonth> monthComboBox = new JComboBox<>(CzechMonth.values());
    private final JLabel errorLabel = UiTheme.createErrorLabel();
    private JButton continueButton;
    private SpreadsheetSource spreadsheetSource;

    public SourcesSelectionWindow() {
        UiTheme.apply();
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
        JPanel root = UiTheme.createBackgroundPanel();
        root.setLayout(new BorderLayout());
        root.setBorder(UiTheme.createContentBorder());

        // Header section
        JPanel headerPanel = new JPanel();
        headerPanel.setOpaque(false);
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBorder(new EmptyBorder(0, 0, UiTheme.SPACING_MD, 0));

        JLabel titleLabel = new JLabel("Výběr zdrojů");
        titleLabel.setFont(UiTheme.FONT_TITLE);
        titleLabel.setForeground(UiTheme.TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerPanel.add(titleLabel);

        headerPanel.add(Box.createVerticalStrut(UiTheme.SPACING_XS));

        JLabel subtitleLabel = UiTheme.createDescriptionLabel("Vyberte období, složku s reporty a tabulku se seznamem klientů.");
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerPanel.add(subtitleLabel);

        root.add(headerPanel, BorderLayout.NORTH);

        // Main form card
        JPanel formCard = UiTheme.createCardPanel();
        formCard.setLayout(new BoxLayout(formCard, BoxLayout.Y_AXIS));

        // Month section - FIRST
        formCard.add(createMonthSectionPanel());
        formCard.add(Box.createVerticalStrut(UiTheme.SPACING_MD));

        // Folder section - SECOND
        formCard.add(createSectionPanel("Složka s reporty",
            "Vyberte složku obsahující PDF reporty zdravotního pojištění.",
            createPickerPanel(folderField, this::pickFolder)));

        formCard.add(Box.createVerticalStrut(UiTheme.SPACING_MD));

        // Spreadsheet section - THIRD
        formCard.add(createSectionPanel("Zdroj dat klientů",
            "Vyberte Excel soubor nebo Google Sheets s údaji klientů.",
            createPickerPanel(spreadsheetField, this::pickSpreadsheet)));

        formCard.add(Box.createVerticalStrut(UiTheme.SPACING_MD));

        // Error label
        errorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        formCard.add(errorLabel);

        root.add(formCard, BorderLayout.CENTER);

        // Bottom buttons panel
        root.add(buildBottomPanel(), BorderLayout.SOUTH);

        setContentPane(root);
        setMinimumSize(new Dimension(580, 550));
        setPreferredSize(new Dimension(620, 600));

        SwingUtilities.invokeLater(this::validateInputs);
    }

    private JPanel createSectionPanel(String title, String description, JPanel contentPanel) {
        JPanel section = new JPanel();
        section.setOpaque(false);
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel titleLabel = UiTheme.createSectionHeader(title);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.add(titleLabel);

        section.add(Box.createVerticalStrut(UiTheme.SPACING_XS));

        JLabel descLabel = UiTheme.createDescriptionLabel(description);
        descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.add(descLabel);

        section.add(Box.createVerticalStrut(UiTheme.SPACING_SM));

        contentPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.add(contentPanel);

        return section;
    }

    private JPanel createMonthSectionPanel() {
        JPanel section = new JPanel();
        section.setOpaque(false);
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel titleLabel = UiTheme.createSectionHeader("Období kontroly");
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.add(titleLabel);

        section.add(Box.createVerticalStrut(UiTheme.SPACING_XS));

        JLabel descLabel = UiTheme.createDescriptionLabel("Vyberte měsíc, za který chcete provést kontrolu.");
        descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.add(descLabel);

        section.add(Box.createVerticalStrut(UiTheme.SPACING_SM));

        UiTheme.styleComboBox(monthComboBox);
        monthComboBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        monthComboBox.setMaximumSize(new Dimension(200, 36));
        monthComboBox.addActionListener(e -> {
            savePreferences();
            validateInputs();
        });
        section.add(monthComboBox);

        return section;
    }

    private JPanel createPickerPanel(JTextField field, Runnable picker) {
        JPanel panel = new JPanel(new BorderLayout(UiTheme.SPACING_SM, 0));
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        field.setEditable(false);
        panel.add(field, BorderLayout.CENTER);

        JButton browse = UiTheme.createSecondaryButton("Vybrat...");
        browse.addActionListener(e -> picker.run());
        panel.add(browse, BorderLayout.EAST);

        return panel;
    }

    private JPanel buildBottomPanel() {
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.setBorder(new EmptyBorder(UiTheme.SPACING_LG, 0, 0, 0));

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, UiTheme.SPACING_SM, 0));
        buttonsPanel.setOpaque(false);

        JButton cancelBtn = UiTheme.createSecondaryButton("Zpět");
        cancelBtn.addActionListener(e -> {
            dispose();
            SwingUtilities.invokeLater(StartWindow::new);
        });
        buttonsPanel.add(cancelBtn);

        continueButton = UiTheme.createPrimaryButton("Pokračovat");
        continueButton.setEnabled(false);
        continueButton.addActionListener(e -> {
            dispose();
            String folder = folderField.getText();
            SpreadsheetSource source = buildSource();
            CzechMonth month = (CzechMonth) monthComboBox.getSelectedItem();
            SwingUtilities.invokeLater(() -> new ResultsWindow(folder, source, month));
        });
        buttonsPanel.add(continueButton);

        bottom.add(buttonsPanel, BorderLayout.EAST);
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
