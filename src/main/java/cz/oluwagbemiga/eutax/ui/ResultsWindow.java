package cz.oluwagbemiga.eutax.ui;

import cz.oluwagbemiga.eutax.pojo.*;
import cz.oluwagbemiga.eutax.tools.FileMover;
import cz.oluwagbemiga.eutax.tools.InfoFromFiles;
import cz.oluwagbemiga.eutax.tools.MatchEvaluator;
import cz.oluwagbemiga.eutax.tools.SpreadsheetWorker;
import cz.oluwagbemiga.eutax.tools.SpreadsheetWorkerFactory;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Window that displays the results of the match evaluation.
 * Shows clients with missing reports and any errors encountered.
 */
@Slf4j
public class ResultsWindow extends JFrame {

    private final String folderPath;
    private final SpreadsheetSource spreadsheetSource;
    private final CzechMonth month;

    private final DefaultTableModel successfulMatchesModel;
    private final DefaultTableModel missingReportsModel;
    private final DefaultTableModel errorsModel;
    // New model for parsed files that are not present in the clients table
    private final DefaultTableModel missingInTableModel;
    private final DefaultTableModel invalidDirTableModel;

    private final JLabel statusLabel = new JLabel(" ");
    private JButton saveButton;
    private JButton backButton;
    private JButton refreshButton;
    private JTabbedPane tabbedPane;
    private JTable successfulTable;
    private JTable missingTable;
    private JTable errorsTable;
    private JTable missingInTable;
    private JTable invalidDirTable;
    private List<Client> lastEvaluatedClients = List.of();

    // Progress components
    private JPanel progressPanel;
    private JLabel progressLabel;
    private JPanel contentCard;

    // Success message components
    private JPanel successPanel;

