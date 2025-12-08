package cz.oluwagbemiga.eutax.ui;

import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

@Slf4j
public class StartWindow extends JFrame {

    public StartWindow() {
        configureLookAndFeel();
        buildUi();
    }

    private void configureLookAndFeel() {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception ignored) {
            // Nimbus may not be available on all JRE distributions; fall back silently.
        }
    }

    private void buildUi() {
        setTitle("Health Insurance Reporter");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(new Color(245, 247, 250));

        JPanel card = new JPanel(new GridBagLayout());
        card.setBorder(new EmptyBorder(24, 32, 24, 32));
        card.setBackground(Color.WHITE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(0, 0, 16, 0);

        JLabel title = new JLabel("Health Insurance Reporter");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
        card.add(title, gbc);

        gbc.gridy++;
        JLabel subtitle = new JLabel("Generate and manage health insurance reports with ease.");
        subtitle.setFont(subtitle.getFont().deriveFont(Font.PLAIN, 14f));
        SubtitleWrap.wrap(subtitle, 400);
        gbc.insets = new Insets(0, 0, 24, 0);
        card.add(subtitle, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 12, 0);
        JButton newReportBtn = new JButton("New Report");
        card.add(newReportBtn, gbc);

        gbc.gridy++;
        JButton openReportBtn = new JButton("Open Existing Report");
        card.add(openReportBtn, gbc);

        gbc.gridy++;
        JButton settingsBtn = new JButton("Settings");
        card.add(settingsBtn, gbc);

        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(40, 40, 40, 40);
        root.add(card, gbc);

        add(root, BorderLayout.CENTER);
        setPreferredSize(new Dimension(700, 400));
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(StartWindow::new);
    }
}

// Utility class to wrap JLabel text within a given width using simple HTML.
class SubtitleWrap {

    private SubtitleWrap() {
        throw new IllegalStateException("Utility class");
    }

    static void wrap(JLabel label, int widthPx) {
        String text = label.getText();
        label.setText(String.format("<html><div style='width:%dpx;'>%s</div></html>", widthPx, text));
        label.setVerticalAlignment(SwingConstants.TOP);
    }
}
