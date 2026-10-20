package com.dungeonescape.ui;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;

/**
 * Compact, fixed-height heads-up display.
 *
 * <p>Stage 2 scope: this is purely presentational. The title, level label, and
 * score/lives values are placeholders and are not wired to any game state.</p>
 */
public class HudPanel extends JPanel {

    private static final Color BACKGROUND = new Color(18, 14, 16);
    private static final Color ACCENT = new Color(120, 84, 40);
    private static final Color GOLD = new Color(232, 196, 110);
    private static final Color MUTED = new Color(176, 166, 154);
    private static final Color LIVES = new Color(214, 96, 96);

    /** Creates the HUD. Callers should invoke this on the Swing EDT. */
    public HudPanel() {
        setBackground(BACKGROUND);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 2, 0, ACCENT),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)));
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(768, 56));

        add(label("DUNGEON ESCAPE V2", GOLD, new Font("Serif", Font.BOLD, 20)),
                BorderLayout.WEST);

        JPanel right = new JPanel(new GridLayout(1, 2, 20, 0));
        right.setOpaque(false);
        right.add(label("Level 1", MUTED, new Font("SansSerif", Font.BOLD, 14)));

        JPanel stats = new JPanel(new GridLayout(2, 1, 0, 2));
        stats.setOpaque(false);
        stats.add(label("Score  0", MUTED, new Font("SansSerif", Font.PLAIN, 12)));
        stats.add(label("Lives  3 / 3", LIVES, new Font("SansSerif", Font.PLAIN, 12)));
        right.add(stats);

        add(right, BorderLayout.EAST);
    }

    private static JLabel label(String text, Color foreground, Font font) {
        JLabel label = new JLabel(text);
        label.setForeground(foreground);
        label.setFont(font);
        return label;
    }
}
