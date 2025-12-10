package cz.oluwagbemiga.eutax.ui;

import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Centralizes the Swing look-and-feel selection and theming so every window stays consistent.
 */
@Slf4j
public final class UiTheme {

    // ===== COLOR PALETTE =====
    /** Primary brand color - professional blue */
    public static final Color PRIMARY = new Color(41, 98, 255);
    /** Lighter primary for hover states */
    public static final Color PRIMARY_LIGHT = new Color(77, 128, 255);
    /** Darker primary for pressed states */
    public static final Color PRIMARY_DARK = new Color(25, 72, 200);

    /** Success color - green */
    public static final Color SUCCESS = new Color(34, 139, 34);
    /** Error color - red */
    public static final Color ERROR = new Color(200, 40, 40);
    /** Warning color - orange */
    public static final Color WARNING = new Color(230, 140, 0);

    /** Background color for main content areas */
    public static final Color BG_PRIMARY = new Color(248, 250, 252);
    /** Background color for cards/panels */
    public static final Color BG_CARD = Color.WHITE;
    /** Background color for secondary sections */
    public static final Color BG_SECONDARY = new Color(241, 245, 249);

    /** Primary text color */
    public static final Color TEXT_PRIMARY = new Color(30, 41, 59);
    /** Secondary/muted text color */
    public static final Color TEXT_SECONDARY = new Color(100, 116, 139);
    /** Light text for contrasts */
    public static final Color TEXT_LIGHT = Color.WHITE;

    /** Border color for cards and inputs */
    public static final Color BORDER_COLOR = new Color(226, 232, 240);
    /** Border color for focused elements */
    public static final Color BORDER_FOCUS = PRIMARY;

    // ===== SPACING =====
    public static final int SPACING_XS = 4;
    public static final int SPACING_SM = 8;
    public static final int SPACING_MD = 16;
    public static final int SPACING_LG = 24;
    public static final int SPACING_XL = 32;

    // ===== FONTS =====
    public static Font FONT_TITLE;
    public static Font FONT_SUBTITLE;
    public static Font FONT_BODY;
    public static Font FONT_SMALL;
    public static Font FONT_BUTTON;

    // ===== BORDERS =====
    public static final int BORDER_RADIUS = 8;

