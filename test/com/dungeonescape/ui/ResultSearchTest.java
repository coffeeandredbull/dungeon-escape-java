package com.dungeonescape.ui;

import com.dungeonescape.game.HighScoreBoard;
import java.util.List;

/**
 * Dependency-free tests for the result-screen leaderboard search: reuse of
 * HighScoreBoard's existing linear search methods, case-insensitive and
 * whitespace-trimmed matching, not-found and blank-query messages, duplicate
 * name rows, and proof that searching never mutates the leaderboard.
 *
 * Run with: {@code java -cp out;test_out com.dungeonescape.ui.ResultSearchTest}
 * Exit code 0 means every assertion passed.
 */
public class ResultSearchTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        testSearchReportsBestScoreCaseInsensitively();
        testSearchNotFoundShowsClearMessage();
        testBlankInputShowsPromptAndClearsOldResults();
        testDuplicateNamesShowBestScoreAndAllRows();
        testSearchesLeaveTheLeaderboardUnchanged();
        testSearchResultLinesFormatting();

        System.out.println();
        System.out.println("Passed: " + passed + ", Failed: " + failed);
        if (failed > 0) {
            System.out.println("TESTS FAILED");
            System.exit(1);
        }
        System.out.println("All tests passed.");
        System.exit(0);
    }

    private static void testSearchReportsBestScoreCaseInsensitively() {
        HighScoreBoard board = new HighScoreBoard();
        board.record("Alice", 50);

        ResultPanel panel = new ResultPanel(() -> {
        }, board);
        panel.typeSearch("  aLiCe  ");
        panel.runSearch();

        String text = panel.resultRowsText();
        assertTrue("found line names the player", text.contains("Found Alice"));
        assertTrue("best score is reported", text.contains("best score 50"));
        assertTrue("single match adds no extra row list",
                !text.contains("leaderboard rows"));
    }

    private static void testSearchNotFoundShowsClearMessage() {
        HighScoreBoard board = new HighScoreBoard();
        board.record("Alice", 50);

        ResultPanel panel = new ResultPanel(() -> {
        }, board);
        panel.typeSearch("Bob");
        panel.runSearch();

        String text = panel.resultRowsText();
        assertTrue("missing player shows a clear message",
                text.contains("No leaderboard entry for 'Bob'."));
        assertTrue("missing player reports no best score",
                !text.contains("best score"));
    }

    private static void testBlankInputShowsPromptAndClearsOldResults() {
        HighScoreBoard board = new HighScoreBoard();
        board.record("Alice", 50);

        ResultPanel panel = new ResultPanel(() -> {
        }, board);

        // An old result must not stay visible after a blank query.
        panel.typeSearch("Alice");
        panel.runSearch();
        assertTrue("previous search worked",
                panel.resultRowsText().contains("Found Alice"));

        panel.typeSearch("");
        panel.runSearch();
        String empty = panel.resultRowsText();
        assertTrue("empty query shows the prompt",
                empty.contains("Type a player name"));
        assertTrue("empty query clears old results",
                !empty.contains("Alice"));

        // Whitespace-only input is treated the same as empty.
        panel.typeSearch("   ");
        panel.runSearch();
        String blank = panel.resultRowsText();
        assertTrue("blank query shows the prompt",
                blank.contains("Type a player name"));
        assertTrue("blank query never touches the board",
                !blank.contains("best score"));
    }

    private static void testDuplicateNamesShowBestScoreAndAllRows() {
        HighScoreBoard board = new HighScoreBoard();
        board.record("Alice", 20);
        board.record("Alice", 60);
        board.record("Alice", 40);

        ResultPanel panel = new ResultPanel(() -> {
        }, board);
        panel.typeSearch("aLiCe");
        panel.runSearch();

        String text = panel.resultRowsText();
        assertTrue("duplicate search names the player",
                text.contains("Found Alice"));
        assertTrue("best score is the highest duplicate",
                text.contains("best score 60"));
        assertTrue("duplicate rows announced",
                text.contains("3 leaderboard rows"));

        int firstRow = text.indexOf(" - 60");
        int secondRow = text.indexOf(" - 40");
        int thirdRow = text.indexOf(" - 20");

        assertTrue("all three duplicate rows displayed",
                firstRow >= 0 && secondRow > firstRow && thirdRow > secondRow);
    }

    private static void testSearchesLeaveTheLeaderboardUnchanged() {
        HighScoreBoard board = new HighScoreBoard();
        board.record("Alice", 120);
        board.record("Zed", 90);
        board.record("Bob", 50);

        String before = describe(board);

        ResultPanel panel = new ResultPanel(() -> {
        }, board);
        panel.typeSearch("Alice");
        panel.runSearch();
        panel.typeSearch("Nobody");
        panel.runSearch();
        panel.typeSearch("");
        panel.runSearch();

        assertTrue("searches never re-rank the leaderboard",
                describe(board).equals(before));
    }

    private static void testSearchResultLinesFormatting() {
        HighScoreBoard board = new HighScoreBoard();
        board.record("Alice", 20);
        board.record("Alice", 60);
        board.record("Alice", 40);

        HighScoreBoard.ScoreEntry best =
                board.findBestScoreByName("Alice");
        List<HighScoreBoard.ScoreEntry> rows =
                board.findScoresByName("alice");

        List<String> lines =
                ResultPanel.searchResultLines("alice", best, rows);

        assertEquals("headline uses the stored name and best score",
                "Found Alice - best score 60.", lines.get(0));
        assertEquals("multiple matches announce the row count",
                "3 leaderboard rows for Alice, best first:", lines.get(1));
        assertEquals("first row best first", "1. Alice - 60", lines.get(2));
        assertEquals("second row", "2. Alice - 40", lines.get(3));
        assertEquals("third row", "3. Alice - 20", lines.get(4));

        List<String> notFound =
                ResultPanel.searchResultLines("  Bob  ", null, List.of());

        assertEquals("not-found message echoes the trimmed query",
                "No leaderboard entry for 'Bob'.", notFound.get(0));
        assertEquals("not-found answer has a single line", 1, notFound.size());
    }

    /** Compact snapshot of a board, used for before/after comparisons. */
    private static String describe(HighScoreBoard board) {
        StringBuilder text = new StringBuilder();

        for (HighScoreBoard.ScoreEntry entry : board.entries()) {
            text.append(entry.player()).append(':')
                    .append(entry.score()).append(';');
        }

        return text.toString();
    }

    private static void assertTrue(String message, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("PASS: " + message);
        } else {
            failed++;
            System.out.println("FAIL: " + message);
        }
    }

    private static void assertEquals(
            String message, String expected, String actual) {
        assertTrue(message + " (expected '" + expected
                + "', got '" + actual + "')",
                expected.equals(actual));
    }

    private static void assertEquals(
            String message, int expected, int actual) {
        assertTrue(message + " (expected " + expected
                + ", got " + actual + ")", expected == actual);
    }
}
