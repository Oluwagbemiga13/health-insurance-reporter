package cz.oluwagbemiga.eutax.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class StartWindow extends JFrame {

    public StartWindow() {
        UiTheme.apply();
        AppIconProvider.apply(this);
        buildUi();
    }

    private void buildUi() {
        setTitle("Kontrola přehledů zdravotního pojištění");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Main background panel
        JPanel root = UiTheme.createBackgroundPanel();
        root.setLayout(new GridBagLayout());

        // Card panel with content
        JPanel card = UiTheme.createCardPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(UiTheme.SPACING_XL, UiTheme.SPACING_XL + 16, UiTheme.SPACING_XL, UiTheme.SPACING_XL + 16));

        // Icon/Logo area (optional visual enhancement)
        JPanel headerPanel = new JPanel();
        headerPanel.setOpaque(false);
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Health/Document icon using a styled box
        JLabel iconLabel = new JLabel("\uD83D\uDDCE"); // Medical symbol (caduceus)
        iconLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 42));
        iconLabel.setForeground(UiTheme.PRIMARY);
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerPanel.add(iconLabel);
        headerPanel.add(Box.createVerticalStrut(UiTheme.SPACING_MD));

        card.add(headerPanel);

        // Title
        JLabel title = new JLabel("Kontrola přehledů zdravotního pojištění");
        title.setFont(UiTheme.FONT_TITLE);
        title.setForeground(UiTheme.TEXT_PRIMARY);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(title);
        card.add(Box.createVerticalStrut(UiTheme.SPACING_SM));

        // Subtitle
        JLabel subtitle = new JLabel("<html><div style='text-align: center; width: 350px;'>Snadno kontrolujte a spravujte reporty pro zdravotní pojištění vašich klientů.</div></html>");
        subtitle.setFont(UiTheme.FONT_SUBTITLE);
        subtitle.setForeground(UiTheme.TEXT_SECONDARY);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(subtitle);
        card.add(Box.createVerticalStrut(UiTheme.SPACING_XL));

        // Separator
        JSeparator separator = new JSeparator();
        separator.setForeground(UiTheme.BORDER_COLOR);
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        card.add(separator);
        card.add(Box.createVerticalStrut(UiTheme.SPACING_LG));

        // Buttons panel
        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setOpaque(false);
        buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.Y_AXIS));
        buttonsPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // New Report button (primary)
        JButton newReportBtn = UiTheme.createPrimaryButton("Nová kontrola");
        newReportBtn.setMaximumSize(new Dimension(280, 44));
        newReportBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        newReportBtn.addActionListener(e -> openSourcesSelectionWindow());
        buttonsPanel.add(newReportBtn);
        buttonsPanel.add(Box.createVerticalStrut(UiTheme.SPACING_MD));

        card.add(buttonsPanel);

        // Footer with version and docs link
        card.add(Box.createVerticalStrut(UiTheme.SPACING_XL));

        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        footerPanel.setOpaque(false);
        footerPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel versionLabel = new JLabel("v1.0");
        versionLabel.setFont(UiTheme.FONT_SMALL);
        versionLabel.setForeground(UiTheme.TEXT_SECONDARY);
        footerPanel.add(versionLabel);

        JLabel separatorLabel = new JLabel("•");
        separatorLabel.setFont(UiTheme.FONT_SMALL);
        separatorLabel.setForeground(UiTheme.TEXT_SECONDARY);
        footerPanel.add(separatorLabel);

        JLabel docsLink = new JLabel("<html>Dokumentace</html>");
        docsLink.setFont(UiTheme.FONT_SMALL);
        docsLink.setForeground(UiTheme.PRIMARY);
        docsLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        docsLink.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                docsLink.setText("<html><u>Dokumentace</u></html>");
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                docsLink.setText("<html>Dokumentace</html>");
            }
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                openDocumentation();
            }
        });
        footerPanel.add(docsLink);

        card.add(footerPanel);

        root.add(card);
        add(root, BorderLayout.CENTER);

        setPreferredSize(new Dimension(550, 420));
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void openSourcesSelectionWindow() {
        SwingUtilities.invokeLater(() -> {
            new SourcesSelectionWindow();
            dispose();
        });
    }

    private void openDocumentation() {
        DocumentationHelper.openDocumentation("readme.html", this);
    }
}

