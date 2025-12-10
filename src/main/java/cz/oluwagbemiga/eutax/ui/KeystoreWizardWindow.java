package cz.oluwagbemiga.eutax.ui;

import cz.oluwagbemiga.eutax.exceptions.LoginException;
import cz.oluwagbemiga.eutax.security.KeystoreCreator;
import cz.oluwagbemiga.eutax.security.SecretsRepository;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.util.Arrays;
import java.util.Objects;

/**
 * Wizard dialog that guides users through creating a PKCS12 keystore from a JSON file.
 */
@Slf4j
public final class KeystoreWizardWindow extends JDialog {

    private static final int STEP_WELCOME = 0;
    private static final int STEP_SELECT_FILE = 1;
    private static final int STEP_SET_PASSWORD = 2;
    private static final int STEP_RESULT = 3;

    private final Runnable onSuccess;
    private final Runnable onCancel;
    private final boolean isRecreate;

    private int currentStep = STEP_WELCOME;
    private File selectedJsonFile;
    private char[] enteredPassword;

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardPanel = new JPanel(cardLayout);
    private JButton backButton;
    private JButton nextButton;
    private JButton cancelButton;

    // Step indicators
    private JPanel stepIndicatorPanel;

    // Step 1: File selection components
    private final JTextField filePathField = UiTheme.createTextField();
    private final JLabel fileErrorLabel = UiTheme.createErrorLabel();

    // Step 2: Password components
    private final JPasswordField passwordField = UiTheme.createPasswordField();
    private final JPasswordField confirmPasswordField = UiTheme.createPasswordField();
    private final JLabel passwordErrorLabel = UiTheme.createErrorLabel();

    // Step 3: Result components
    private final JLabel resultLabel = new JLabel(" ");
    private final JLabel resultDetailLabel = new JLabel(" ");

