
package com.dungeonescape.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Stores the ten highest scores in descending order.
 *
 * Insertion sort keeps the array sorted whenever a score is recorded.
 * Linear search is used to find scores by player name.
 */
public class HighScoreBoard implements SearchableScores {

    public static final int MAX_ENTRIES = 10;

    private final ScoreEntry[] entries = new ScoreEntry[MAX_ENTRIES];
    private int size;

    /**
     * One leaderboard entry.
     */
    public record ScoreEntry(String player, int score) {
    }

    /**
     * Records a score if it qualifies for the leaderboard.
     *
     * Scores are sorted from highest to lowest. Equal scores are kept.
     * If the board is full, a score must be strictly higher than the
     * current lowest score to be accepted.
     *
     * @param player player name; blank or null names become "Player"
     * @param score completed run's score
     * @return true if the score was added to the leaderboard
     */
    public boolean record(String player, int score) {
        String cleanedName = cleanName(player);

        if (size == MAX_ENTRIES && score <= entries[size - 1].score()) {
            return false;
        }

        ScoreEntry newEntry = new ScoreEntry(cleanedName, score);

        int insertAt = size;

        // Insertion sort: move lower scores one position to the right.
        while (insertAt > 0
                && entries[insertAt - 1].score() < newEntry.score()) {
            if (insertAt < MAX_ENTRIES) {
                entries[insertAt] = entries[insertAt - 1];
            }
            insertAt--;
        }

        entries[insertAt] = newEntry;

        if (size < MAX_ENTRIES) {
            size++;
        }

        return true;
    }

    /**
     * Returns the entries currently stored, highest score first.
     * The returned list cannot modify the internal leaderboard.
     */
    public List<ScoreEntry> entries() {
        List<ScoreEntry> result = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            result.add(entries[i]);
        }

        return Collections.unmodifiableList(result);
    }

    /**
     * Finds the highest score recorded for a player.
     *
     * Uses linear search and ignores differences in letter case
     * and surrounding whitespace.
     *
     * @param player player name to search for
     * @return the player's best entry, or null if not found
     */
    public ScoreEntry findBestScoreByName(String player) {
        String searchName = normalizeName(player);

        if (searchName.isEmpty()) {
            return null;
        }

        ScoreEntry best = null;

        for (int i = 0; i < size; i++) {
            ScoreEntry entry = entries[i];

            if (normalizeName(entry.player()).equals(searchName)) {
                if (best == null || entry.score() > best.score()) {
                    best = entry;
                }
            }
        }

        return best;
    }

    /**
     * Finds every leaderboard entry matching a player name.
     *
     * Uses linear search. Multiple entries for the same player
     * are allowed.
     *
     * @param player player name to search for
     * @return matching entries, in leaderboard order
     */
    public List<ScoreEntry> findScoresByName(String player) {
        String searchName = normalizeName(player);
        List<ScoreEntry> matches = new ArrayList<>();

        if (searchName.isEmpty()) {
            return matches;
        }

        for (int i = 0; i < size; i++) {
            ScoreEntry entry = entries[i];

            if (normalizeName(entry.player()).equals(searchName)) {
                matches.add(entry);
            }
        }

        return Collections.unmodifiableList(matches);
    }

    /**
     * Returns the number of stored entries.
     */
    public int size() {
        return size;
    }

    /**
     * Returns whether the leaderboard contains no entries.
     */
    public boolean isEmpty() {
        return size == 0;
    }

    private static String cleanName(String player) {
        if (player == null || player.isBlank()) {
            return "Player";
        }

        return player.trim();
    }

    private static String normalizeName(String player) {
        if (player == null) {
            return "";
        }

        return player.trim().toLowerCase(Locale.ROOT);
    }
}
