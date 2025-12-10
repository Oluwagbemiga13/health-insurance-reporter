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

        JLabel title = new JLabel("Kontrola přehledů zdravotního pojištění");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
        card.add(title, gbc);

        gbc.gridy++;
        JLabel subtitle = new JLabel("Snadno kontrolujte reporty pro zdravotní pojištění.");
        subtitle.setFont(subtitle.getFont().deriveFont(Font.PLAIN, 14f));
        SubtitleWrap.wrap(subtitle, 400);
        gbc.insets = new Insets(0, 0, 24, 0);
        card.add(subtitle, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 12, 0);
        JButton newReportBtn = new JButton("Nová kontrola");
        newReportBtn.addActionListener(e -> openSourcesSelectionWindow());
        card.add(newReportBtn, gbc);

        gbc.gridy++;
        JButton openReportBtn = new JButton("Otevřít existující kontrolu <JE TO POTŘEBNÉ?> ");
        card.add(openReportBtn, gbc);

        gbc.gridy++;
        JButton settingsBtn = new JButton("Nastavení <JE TO POTŘEBNÉ?> ");
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

    private void openSourcesSelectionWindow() {
        SwingUtilities.invokeLater(() -> {
            new SourcesSelectionWindow();
            dispose();
        });
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
