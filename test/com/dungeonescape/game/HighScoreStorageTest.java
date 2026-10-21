package com.dungeonescape.game;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Dependency-free tests for leaderboard CSV persistence: save/load round
 * trips, multiple players across runs, duplicate scores, case-insensitive
 * search after reload, CSV special-character escaping, missing files,
 * malformed records, and safe I/O failures.
 *
 * Run with:
 * java -cp "out;test_out" com.dungeonescape.game.HighScoreStorageTest
 */
public class HighScoreStorageTest {

    private static int passed = 0;
    private static int failed = 0;
    private static Path tempDir;

    public static void main(String[] args) throws IOException {
        tempDir = Files.createTempDirectory("score-storage-test");

        testMissingFileLoadsAsEmptyBoard();
        testSaveThenLoadRoundTrip();
        testMultiplePlayersAcrossRuns();
        testDuplicateScoresSurviveReload();
        testCaseInsensitiveSearchAfterReload();
        testCsvSpecialCharactersSurviveRoundTrip();
        testMalformedRecordsAreSkipped();
        testEscapingProducesQuotedFields();
        testUnwritableFileReportsFailureSafely();

        System.out.println();
        System.out.println("Passed: " + passed + ", Failed: " + failed);

        if (failed > 0) {
            System.out.println("TESTS FAILED");
            System.exit(1);
        }

        System.out.println("All tests passed.");
        System.exit(0);
    }

    private static void testMissingFileLoadsAsEmptyBoard() throws IOException {
        HighScoreBoard board = new HighScoreBoard();
        Path missing = tempDir.resolve("does-not-exist.csv");

        assertEquals("missing file loads zero entries", 0,
                HighScoreStorage.load(board, missing));
        assertTrue("board stays empty on missing file", board.isEmpty());
    }

    private static void testSaveThenLoadRoundTrip() {
        HighScoreBoard board = new HighScoreBoard();
        board.record("Ada", 900);
        board.record("Grace", 700);
        board.record("Linus", 500);

        Path file = tempDir.resolve("round-trip.csv");
        assertTrue("save succeeded", HighScoreStorage.save(board, file));

        HighScoreBoard reloaded = new HighScoreBoard();
        assertEquals("three entries reloaded", 3,
                HighScoreStorage.load(reloaded, file));

        List<HighScoreBoard.ScoreEntry> entries = reloaded.entries();
        assertEquals("first entry is the best score", "Ada",
                entries.get(0).player());
        assertEquals("descending order preserved", 900,
                entries.get(0).score());
        assertEquals("second entry", "Grace", entries.get(1).player());
        assertEquals("third entry", "Linus", entries.get(2).player());
    }

    private static void testMultiplePlayersAcrossRuns() {
        Path file = tempDir.resolve("multi-player.csv");

        // Two separate runs on two separate boards, as across restarts.
        HighScoreBoard firstRun = new HighScoreBoard();
        firstRun.record("Rina", 300);
        HighScoreStorage.save(firstRun, file);

        HighScoreBoard reloaded = new HighScoreBoard();
        HighScoreStorage.load(reloaded, file);
        reloaded.record("Sam", 800);

        HighScoreStorage.save(reloaded, file);

        HighScoreBoard afterRestart = new HighScoreBoard();
        HighScoreStorage.load(afterRestart, file);

        List<HighScoreBoard.ScoreEntry> entries = afterRestart.entries();
        assertEquals("both runs are kept", 2, entries.size());
        assertEquals("second run's score leads", "Sam",
                entries.get(0).player());
        assertEquals("first run's score follows", "Rina",
                entries.get(1).player());
    }

    private static void testDuplicateScoresSurviveReload() {
        HighScoreBoard board = new HighScoreBoard();
        assertTrue(board.record("Zed", 400));
        assertTrue(board.record("Yuki", 400));
        assertTrue(board.record("Xan", 400));

        Path file = tempDir.resolve("duplicates.csv");
        HighScoreStorage.save(board, file);

        HighScoreBoard reloaded = new HighScoreBoard();
        assertEquals("three equal scores reloaded", 3,
                HighScoreStorage.load(reloaded, file));
        assertEquals("all duplicates present", 3,
                reloaded.findScoresByName("Zed")
                        .size()
                        + reloaded.findScoresByName("Yuki").size()
                        + reloaded.findScoresByName("Xan").size());
        assertEquals("duplicate score value kept", 400,
                reloaded.findScoresByName("YUKI").get(0).score());
    }

