package com.dungeonescape.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * End-of-run screen showing victory or game over, with a Play Again action.
 *
 * <p>Reuses the dungeon's stone background and gold/muted palette.</p>
 */
public class ResultPanel extends JPanel {

    private static final Color BACKGROUND = new Color(14, 15, 20);
    private static final Color FLOOR = new Color(24, 26, 34);
    private static final Color FLOOR_ALT = new Color(29, 32, 42);
    private static final Color OVERLAY = new Color(8, 8, 12, 190);
    private static final Color GOLD = new Color(232, 196, 110);
    private static final Color MUTED = new Color(186, 176, 164);
    private static final Color LIVES = new Color(214, 96, 96);
    private static final Color ACCENT = new Color(120, 84, 40);

    private final Runnable onRestart;
    private final JLabel titleLabel;
    private final JLabel messageLabel;

    /**
     * Creates the result screen.
     *
     * @param onRestart invoked when the player chooses to play again
     */
    public ResultPanel(Runnable onRestart) {
        this.onRestart = onRestart;
        setBackground(BACKGROUND);
        setPreferredSize(new Dimension(768, 640));
        setLayout(new GridBagLayout());

        titleLabel = label("", GOLD, new Font("Serif", Font.BOLD, 42));
        messageLabel = label("", MUTED, new Font("SansSerif", Font.PLAIN, 16));

        JButton again = new JButton("PLAY AGAIN");
        again.setFocusPainted(false);
        again.setForeground(GOLD);
        again.setBackground(new Color(30, 24, 18));
        again.setFont(new Font("Serif", Font.BOLD, 20));
        again.setPreferredSize(new Dimension(200, 54));
        again.setBorder(BorderFactory.createLineBorder(ACCENT, 2));
        again.addActionListener(e -> onRestart.run());

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(10, 20, 10, 20);
        c.weightx = 1;
        c.fill = java.awt.GridBagConstraints.HORIZONTAL;
        c.anchor = java.awt.GridBagConstraints.CENTER;
        c.gridx = 0;
        c.gridy = 0;
        add(titleLabel, c);
        c.insets = new Insets(8, 20, 26, 20);
        c.gridy = 1;
        add(messageLabel, c);
        c.insets = new Insets(10, 20, 30, 20);
        c.gridy = 2;
        add(again, c);
    }

    /** Sets the headline and body text for this screen. */
    public void configure(String title, String message, boolean danger) {
        titleLabel.setText(title);
        titleLabel.setForeground(danger ? LIVES : GOLD);
        messageLabel.setText("<html><div style='text-align:center'>" + message
                + "</div></html>");
    }

    private static JLabel label(String text, Color foreground, Font font) {
        JLabel label = new JLabel(text, JLabel.CENTER);
        label.setForeground(foreground);
        label.setFont(font);
        return label;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        int tile = 48;
        for (int y = 0; y < getHeight(); y += tile) {
            for (int x = 0; x < getWidth(); x += tile) {
                g2.setColor(((x / tile + y / tile) & 1) == 0 ? FLOOR : FLOOR_ALT);
                g2.fillRect(x, y, tile, tile);
            }
        }
        g2.setColor(OVERLAY);
        g2.fillRoundRect(120, 150, getWidth() - 240, getHeight() - 300, 24, 24);
        g2.dispose();
    }
}