    public ResultsWindow(String folderPath, SpreadsheetSource spreadsheetSource, CzechMonth month) {
        this.folderPath = folderPath;
        this.spreadsheetSource = spreadsheetSource;
        this.month = month;

        UiTheme.apply();
        setTitle("Výsledky kontroly - " + month.getCzechName());
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        AppIconProvider.apply(this);

        successfulMatchesModel = new DefaultTableModel(new String[]{"Jméno klienta", "IČO"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        // Add third column to show which insurers are missing
        missingReportsModel = new DefaultTableModel(new String[]{"Jméno klienta", "IČO", "Chybějící pojišťovny"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        errorsModel = new DefaultTableModel(new String[]{"Název souboru", "Chyba"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        // New model: parsed files that don't have corresponding client in the spreadsheet
        missingInTableModel = new DefaultTableModel(new String[]{"Soubor", "IČO", "Pojišťovna"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        invalidDirTableModel = new DefaultTableModel(new String[]{"Soubor", "IČO", "Pojišťovna", "Složka"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        buildUi();
        pack();
        setLocationRelativeTo(null);
        setVisible(true);

        loadData();
    }

    private void buildUi() {
        JPanel root = UiTheme.createBackgroundPanel();
        root.setLayout(new BorderLayout());
        root.setBorder(UiTheme.createContentBorder());

        // Header section
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(new EmptyBorder(0, 0, UiTheme.SPACING_MD, 0));

        JPanel titlePanel = new JPanel();
        titlePanel.setOpaque(false);
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel("Výsledky kontroly");
        titleLabel.setFont(UiTheme.FONT_TITLE);
        titleLabel.setForeground(UiTheme.TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        titlePanel.add(titleLabel);

        titlePanel.add(Box.createVerticalStrut(UiTheme.SPACING_XS));

        JLabel monthLabel = UiTheme.createDescriptionLabel("Období: " + month.getCzechName());
        monthLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        titlePanel.add(monthLabel);

        headerPanel.add(titlePanel, BorderLayout.WEST);

        // Status bar in header
        JPanel statusPanel = createStatusBar();
        headerPanel.add(statusPanel, BorderLayout.SOUTH);

        root.add(headerPanel, BorderLayout.NORTH);

        // Progress panel (initially hidden)
        progressPanel = createProgressPanel();
        progressPanel.setVisible(false);

        // Main content - tabbed pane with tables
        contentCard = UiTheme.createCardPanel();
        contentCard.setLayout(new BorderLayout());

        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(UiTheme.FONT_BODY);

        // Successful matches tab (first)
        JPanel successfulPanel = new JPanel(new BorderLayout());
        successfulPanel.setOpaque(false);
        successfulPanel.setBorder(new EmptyBorder(UiTheme.SPACING_SM, UiTheme.SPACING_SM, UiTheme.SPACING_SM, UiTheme.SPACING_SM));

        successfulTable = new JTable(successfulMatchesModel);
        styleTable(successfulTable);
        successfulTable.getColumnModel().getColumn(0).setPreferredWidth(280);
        successfulTable.getColumnModel().getColumn(1).setPreferredWidth(120);

        JScrollPane successfulScroll = new JScrollPane(successfulTable);
        successfulScroll.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER_COLOR));
        successfulPanel.add(successfulScroll, BorderLayout.CENTER);
        tabbedPane.addTab("Úspěšné shody", successfulPanel);

        // Missing reports tab
        JPanel missingPanel = new JPanel(new BorderLayout());
        missingPanel.setOpaque(false);
        missingPanel.setBorder(new EmptyBorder(UiTheme.SPACING_SM, UiTheme.SPACING_SM, UiTheme.SPACING_SM, UiTheme.SPACING_SM));

        missingTable = new JTable(missingReportsModel);
        styleTable(missingTable);
        missingTable.getColumnModel().getColumn(0).setPreferredWidth(280);
        missingTable.getColumnModel().getColumn(1).setPreferredWidth(120);
        // third column for missing insurers
        missingTable.getColumnModel().getColumn(2).setPreferredWidth(300);

        JScrollPane missingScroll = new JScrollPane(missingTable);
        missingScroll.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER_COLOR));
        missingPanel.add(missingScroll, BorderLayout.CENTER);
        tabbedPane.addTab("Chybějící reporty", missingPanel);

        // Errors tab
        JPanel errorsPanel = new JPanel(new BorderLayout());
        errorsPanel.setOpaque(false);
        errorsPanel.setBorder(new EmptyBorder(UiTheme.SPACING_SM, UiTheme.SPACING_SM, UiTheme.SPACING_SM, UiTheme.SPACING_SM));

        errorsTable = new JTable(errorsModel);
        styleTable(errorsTable);
        errorsTable.getColumnModel().getColumn(0).setPreferredWidth(220);
        errorsTable.getColumnModel().getColumn(1).setPreferredWidth(320);

        JScrollPane errorsScroll = new JScrollPane(errorsTable);
        errorsScroll.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER_COLOR));
        errorsPanel.add(errorsScroll, BorderLayout.CENTER);
        tabbedPane.addTab("Chyby při načítání", errorsPanel);

        // New tab: parsed files that are missing in the spreadsheet table
        JPanel missingInPanel = new JPanel(new BorderLayout());
        missingInPanel.setOpaque(false);
        missingInPanel.setBorder(new EmptyBorder(UiTheme.SPACING_SM, UiTheme.SPACING_SM, UiTheme.SPACING_SM, UiTheme.SPACING_SM));

        missingInTable = new JTable(missingInTableModel);
        styleTable(missingInTable);
        missingInTable.getColumnModel().getColumn(0).setPreferredWidth(360); // file path
        missingInTable.getColumnModel().getColumn(1).setPreferredWidth(120); // ico
        missingInTable.getColumnModel().getColumn(2).setPreferredWidth(160); // insurer

        JScrollPane missingInScroll = new JScrollPane(missingInTable);
        missingInScroll.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER_COLOR));
        missingInPanel.add(missingInScroll, BorderLayout.CENTER);
        tabbedPane.addTab("Chybí v tabulce", missingInPanel);

        // New tab: files with invalid parent directories
        JPanel invalidDirPanel = new JPanel(new BorderLayout());
        invalidDirPanel.setOpaque(false);
        invalidDirPanel.setBorder(new EmptyBorder(UiTheme.SPACING_SM, UiTheme.SPACING_SM, UiTheme.SPACING_SM, UiTheme.SPACING_SM));

        invalidDirTable = new JTable(invalidDirTableModel);
        styleTable(invalidDirTable);
        invalidDirTable.getColumnModel().getColumn(0).setPreferredWidth(360);
        invalidDirTable.getColumnModel().getColumn(1).setPreferredWidth(120);
        invalidDirTable.getColumnModel().getColumn(2).setPreferredWidth(160);
        invalidDirTable.getColumnModel().getColumn(3).setPreferredWidth(140);

        JScrollPane invalidDirScroll = new JScrollPane(invalidDirTable);
        invalidDirScroll.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER_COLOR));
        invalidDirPanel.add(invalidDirScroll, BorderLayout.CENTER);

        // Add action buttons under the invalid-dir table
        JPanel invalidActions = new JPanel(new FlowLayout(FlowLayout.LEFT, UiTheme.SPACING_SM, 0));
        invalidActions.setOpaque(false);

        JButton moveSelectedBtn = UiTheme.createPrimaryButton("Přesunout vybrané");
        moveSelectedBtn.addActionListener(e -> moveSelectedInvalidFiles());
        invalidActions.add(moveSelectedBtn);

        JButton moveAllBtn = UiTheme.createSecondaryButton("Přesunout vše");
        moveAllBtn.addActionListener(e -> moveAllInvalidFiles());
        invalidActions.add(moveAllBtn);

        // place actions at bottom of the invalidDirPanel
        invalidDirPanel.add(invalidActions, BorderLayout.SOUTH);

        tabbedPane.addTab("Nesprávné adresáře", invalidDirPanel);

        contentCard.add(tabbedPane, BorderLayout.CENTER);

        // Success panel (initially hidden)
        successPanel = createSuccessPanel();
        successPanel.setVisible(false);

        // Wrapper panel to hold content, progress, and success panels
        JPanel centerWrapper = new JPanel(new BorderLayout());
        centerWrapper.setOpaque(false);
        centerWrapper.add(progressPanel, BorderLayout.NORTH);
        centerWrapper.add(contentCard, BorderLayout.CENTER);
        centerWrapper.add(successPanel, BorderLayout.SOUTH);

        root.add(centerWrapper, BorderLayout.CENTER);

        // Bottom buttons panel
        root.add(buildBottomPanel(), BorderLayout.SOUTH);

        setContentPane(root);
        setPreferredSize(new Dimension(750, 550));
    }

    private void styleTable(JTable table) {
        table.setFont(UiTheme.FONT_BODY);
        table.setRowHeight(32);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setSelectionBackground(UiTheme.PRIMARY_LIGHT);
        table.setSelectionForeground(UiTheme.TEXT_LIGHT);
        table.setFillsViewportHeight(true);
        table.setBackground(UiTheme.BG_CARD);

        // Style header
        JTableHeader header = table.getTableHeader();
        header.setFont(UiTheme.FONT_BODY.deriveFont(Font.BOLD));
        header.setBackground(UiTheme.BG_SECONDARY);
        header.setForeground(UiTheme.TEXT_PRIMARY);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UiTheme.BORDER_COLOR));

