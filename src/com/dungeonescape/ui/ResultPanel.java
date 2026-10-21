package com.dungeonescape.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

import com.dungeonescape.game.HighScoreBoard;
import com.dungeonescape.game.SearchableScores;

/**
 * End-of-run screen showing victory or game over, with a Play Again action.
 *
 * <p>Reuses the dungeon's stone background and gold/muted palette.</p>
 *
 * <p>When built with a {@link SearchableScores} source — the top-10
 * leaderboard or the complete score history — the screen also offers a
 * player-name search box that reuses the source's existing linear search
 * methods; no separate search algorithm is implemented here.</p>
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
    private final JLabel scoreLinesLabel;
    private final SearchableScores scoreSource;
    private JTextField queryField;

    /**
     * Creates the result screen without a leaderboard search box.
     *
     * @param onRestart invoked when the player chooses to play again
     */
    public ResultPanel(Runnable onRestart) {
        this(onRestart, null);
    }

    /**
     * Creates the result screen with a name-search box backed by the
     * supplied score source. The {@code GameWindow} wires the complete
     * score history here, so the search covers every recorded run,
     * including runs outside the displayed top 10. A null source keeps
     * the original layout; searching on such a screen shows an
     * availability message.
     *
     * @param onRestart  invoked when the player chooses to play again
     * @param scoreSource searchable scores backing the name search, or null
     */
    public ResultPanel(Runnable onRestart, SearchableScores scoreSource) {
        this.onRestart = onRestart;
        this.scoreSource = scoreSource;
        setBackground(BACKGROUND);
        setPreferredSize(new Dimension(768, 640));
        setLayout(new GridBagLayout());

        titleLabel = label("", GOLD, new Font("Serif", Font.BOLD, 42));
        messageLabel = label("", MUTED, new Font("SansSerif", Font.PLAIN, 16));
        scoreLinesLabel = label("", GOLD, new Font("SansSerif", Font.PLAIN, 14));

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
        c.insets = new Insets(8, 20, 16, 20);
        c.gridy = 1;
        add(messageLabel, c);
        c.insets = new Insets(2, 20, 6, 20);
        c.gridy = 2;
        add(scoreLinesLabel, c);

        if (scoreSource != null) {
            addSearchRow(c);
            c.insets = new Insets(10, 20, 30, 20);
            c.gridy = 4;
        } else {
            c.insets = new Insets(10, 20, 30, 20);
            c.gridy = 3;
        }

        add(again, c);
    }

    /**
     * Builds the player-name search row: a text field plus a search
     * button. The button handler calls {@link #runSearch()}, which reuses
     * the score source's own linear search methods.
     */
    private void addSearchRow(GridBagConstraints c) {
        queryField = new JTextField();
        queryField.setHorizontalAlignment(JTextField.CENTER);
        queryField.setFont(new Font("SansSerif", Font.PLAIN, 15));
        queryField.setForeground(GOLD);
        queryField.setBackground(new Color(30, 24, 18));
        queryField.setCaretColor(GOLD);
        queryField.setBorder(BorderFactory.createLineBorder(ACCENT, 2));
        queryField.setPreferredSize(new Dimension(200, 36));
        queryField.setToolTipText("Search leaderboard scores by player name");

        JButton search = new JButton("SEARCH");
        search.setFocusPainted(false);
        search.setForeground(GOLD);
        search.setBackground(new Color(30, 24, 18));
        search.setFont(new Font("Serif", Font.BOLD, 16));
        search.setPreferredSize(new Dimension(120, 36));
        search.setBorder(BorderFactory.createLineBorder(ACCENT, 2));
        search.addActionListener(e -> runSearch());

        JPanel searchRow = new JPanel(
                new FlowLayout(FlowLayout.CENTER, 8, 0));
        searchRow.setOpaque(false);
        searchRow.add(queryField);
        searchRow.add(search);

        c.insets = new Insets(0, 20, 14, 20);
        c.gridy = 3;
        add(searchRow, c);
    }

    /**
     * Runs the player-name search typed into the search box and shows the
     * answer in the rows area.
     *
     * <p>Reuses {@link SearchableScores#findBestScoreByName(String)} and
     * {@link SearchableScores#findScoresByName(String)} exactly — the
     * search normalization (letter case, surrounding whitespace) lives in
     * the score source, so the UI adds no search logic of its own.</p>
     *
     * <p>A blank query shows a prompt and leaves the rows untouched. An
     * unknown name shows a clear not-found message. Duplicate names show
     * the player's best score first, followed by every matching run.</p>
     */
    void runSearch() {
        if (scoreSource == null || queryField == null) {
            scoreLinesLabel.setText("Leaderboard search is not available.");
            return;
        }

        String query = queryField.getText() == null
                ? ""
                : queryField.getText().trim();

        if (query.isEmpty()) {
            scoreLinesLabel.setText(
                    "Type a player name above to search the leaderboard.");
            return;
        }

        HighScoreBoard.ScoreEntry best =
                scoreSource.findBestScoreByName(query);

        setScoreLines(searchResultLines(
                query, best, scoreSource.findScoresByName(query)));
    }

    /**
     * Builds the lines displayed for one search result.
     *
     * <p>The first line reports the player's best score; when the name
     * appears more than once, every matching row is listed below it, best
     * first. A null best (no matching player) becomes a single clear
     * not-found line that echoes the trimmed query.</p>
     *
     * <p>Package-private: tests assert the exact lines.</p>
     */
    static List<String> searchResultLines(
            String query,
            HighScoreBoard.ScoreEntry best,
            List<HighScoreBoard.ScoreEntry> matches) {

        List<String> lines = new ArrayList<>();

        if (best == null) {
            lines.add("No leaderboard entry for '" + query.trim() + "'.");
            return lines;
        }

        lines.add("Found " + best.player()
                + " - best score " + best.score() + ".");

        if (matches.size() > 1) {
            lines.add(matches.size() + " leaderboard rows for "
                    + best.player() + ", best first:");

            for (int i = 0; i < matches.size(); i++) {
                HighScoreBoard.ScoreEntry entry = matches.get(i);

                lines.add((i + 1) + ". " + entry.player()
                        + " - " + entry.score());
            }
        }

        return lines;
    }

    /**
     * Shows the leaderboard rows beneath the result message, e.g.
     * {@code 1. ALICE - 320}. A null or empty list removes the rows.
     *
     * @param lines preformatted rows, highest score first
     */
    public void setScoreLines(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            scoreLinesLabel.setText("");
            return;
        }

        StringBuilder html = new StringBuilder(
                "<html><div style='text-align:center'>");

        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                html.append("<br>");
            }
            html.append(escapeForHtml(lines.get(i)));
        }

        html.append("</div></html>");
        scoreLinesLabel.setText(html.toString());
    }

    private static String escapeForHtml(String line) {
        return line.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    /** Types text into the search box (package-private test seam). */
    void typeSearch(String query) {
        queryField.setText(query);
    }

    /** The exact text shown in the rows area, HTML markup included. */
    String resultRowsText() {
        return scoreLinesLabel.getText();
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