    public KeystoreWizardWindow(Frame owner, Runnable onSuccess, Runnable onCancel, boolean isRecreate) {
        super(owner, isRecreate ? "Obnovení úložiště klíčů" : "Vytvoření úložiště klíčů", true);
        this.onSuccess = Objects.requireNonNull(onSuccess, "onSuccess");
        this.onCancel = onCancel != null ? onCancel : () -> {};
        this.isRecreate = isRecreate;

        UiTheme.apply();
        AppIconProvider.apply(this);
        buildUi();
        pack();
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                handleCancel();
            }
        });
    }

    public void showWizard() {
        setVisible(true);
    }

    private void buildUi() {
        JPanel root = UiTheme.createBackgroundPanel();
        root.setLayout(new BorderLayout());
        root.setBorder(UiTheme.createContentBorder());

        // Step indicator at top
        stepIndicatorPanel = createStepIndicator();
        root.add(stepIndicatorPanel, BorderLayout.NORTH);

        // Card panel for wizard steps
        cardPanel.setOpaque(false);
        cardPanel.add(buildWelcomePanel(), String.valueOf(STEP_WELCOME));
        cardPanel.add(buildFileSelectionPanel(), String.valueOf(STEP_SELECT_FILE));
        cardPanel.add(buildPasswordPanel(), String.valueOf(STEP_SET_PASSWORD));
        cardPanel.add(buildResultPanel(), String.valueOf(STEP_RESULT));

        root.add(cardPanel, BorderLayout.CENTER);
        root.add(buildButtonPanel(), BorderLayout.SOUTH);

        setContentPane(root);
        setMinimumSize(new Dimension(520, 420));
        updateButtons();
        updateStepIndicator();
    }

    private JPanel createStepIndicator() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, UiTheme.SPACING_LG, 0));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(0, 0, UiTheme.SPACING_LG, 0));

        String[] steps = {"Úvod", "Soubor", "Heslo", "Hotovo"};
        for (int i = 0; i < steps.length; i++) {
            JPanel stepPanel = new JPanel();
            stepPanel.setOpaque(false);
            stepPanel.setLayout(new BoxLayout(stepPanel, BoxLayout.Y_AXIS));

            JLabel numberLabel = new JLabel(String.valueOf(i + 1), SwingConstants.CENTER);
            numberLabel.setOpaque(true);
            numberLabel.setPreferredSize(new Dimension(28, 28));
            numberLabel.setMinimumSize(new Dimension(28, 28));
            numberLabel.setMaximumSize(new Dimension(28, 28));
            numberLabel.setHorizontalAlignment(SwingConstants.CENTER);
            numberLabel.setFont(UiTheme.FONT_BODY.deriveFont(Font.BOLD));
            numberLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            numberLabel.setName("step-" + i);

            JLabel textLabel = new JLabel(steps[i]);
            textLabel.setFont(UiTheme.FONT_SMALL);
            textLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            textLabel.setName("step-text-" + i);

            stepPanel.add(numberLabel);
            stepPanel.add(Box.createVerticalStrut(UiTheme.SPACING_XS));
            stepPanel.add(textLabel);

            panel.add(stepPanel);

            if (i < steps.length - 1) {
                JLabel arrow = new JLabel("→");
                arrow.setFont(UiTheme.FONT_BODY);
                arrow.setForeground(UiTheme.TEXT_SECONDARY);
                panel.add(arrow);
            }
        }

        return panel;
    }

    private void updateStepIndicator() {
        for (Component comp : stepIndicatorPanel.getComponents()) {
            if (comp instanceof JPanel stepPanel) {
                for (Component inner : stepPanel.getComponents()) {
                    if (inner instanceof JLabel label && label.getName() != null) {
                        if (label.getName().startsWith("step-text-")) {
                            int stepIndex = Integer.parseInt(label.getName().replace("step-text-", ""));
                            label.setForeground(stepIndex <= currentStep ? UiTheme.PRIMARY : UiTheme.TEXT_SECONDARY);
                        } else if (label.getName().startsWith("step-")) {
                            int stepIndex = Integer.parseInt(label.getName().replace("step-", ""));
                            if (stepIndex < currentStep) {
                                label.setBackground(UiTheme.SUCCESS);
                                label.setForeground(UiTheme.TEXT_LIGHT);
                                label.setText("OK");
                            } else if (stepIndex == currentStep) {
                                label.setBackground(UiTheme.PRIMARY);
                                label.setForeground(UiTheme.TEXT_LIGHT);
                                label.setText(String.valueOf(stepIndex + 1));
                            } else {
                                label.setBackground(UiTheme.BG_SECONDARY);
                                label.setForeground(UiTheme.TEXT_SECONDARY);
                                label.setText(String.valueOf(stepIndex + 1));
                            }
                        }
                    }
                }
            }
        }
    }

    private JPanel buildWelcomePanel() {
        JPanel panel = UiTheme.createCardPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // Icon - using a styled panel with text
        JPanel iconPanel = createIconPanel("1");
        panel.add(iconPanel);
        panel.add(Box.createVerticalStrut(UiTheme.SPACING_MD));

        // Title
        JLabel title = new JLabel(isRecreate ? "Obnovení úložiště klíčů" : "Vítejte v průvodci nastavením");
        title.setFont(UiTheme.FONT_TITLE);
        title.setForeground(UiTheme.TEXT_PRIMARY);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(title);
        panel.add(Box.createVerticalStrut(UiTheme.SPACING_MD));

        // Description
        String descText = isRecreate
                ? "Tento průvodce vám pomůže vytvořit nové úložiště klíčů. Budete potřebovat JSON soubor s přihlašovacími údaji ke Google účtu služby."
                : "Pro spuštění aplikace je potřeba vytvořit zabezpečené úložiště klíčů. Budete potřebovat JSON soubor s přihlašovacími údaji ke Google účtu služby.";
        JLabel desc = new JLabel("<html><div style='text-align: center; width: 350px;'>" + descText + "</div></html>");
        desc.setFont(UiTheme.FONT_BODY);
        desc.setForeground(UiTheme.TEXT_SECONDARY);
        desc.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(desc);
        panel.add(Box.createVerticalStrut(UiTheme.SPACING_LG));

        // Steps info
        JPanel stepsInfo = new JPanel();
        stepsInfo.setOpaque(false);
        stepsInfo.setLayout(new BoxLayout(stepsInfo, BoxLayout.Y_AXIS));
        stepsInfo.setAlignmentX(Component.CENTER_ALIGNMENT);

        String[] steps = {
            "1. Vyberte JSON soubor s přihlašovacími údaji",
            "2. Nastavte heslo pro ochranu úložiště",
            "3. Dokončete vytvoření"
        };
        for (String step : steps) {
            JLabel stepLabel = new JLabel("  " + step);
            stepLabel.setFont(UiTheme.FONT_BODY);
            stepLabel.setForeground(UiTheme.TEXT_PRIMARY);
            stepLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            stepsInfo.add(stepLabel);
            stepsInfo.add(Box.createVerticalStrut(UiTheme.SPACING_XS));
        }
        panel.add(stepsInfo);

        return panel;
    }

    private JPanel buildFileSelectionPanel() {
        JPanel panel = UiTheme.createCardPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // Icon - using a styled panel with text
        JPanel iconPanel = createIconPanel("2");
        panel.add(iconPanel);
        panel.add(Box.createVerticalStrut(UiTheme.SPACING_SM));

        // Title
        JLabel title = new JLabel("Vyberte JSON soubor");
        title.setFont(UiTheme.FONT_TITLE);
        title.setForeground(UiTheme.TEXT_PRIMARY);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(title);
        panel.add(Box.createVerticalStrut(UiTheme.SPACING_SM));

        // Description
        JLabel desc = new JLabel("<html><div style='text-align: center; width: 380px;'>Vyberte JSON soubor s přihlašovacími údaji ke Google účtu služby. Tento soubor jste obdrželi při vytváření účtu služby v Google Cloud Console.</div></html>");
        desc.setFont(UiTheme.FONT_BODY);
        desc.setForeground(UiTheme.TEXT_SECONDARY);
        desc.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(desc);
        panel.add(Box.createVerticalStrut(UiTheme.SPACING_LG));

        // File picker
        JPanel pickerPanel = new JPanel(new BorderLayout(UiTheme.SPACING_SM, 0));
        pickerPanel.setOpaque(false);
        pickerPanel.setMaximumSize(new Dimension(400, 40));
        pickerPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        filePathField.setEditable(false);
        pickerPanel.add(filePathField, BorderLayout.CENTER);

        JButton browseButton = UiTheme.createSecondaryButton("Procházet...");
        browseButton.addActionListener(e -> selectJsonFile());
        pickerPanel.add(browseButton, BorderLayout.EAST);

        panel.add(pickerPanel);
        panel.add(Box.createVerticalStrut(UiTheme.SPACING_SM));

        // Error label
        fileErrorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(fileErrorLabel);

        return panel;
    }

    private JPanel buildPasswordPanel() {
        JPanel panel = UiTheme.createCardPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // Icon - using a styled panel with text
        JPanel iconPanel = createIconPanel("3");
        panel.add(iconPanel);
        panel.add(Box.createVerticalStrut(UiTheme.SPACING_SM));

        // Title
        JLabel title = new JLabel("Nastavte heslo");
        title.setFont(UiTheme.FONT_TITLE);
        title.setForeground(UiTheme.TEXT_PRIMARY);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(title);
        panel.add(Box.createVerticalStrut(UiTheme.SPACING_SM));

        // Description
        JLabel desc = new JLabel("<html><div style='text-align: center; width: 380px;'>Zvolte silné heslo pro ochranu vašeho úložiště klíčů. Toto heslo budete zadávat při každém spuštění aplikace.</div></html>");
        desc.setFont(UiTheme.FONT_BODY);
        desc.setForeground(UiTheme.TEXT_SECONDARY);
        desc.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(desc);
        panel.add(Box.createVerticalStrut(UiTheme.SPACING_LG));

        // Form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        formPanel.setMaximumSize(new Dimension(350, 120));
        formPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(UiTheme.SPACING_XS, 0, UiTheme.SPACING_XS, UiTheme.SPACING_SM);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        JLabel passLabel = new JLabel("Heslo:");
        passLabel.setFont(UiTheme.FONT_BODY);
        formPanel.add(passLabel, gbc);

        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
        formPanel.add(passwordField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        JLabel confirmLabel = new JLabel("Potvrzení:");
        confirmLabel.setFont(UiTheme.FONT_BODY);
        formPanel.add(confirmLabel, gbc);

        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
        formPanel.add(confirmPasswordField, gbc);

        panel.add(formPanel);
        panel.add(Box.createVerticalStrut(UiTheme.SPACING_SM));

        // Error label
        passwordErrorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(passwordErrorLabel);

        return panel;
    }

    private JPanel buildResultPanel() {
        JPanel panel = UiTheme.createCardPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // Result icon (will be updated dynamically) - using styled panel
        JPanel iconPanel = createIconPanel("4");
        iconPanel.setName("result-icon-panel");
        panel.add(iconPanel);
        panel.add(Box.createVerticalStrut(UiTheme.SPACING_MD));

        // Result label
        resultLabel.setFont(UiTheme.FONT_TITLE);
        resultLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(resultLabel);
        panel.add(Box.createVerticalStrut(UiTheme.SPACING_SM));

        // Detail label
        resultDetailLabel.setFont(UiTheme.FONT_BODY);
        resultDetailLabel.setForeground(UiTheme.TEXT_SECONDARY);
        resultDetailLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(resultDetailLabel);

        return panel;
    }

    private JPanel buildButtonPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(UiTheme.SPACING_LG, 0, 0, 0));

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftPanel.setOpaque(false);
        cancelButton = UiTheme.createSecondaryButton("Zrušit");
        cancelButton.addActionListener(e -> handleCancel());
        leftPanel.add(cancelButton);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, UiTheme.SPACING_SM, 0));
        rightPanel.setOpaque(false);

        backButton = UiTheme.createSecondaryButton("Zpět");
        backButton.addActionListener(e -> goBack());
        rightPanel.add(backButton);

        nextButton = UiTheme.createPrimaryButton("Další");
        nextButton.addActionListener(e -> goNext());
        rightPanel.add(nextButton);

        panel.add(leftPanel, BorderLayout.WEST);
        panel.add(rightPanel, BorderLayout.EAST);
        return panel;
    }

    private void updateButtons() {
        backButton.setEnabled(currentStep > STEP_WELCOME && currentStep < STEP_RESULT);

        if (currentStep == STEP_RESULT) {
            nextButton.setText("Dokončit");
        } else if (currentStep == STEP_SET_PASSWORD) {
            nextButton.setText("Vytvořit");
        } else {
            nextButton.setText("Další");
        }

        cancelButton.setVisible(currentStep != STEP_RESULT);
    }

    private void goBack() {
        if (currentStep > STEP_WELCOME) {
            currentStep--;
            cardLayout.show(cardPanel, String.valueOf(currentStep));
            updateButtons();
            updateStepIndicator();
        }
    }

    private void goNext() {
        switch (currentStep) {
            case STEP_WELCOME -> {
                currentStep = STEP_SELECT_FILE;
                cardLayout.show(cardPanel, String.valueOf(currentStep));
            }
            case STEP_SELECT_FILE -> {
                if (validateFileSelection()) {
                    currentStep = STEP_SET_PASSWORD;
                    cardLayout.show(cardPanel, String.valueOf(currentStep));
                }
            }
            case STEP_SET_PASSWORD -> {
                if (validatePassword()) {
                    createKeystore();
                }
            }
            case STEP_RESULT -> {
                dispose();
                onSuccess.run();
            }
        }
        updateButtons();
        updateStepIndicator();
    }

    private void selectJsonFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Vyberte JSON soubor s přihlašovacími údaji");
        chooser.setFileFilter(new FileNameExtensionFilter("JSON soubory (*.json)", "json"));
        chooser.setAcceptAllFileFilterUsed(false);

        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            selectedJsonFile = chooser.getSelectedFile();
            filePathField.setText(selectedJsonFile.getAbsolutePath());
            fileErrorLabel.setText(" ");
        }
    }

    private boolean validateFileSelection() {
        if (selectedJsonFile == null || !selectedJsonFile.exists()) {
            fileErrorLabel.setText("Vyberte platný JSON soubor.");
            return false;
        }
        if (!selectedJsonFile.getName().toLowerCase().endsWith(".json")) {
            fileErrorLabel.setText("Vybraný soubor musí být JSON soubor.");
            return false;
        }
        fileErrorLabel.setText(" ");
        return true;
    }

    private boolean validatePassword() {
        char[] password = passwordField.getPassword();
        char[] confirm = confirmPasswordField.getPassword();

        if (password.length == 0) {
            passwordErrorLabel.setText("Heslo nesmí být prázdné.");
            return false;
        }
        if (password.length < 6) {
            passwordErrorLabel.setText("Heslo musí mít alespoň 6 znaků.");
            clearPasswords(password, confirm);
            return false;
        }
        if (!Arrays.equals(password, confirm)) {
            passwordErrorLabel.setText("Hesla se neshodují.");
            clearPasswords(password, confirm);
            return false;
        }

        enteredPassword = password;
        clearPasswords(confirm);
        passwordErrorLabel.setText(" ");
        return true;
    }

    private void createKeystore() {
        toggleUi(false);

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private String errorMessage = null;

            @Override
            protected Void doInBackground() {
                try {
                    File outputFile = SecretsRepository.getExternalKeystoreFile();

                    if (outputFile.exists() && isRecreate) {
                        log.info("Overwriting existing keystore at: {}", outputFile.getAbsolutePath());
                    }

                    KeystoreCreator.createKeystoreFromJson(selectedJsonFile, outputFile, enteredPassword);
                    log.info("Keystore created successfully at: {}", outputFile.getAbsolutePath());

                } catch (LoginException ex) {
                    log.error("Failed to create keystore", ex);
                    errorMessage = ex.getMessage();
                } catch (Exception ex) {
                    log.error("Unexpected error creating keystore", ex);
                    errorMessage = "Neočekávaná chyba: " + ex.getMessage();
                }
                return null;
            }

            @Override
            protected void done() {
                clearPasswords(enteredPassword);
                enteredPassword = null;

                // Update result icon panel color
                for (Component comp : cardPanel.getComponents()) {
                    if (comp instanceof JPanel panel) {
                        for (Component inner : panel.getComponents()) {
                            if (inner instanceof JPanel iconPanel && "result-icon-panel".equals(iconPanel.getName())) {
                                Color bgColor = errorMessage != null ? UiTheme.ERROR : UiTheme.SUCCESS;
                                iconPanel.setBackground(bgColor);
                                iconPanel.setBorder(BorderFactory.createLineBorder(bgColor, 3, true));
                                for (Component c : iconPanel.getComponents()) {
                                    if (c instanceof JLabel lbl) {
                                        lbl.setText(errorMessage != null ? "X" : "OK");
                                    }
                                }
                            }
                        }
                    }
                }

                if (errorMessage != null) {
                    resultLabel.setText("Chyba při vytváření úložiště");
                    resultLabel.setForeground(UiTheme.ERROR);
                    resultDetailLabel.setText("<html><div style='width:380px; text-align:center;'>" + errorMessage + "</div></html>");
                    backButton.setEnabled(true);
                } else {
                    resultLabel.setText("Úložiště bylo úspěšně vytvořeno!");
                    resultLabel.setForeground(UiTheme.SUCCESS);
                    File keystoreFile = SecretsRepository.getExternalKeystoreFile();
                    resultDetailLabel.setText("<html><div style='width:380px; text-align:center;'>Soubor: " + keystoreFile.getAbsolutePath() + "</div></html>");
                }

                currentStep = STEP_RESULT;
                cardLayout.show(cardPanel, String.valueOf(currentStep));
                updateButtons();
                updateStepIndicator();
                toggleUi(true);
            }
        };

        worker.execute();
    }

    /**
     * Creates a styled icon panel with a step number for the wizard.
     */
    private JPanel createIconPanel(String text) {
        JPanel iconPanel = new JPanel(new GridBagLayout());
        iconPanel.setBackground(UiTheme.PRIMARY);
        iconPanel.setPreferredSize(new Dimension(50, 50));
        iconPanel.setMaximumSize(new Dimension(50, 50));
        iconPanel.setMinimumSize(new Dimension(50, 50));
        iconPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        iconPanel.setBorder(BorderFactory.createLineBorder(UiTheme.PRIMARY, 3, true));

        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        label.setForeground(UiTheme.TEXT_LIGHT);
        iconPanel.add(label);

        return iconPanel;
    }

    private void handleCancel() {
        boolean confirmed = UiTheme.showConfirmDialog(
                this,
                "Opravdu chcete zrušit průvodce?",
                "Potvrzení"
        );
        if (confirmed) {
            clearPasswords(enteredPassword, passwordField.getPassword(), confirmPasswordField.getPassword());
            dispose();
            onCancel.run();
        }
    }

    private void toggleUi(boolean enabled) {
        backButton.setEnabled(enabled && currentStep > STEP_WELCOME);
        nextButton.setEnabled(enabled);
        cancelButton.setEnabled(enabled);
        passwordField.setEnabled(enabled);
        confirmPasswordField.setEnabled(enabled);
    }

    private void clearPasswords(char[]... passwords) {
        for (char[] password : passwords) {
            if (password != null) {
                Arrays.fill(password, '\0');
            }
        }
    }

    /**
     * Shows the keystore wizard for initial creation.
     *
     * @param onSuccess callback when keystore is created successfully
     * @param onCancel  callback when user cancels
     */
    public static void showCreateWizard(Runnable onSuccess, Runnable onCancel) {
        KeystoreWizardWindow wizard = new KeystoreWizardWindow(null, onSuccess, onCancel, false);
        wizard.showWizard();
    }

    /**
     * Shows the keystore wizard for recreating an existing keystore.
     *
     * @param owner     the parent frame
     * @param onSuccess callback when keystore is recreated successfully
     */
    public static void showRecreateWizard(Frame owner, Runnable onSuccess) {
        File existingFile = SecretsRepository.getExternalKeystoreFile();
        if (existingFile.exists()) {
            boolean confirmed = UiTheme.showWarningConfirmDialog(
                    owner,
                    "Existující úložiště klíčů bude přepsáno. Pokračovat?",
                    "Upozornění"
            );
            if (!confirmed) {
                return;
            }
        }
        KeystoreWizardWindow wizard = new KeystoreWizardWindow(owner, onSuccess, () -> {}, true);
        wizard.showWizard();
    }
}

