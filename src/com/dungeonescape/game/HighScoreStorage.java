package com.dungeonescape.game;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Persists a {@link HighScoreBoard} to a simple CSV file.
 *
 * <p>By default the file is {@code dungeon-escape-scores.csv} inside the
 * user's home directory ({@link System#getProperty user.home}). Saving
 * happens once per finished run from the result screen (never during
 * gameplay), and loading happens once at application start, so the tiny
 * file writes never delay a moving player.</p>
 *
 * <p>Safety rules, no exceptions thrown to callers:</p>
 * <ul>
 *   <li>A missing file simply loads an empty leaderboard (count 0).</li>
 *   <li>Malformed rows (wrong column count, non-numeric score, blank
 *       row) are skipped while valid rows around them keep loading.</li>
 *   <li>I/O errors on save return {@code false} and leave the game
 *       running; load failures return 0 and an unchanged board.</li>
 * </ul>
 *
 * <p>CSV escaping: a field containing a comma, quote, or line break is
 * wrapped in double quotes and every quote inside it is doubled
 * ({@code "} becomes {@code ""}), so player names with special
 * characters survive the round trip.</p>
 */
public final class HighScoreStorage {

    /** Saved file name inside the user's home directory. */
    public static final String DEFAULT_FILE_NAME = "dungeon-escape-scores.csv";

    private static final String HEADER = "player,score";

    private HighScoreStorage() {
        // Static utility class; no instances.
    }

    /**
     * The default save-file location: {@code dungeon-escape-scores.csv}
     * in the user's home directory.
     */
    public static Path defaultFile() {
        return Path.of(System.getProperty("user.home"), DEFAULT_FILE_NAME);
    }

    /**
     * Writes every current leaderboard entry to the file, best score
     * first, with a {@code player,score} header.
     *
     * @return true when the file was written; false on any I/O error
     */
    public static boolean save(HighScoreBoard board, Path file) {
        if (board == null) {
            return false;
        }

        return saveEntries(board.entries(), file);
    }

    /**
     * Writes the complete score history to the file — every completed
     * run, including scores outside the top 10, so none is lost.
     *
     * @return true when the file was written; false on any I/O error
     */
    public static boolean save(ScoreHistory history, Path file) {
        if (history == null) {
            return false;
        }

        return saveEntries(history.entries(), file);
    }

    /**
     * Shared CSV writer for either score container.
     */
    private static boolean saveEntries(
            List<HighScoreBoard.ScoreEntry> entries, Path file) {

        if (file == null) {
            return false;
        }

        StringBuilder csv = new StringBuilder(HEADER);

        for (HighScoreBoard.ScoreEntry entry : entries) {
            csv.append('\n')
                    .append(escape(entry.player()))
                    .append(',')
                    .append(entry.score());
        }

        try {
            Files.writeString(
                    file, csv.toString(), StandardCharsets.UTF_8);
            return true;
        } catch (IOException | SecurityException | UnsupportedOperationException e) {
            System.err.println("Could not save scores to "
                    + file + ": " + e);
            return false;
        }
    }

    /**
     * Reads saved entries back into the board through {@link
     * HighScoreBoard#record}, so the existing descending insertion sort
     * and capacity rules decide the final order. Only runs qualifying
     * for the top 10 land in the board; {@link #load(ScoreHistory, Path)}
     * restores the complete history.
     *
     * @return the number of entries successfully loaded; 0 when the file
     *         is missing, unreadable, or empty
     */
    public static int load(HighScoreBoard board, Path file) {
        if (board == null) {
            return 0;
        }

        int loaded = 0;

        for (String[] fields : readRecords(file)) {
            HighScoreBoard.ScoreEntry entry = parseEntry(fields);

            if (entry == null) {
                continue;
            }

            if (board.record(entry.player(), entry.score())) {
                loaded++;
            }
        }

        return loaded;
    }

    /**
     * Reads the complete saved run history into the history container —
     * every valid row, including runs outside the top 10.
     *
     * @return the number of runs successfully loaded; 0 when the file
     *         is missing, unreadable, or empty
     */
    public static int load(ScoreHistory history, Path file) {
        if (history == null) {
            return 0;
        }

        int loaded = 0;

        for (String[] fields : readRecords(file)) {
            HighScoreBoard.ScoreEntry entry = parseEntry(fields);

            if (entry == null) {
                continue;
            }

            history.append(entry.player(), entry.score());
            loaded++;
        }

        return loaded;
    }

    /**
     * Reads and parses the file; null when it is missing or unreadable
     * (an empty leaderboard outcome for both callers).
     */
    private static List<String[]> readRecords(Path file) {
        if (file == null) {
            return List.of();
        }

        String csv;

        try {
            csv = Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            // A missing or unreadable file is simply an empty leaderboard.
            return List.of();
        }

        return parseRecords(csv);
    }

    /**
     * Turns one CSV record into an entry, or null when the record is
     * malformed (wrong column count, the header, a non-numeric score).
     */
    private static HighScoreBoard.ScoreEntry parseEntry(String[] fields) {
        if (fields.length != 2) {
            return null;
        }

        if (fields[0].equals("player") && fields[1].equals("score")) {
            return null;
        }

        int score;

        try {
            score = Integer.parseInt(fields[1].trim());
        } catch (NumberFormatException e) {
            return null;
        }

        return new HighScoreBoard.ScoreEntry(fields[0], score);
    }

    /**
     * Escapes one CSV field: quotes wrap fields containing commas,
     * quotes, or line breaks, and embedded quotes are doubled.
     */
    static String escape(String field) {
        if (field == null) {
            return "";
        }

        if (!field.contains(",") && !field.contains("\"")
                && !field.contains("\n") && !field.contains("\r")) {
            return field;
        }

        return '"' + field.replace("\"", "\"\"") + '"';
    }

    /**
     * Splits CSV text into records of fields, honoring quoted fields
     * (which may contain commas, quotes, and line breaks) and accepting
     * both {@code \r\n} and {@code \n} row separators. Blank trailing
     * rows and leftover empty fields are skipped.
     */
    static List<String[]> parseRecords(String csv) {
        List<String[]> records = new ArrayList<>();
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        boolean partialRecord = false;

        for (int i = 0; i < csv.length(); i++) {
            char c = csv.charAt(i);

            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < csv.length()
                            && csv.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    field.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                fields.add(field.toString());
                field.setLength(0);
                partialRecord = true;
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < csv.length()
                        && csv.charAt(i + 1) == '\n') {
                    i++;
                }

                if (!field.isEmpty() || !fields.isEmpty()) {
                    fields.add(field.toString());
                    field.setLength(0);
                    records.add(fields.toArray(new String[0]));
                    fields.clear();
                    partialRecord = false;
                }
            } else {
                field.append(c);
            }
        }

        if (partialRecord || !field.isEmpty()) {
            fields.add(field.toString());
            records.add(fields.toArray(new String[0]));
        }

        return records;
    }
}
