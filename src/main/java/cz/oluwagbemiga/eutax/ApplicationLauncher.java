package cz.oluwagbemiga.eutax;

import cz.oluwagbemiga.eutax.ui.LoginWindow;
import cz.oluwagbemiga.eutax.ui.UiTheme;

import javax.swing.*;

/**
 * Entry point responsible for applying global UI settings and showing the secure login window.
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
            new LoginWindow(cz.oluwagbemiga.eutax.ui.StartWindow::new);
        });
    }
}

