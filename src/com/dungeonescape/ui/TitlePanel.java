package com.dungeonescape.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * Title screen for Dungeon Escape.
 *
 * <p>Shows the game title, concise instructions, and a Start action. The stone
 * background reuses the dungeon's floor palette so the style stays consistent.
 * Pressing Start (or Enter) invokes the supplied callback.</p>
 */
public class TitlePanel extends JPanel {

    private static final Color BACKGROUND = new Color(14, 15, 20);
    private static final Color FLOOR = new Color(24, 26, 34);
    private static final Color FLOOR_ALT = new Color(29, 32, 42);
    private static final Color OVERLAY = new Color(8, 8, 12, 180);
    private static final Color GOLD = new Color(232, 196, 110);
    private static final Color MUTED = new Color(186, 176, 164);
    private static final Color ACCENT = new Color(120, 84, 40);

    private final Runnable onStart;
    private final JTextField nameField;

    /**
     * Creates the title screen.
     *
     * @param onStart invoked when the player starts the game
     */
    public TitlePanel(Runnable onStart) {
        this.onStart = onStart;
        setBackground(BACKGROUND);
        setPreferredSize(new Dimension(768, 640));
        setLayout(new GridBagLayout());
        setFocusable(true);

        JLabel title = label("DUNGEON ESCAPE", GOLD, new Font("Serif", Font.BOLD, 46));
        JLabel subtitle = label("Escape the dungeon", MUTED, new Font("Serif", Font.ITALIC, 18));

        JLabel instructions = label("<html><div style='text-align:center'>"
                + "Move with <b>W A S D</b> or the <b>arrow keys</b>.<br>"
                + "Find the <b>key</b>, open the <b>chest</b> for gold, and drink <b>potions</b> for extra life.<br>"
                + "Avoid the <b>monster</b> and unlock the <b>door</b> to escape.<br>"
                + "Press <b>Z</b> to undo your last step.</div></html>",
                MUTED, new Font("SansSerif", Font.PLAIN, 15));

        nameField = new JTextField();
        nameField.setHorizontalAlignment(JTextField.CENTER);
        nameField.setFont(new Font("SansSerif", Font.PLAIN, 16));
        nameField.setPreferredSize(new Dimension(220, 36));
        nameField.setToolTipText("Type your name for the leaderboard (optional)");

        JButton start = new JButton("START");
        start.setFocusPainted(false);
        start.setForeground(GOLD);
        start.setBackground(new Color(30, 24, 18));
        start.setFont(new Font("Serif", Font.BOLD, 22));
        start.setPreferredSize(new Dimension(190, 56));
        start.setBorder(BorderFactory.createLineBorder(ACCENT, 2));
        start.addActionListener(e -> onStart.run());

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 20, 6, 20);
        c.weightx = 1;
        c.weighty = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.CENTER;
        c.gridx = 0;

        c.gridy = 0;
        add(title, c);

c.gridy = 1;
add(subtitle, c);

c.gridy = 2;
c.insets = new Insets(18, 30, 18, 30);
c.fill = GridBagConstraints.HORIZONTAL;
c.weightx = 1;
add(instructions, c);

c.gridy = 3;
c.insets = new Insets(0, 20, 6, 20);
c.fill = GridBagConstraints.NONE;
c.weightx = 0;
c.anchor = GridBagConstraints.CENTER;
add(nameField, c);

c.insets = new Insets(14, 20, 20, 20);
c.fill = GridBagConstraints.NONE;
c.weightx = 0;
c.anchor = GridBagConstraints.CENTER;
c.gridy = 4;
add(start, c);
    }

    /**
     * Trimmed player name typed into the title-screen field, or {@code null}
     * when left blank. The leaderboard stores its default name in that case.
     */
    public String playerName() {
        String text = nameField.getText();

        if (text == null) {
            return null;
        }

        String trimmed = text.trim();

        return trimmed.isEmpty() ? null : trimmed;
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
        g2.fillRoundRect(120, 120, getWidth() - 240, getHeight() - 240, 24, 24);
        g2.dispose();
    }
}
