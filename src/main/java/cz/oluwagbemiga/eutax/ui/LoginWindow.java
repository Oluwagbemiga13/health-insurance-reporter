package cz.oluwagbemiga.eutax.ui;

import cz.oluwagbemiga.eutax.security.SecretsRepository;
import cz.oluwagbemiga.eutax.tools.GoogleWorker;
import cz.oluwagbemiga.eutax.exceptions.LoginException;
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

    private final JPasswordField passwordField = new JPasswordField(20);
    private final JLabel statusLabel = new JLabel(" ");
    private final JButton continueButton = new JButton("Přihlásit se");
    private final Runnable onSuccess;

    public LoginWindow(Runnable onSuccess) {
        super((Frame) null, "Bezpečné přihlášení", true);
        this.onSuccess = Objects.requireNonNull(onSuccess, "onSuccess");
        UiTheme.apply();
        AppIconProvider.apply(this);
        buildUi();
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void buildUi() {
        JPanel content = new JPanel(new BorderLayout());
        content.setBorder(new EmptyBorder(24, 24, 24, 24));
        content.add(buildForm(), BorderLayout.CENTER);
        content.add(buildButtons(), BorderLayout.SOUTH);
        setContentPane(content);
        getRootPane().setDefaultButton(continueButton);
    }

    private JPanel buildForm() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 8, 0);

        JLabel title = new JLabel("Zadejte heslo pro odemčení aplikace");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        panel.add(title, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(8, 0, 8, 0);
        panel.add(new JLabel("Heslo"), gbc);

        gbc.gridy++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        panel.add(passwordField, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(16, 0, 0, 0);
        statusLabel.setForeground(new Color(176, 0, 32));
        panel.add(statusLabel, gbc);
        return panel;
    }

    private JPanel buildButtons() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancel = new JButton("Ukončit");
        cancel.addActionListener(e -> System.exit(0));
        panel.add(cancel);

        continueButton.addActionListener(e -> authenticate());
        panel.add(continueButton);
        return panel;
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
                }
                catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    log.error("Login interrupted", ex);
                    showError("Přihlášení bylo přerušeno.");
                    toggleUi(true);
                }
                catch (Exception ex) {
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
    }

    private void showError(String message) {
        statusLabel.setText(message);
    }
}
