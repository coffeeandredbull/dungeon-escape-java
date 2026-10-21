
package com.dungeonescape.game;

import java.util.List;

/**
 * Dependency-free tests for HighScoreBoard.
 *
 * Run with:
 * java -cp "out;test_out" com.dungeonescape.game.HighScoreBoardTest
 */
public class HighScoreBoardTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        testNewBoardIsEmpty();
        testSearchOnEmptyBoardFindsNothing();
        testScoresAreSortedDescending();
        testEqualScoresAreKept();
        testBoardHasMaximumCapacity();
        testHigherScoreReplacesLowestEntry();
        testLowerScoreIsRejectedWhenFull();
        testSearchIgnoresCaseAndWhitespace();
        testBlankAndNullNamesUseDefault();
        testDuplicateNamesAreAllowed();

        System.out.println();
        System.out.println("Passed: " + passed + ", Failed: " + failed);

        if (failed > 0) {
            System.out.println("TESTS FAILED");
            System.exit(1);
        }

        System.out.println("All tests passed.");
        System.exit(0);
    }

    private static void testNewBoardIsEmpty() {
        HighScoreBoard board = new HighScoreBoard();

        assertTrue("new board has no entries", board.isEmpty());
        assertEquals("new board size is zero", 0, board.size());
    }

    private static void testSearchOnEmptyBoardFindsNothing() {
        HighScoreBoard board = new HighScoreBoard();

        assertEquals("search on empty board finds nothing",
                null, board.findBestScoreByName("Alex"));
        assertEquals("all-score search on empty board finds nothing",
                0, board.findScoresByName("Alex").size());
    }

    private static void testScoresAreSortedDescending() {
        HighScoreBoard board = new HighScoreBoard();

        board.record("Alex", 50);
        board.record("Sam", 120);
        board.record("Taylor", 90);

        List<HighScoreBoard.ScoreEntry> entries = board.entries();

        assertEquals("three rows stored", 3, entries.size());
        assertEquals("highest row first", 120, entries.get(0).score());
        assertEquals("second row in middle", 90, entries.get(1).score());
        assertEquals("lowest row last", 50, entries.get(2).score());
    }

    private static void testEqualScoresAreKept() {
        HighScoreBoard board = new HighScoreBoard();

        board.record("Alex", 100);
        board.record("Sam", 100);
        board.record("Taylor", 100);

        assertEquals("all three equal scores kept",
                3, board.size());

        for (HighScoreBoard.ScoreEntry entry : board.entries()) {
            assertEquals("equal score preserved", 100, entry.score());
        }
    }

    private static void testBoardHasMaximumCapacity() {
        HighScoreBoard board = new HighScoreBoard();

        for (int i = 1; i <= 15; i++) {
            board.record("Player" + i, i * 10);
        }

        assertEquals("board stores only ten entries",
                HighScoreBoard.MAX_ENTRIES, board.size());
        assertEquals("highest score retained",
                150, board.entries().get(0).score());
        assertEquals("lowest stored score retained",
                60, board.entries().get(9).score());
    }

    private static void testHigherScoreReplacesLowestEntry() {
        HighScoreBoard board = new HighScoreBoard();

        for (int i = 1; i <= HighScoreBoard.MAX_ENTRIES; i++) {
            board.record("Player" + i, i * 10);
        }

        boolean added = board.record("Champion", 110);

        assertTrue("higher score accepted", added);
        assertEquals("board remains full",
                HighScoreBoard.MAX_ENTRIES, board.size());
        assertEquals("new highest score is first",
                110, board.entries().get(0).score());
        assertEquals("new entry is first",
                "Champion", board.entries().get(0).player());
        assertEquals("old lowest entry removed",
                null, board.findBestScoreByName("Player1"));
    }

    private static void testLowerScoreIsRejectedWhenFull() {
        HighScoreBoard board = new HighScoreBoard();

        for (int i = 1; i <= HighScoreBoard.MAX_ENTRIES; i++) {
            board.record("Player" + i, i * 10);
        }

        boolean added = board.record("TooLow", 1);

        assertFalse("lower score rejected", added);
        assertEquals("board size unchanged",
                HighScoreBoard.MAX_ENTRIES, board.size());
        assertEquals("rejected score not stored",
                null, board.findBestScoreByName("TooLow"));
    }

    private static void testSearchIgnoresCaseAndWhitespace() {
        HighScoreBoard board = new HighScoreBoard();
        board.record("  Tharushi  ", 95);

        HighScoreBoard.ScoreEntry result =
                board.findBestScoreByName("tharushi");

        assertTrue("search finds matching name", result != null);

        if (result != null) {
            assertEquals("stored name is trimmed",
                    "Tharushi", result.player());
            assertEquals("stored score is correct",
                    95, result.score());
        }

        assertEquals("null name search finds nothing",
                null, board.findBestScoreByName(null));
        assertEquals("blank name search finds nothing",
                null, board.findBestScoreByName("   "));
    }

    private static void testBlankAndNullNamesUseDefault() {
        HighScoreBoard board = new HighScoreBoard();

        board.record(null, 30);
        board.record("   ", 20);

        assertEquals("null name defaults to Player",
                "Player", board.entries().get(0).player());
        assertEquals("blank name defaults to Player",
                "Player", board.entries().get(1).player());
    }

    private static void testDuplicateNamesAreAllowed() {
        HighScoreBoard board = new HighScoreBoard();

        board.record("Alex", 40);
        board.record("alex", 80);
        board.record("Alex", 60);

        List<HighScoreBoard.ScoreEntry> matches =
                board.findScoresByName("ALEX");

        assertEquals("all duplicate-name scores are found",
                3, matches.size());
        assertEquals("duplicate scores remain sorted",
                80, matches.get(0).score());
        assertEquals("second duplicate score",
                60, matches.get(1).score());
        assertEquals("third duplicate score",
                40, matches.get(2).score());

        HighScoreBoard.ScoreEntry best =
                board.findBestScoreByName("alex");

        assertTrue("best score search finds an entry", best != null);

        if (best != null) {
            assertEquals("best duplicate score returned",
                    80, best.score());
        }
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

    private static void assertFalse(String message, boolean condition) {
        assertTrue(message, !condition);
    }

    private static void assertEquals(
            String message, int expected, int actual) {
        assertTrue(message + " (expected " + expected
                + ", got " + actual + ")", expected == actual);
    }

    private static void assertEquals(
            String message, String expected, String actual) {
        assertTrue(message + " (expected '" + expected
                + "', got '" + actual + "')",
                expected == null ? actual == null : expected.equals(actual));
    }

    private static void assertEquals(
            String message, Object expected, Object actual) {
        assertTrue(message + " (expected " + expected
                + ", got " + actual + ")",
                expected == null ? actual == null : expected.equals(actual));
    }
}