    private static final List<String> LOOK_AND_FEEL_CANDIDATES = buildCandidates();
    private static boolean initialized = false;

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
                    initializeFonts();
                    applyGlobalUIDefaults();
                    return;
                }
                UIManager.setLookAndFeel(laf);
                initializeFonts();
                applyGlobalUIDefaults();
                return;
            } catch (Exception ex) {
                log.debug("Look and feel {} unavailable, trying next.", laf, ex);
            }
        }
        log.warn("Unable to apply preferred look and feel; using Swing default.");
        initializeFonts();
        applyGlobalUIDefaults();
    }

    private static void initializeFonts() {
        if (initialized) return;
        initialized = true;

        // Use a safe default font family
        String fontFamily = Font.SANS_SERIF;

        try {
            // Only try to get system fonts if not in headless mode
            if (!GraphicsEnvironment.isHeadless()) {
                Font labelFont = new JLabel().getFont();
                if (labelFont != null) {
                    fontFamily = labelFont.getFamily();
                }

                // Try to use Segoe UI on Windows for a modern look
                if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
                    Font segoe = new Font("Segoe UI", Font.PLAIN, 13);
                    if (segoe.getFamily().equals("Segoe UI")) {
                        fontFamily = "Segoe UI";
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Could not determine system font, using default", e);
        }

        FONT_TITLE = new Font(fontFamily, Font.BOLD, 22);
        FONT_SUBTITLE = new Font(fontFamily, Font.PLAIN, 15);
        FONT_BODY = new Font(fontFamily, Font.PLAIN, 13);
        FONT_SMALL = new Font(fontFamily, Font.PLAIN, 11);
        FONT_BUTTON = new Font(fontFamily, Font.BOLD, 13);
    }

    private static void applyGlobalUIDefaults() {
        UIManager.put("Panel.background", BG_PRIMARY);
        UIManager.put("Button.font", FONT_BUTTON);
        UIManager.put("Label.font", FONT_BODY);
        UIManager.put("TextField.font", FONT_BODY);
        UIManager.put("Table.font", FONT_BODY);
        UIManager.put("TabbedPane.font", FONT_BODY);
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

    // ===== STYLING UTILITIES =====

    /**
     * Creates a styled primary button (filled background).
     */
    public static JButton createPrimaryButton(String text) {
        JButton button = new JButton(text);
        button.setFont(FONT_BUTTON);
        button.setForeground(TEXT_LIGHT);
        button.setBackground(PRIMARY);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(new EmptyBorder(SPACING_SM, SPACING_LG, SPACING_SM, SPACING_LG));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(PRIMARY_LIGHT);
                }
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                button.setBackground(PRIMARY);
            }
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(PRIMARY_DARK);
                }
            }
            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(PRIMARY_LIGHT);
                }
            }
        });

        return button;
    }

    /**
     * Creates a styled secondary button (outlined).
     */
    public static JButton createSecondaryButton(String text) {
        JButton button = new JButton(text);
        button.setFont(FONT_BUTTON);
        button.setForeground(TEXT_PRIMARY);
        button.setBackground(BG_CARD);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(new CompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(SPACING_SM - 1, SPACING_LG - 1, SPACING_SM - 1, SPACING_LG - 1)
        ));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(BG_SECONDARY);
                }
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                button.setBackground(BG_CARD);
            }
        });

        return button;
    }

    /**
     * Creates a styled text field.
     */
    public static JTextField createTextField() {
        ensureInitialized();
        JTextField field = new JTextField();
        if (FONT_BODY != null) {
            field.setFont(FONT_BODY);
        }
        field.setBorder(new CompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(SPACING_SM, SPACING_SM, SPACING_SM, SPACING_SM)
        ));
        return field;
    }

    /**
     * Creates a styled password field.
     */
    public static JPasswordField createPasswordField() {
        ensureInitialized();
        JPasswordField field = new JPasswordField();
        if (FONT_BODY != null) {
            field.setFont(FONT_BODY);
        }
        field.setBorder(new CompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(SPACING_SM, SPACING_SM, SPACING_SM, SPACING_SM)
        ));
        return field;
    }

    /**
     * Ensures fonts are initialized without applying full theme.
     */
    private static void ensureInitialized() {
        if (!initialized) {
            initializeFonts();
        }
    }

    /**
     * Creates a card panel with shadow effect.
     */
    public static JPanel createCardPanel() {
        JPanel card = new JPanel();
        card.setBackground(BG_CARD);
        card.setBorder(new CompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(SPACING_LG, SPACING_LG, SPACING_LG, SPACING_LG)
        ));
        return card;
    }

    /**
     * Creates a section header label.
     */
    public static JLabel createSectionHeader(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FONT_SUBTITLE.deriveFont(Font.BOLD));
        label.setForeground(TEXT_PRIMARY);
        return label;
    }

    /**
     * Creates a description/subtitle label.
     */
    public static JLabel createDescriptionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FONT_BODY);
        label.setForeground(TEXT_SECONDARY);
        return label;
    }

    /**
     * Creates an error message label.
     */
    public static JLabel createErrorLabel() {
        JLabel label = new JLabel(" ");
        label.setFont(FONT_SMALL);
        label.setForeground(ERROR);
        return label;
    }

    /**
     * Creates a success message label.
     */
    public static JLabel createSuccessLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FONT_SMALL);
        label.setForeground(SUCCESS);
        return label;
    }

    /**
     * Creates a panel with a consistent background.
     */
    public static JPanel createBackgroundPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(BG_PRIMARY);
        return panel;
    }

    /**
     * Styles a combo box to match the theme.
     */
    public static <T> void styleComboBox(JComboBox<T> comboBox) {
        comboBox.setFont(FONT_BODY);
        comboBox.setBackground(BG_CARD);
    }

    /**
     * Styles a table to match the theme.
     */
    public static void styleTable(JTable table) {
        table.setFont(FONT_BODY);
        table.setRowHeight(SPACING_LG + SPACING_SM);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setGridColor(BORDER_COLOR);
        table.setSelectionBackground(PRIMARY_LIGHT);
        table.setSelectionForeground(TEXT_LIGHT);
        table.getTableHeader().setFont(FONT_BODY.deriveFont(Font.BOLD));
        table.getTableHeader().setBackground(BG_SECONDARY);
        table.getTableHeader().setForeground(TEXT_PRIMARY);
        table.getTableHeader().setBorder(new LineBorder(BORDER_COLOR));
    }

    /**
     * Creates a styled titled separator.
     */
    public static JPanel createTitledSeparator(String title) {
        JPanel panel = new JPanel(new BorderLayout(SPACING_SM, 0));
        panel.setOpaque(false);

        JLabel label = new JLabel(title);
        label.setFont(FONT_SMALL.deriveFont(Font.BOLD));
        label.setForeground(TEXT_SECONDARY);
        panel.add(label, BorderLayout.WEST);

        JSeparator separator = new JSeparator();
        separator.setForeground(BORDER_COLOR);
        panel.add(separator, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Creates standard content padding border.
     */
    public static Border createContentBorder() {
        return new EmptyBorder(SPACING_LG, SPACING_LG, SPACING_LG, SPACING_LG);
    }

    /**
     * Shows a confirmation dialog with Czech "Ano/Ne" buttons.
     * @return true if user clicked "Ano", false otherwise
     */
    public static boolean showConfirmDialog(Component parent, String message, String title) {
        Object[] options = {"Ano", "Ne"};
        int result = JOptionPane.showOptionDialog(
                parent,
                message,
                title,
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[1] // default to "Ne"
        );
        return result == 0; // 0 = "Ano"
    }

    /**
     * Shows a warning confirmation dialog with Czech "Ano/Ne" buttons.
     * @return true if user clicked "Ano", false otherwise
     */
    public static boolean showWarningConfirmDialog(Component parent, String message, String title) {
        Object[] options = {"Ano", "Ne"};
        int result = JOptionPane.showOptionDialog(
                parent,
                message,
                title,
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                options,
                options[1] // default to "Ne"
        );
        return result == 0; // 0 = "Ano"
    }
}
