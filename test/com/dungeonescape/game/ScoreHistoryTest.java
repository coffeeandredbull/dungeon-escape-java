package com.dungeonescape.game;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Dependency-free tests for the persistent score history: more than ten
 * completed runs preserved in CSV, runs outside the top 10 staying
 * searchable after reload, repeated runs by one player kept separate,
 * the top-10 leaderboard still rebuildable from history through the
 * board's insertion sort, and CSV/malformed-file safety.
 *
 * Run with:
 * java -cp "out;test_out" com.dungeonescape.game.ScoreHistoryTest
 */
public class ScoreHistoryTest {

    private static int passed = 0;
    private static int failed = 0;
    private static Path tempDir;

    public static void main(String[] args) throws IOException {
        tempDir = Files.createTempDirectory("score-history-test");

        testHistoryKeepsUnqualifiedRuns();
        testHistoryGrowsBeyondInitialCapacity();
        testMoreThanTenRunsPreservedInCsv();
        testPlayerOutsideTopTenSearchableAfterReload();
        testRepeatedRunsStaySeparate();
        testTopTenLeaderboardRebuildsFromHistory();
        testBestScoreCanBeOutsideTheTopTen();
        testSpecialCharactersViaHistoryRoundTrip();
        testMissingAndMalformedFilesLoadSafely();
        testNullAndBlankNamesHandledLikeTheBoard();

        System.out.println();
        System.out.println("Passed: " + passed + ", Failed: " + failed);

        if (failed > 0) {
            System.out.println("TESTS FAILED");
            System.exit(1);
        }

        System.out.println("All tests passed.");
        System.exit(0);
    }

    private static void testHistoryKeepsUnqualifiedRuns() {
        ScoreHistory history = new ScoreHistory();

        history.append("First", 900);
        history.append("Second", 10);
        history.append("Third", 20);

        assertEquals("every run is kept, however low its score", 3,
                history.size());

        List<HighScoreBoard.ScoreEntry> second =
                history.findScoresByName("second");

        assertEquals("the low-scored run is still there", 1,
                second.size());
        assertTrue("its score survived unchanged",
                second.get(0).score() == 10);
    }

    private static void testHistoryGrowsBeyondInitialCapacity() {
        ScoreHistory history = new ScoreHistory();

        for (int i = 0; i < 40; i++) {
            history.append("Runner" + (i % 7), 100 + i);
        }

        assertEquals("history grows past the initial array size", 40,
                history.size());
        assertTrue("no run lost while growing",
                history.findScoresByName("Runner0").size() == 6);
        assertTrue("search still finds every player",
                history.findScoresByName("Runner3").size() == 6);
    }

    private static void testMoreThanTenRunsPreservedInCsv() {
        ScoreHistory history = new ScoreHistory();

        for (int i = 0; i < 12; i++) {
            history.append("Runner" + i, 100 + i);
        }

        Path file = tempDir.resolve("twelve-runs.csv");
        assertTrue("save succeeded", HighScoreStorage.save(history, file));

        ScoreHistory reloaded = new ScoreHistory();
        assertEquals("all 12 runs reload — CSV keeps beyond top 10", 12,
                HighScoreStorage.load(reloaded, file));

        assertTrue("run 0 survived the round trip",
                reloaded.findScoresByName("Runner0").size() == 1);
        assertTrue("run 11 survived the round trip",
                reloaded.findScoresByName("Runner11").size() == 1);
    }

    private static void testPlayerOutsideTopTenSearchableAfterReload() {
        Path file = tempDir.resolve("outside-top-ten.csv");

        // 12 qualifying runs by other players, then one slow run.
        ScoreHistory recorded = new ScoreHistory();

        for (int i = 0; i < 12; i++) {
            recorded.append("Racer" + i, 200 + i);
        }

        recorded.append("Wanderer", 55);
        HighScoreStorage.save(recorded, file);

        // Restart: load history, rebuild the board exactly like the game.
        ScoreHistory reloaded = new ScoreHistory();
        HighScoreStorage.load(reloaded, file);

        HighScoreBoard board = new HighScoreBoard();

        for (HighScoreBoard.ScoreEntry run : reloaded.entries()) {
            board.record(run.player(), run.score());
        }

        assertTrue("worst runs fall out of the ten-row display",
                reloaded.findScoresByName("Wanderer").size() == 1
                        && board.findScoresByName("Wanderer").size() == 0);

        HighScoreBoard.ScoreEntry found =
                reloaded.findBestScoreByName("WANDERER");

        assertTrue("the buried run is searchable from history", found != null);
        assertTrue("its full score is returned", found.score() == 55);
        assertTrue("second-best search also returns the buried run",
                reloaded.findScoresByName(" Wanderer ").size() == 1);
    }

    private static void testRepeatedRunsStaySeparate() {
        ScoreHistory history = new ScoreHistory();

        history.append("Echo", 300);
        history.append("Echo", 450);
        history.append("Echo", 400);

        List<HighScoreBoard.ScoreEntry> runs =
                history.findScoresByName("echo");

        assertEquals("three separate runs for one player", 3, runs.size());
        assertTrue("best run first", runs.get(0).score() == 450);
        assertTrue("equal-or-lower scores keep run order", runs.get(1).score() == 400);
        assertTrue("then the earliest", runs.get(2).score() == 300);
        assertTrue("best-score query returns the highest run",
                history.findBestScoreByName("Echo").score() == 450);
    }

