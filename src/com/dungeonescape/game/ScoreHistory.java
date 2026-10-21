package com.dungeonescape.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Complete, unbounded record of every completed run — the searchable
 * score history.
 *
 * <p>The {@link HighScoreBoard} keeps only the best ten runs for display;
 * this class keeps all of them, so a run whose score does not qualify for
 * the top 10 stays persistently searchable after the application restarts.
 * The two live side by side in {@code GameWindow}: the board is the
 * displayed top-10 leaderboard, the history is the archive the result
 * screen searches.</p>
 *
 * <p>Storage: one plain {@code ScoreEntry[]} array with an explicit size,
 * grown by hand when full (capacity doubling with an explicit copy loop —
 * no collection classes). Appending runs in O(1) amortized; searching is
 * a linear scan, matching names case-insensitively (Locale.ROOT) and
 * ignoring surrounding whitespace, exactly like the board's search.
 * Multiple matching runs are returned best score first, keeping recording
 * order for equal scores. Pure game logic with no Swing dependency.</p>
 */
public class ScoreHistory implements SearchableScores {

    /** Starting array size; the array doubles whenever it fills. */
    public static final int INITIAL_CAPACITY = 16;

    private HighScoreBoard.ScoreEntry[] entries =
            new HighScoreBoard.ScoreEntry[INITIAL_CAPACITY];
    private int size;

    /**
     * Appends one completed run to the history. Every run is kept,
     * whatever its score — unlike the top-10 board, there is no
     * qualification rule.
     *
     * @param player player name; blank or null names become "Player",
     *               the same cleaning the board applies
     * @param score  completed run's score
     */
    public void append(String player, int score) {
        String cleanedName;

        if (player == null || player.isBlank()) {
            cleanedName = "Player";
        } else {
            cleanedName = player.trim();
        }

        if (size == entries.length) {
            grow();
        }

        entries[size] = new HighScoreBoard.ScoreEntry(cleanedName, score);
        size++;
    }

    /**
     * Restores the best score recorded for a player.
     *
     * <p>Uses linear search and ignores differences in letter case and
     * surrounding whitespace. Because the history contains every run,
     * the answer can be a score that the top-10 display does not show;
     * matched runs are listed best score first, so the first match is
     * that player's best result.</p>
     */
    @Override
    public HighScoreBoard.ScoreEntry findBestScoreByName(String player) {
        String searchName = normalizeName(player);

        if (searchName.isEmpty()) {
            return null;
        }

        HighScoreBoard.ScoreEntry best = null;

        for (int i = 0; i < size; i++) {
            HighScoreBoard.ScoreEntry entry = entries[i];

            if (normalizeName(entry.player()).equals(searchName)) {
                if (best == null || entry.score() > best.score()) {
                    best = entry;
                }
            }
        }

        return best;
    }

    /**
     * Finds every run recorded for a player.
     *
     * <p>Uses linear search. Matching runs are returned best score first;
     * equal scores keep the order in which the runs were recorded.</p>
     */
    @Override
    public List<HighScoreBoard.ScoreEntry> findScoresByName(String player) {
        String searchName = normalizeName(player);
        List<HighScoreBoard.ScoreEntry> matches = new ArrayList<>();

        if (searchName.isEmpty()) {
            return matches;
        }

        for (int i = 0; i < size; i++) {
            HighScoreBoard.ScoreEntry entry = entries[i];

            if (normalizeName(entry.player()).equals(searchName)) {
                // Insertion keeps the matched subset best first; equal
                // scores keep recording order (stable).
                insertBestFirst(matches, entry);
            }
        }

        return Collections.unmodifiableList(matches);
    }

    /**
     * Returns every recorded run in the order the runs completed.
     * The returned list cannot modify the history.
     */
    public List<HighScoreBoard.ScoreEntry> entries() {
        List<HighScoreBoard.ScoreEntry> result = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            result.add(entries[i]);
        }

        return Collections.unmodifiableList(result);
    }

    /**
     * Returns the number of recorded runs.
     */
    public int size() {
        return size;
    }

    /**
     * Returns whether no run has been recorded yet.
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Doubles the array capacity by hand when the history fills it.
     */
    private void grow() {
        HighScoreBoard.ScoreEntry[] bigger =
                new HighScoreBoard.ScoreEntry[entries.length * 2];

        for (int i = 0; i < size; i++) {
            bigger[i] = entries[i];
        }

        entries = bigger;
    }

    /**
     * Inserts one match into a list kept in descending score order,
     * after every entry with an equal or higher score. Plain insertion,
     * mirroring the board's stable sort behaviour.
     */
    private static void insertBestFirst(
            List<HighScoreBoard.ScoreEntry> matches,
            HighScoreBoard.ScoreEntry entry) {

        int insertAt = matches.size();

        while (insertAt > 0
                && matches.get(insertAt - 1).score() < entry.score()) {
            insertAt--;
        }

        matches.add(insertAt, entry);
    }

    private static String normalizeName(String player) {
        if (player == null) {
            return "";
        }

        return player.trim().toLowerCase(Locale.ROOT);
    }
}