        // Alternate row colors
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? UiTheme.BG_CARD : UiTheme.BG_SECONDARY);
                }
                setBorder(new EmptyBorder(0, UiTheme.SPACING_SM, 0, UiTheme.SPACING_SM));
                return c;
            }
        });
    }

    private JPanel createStatusBar() {
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setOpaque(false);
        statusPanel.setBorder(new EmptyBorder(UiTheme.SPACING_SM, 0, 0, 0));

        statusLabel.setFont(UiTheme.FONT_BODY);
        statusLabel.setForeground(UiTheme.TEXT_SECONDARY);
        statusPanel.add(statusLabel, BorderLayout.WEST);

        return statusPanel;
    }

    private JPanel createProgressPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(UiTheme.BG_CARD);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UiTheme.BORDER_COLOR),
                new EmptyBorder(UiTheme.SPACING_LG, UiTheme.SPACING_LG, UiTheme.SPACING_LG, UiTheme.SPACING_LG)
        ));

        // Progress label with icon-like indicator
        JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, UiTheme.SPACING_SM, 0));
        labelPanel.setOpaque(false);

        progressLabel = new JLabel("Načítání dat...");
        progressLabel.setFont(UiTheme.FONT_SUBTITLE);
        progressLabel.setForeground(UiTheme.TEXT_PRIMARY);
        labelPanel.add(progressLabel);

        labelPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(labelPanel);

        panel.add(Box.createVerticalStrut(UiTheme.SPACING_MD));

        // Animated spinner using custom painted component
        JPanel spinnerWrapper = getSpinnerWrapper();
        panel.add(spinnerWrapper);

        panel.add(Box.createVerticalStrut(UiTheme.SPACING_SM));

        // Subtitle with additional info
        JLabel subtitleLabel = UiTheme.createDescriptionLabel("Prosím vyčkejte...");
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(subtitleLabel);

        return panel;
    }

    private JPanel getSpinnerWrapper() {
        JPanel spinnerWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        spinnerWrapper.setOpaque(false);
        spinnerWrapper.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Create animated spinning indicator
        JPanel spinner = new JPanel() {
            private int angle = 0;
            private final Timer timer = new Timer(50, e -> {
                angle = (angle + 30) % 360;
                repaint();
            });

            {
                setPreferredSize(new Dimension(40, 40));
                setOpaque(false);
                timer.start();
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int centerX = getWidth() / 2;
                int centerY = getHeight() / 2;
                int radius = 14;
                int dotRadius = 4;

                for (int i = 0; i < 8; i++) {
                    double theta = Math.toRadians(angle + i * 45);
                    int x = (int) (centerX + radius * Math.cos(theta));
                    int y = (int) (centerY + radius * Math.sin(theta));

                    // Fade out dots based on position
                    float alpha = 1.0f - (i * 0.1f);
                    g2d.setColor(new Color(
                            UiTheme.PRIMARY.getRed(),
                            UiTheme.PRIMARY.getGreen(),
                            UiTheme.PRIMARY.getBlue(),
                            (int) (255 * alpha)
                    ));
                    g2d.fillOval(x - dotRadius, y - dotRadius, dotRadius * 2, dotRadius * 2);
                }
                g2d.dispose();
            }
        };

        spinnerWrapper.add(spinner);
        return spinnerWrapper;
    }

    private JPanel createSuccessPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(UiTheme.BG_CARD);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiTheme.SUCCESS, 2),
                new EmptyBorder(UiTheme.SPACING_LG * 2, UiTheme.SPACING_LG, UiTheme.SPACING_LG * 2, UiTheme.SPACING_LG)
        ));

        // Add vertical glue to center content
        panel.add(Box.createVerticalGlue());

        // Checkmark icon using Unicode
        JLabel checkmarkLabel = new JLabel("✓");
        checkmarkLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 48));
        checkmarkLabel.setForeground(UiTheme.SUCCESS);
        checkmarkLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(checkmarkLabel);

        panel.add(Box.createVerticalStrut(UiTheme.SPACING_MD));

        // Main success message
        JLabel successLabel = new JLabel("Data byla úspěšně uložena!");
        successLabel.setFont(UiTheme.FONT_TITLE.deriveFont(Font.BOLD, 24f));
        successLabel.setForeground(UiTheme.SUCCESS);
        successLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(successLabel);

        panel.add(Box.createVerticalStrut(UiTheme.SPACING_SM));

        // Subtitle
        JLabel subtitleLabel = UiTheme.createDescriptionLabel("Změny byly uloženy do tabulky.");
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(subtitleLabel);

        // Add vertical glue to center content
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    private void showSuccess() {
        successPanel.setVisible(true);
        contentCard.setVisible(false);
        progressPanel.setVisible(false);
        backButton.setEnabled(true);
        refreshButton.setEnabled(true);
        saveButton.setEnabled(false);
    }

    private void showProgress(String message) {
        progressLabel.setText(message);
        progressPanel.setVisible(true);
        contentCard.setVisible(false);
        successPanel.setVisible(false);
        backButton.setEnabled(false);
        refreshButton.setEnabled(false);
        saveButton.setEnabled(false);
    }

    private void hideProgress() {
        progressPanel.setVisible(false);
        successPanel.setVisible(false);
        contentCard.setVisible(true);
        backButton.setEnabled(true);
        refreshButton.setEnabled(true);
    }

    private JPanel buildBottomPanel() {
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.setBorder(new EmptyBorder(UiTheme.SPACING_LG, 0, 0, 0));

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, UiTheme.SPACING_SM, 0));
        buttonsPanel.setOpaque(false);

        backButton = UiTheme.createSecondaryButton("Zpět");
        backButton.addActionListener(e -> {
            dispose();
            SwingUtilities.invokeLater(SourcesSelectionWindow::new);
        });
        buttonsPanel.add(backButton);

        refreshButton = UiTheme.createSecondaryButton("Znovu načíst");
        refreshButton.addActionListener(e -> loadData());
        buttonsPanel.add(refreshButton);

        saveButton = UiTheme.createPrimaryButton("Uložit");
        saveButton.setEnabled(false);
        saveButton.addActionListener(e -> saveData());
        buttonsPanel.add(saveButton);

        bottom.add(buttonsPanel, BorderLayout.EAST);
        return bottom;
    }

    private void saveData() {
        if (lastEvaluatedClients.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Nejsou k dispozici žádná data k uložení.",
                    "Upozornění",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        showProgress("Ukládání dat do tabulky...");
        statusLabel.setText("Ukládání dat...");
        statusLabel.setForeground(UiTheme.TEXT_SECONDARY);

        AtomicReference<String> saveErrorMsg = new AtomicReference<>();

         SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
             @Override
             protected Boolean doInBackground() {
                 try {
                     SpreadsheetWorker spreadsheetWorker = SpreadsheetWorkerFactory.create(spreadsheetSource);
                     spreadsheetWorker.updateReportGeneratedStatus(spreadsheetSource.identifier(), lastEvaluatedClients, month);
                     return true;
                 } catch (Exception e) {
                     log.error("Error saving data", e);
                     saveErrorMsg.set(e.getMessage());
                     return false;
                 }
             }

             @Override
             protected void done() {
                 try {
                     boolean success = get();
                     if (success) {
                         showSuccess();
                         statusLabel.setText("Data byla úspěšně uložena.");
                         statusLabel.setForeground(UiTheme.SUCCESS);
                     } else {
                         hideProgress();
                         statusLabel.setText("Chyba při ukládání dat.");
                         statusLabel.setForeground(UiTheme.ERROR);
                         saveButton.setEnabled(true);
                         String msg = saveErrorMsg.get();
                         if (msg != null && !msg.isBlank()) {
                             if (msg.contains("Google API error")) {
                                 String cz = "Nepodařilo se uložit změny do Google tabulky.\n" +
                                         "Důvod: " + msg + "\n\n" +
                                         "Doporučení: Zkontrolujte, že služební účet má přístup k tabulce (sdílejte tabulku s e‑mailem servisního účtu)\n" +
                                         "a že je povoleno Google Sheets API.";
                                 JOptionPane.showMessageDialog(ResultsWindow.this, cz, "Chyba při ukládání", JOptionPane.ERROR_MESSAGE);
                             } else {
                                 JOptionPane.showMessageDialog(ResultsWindow.this, "Chyba při ukládání: " + msg, "Chyba při ukládání", JOptionPane.ERROR_MESSAGE);
                             }
                         }
                     }
                 } catch (InterruptedException e) {
                     Thread.currentThread().interrupt();
                     hideProgress();
                     statusLabel.setText("Ukládání bylo přerušeno.");
                     statusLabel.setForeground(UiTheme.ERROR);
                     saveButton.setEnabled(true);
                 } catch (Exception e) {
                     log.error("Error getting save result", e);
                     hideProgress();
                     statusLabel.setText("Chyba při ukládání dat.");
                     statusLabel.setForeground(UiTheme.ERROR);
                     saveButton.setEnabled(true);
                     String msg = e.getMessage();
                     if (msg != null && !msg.isBlank()) {
                         if (msg.contains("Google API error")) {
                             String cz = "Nepodařilo se uložit změny do Google tabulky.\n" +
                                     "Důvod: " + msg + "\n\n" +
                                     "Doporučení: Zkontrolujte, že služební účet má přístup k tabulce (sdílejte tabulku s e‑mailem servisního účtu)\n" +
                                     "a že je povoleno Google Sheets API.";
                             JOptionPane.showMessageDialog(ResultsWindow.this, cz, "Chyba při ukládání", JOptionPane.ERROR_MESSAGE);
                         } else {
                             JOptionPane.showMessageDialog(ResultsWindow.this, "Chyba při ukládání: " + msg, "Chyba při ukládání", JOptionPane.ERROR_MESSAGE);
                         }
                     }
                 }
             }
         };

         worker.execute();
    }

    private void loadData() {
        showProgress("Porovnávání souborů a klientů...");
        statusLabel.setText("Načítání dat...");
        statusLabel.setForeground(UiTheme.TEXT_SECONDARY);
        successfulMatchesModel.setRowCount(0);
        missingReportsModel.setRowCount(0);
        errorsModel.setRowCount(0);
        missingInTableModel.setRowCount(0);
        invalidDirTableModel.setRowCount(0);

        SwingWorker<EvaluationResult, Void> worker = new SwingWorker<>() {
            @Override
            protected EvaluationResult doInBackground() {
                try {
                    SpreadsheetWorker spreadsheetWorker = SpreadsheetWorkerFactory.create(spreadsheetSource);
                    InfoFromFiles infoFromFiles = new InfoFromFiles();
                    MatchEvaluator evaluator = new MatchEvaluator(spreadsheetWorker, infoFromFiles);

                    List<Client> clients = evaluator.evaluateMatches(spreadsheetSource.identifier(), folderPath, month);
                    WalkerResult walkerResult = infoFromFiles.readReports(folderPath);

                    return new EvaluationResult(clients, walkerResult.errorReports(), null, walkerResult.parsedFileNames());
                } catch (Exception e) {
                    log.error("Error during evaluation", e);
                    return new EvaluationResult(List.of(), List.of(), e.getMessage(), List.of());
                }
            }

            @Override
            protected void done() {
                hideProgress();
                try {
                    EvaluationResult result = get();

                    if (result.errorMessage() != null) {
                        // If the evaluator returned an error message coming from Google API propagate
                        // it as an error dialog so the user sees the detailed message.
                        if (result.errorMessage().contains("Google API error")) {
                            String details = result.errorMessage();
                            String cz = "Nepodařilo se načíst Google tabulku.\n" +
                                    "Důvod: " + details + "\n\n" +
                                    "Doporučení: Zkontrolujte, že služební účet má přístup k tabulce (sdílejte tabulku s e‑mailem servisního účtu)\n" +
                                    "a že je povoleno Google Sheets API."
                                    ;
                            JOptionPane.showMessageDialog(ResultsWindow.this,
                                    cz,
                                    "Chyba při čtení tabulky", JOptionPane.ERROR_MESSAGE);
                        }
                        statusLabel.setText("Chyba: " + result.errorMessage());
                        statusLabel.setForeground(UiTheme.ERROR);
                        saveButton.setEnabled(false);
                        return;
                    }

                    lastEvaluatedClients = result.clients();
                    List<Client> successfulClients = result.clients().stream()
                            .filter(Client::reportGenerated)
                            .toList();
                    List<Client> missingClients = result.clients().stream()
                            .filter(c -> !c.reportGenerated())
                            .toList();

                    // Build ICO -> available insurers map for the same target month/year used during evaluation
                    int targetYear = LocalDate.now().getYear();
                    int targetMonth = month.getMonthNumber();
                    // Build two ICO -> available insurers maps:
                    // 1) For evaluation display (includes files even from invalid directories) so "Chybějící pojišťovny" reflects presence of files
                    Map<String, Set<InsuranceCompany>> icoToInsurersForDisplay = result.parsedFiles().stream()
                            .filter(pf -> pf.date().getYear() == targetYear && pf.date().getMonthValue() == targetMonth)
                            .collect(Collectors.groupingBy(
                                    ParsedFileName::ico,
                                    Collectors.mapping(ParsedFileName::insuranceCompany, Collectors.toSet())
                            ));

                    // 2) For evaluation saving (matches MatchEvaluator behavior) - exclude invalid-directory files
                    Map<String, Set<InsuranceCompany>> icoToInsurersForEvaluation = result.parsedFiles().stream()
                            .filter(pf -> !pf.invalidDirectory())
                            .filter(pf -> pf.date().getYear() == targetYear && pf.date().getMonthValue() == targetMonth)
                            .collect(Collectors.groupingBy(
                                    ParsedFileName::ico,
                                    Collectors.mapping(ParsedFileName::insuranceCompany, Collectors.toSet())
                            ));

                    for (Client client : successfulClients) {
                        successfulMatchesModel.addRow(new Object[]{client.name(), client.ico()});
                    }

                    for (Client client : missingClients) {
                        // Compute which insurers are missing for this client
                        String missingInsurers;
                        if (client.insuranceCompanies() == null || client.insuranceCompanies().isEmpty()) {
                            missingInsurers = "";
                        } else {
                            // Use the evaluation map (which excludes files in invalid directories)
                            // so that files located in wrong folders are considered missing here.
                            Set<InsuranceCompany> available = icoToInsurersForEvaluation.get(client.ico());
                            String joined = client.insuranceCompanies().stream()
                                    .filter(req -> available == null || !available.contains(req))
                                    .map(InsuranceCompany::toString)
                                    .collect(Collectors.joining(", "));
                            missingInsurers = joined;
                        }

                        missingReportsModel.addRow(new Object[]{client.name(), client.ico(), missingInsurers});
                    }

                    // Identify parsed files that don't have a corresponding client in the table
                    Set<String> clientIcos = result.clients().stream()
                            .map(Client::ico)
                            .collect(Collectors.toSet());

                    // Files missing in the spreadsheet table (include files even from invalid directories so they appear in both tabs)
                    List<ParsedFileName> missingInTable = result.parsedFiles().stream()
                            .filter(pf -> pf.date().getYear() == targetYear && pf.date().getMonthValue() == targetMonth)
                            .filter(pf -> !clientIcos.contains(pf.ico()))
                            .toList();

                    for (ParsedFileName pf : missingInTable) {
                        missingInTableModel.addRow(new Object[]{pf.filePath(), pf.ico(), pf.insuranceCompany().toString()});
                    }

                    // Files where parent directory does not match insurer display names
                    List<ParsedFileName> invalidDirFiles = result.parsedFiles().stream()
                            .filter(ParsedFileName::invalidDirectory)
                            .filter(pf -> pf.date().getYear() == targetYear && pf.date().getMonthValue() == targetMonth)
                            .toList();

                    for (ParsedFileName pf : invalidDirFiles) {
                        invalidDirTableModel.addRow(new Object[]{pf.filePath(), pf.ico(), pf.insuranceCompany().toString(), pf.parentDirName()});
                    }

                    for (ErrorReport error : result.errors()) {
                        errorsModel.addRow(new Object[]{error.fileName(), error.errorMessage()});
                    }

                    long totalClients = result.clients().size();
                    long clientsWithReports = successfulClients.size();

                    String status = String.format(
                            "Zdroj: %s | Celkem: %d | S reporty: %d | Chybějící: %d | Chyby: %d | Chyby adresářů: %d | Chybí v tabulce: %d",
                            spreadsheetSource.type(), totalClients, clientsWithReports,
                            missingClients.size(), result.errors().size(), invalidDirFiles.size(), missingInTable.size()
                    );
                    statusLabel.setText(status);
                    statusLabel.setForeground(UiTheme.TEXT_PRIMARY);

                    tabbedPane.setTitleAt(0, "Úspěšné shody (" + successfulClients.size() + ")");
                    tabbedPane.setTitleAt(1, "Chybějící reporty (" + missingClients.size() + ")");
                    tabbedPane.setTitleAt(2, "Chyby při načítání (" + result.errors().size() + ")");
                    tabbedPane.setTitleAt(3, "Chybí v tabulce (" + missingInTable.size() + ")");
                    tabbedPane.setTitleAt(4, "Nesprávné adresáře (" + invalidDirFiles.size() + ")");

                    saveButton.setEnabled(!result.clients().isEmpty());

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("Interrupted while getting evaluation result", e);
                    statusLabel.setText("Zpracování bylo přerušeno");
                    statusLabel.setForeground(UiTheme.ERROR);
                    saveButton.setEnabled(false);
                } catch (Exception e) {
                    log.error("Error getting evaluation result", e);
                    // If the evaluation threw a runtime exception with a message from the Google API
                    // show it to the user — this typically carries the API's 'forbidden' message.
                    String msg = e.getMessage();
                    if (msg != null && msg.contains("Google API error")) {
                        String cz = "Nepodařilo se načíst Google tabulku.\n" +
                                "Důvod: " + msg + "\n\n" +
                                "Doporučení: Zkontrolujte, že služební účet má přístup k tabulce (sdílejte tabulku s e‑mailem servisního účtu)\n" +
                                "a že je povoleno Google Sheets API.";
                        JOptionPane.showMessageDialog(ResultsWindow.this,
                                cz, "Chyba při čtení tabulky", JOptionPane.ERROR_MESSAGE);
                    }
                     statusLabel.setText("Chyba při zpracování výsledků");
                     statusLabel.setForeground(UiTheme.ERROR);
                     saveButton.setEnabled(false);
                 }
             }
         };

         worker.execute();
     }

     private record EvaluationResult(
             List<Client> clients,
             List<ErrorReport> errors,
             String errorMessage,
             List<ParsedFileName> parsedFiles
     ) {
     }

    /**
     * Move only selected rows from the invalid-dir table.
     */
    private void moveSelectedInvalidFiles() {
        int[] selected = invalidDirTable.getSelectedRows();
        if (selected == null || selected.length == 0) {
            JOptionPane.showMessageDialog(this, "Vyberte alespoň jeden soubor k přesunu.", "Upozornění", JOptionPane.WARNING_MESSAGE);
            return;
        }
        moveInvalidFilesByViewIndices(selected);
    }

    /**
     * Move all rows from the invalid-dir table.
     */
    private void moveAllInvalidFiles() {
        int rowCount = invalidDirTableModel.getRowCount();
        if (rowCount == 0) {
            JOptionPane.showMessageDialog(this, "Žádné soubory k přesunu.", "Informace", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int[] all = new int[rowCount];
        for (int i = 0; i < rowCount; i++) all[i] = i;
        moveInvalidFilesByViewIndices(all);
    }

    /**
     * Common mover that accepts view indices from the table, moves files in background and updates the UI model.
     */
    private void moveInvalidFilesByViewIndices(int[] viewRows) {
        // convert to model indices and collect file info
        int[] modelRows = new int[viewRows.length];
        for (int i = 0; i < viewRows.length; i++) {
            modelRows[i] = invalidDirTable.convertRowIndexToModel(viewRows[i]);
        }

        // collect data
        var tasks = new java.util.ArrayList<MoveTask>();
        for (int mr : modelRows) {
            String filePath = (String) invalidDirTableModel.getValueAt(mr, 0);
            String ico = (String) invalidDirTableModel.getValueAt(mr, 1);
            String insurerName = (String) invalidDirTableModel.getValueAt(mr, 2);
            tasks.add(new MoveTask(mr, filePath, ico, insurerName));
        }

        // confirm
        if (JOptionPane.showConfirmDialog(this,
                "Opravdu chcete přesunout " + tasks.size() + " souborů do odpovídajících složek pojišťoven?",
                "Potvrdit přesun",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }

        showProgress("Přesouvání souborů...");
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            final java.util.List<String> errors = new java.util.ArrayList<>();
            @Override
            protected Void doInBackground() {
                Path root = Paths.get(folderPath);
                FileMover mover = new FileMover(root);
                for (MoveTask t : tasks) {
                    try {
                        // attempt to resolve insurer from display name (model stores insurer.toString())
                        java.util.Optional<InsuranceCompany> maybe = InsuranceCompany.fromDisplayName(t.insurerName);
                        InsuranceCompany insurer = maybe.orElseGet(() -> {
                            // fallback: try to match by first token
                            return InsuranceCompany.values()[0];
                        });
                        Path target = mover.ensureOrCreateFolder(insurer);
                        Path src = Paths.get(t.filePath);
                        mover.moveFileToFolder(src, target);
                    } catch (IOException ex) {
                        log.error("Failed to move file {}", t.filePath, ex);
                        errors.add(t.filePath + ": " + ex.getMessage());
                    }
                }
                return null;
            }

            @Override
            protected void done() {
                hideProgress();

                // Always reload the data so tables reflect the current filesystem state after moves.
                // Show user feedback first, then refresh.
                if (errors.isEmpty()) {
                    JOptionPane.showMessageDialog(ResultsWindow.this, "Soubory byly přesunuty.", "Hotovo", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    String msg = "Některé soubory se nepodařilo přesunout:\n" + String.join("\n", errors);
                    JOptionPane.showMessageDialog(ResultsWindow.this, msg, "Chyba při přesunu", JOptionPane.ERROR_MESSAGE);
                }

                // Trigger a full reload to refresh all tabs (runs on EDT since done() is called on EDT)
                loadData();
            }
        };
        worker.execute();
    }

    private static class MoveTask {
        final int modelRow;
        final String filePath;
        final String ico;
        final String insurerName;
        MoveTask(int modelRow, String filePath, String ico, String insurerName) {
            this.modelRow = modelRow;
            this.filePath = filePath;
            this.ico = ico;
            this.insurerName = insurerName;
        }
    }
}