    private static void testTopTenLeaderboardRebuildsFromHistory() {
        ScoreHistory history = new ScoreHistory();

        for (int i = 0; i < 14; i++) {
            history.append("Player" + i, 91 * (14 - i));
        }

        HighScoreBoard board = new HighScoreBoard();

        for (HighScoreBoard.ScoreEntry run : history.entries()) {
            board.record(run.player(), run.score());
        }

        assertEquals("board still capped at ten rows", 10, board.size());
        assertEquals("board best is the history best",
                history.entries().get(0).player(),
                board.entries().get(0).player());
        assertTrue("board is descending at the top",
                board.entries().get(0).score() >= board.entries().get(1).score());

        List<HighScoreBoard.ScoreEntry> boardRows = board.entries();

        for (int i = 1; i < boardRows.size(); i++) {
            assertTrue("insertion sort keeps every row ordered",
                    boardRows.get(i - 1).score()
                            >= boardRows.get(i).score());
        }

        assertTrue("board contains only qualifying history runs",
                board.findScoresByName("Player0").size() == 1
                        && board.findScoresByName("Player9").size() == 1
                        && board.findScoresByName("Player13").size() == 0);
    }

    private static void testBestScoreCanBeOutsideTheTopTen() {
        ScoreHistory history = new ScoreHistory();

        // The player's only run that qualifies for the board is 100, but
        // a later weak run does not qualify; the history still answers
        // best-score queries with the highest of all runs.
        history.append("Solo", 100);
        history.append("Solo", 50);

        HighScoreBoard board = new HighScoreBoard();
        board.record("Solo", 100);
        board.record("Solo", 50);

        assertTrue("qualifying run stays on the board",
                board.findScoresByName("Solo").size() == 2);
        assertEquals("history records both runs", 2, history.size());
        assertTrue("history best equals board best here",
                history.findBestScoreByName("solo").score() == 100);

        // Without qualifying runs, only history answers: fill the board
        // with ten better runs first so 'Hermit's scores are rejected.
        ScoreHistory buried = new ScoreHistory();
        buried.append("Hermit", 30);
        buried.append("Hermit", 20);
        HighScoreBoard blind = new HighScoreBoard();

        for (int i = 0; i < 10; i++) {
            blind.record("Top" + i, 200 + i);
        }

        for (HighScoreBoard.ScoreEntry run : buried.entries()) {
            blind.record(run.player(), run.score());
        }

        assertTrue("board full with better runs",
                blind.findScoresByName("Top" + 9).size() == 1);
        assertTrue("unqualified player has no board row",
                blind.findBestScoreByName("Hermit") == null);
        assertTrue("but history reports the best known run",
                buried.findBestScoreByName("Hermit").score() == 30);
    }

    private static void testSpecialCharactersViaHistoryRoundTrip() {
        ScoreHistory history = new ScoreHistory();
        String trickyName = 'Q' + "uirk, \"The\nComma\"" + ' ';
        history.append(trickyName, 480);

        Path file = tempDir.resolve("history-special.csv");
        HighScoreStorage.save(history, file);

        ScoreHistory reloaded = new ScoreHistory();
        HighScoreStorage.load(reloaded, file);

        List<HighScoreBoard.ScoreEntry> matches =
                reloaded.findScoresByName(trickyName);

        assertTrue("name with comma, quote, and newline restored", matches.size() == 1);
        assertTrue("escaped name kept the trimmed characters",
                trickyName.trim().equals(matches.get(0).player()));
    }

    private static void testMissingAndMalformedFilesLoadSafely() throws IOException {
        ScoreHistory empty = new ScoreHistory();

        assertEquals("missing file loads zero runs", 0,
                HighScoreStorage.load(empty,
                        tempDir.resolve("nope.csv")));
        assertTrue("history unchanged on missing file", empty.isEmpty());

        Path bad = tempDir.resolve("bad-history.csv");
        Files.writeString(bad, "player,score\n"
                + "Good,700\n"
                + "\n"
                + "LoneName\n"
                + "Bad,12,x,y\n"
                + "NumericNo,nope\n"
                + "Good,250\n"
                + "Good,999\n");

        ScoreHistory history = new ScoreHistory();
        assertEquals("only valid rows load into history", 3,
                HighScoreStorage.load(history, bad));

        assertTrue("first valid run survived",
                history.findScoresByName("Good").size() == 3);
        assertTrue("best run found among valid rows",
                history.findBestScoreByName("Good").score() == 999);
        assertTrue("invalid rows skipped",
                history.findScoresByName("nope").isEmpty());
    }

    private static void testNullAndBlankNamesHandledLikeTheBoard() {
        ScoreHistory history = new ScoreHistory();

        history.append(null, 42);
        history.append("   ", 43);
        history.append("  Kiwi  ", 44);

        assertTrue("null name becomes Player",
                history.findScoresByName("Player").size() == 2);
        assertTrue("blank name becomes Player too",
                history.findBestScoreByName("  PLAYER  ") != null);
        assertEquals("board semantics: names trimmed",
                "Kiwi", history.findScoresByName("kiwi").get(0).player());
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
