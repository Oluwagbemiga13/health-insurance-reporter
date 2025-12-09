package cz.oluwagbemiga.eutax.ui;

import cz.oluwagbemiga.eutax.pojo.Client;
import cz.oluwagbemiga.eutax.pojo.CzechMonth;
import cz.oluwagbemiga.eutax.pojo.ErrorReport;
import cz.oluwagbemiga.eutax.pojo.WalkerResult;
import cz.oluwagbemiga.eutax.tools.ExcelWorker;
import cz.oluwagbemiga.eutax.tools.IcoFromFiles;
import cz.oluwagbemiga.eutax.tools.MatchEvaluator;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Window that displays the results of the match evaluation.
 * Shows clients with missing reports and any errors encountered.
 */
@Slf4j
public class ResultsWindow extends JFrame {

    private final String folderPath;
    private final String spreadsheetPath;
    private final CzechMonth month;

    private final DefaultTableModel missingReportsModel;
    private final DefaultTableModel errorsModel;
    private final JLabel statusLabel = new JLabel(" ");
    private JTabbedPane tabbedPane;
    private List<Client> lastEvaluatedClients = List.of();
    private final JButton saveButton = new JButton("Uložit do Excel");

    public ResultsWindow(String folderPath, String spreadsheetPath, CzechMonth month) {
        this.folderPath = folderPath;
        this.spreadsheetPath = spreadsheetPath;
        this.month = month;

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

        // Run evaluation in background
        loadData();
    }

    private void buildUi() {
        JPanel content = new JPanel(new BorderLayout());
        content.setBorder(new EmptyBorder(16, 16, 16, 16));

        // Tabbed pane for missing reports and errors
        tabbedPane = new JTabbedPane();

        // Missing reports panel
        JPanel missingPanel = new JPanel(new BorderLayout());
        missingPanel.setBorder(new EmptyBorder(8, 8, 8, 8));
        JTable missingTable = new JTable(missingReportsModel);
        missingTable.setFillsViewportHeight(true);
        missingTable.getColumnModel().getColumn(0).setPreferredWidth(250);
        missingTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        JScrollPane missingScroll = new JScrollPane(missingTable);
        missingPanel.add(missingScroll, BorderLayout.CENTER);
        tabbedPane.addTab("Chybějící reporty", missingPanel);

        // Errors panel
        JPanel errorsPanel = new JPanel(new BorderLayout());
        errorsPanel.setBorder(new EmptyBorder(8, 8, 8, 8));
        JTable errorsTable = new JTable(errorsModel);
        errorsTable.setFillsViewportHeight(true);
        errorsTable.getColumnModel().getColumn(0).setPreferredWidth(200);
        errorsTable.getColumnModel().getColumn(1).setPreferredWidth(300);
        JScrollPane errorsScroll = new JScrollPane(errorsTable);
        errorsPanel.add(errorsScroll, BorderLayout.CENTER);
        tabbedPane.addTab("Chyby při načítání", errorsPanel);

        content.add(tabbedPane, BorderLayout.CENTER);

        // Status bar
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(new EmptyBorder(8, 0, 0, 0));
        statusLabel.setForeground(Color.DARK_GRAY);
        statusPanel.add(statusLabel, BorderLayout.WEST);
        content.add(statusPanel, BorderLayout.NORTH);

        // Bottom panel with buttons
        content.add(buildBottomPanel(), BorderLayout.SOUTH);

        setContentPane(content);
        setPreferredSize(new Dimension(700, 500));
    }

