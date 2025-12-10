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

    private final DefaultTableModel missingReportsModel;
    private final DefaultTableModel errorsModel;
    private final JLabel statusLabel = new JLabel(" ");
    private JButton saveButton;
    private JButton backButton;
    private JButton refreshButton;
    private JTabbedPane tabbedPane;
    private JTable missingTable;
    private JTable errorsTable;
    private List<Client> lastEvaluatedClients = List.of();

    public ResultsWindow(String folderPath, SpreadsheetSource spreadsheetSource, CzechMonth month) {
        this.folderPath = folderPath;
        this.spreadsheetSource = spreadsheetSource;
        this.month = month;

        UiTheme.apply();
        setTitle("Výsledky kontroly - " + month.getCzechName());
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        AppIconProvider.apply(this);

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

        // Main content - tabbed pane with tables
        JPanel contentCard = UiTheme.createCardPanel();
        contentCard.setLayout(new BorderLayout());

        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(UiTheme.FONT_BODY);

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
        root.add(contentCard, BorderLayout.CENTER);

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

        saveButton.setEnabled(false);
        refreshButton.setEnabled(false);
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
                        statusLabel.setText("Data byla úspěšně uložena.");
                        statusLabel.setForeground(UiTheme.SUCCESS);
                    } else {
                        statusLabel.setText("Chyba při ukládání dat.");
                        statusLabel.setForeground(UiTheme.ERROR);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    statusLabel.setText("Ukládání bylo přerušeno.");
                    statusLabel.setForeground(UiTheme.ERROR);
                } catch (Exception e) {
                    log.error("Error getting save result", e);
                    statusLabel.setText("Chyba při ukládání dat.");
                    statusLabel.setForeground(UiTheme.ERROR);
                } finally {
                    saveButton.setEnabled(true);
                    refreshButton.setEnabled(true);
                }
            }
        };

        worker.execute();
    }

    private void loadData() {
        statusLabel.setText("Načítání dat...");
        statusLabel.setForeground(UiTheme.TEXT_SECONDARY);
        missingReportsModel.setRowCount(0);
        errorsModel.setRowCount(0);
        saveButton.setEnabled(false);
        refreshButton.setEnabled(false);

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
                try {
                    EvaluationResult result = get();

                    if (result.errorMessage() != null) {
                        statusLabel.setText("Chyba: " + result.errorMessage());
                        statusLabel.setForeground(UiTheme.ERROR);
                        saveButton.setEnabled(false);
                        refreshButton.setEnabled(true);
                        return;
                    }

                    lastEvaluatedClients = result.clients();
                    List<Client> missingClients = result.clients().stream()
                            .filter(c -> !c.reportGenerated())
                            .toList();

                    for (Client client : missingClients) {
                        missingReportsModel.addRow(new Object[]{client.name(), client.ico()});
                    }

                    for (ErrorReport error : result.errors()) {
                        errorsModel.addRow(new Object[]{error.fileName(), error.errorMessage()});
                    }

                    long totalClients = result.clients().size();
                    long clientsWithReports = result.clients().stream()
                            .filter(Client::reportGenerated)
                            .count();

                    String status = String.format(
                            "Zdroj: %s | Celkem: %d | S reporty: %d | Chybějící: %d | Chyby: %d",
                            spreadsheetSource.type(), totalClients, clientsWithReports,
                            missingClients.size(), result.errors().size()
                    );
                    statusLabel.setText(status);
                    statusLabel.setForeground(UiTheme.TEXT_PRIMARY);

                    tabbedPane.setTitleAt(0, "Chybějící reporty (" + missingClients.size() + ")");
                    tabbedPane.setTitleAt(1, "Chyby při načítání (" + result.errors().size() + ")");

                    saveButton.setEnabled(!result.clients().isEmpty());
                    refreshButton.setEnabled(true);

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("Interrupted while getting evaluation result", e);
                    statusLabel.setText("Zpracování bylo přerušeno");
                    statusLabel.setForeground(UiTheme.ERROR);
                    saveButton.setEnabled(false);
                    refreshButton.setEnabled(true);
                } catch (Exception e) {
                    log.error("Error getting evaluation result", e);
                    statusLabel.setText("Chyba při zpracování výsledků");
                    statusLabel.setForeground(UiTheme.ERROR);
                    saveButton.setEnabled(false);
                    refreshButton.setEnabled(true);
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