    private static void testCaseInsensitiveSearchAfterReload() {
        HighScoreBoard board = new HighScoreBoard();
        board.record("  Morrigan ", 640);

        Path file = tempDir.resolve("case-search.csv");
        HighScoreStorage.save(board, file);

        HighScoreBoard reloaded = new HighScoreBoard();
        HighScoreStorage.load(reloaded, file);

        HighScoreBoard.ScoreEntry found =
                reloaded.findBestScoreByName("morrigan");

        assertTrue("reloaded entry found ignoring case", found != null);
        assertTrue("name trimmed to the stored value",
                "Morrigan".equals(found.player()));
        assertTrue("search still works with mixed case and spaces",
                reloaded.findScoresByName("  MORRIGAN  ").size() == 1);
    }

    private static void testCsvSpecialCharactersSurviveRoundTrip() {
        HighScoreBoard board = new HighScoreBoard();
        String trickyName = 'A' + "da, \"The\nCoder\"" + ',';
        board.record(trickyName, 250);

        Path file = tempDir.resolve("special-chars.csv");
        HighScoreStorage.save(board, file);

        HighScoreBoard reloaded = new HighScoreBoard();
        HighScoreStorage.load(reloaded, file);

        List<HighScoreBoard.ScoreEntry> matches =
                reloaded.findScoresByName(trickyName);

        assertTrue("name with comma, quote, and newline restored",
                matches.size() == 1);
        assertTrue("escaped name kept every character",
                trickyName.equals(matches.get(0).player()));
    }

    private static void testMalformedRecordsAreSkipped() throws IOException {
        String csv = "player,score\n"
                + "Ada,900\n"
                + "\n"
                + "onlyName\n"
                + "Bob,not,a,number,value\n"
                + "Cara,notANumber\n"
                + "Dara,850\n";

        Path file = tempDir.resolve("malformed.csv");
        Files.writeString(file, csv);

        HighScoreBoard board = new HighScoreBoard();
        assertEquals("only the two valid rows load", 2,
                HighScoreStorage.load(board, file));

        assertTrue("first valid row loaded",
                board.findBestScoreByName("ada") != null);
        assertTrue("last valid row loaded",
                board.findBestScoreByName("dara") != null);
        assertTrue("invalid rows skipped",
                board.findBestScoreByName("cara") == null
                        && board.findBestScoreByName("bob") == null);
    }

    private static void testEscapingProducesQuotedFields() {
        assertTrue("plain field stays unquoted",
                "Ada".equals(HighScoreStorage.escape("Ada")));
        assertTrue("comma field is quoted",
                "\"A,da\"".equals(HighScoreStorage.escape("A,da")));
        assertTrue("inner quotes are doubled",
                "\"A\"\"da\"".equals(HighScoreStorage.escape("A\"da")));
        assertTrue("newline field is quoted",
                "\"A\nda\"".equals(HighScoreStorage.escape("A\nda")));
        assertTrue("null field becomes empty",
                "".equals(HighScoreStorage.escape(null)));
    }

    private static void testUnwritableFileReportsFailureSafely() {
        HighScoreBoard board = new HighScoreBoard();
        board.record("Ada", 900);

        // Saving "into" a directory fails at the OS level; the call must
        // return false without throwing.
        assertTrue("save into a directory reports failure, no crash",
                HighScoreStorage.save(board, tempDir) == false);

        HighScoreBoard nullBoard = new HighScoreBoard();
        assertTrue("null board handled", !HighScoreStorage.save(
                (HighScoreBoard) null, tempDir.resolve("x.csv")));
        assertTrue("null path handled", !HighScoreStorage.save(
                nullBoard, null));
        assertTrue("load with null board handled",
                HighScoreStorage.load(board, tempDir.resolve("y.csv")) == 0);
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

    private static void assertTrue(boolean condition) {
        assertTrue("assertion", condition);
    }

    private static void assertEquals(String expected, String actual) {
        assertTrue("expected " + expected + ", got " + actual,
                expected == null ? actual == null : expected.equals(actual));
    }

    private static void assertEquals(
            String message, int expected, int actual) {
        assertTrue(message + " (expected " + expected
                + ", got " + actual + ")", expected == actual);
    }

    private static void assertEquals(
            String message, String expected, String actual) {
        assertTrue(message + " (expected " + expected
                + ", got " + actual + ")",
                expected == null ? actual == null : expected.equals(actual));
    }
}