    private JPanel buildBottomPanel() {
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton backButton = new JButton("Zpět");
        backButton.addActionListener(e -> {
            dispose();
            SwingUtilities.invokeLater(SourcesSelectionWindow::new);
        });
        bottom.add(backButton);

        JButton refreshButton = new JButton("Aktualizovat");
        refreshButton.addActionListener(e -> loadData());
        bottom.add(refreshButton);

        saveButton.setEnabled(false);
        saveButton.addActionListener(e -> saveData());
        bottom.add(saveButton);

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
        statusLabel.setText("Ukládání dat...");

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                try {
                    ExcelWorker excelWorker = new ExcelWorker();
                    excelWorker.updateReportGeneratedStatus(spreadsheetPath, lastEvaluatedClients, month);
                    return true;
                } catch (Exception e) {
                    log.error("Error saving data to Excel", e);
                    return false;
                }
            }

            @Override
            protected void done() {
                try {
                    boolean success = get();
                    if (success) {
                        statusLabel.setText("Data byla úspěšně uložena.");
                        statusLabel.setForeground(new Color(0, 128, 0));
                    } else {
                        statusLabel.setText("Chyba při ukládání dat.");
                        statusLabel.setForeground(new Color(176, 0, 32));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    statusLabel.setText("Ukládání bylo přerušeno.");
                    statusLabel.setForeground(new Color(176, 0, 32));
                } catch (Exception e) {
                    log.error("Error getting save result", e);
                    statusLabel.setText("Chyba při ukládání dat.");
                    statusLabel.setForeground(new Color(176, 0, 32));
                } finally {
                    saveButton.setEnabled(true);
                }
            }
        };

        worker.execute();
    }

    private void loadData() {
        statusLabel.setText("Načítání dat...");
        missingReportsModel.setRowCount(0);
        errorsModel.setRowCount(0);

        SwingWorker<EvaluationResult, Void> worker = new SwingWorker<>() {
            @Override
            protected EvaluationResult doInBackground() {
                try {
                    ExcelWorker excelWorker = new ExcelWorker();
                    IcoFromFiles icoFromFiles = new IcoFromFiles();
                    MatchEvaluator evaluator = new MatchEvaluator(excelWorker, icoFromFiles);

                    List<Client> clients = evaluator.evaluateMatches(spreadsheetPath, folderPath, month);
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
                        statusLabel.setForeground(new Color(176, 0, 32));
                        saveButton.setEnabled(false);
                        return;
                    }

                    // Store evaluated clients for saving
                    lastEvaluatedClients = result.clients();

                    // Populate missing reports table
                    List<Client> missingClients = result.clients().stream()
                            .filter(c -> !c.reportGenerated())
                            .toList();

                    for (Client client : missingClients) {
                        missingReportsModel.addRow(new Object[]{client.name(), client.ico()});
                    }

                    // Populate errors table
                    for (ErrorReport error : result.errors()) {
                        errorsModel.addRow(new Object[]{error.fileName(), error.errorMessage()});
                    }

                    // Update status
                    long totalClients = result.clients().size();
                    long clientsWithReports = result.clients().stream()
                            .filter(Client::reportGenerated)
                            .count();

                    String status = String.format(
                            "Celkem klientů: %d | S reporty: %d | Chybějící: %d | Chyby při načítání: %d",
                            totalClients, clientsWithReports, missingClients.size(), result.errors().size()
                    );
                    statusLabel.setText(status);
                    statusLabel.setForeground(Color.DARK_GRAY);

                    // Update tab titles with counts
                    tabbedPane.setTitleAt(0, "Chybějící reporty (" + missingClients.size() + ")");
                    tabbedPane.setTitleAt(1, "Chyby při načítání (" + result.errors().size() + ")");

                    // Enable save button
                    saveButton.setEnabled(!result.clients().isEmpty());

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("Interrupted while getting evaluation result", e);
                    statusLabel.setText("Zpracování bylo přerušeno");
                    statusLabel.setForeground(new Color(176, 0, 32));
                    saveButton.setEnabled(false);
                } catch (Exception e) {
                    log.error("Error getting evaluation result", e);
                    statusLabel.setText("Chyba při zpracování výsledků");
                    statusLabel.setForeground(new Color(176, 0, 32));
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

