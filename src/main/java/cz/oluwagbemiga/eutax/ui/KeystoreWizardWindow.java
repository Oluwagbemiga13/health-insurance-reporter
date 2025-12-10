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
    private final JButton backButton = new JButton("Zpět");
    private final JButton nextButton = new JButton("Další");
    private final JButton cancelButton = new JButton("Zrušit");

    // Step 1: File selection components
    private final JTextField filePathField = new JTextField(30);
    private final JLabel fileErrorLabel = new JLabel(" ");

    // Step 2: Password components
    private final JPasswordField passwordField = new JPasswordField(20);
    private final JPasswordField confirmPasswordField = new JPasswordField(20);
    private final JLabel passwordErrorLabel = new JLabel(" ");

    // Step 3: Result components
    private final JLabel resultLabel = new JLabel(" ");
    private final JLabel resultDetailLabel = new JLabel(" ");

    /**
     * Creates a new keystore wizard window.
     *
     * @param owner     the parent frame
     * @param onSuccess callback when keystore is successfully created
     * @param onCancel  callback when user cancels the wizard
     * @param isRecreate true if recreating an existing keystore
     */
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
        JPanel content = new JPanel(new BorderLayout());
        content.setBorder(new EmptyBorder(16, 24, 16, 24));

        cardPanel.add(buildWelcomePanel(), String.valueOf(STEP_WELCOME));
        cardPanel.add(buildFileSelectionPanel(), String.valueOf(STEP_SELECT_FILE));
        cardPanel.add(buildPasswordPanel(), String.valueOf(STEP_SET_PASSWORD));
        cardPanel.add(buildResultPanel(), String.valueOf(STEP_RESULT));

        content.add(cardPanel, BorderLayout.CENTER);
        content.add(buildButtonPanel(), BorderLayout.SOUTH);

        setContentPane(content);
        setMinimumSize(new Dimension(500, 350));
        updateButtons();
    }

    private JPanel buildWelcomePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(0, 0, 16, 0);

        JLabel title = new JLabel(isRecreate ? "Obnovení úložiště klíčů" : "Vítejte v průvodci nastavením");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        panel.add(title, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 24, 0);
        String descText = isRecreate
                ? "<html><div style='width:380px; text-align:center;'>"
                  + "Tento průvodce vám pomůže vytvořit nové úložiště klíčů. "
                  + "Budete potřebovat JSON soubor s přihlašovacími údaji ke Google účtu služby."
                  + "</div></html>"
                : "<html><div style='width:380px; text-align:center;'>"
                  + "Pro spuštění aplikace je potřeba vytvořit zabezpečené úložiště klíčů. "
                  + "Budete potřebovat JSON soubor s přihlašovacími údaji ke Google účtu služby."
                  + "</div></html>";
        JLabel desc = new JLabel(descText);
        desc.setFont(desc.getFont().deriveFont(Font.PLAIN, 13f));
        panel.add(desc, gbc);

        gbc.gridy++;
        JLabel stepInfo = new JLabel("<html><div style='width:380px;'><b>Kroky:</b><ol>"
                + "<li>Vyberte JSON soubor s přihlašovacími údaji</li>"
                + "<li>Nastavte heslo pro ochranu úložiště</li>"
                + "<li>Dokončete vytvoření</li></ol></div></html>");
        stepInfo.setFont(stepInfo.getFont().deriveFont(Font.PLAIN, 12f));
        panel.add(stepInfo, gbc);

        return panel;
    }

    private JPanel buildFileSelectionPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 16, 0);

        JLabel title = new JLabel("Vyberte JSON soubor");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        panel.add(title, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 8, 0);
        JLabel desc = new JLabel("<html><div style='width:380px;'>"
                + "Vyberte JSON soubor s přihlašovacími údaji ke Google účtu služby. "
                + "Tento soubor jste obdrželi při vytváření účtu služby v Google Cloud Console."
                + "</div></html>");
        desc.setFont(desc.getFont().deriveFont(Font.PLAIN, 12f));
        panel.add(desc, gbc);

        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.insets = new Insets(16, 0, 8, 8);
        filePathField.setEditable(false);
        panel.add(filePathField, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(16, 0, 8, 0);
        JButton browseButton = new JButton("Procházet...");
        browseButton.addActionListener(e -> selectJsonFile());
        panel.add(browseButton, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(8, 0, 0, 0);
        fileErrorLabel.setForeground(new Color(176, 0, 32));
        panel.add(fileErrorLabel, gbc);

        return panel;
    }

    private JPanel buildPasswordPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 16, 0);

        JLabel title = new JLabel("Nastavte heslo");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        panel.add(title, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 16, 0);
        JLabel desc = new JLabel("<html><div style='width:380px;'>"
                + "Zvolte silné heslo pro ochranu vašeho úložiště klíčů. "
                + "Toto heslo budete zadávat při každém spuštění aplikace."
                + "</div></html>");
        desc.setFont(desc.getFont().deriveFont(Font.PLAIN, 12f));
        panel.add(desc, gbc);

        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(8, 0, 4, 8);
        panel.add(new JLabel("Heslo:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.insets = new Insets(8, 0, 4, 0);
        panel.add(passwordField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        gbc.insets = new Insets(4, 0, 4, 8);
        panel.add(new JLabel("Potvrzení hesla:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.insets = new Insets(4, 0, 4, 0);
        panel.add(confirmPasswordField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(16, 0, 0, 0);
        passwordErrorLabel.setForeground(new Color(176, 0, 32));
        panel.add(passwordErrorLabel, gbc);

        return panel;
    }

    private JPanel buildResultPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(0, 0, 16, 0);

        resultLabel.setFont(resultLabel.getFont().deriveFont(Font.BOLD, 18f));
        panel.add(resultLabel, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 8, 0);
        resultDetailLabel.setFont(resultDetailLabel.getFont().deriveFont(Font.PLAIN, 12f));
        panel.add(resultDetailLabel, gbc);

        return panel;
    }

    private JPanel buildButtonPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(16, 0, 0, 0));

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        cancelButton.addActionListener(e -> handleCancel());
        leftPanel.add(cancelButton);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        backButton.addActionListener(e -> goBack());
        rightPanel.add(backButton);

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

                    // Check if file exists and warn user
                    if (outputFile.exists() && isRecreate) {
                        // In recreate mode, we proceed with overwriting
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

                if (errorMessage != null) {
                    resultLabel.setText("Chyba při vytváření úložiště");
                    resultLabel.setForeground(new Color(176, 0, 32));
                    resultDetailLabel.setText("<html><div style='width:380px; text-align:center;'>" + errorMessage + "</div></html>");
                    backButton.setEnabled(true);
                } else {
                    resultLabel.setText("Úložiště bylo úspěšně vytvořeno!");
                    resultLabel.setForeground(new Color(0, 128, 0));
                    File keystoreFile = SecretsRepository.getExternalKeystoreFile();
                    resultDetailLabel.setText("<html><div style='width:380px; text-align:center;'>"
                            + "Soubor: " + keystoreFile.getAbsolutePath()
                            + "</div></html>");
                }

                currentStep = STEP_RESULT;
                cardLayout.show(cardPanel, String.valueOf(currentStep));
                updateButtons();
                toggleUi(true);
            }
        };

        worker.execute();
    }

    private void handleCancel() {
        int result = JOptionPane.showConfirmDialog(
                this,
                "Opravdu chcete zrušit průvodce?",
                "Potvrzení",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
        if (result == JOptionPane.YES_OPTION) {
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
            int confirm = JOptionPane.showConfirmDialog(
                    owner,
                    "Existující úložiště klíčů bude přepsáno. Pokračovat?",
                    "Upozornění",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }
        }
        KeystoreWizardWindow wizard = new KeystoreWizardWindow(owner, onSuccess, () -> {}, true);
        wizard.showWizard();
    }
}

