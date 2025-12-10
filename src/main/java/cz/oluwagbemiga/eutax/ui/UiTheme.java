package cz.oluwagbemiga.eutax.ui;

import lombok.extern.slf4j.Slf4j;

import javax.swing.UIManager;
import java.util.ArrayList;
import java.util.List;

/**
 * Centralizes the Swing look-and-feel selection so every window stays consistent.
 */
@Slf4j
public final class UiTheme {

    private static final List<String> LOOK_AND_FEEL_CANDIDATES = buildCandidates();

    private UiTheme() {
        throw new IllegalStateException("Utility class");
    }

    public static void apply() {
        for (String laf : LOOK_AND_FEEL_CANDIDATES) {
            try {
                String current = UIManager.getLookAndFeel() != null
                        ? UIManager.getLookAndFeel().getClass().getName()
                        : null;
                if (current != null && current.equals(laf)) {
                    return;
                }
                UIManager.setLookAndFeel(laf);
                return;
            } catch (Exception ex) {
                log.debug("Look and feel {} unavailable, trying next.", laf, ex);
            }
        }
        log.warn("Unable to apply preferred look and feel; using Swing default.");
    }

    private static List<String> buildCandidates() {
        List<String> candidates = new ArrayList<>();
        String systemLaf = UIManager.getSystemLookAndFeelClassName();
        if (systemLaf != null && !systemLaf.isBlank()) {
            candidates.add(systemLaf);
        }
        candidates.add("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
        return candidates;
    }
}
