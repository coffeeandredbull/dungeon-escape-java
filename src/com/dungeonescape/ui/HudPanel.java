
package com.dungeonescape.ui;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.Locale;

public class HudPanel extends JPanel {

    private static final Color BACKGROUND = new Color(18, 14, 16);
    private static final Color ACCENT = new Color(120, 84, 40);
    private static final Color GOLD = new Color(232, 196, 110);
    private static final Color MUTED = new Color(210, 204, 194);
    private static final Color LIVES = new Color(255, 125, 125);
    private static final Color KEY_READY = new Color(130, 235, 150);

    private final JLabel levelLabel;
    private final JLabel scoreLabel;
    private final JLabel livesLabel;
    private final JLabel keyLabel;

    public HudPanel() {
        setBackground(BACKGROUND);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 2, 0, ACCENT),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)));
        setLayout(new BorderLayout(18, 0));

        setPreferredSize(new Dimension(768, 64));
        setMinimumSize(new Dimension(0, 64));

        JLabel title = label(
                "DUNGEON ESCAPE",
                GOLD,
                new Font("Serif", Font.BOLD, 19));
        add(title, BorderLayout.WEST);

        JPanel stats = new JPanel(
                new FlowLayout(FlowLayout.RIGHT, 15, 5));
        stats.setOpaque(false);

        Font statFont = new Font("SansSerif", Font.BOLD, 12);

        levelLabel = label("LEVEL 1", MUTED, statFont);
        scoreLabel = label("SCORE 0", MUTED, statFont);
        livesLabel = label("LIVES 3 / 3", LIVES, statFont);
        keyLabel = label("KEY: NO", MUTED, statFont);

        stats.add(levelLabel);
        stats.add(scoreLabel);
        stats.add(livesLabel);
        stats.add(keyLabel);

        add(stats, BorderLayout.CENTER);
    }

    public void setLevel(String text) {
        String value = text == null
                ? "LEVEL —"
                : text.toUpperCase(Locale.ROOT);

        if (!levelLabel.getText().equals(value)) {
            levelLabel.setText(value);
        }
    }

    public void setScore(int score) {
        String value = "SCORE " + score;

        if (!scoreLabel.getText().equals(value)) {
            scoreLabel.setText(value);
        }
    }

    public void setLives(int lives) {
        String value = "LIVES " + lives + " / 3";

        if (!livesLabel.getText().equals(value)) {
            livesLabel.setText(value);
        }
    }

    public void setKey(boolean hasKey) {
        String value = hasKey ? "KEY: YES" : "KEY: NO";
        Color color = hasKey ? KEY_READY : MUTED;

        if (!keyLabel.getText().equals(value)) {
            keyLabel.setText(value);
        }

        if (!keyLabel.getForeground().equals(color)) {
            keyLabel.setForeground(color);
        }
    }

    private static JLabel label(
            String text, Color foreground, Font font) {
        JLabel label = new JLabel(text);
        label.setForeground(foreground);
        label.setFont(font);
        label.setVerticalAlignment(JLabel.CENTER);
        return label;
    }
}
