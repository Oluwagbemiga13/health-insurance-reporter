package cz.oluwagbemiga.eutax;

import cz.oluwagbemiga.eutax.security.SecretsRepository;
import cz.oluwagbemiga.eutax.ui.KeystoreWizardWindow;
import cz.oluwagbemiga.eutax.ui.LoginWindow;
import cz.oluwagbemiga.eutax.ui.UiTheme;

import javax.swing.*;

/**
 * Entry point responsible for applying global UI settings and showing the secure login window.
 * Checks for keystore existence and guides user through creation if needed.
 */
public final class ApplicationLauncher {

    private ApplicationLauncher() {
        throw new IllegalStateException("Utility class");
    }

    public static void main(String[] args) {
        launch();
    }

    public static void launch() {
        SwingUtilities.invokeLater(() -> {
            UiTheme.apply();

            if (SecretsRepository.keystoreExists()) {
                // Keystore exists, proceed to login
                showLoginWindow();
            } else {
                // No keystore found, show creation wizard
                KeystoreWizardWindow.showCreateWizard(
                        ApplicationLauncher::showLoginWindow,
                        () -> System.exit(0)
                );
            }
        });
    }

    private static void showLoginWindow() {
        new LoginWindow(cz.oluwagbemiga.eutax.ui.StartWindow::new);
    }
}

