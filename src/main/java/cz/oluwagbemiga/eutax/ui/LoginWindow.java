package cz.oluwagbemiga.eutax.ui;

import cz.oluwagbemiga.eutax.exceptions.LoginException;
import cz.oluwagbemiga.eutax.security.SecretsRepository;
import cz.oluwagbemiga.eutax.tools.GoogleWorker;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Initial window that validates the keystore password before entering the app.
 */
@Slf4j
public final class LoginWindow extends JDialog {

    private final JPasswordField passwordField = UiTheme.createPasswordField();
    private final JLabel statusLabel = UiTheme.createErrorLabel();
    private JButton continueButton;
    private JButton cancelButton;
    private JButton recreateBtn;
    private final Runnable onSuccess;

    public LoginWindow(Runnable onSuccess) {
        super((Frame) null, "Bezpečné přihlášení", true);
        this.onSuccess = Objects.requireNonNull(onSuccess, "onSuccess");
        UiTheme.apply();
        AppIconProvider.apply(this);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                System.exit(0);
            }
        });
        buildUi();
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void buildUi() {
        JPanel root = UiTheme.createBackgroundPanel();
        root.setLayout(new BorderLayout());
        root.setBorder(UiTheme.createContentBorder());

        // Main card
        JPanel card = UiTheme.createCardPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        // Lock icon
        JLabel iconLabel = new JLabel("\u2718"); // Unicode heavy ballot X as lock alternative
        iconLabel.setText("\u2630"); // trigram symbol - but let's use a simpler approach
        iconLabel.setText("[ ]"); // Simple text representation

        // Actually, let's create a custom lock icon panel instead
        JPanel iconPanel = new JPanel();
        iconPanel.setOpaque(false);
        iconPanel.setPreferredSize(new Dimension(60, 60));
        iconPanel.setMaximumSize(new Dimension(60, 60));
        iconPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        iconPanel.setBorder(BorderFactory.createLineBorder(UiTheme.PRIMARY, 3, true));
        JLabel lockText = new JLabel("\u00B7\u00B7\u00B7"); // centered dots as password symbol
        lockText.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        lockText.setForeground(UiTheme.PRIMARY);
        iconPanel.add(lockText);
        card.add(iconPanel);
        card.add(Box.createVerticalStrut(UiTheme.SPACING_MD));

        // Title
        JLabel title = new JLabel("Bezpečné přihlášení");
        title.setFont(UiTheme.FONT_TITLE);
        title.setForeground(UiTheme.TEXT_PRIMARY);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(title);
        card.add(Box.createVerticalStrut(UiTheme.SPACING_XS));

        // Subtitle
        JLabel subtitle = UiTheme.createDescriptionLabel("Zadejte heslo pro odemčení aplikace");
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(subtitle);
        card.add(Box.createVerticalStrut(UiTheme.SPACING_LG));

        // Password field section
        JPanel formPanel = new JPanel();
        formPanel.setOpaque(false);
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        formPanel.setMaximumSize(new Dimension(300, 80));

        JLabel passwordLabel = new JLabel("Heslo");
        passwordLabel.setFont(UiTheme.FONT_BODY);
        passwordLabel.setForeground(UiTheme.TEXT_PRIMARY);
        passwordLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        formPanel.add(passwordLabel);
        formPanel.add(Box.createVerticalStrut(UiTheme.SPACING_XS));

        passwordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        passwordField.setAlignmentX(Component.LEFT_ALIGNMENT);
        formPanel.add(passwordField);

        card.add(formPanel);
        card.add(Box.createVerticalStrut(UiTheme.SPACING_SM));

        // Status label
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(statusLabel);
        card.add(Box.createVerticalStrut(UiTheme.SPACING_LG));

        // Login button
        continueButton = UiTheme.createPrimaryButton("Přihlásit se");
        continueButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        continueButton.setMaximumSize(new Dimension(200, 44));
        continueButton.addActionListener(e -> authenticate());
        card.add(continueButton);

        root.add(card, BorderLayout.CENTER);

        // Bottom buttons
        root.add(buildButtons(), BorderLayout.SOUTH);

        setContentPane(root);
        getRootPane().setDefaultButton(continueButton);
        setMinimumSize(new Dimension(420, 380));
    }

    private JPanel buildButtons() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(UiTheme.SPACING_MD, 0, 0, 0));

        // Left side - recreate keystore button
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftPanel.setOpaque(false);
        recreateBtn = UiTheme.createSecondaryButton("Nový klíč");
        recreateBtn.setToolTipText("Vytvořit nové úložiště klíčů (např. při expiraci)");
        recreateBtn.addActionListener(e -> recreateKeystore());
        leftPanel.add(recreateBtn);

        // Right side - cancel button
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightPanel.setOpaque(false);
        cancelButton = UiTheme.createSecondaryButton("Ukončit");
        cancelButton.addActionListener(e -> System.exit(0));
        rightPanel.add(cancelButton);

        panel.add(leftPanel, BorderLayout.WEST);
        panel.add(rightPanel, BorderLayout.EAST);
        return panel;
    }

    private void recreateKeystore() {
        KeystoreWizardWindow.showRecreateWizard(null, () -> {
            passwordField.setText("");
            statusLabel.setText("Úložiště bylo obnoveno. Zadejte nové heslo.");
            statusLabel.setForeground(UiTheme.SUCCESS);
        });
    }

    private void authenticate() {
        char[] password = passwordField.getPassword();
        if (password.length == 0) {
            showError("Heslo nesmí být prázdné.");
            return;
        }
        runAsync(() -> SecretsRepository.loadGoogleServiceAccountJson(password));
    }

    private void runAsync(Supplier<String> loader) {
        toggleUi(false);
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                try {
                    return loader.get();
                } catch (Exception ex) {
                    log.warn("Failed to unlock secrets", ex);
                    throw ex;
                }
            }

            @Override
            protected void done() {
                try {
                    String json = get();
                    GoogleWorker.setGoogleServiceAccountJson(json);
                    dispose();
                    onSuccess.run();
                } catch (LoginException ex) {
                    showError(ex.getMessage());
                    passwordField.setText("");
                    toggleUi(true);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    log.error("Login interrupted", ex);
                    showError("Přihlášení bylo přerušeno.");
                    toggleUi(true);
                } catch (Exception ex) {
                    log.error("Unexpected error during login", ex);
                    showError("Heslo není správné nebo došlo k neočekávané chybě.");
                    passwordField.setText("");
                    toggleUi(true);
                }
            }
        };
        worker.execute();
    }

    private void toggleUi(boolean enabled) {
        passwordField.setEnabled(enabled);
        continueButton.setEnabled(enabled);
        cancelButton.setEnabled(enabled);
        recreateBtn.setEnabled(enabled);
    }

    private void showError(String message) {
        statusLabel.setText(message);
        statusLabel.setForeground(UiTheme.ERROR);
    }
}
