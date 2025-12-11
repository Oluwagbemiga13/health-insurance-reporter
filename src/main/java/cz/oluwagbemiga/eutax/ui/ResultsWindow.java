package cz.oluwagbemiga.eutax.ui;

import cz.oluwagbemiga.eutax.pojo.*;
import cz.oluwagbemiga.eutax.tools.IcoFromFiles;
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
import java.util.List;

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
    private final JLabel statusLabel = new JLabel(" ");
    private JButton saveButton;
    private JButton backButton;
    private JButton refreshButton;
    private JTabbedPane tabbedPane;
    private JTable successfulTable;
    private JTable missingTable;
    private JTable errorsTable;
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
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        AppIconProvider.apply(this);

        successfulMatchesModel = new DefaultTableModel(new String[]{"Jméno klienta", "IČO"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        missingReportsModel = new DefaultTableModel(new String[]{"Jméno klienta", "IČO"}, 0) {
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
        panel.add(spinnerWrapper);

        panel.add(Box.createVerticalStrut(UiTheme.SPACING_SM));

        // Subtitle with additional info
        JLabel subtitleLabel = UiTheme.createDescriptionLabel("Prosím vyčkejte...");
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(subtitleLabel);

        return panel;
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

        refreshButton = UiTheme.createSecondaryButton("Aktualizovat");
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

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                try {
                    SpreadsheetWorker spreadsheetWorker = SpreadsheetWorkerFactory.create(spreadsheetSource);
                    spreadsheetWorker.updateReportGeneratedStatus(spreadsheetSource.identifier(), lastEvaluatedClients, month);
                    return true;
                } catch (Exception e) {
                    log.error("Error saving data", e);
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

        SwingWorker<EvaluationResult, Void> worker = new SwingWorker<>() {
            @Override
            protected EvaluationResult doInBackground() {
                try {
                    SpreadsheetWorker spreadsheetWorker = SpreadsheetWorkerFactory.create(spreadsheetSource);
                    IcoFromFiles icoFromFiles = new IcoFromFiles();
                    MatchEvaluator evaluator = new MatchEvaluator(spreadsheetWorker, icoFromFiles);

                    List<Client> clients = evaluator.evaluateMatches(spreadsheetSource.identifier(), folderPath, month);
                    WalkerResult walkerResult = icoFromFiles.readReports(folderPath);

                    return new EvaluationResult(clients, walkerResult.errorReports(), null);
                } catch (Exception e) {
                    log.error("Error during evaluation", e);
                    return new EvaluationResult(List.of(), List.of(), e.getMessage());
                }
            }

            @Override
            protected void done() {
                hideProgress();
                try {
                    EvaluationResult result = get();

                    if (result.errorMessage() != null) {
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

                    for (Client client : successfulClients) {
                        successfulMatchesModel.addRow(new Object[]{client.name(), client.ico()});
                    }

                    for (Client client : missingClients) {
                        missingReportsModel.addRow(new Object[]{client.name(), client.ico()});
                    }

                    for (ErrorReport error : result.errors()) {
                        errorsModel.addRow(new Object[]{error.fileName(), error.errorMessage()});
                    }

                    long totalClients = result.clients().size();
                    long clientsWithReports = successfulClients.size();

                    String status = String.format(
                            "Zdroj: %s | Celkem: %d | S reporty: %d | Chybějící: %d | Chyby: %d",
                            spreadsheetSource.type(), totalClients, clientsWithReports,
                            missingClients.size(), result.errors().size()
                    );
                    statusLabel.setText(status);
                    statusLabel.setForeground(UiTheme.TEXT_PRIMARY);

                    tabbedPane.setTitleAt(0, "Úspěšné shody (" + successfulClients.size() + ")");
                    tabbedPane.setTitleAt(1, "Chybějící reporty (" + missingClients.size() + ")");
                    tabbedPane.setTitleAt(2, "Chyby při načítání (" + result.errors().size() + ")");

                    saveButton.setEnabled(!result.clients().isEmpty());

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("Interrupted while getting evaluation result", e);
                    statusLabel.setText("Zpracování bylo přerušeno");
                    statusLabel.setForeground(UiTheme.ERROR);
                    saveButton.setEnabled(false);
                } catch (Exception e) {
                    log.error("Error getting evaluation result", e);
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
            String errorMessage
    ) {
    }
}
