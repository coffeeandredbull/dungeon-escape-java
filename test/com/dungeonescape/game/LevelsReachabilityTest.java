package com.dungeonescape.game;

import java.util.List;

import static com.dungeonescape.game.GameObject.Kind;

/**
 * Dependency-free tests for the reachability wiring in Levels: every real
 * built level passes the recursive DFS validation at level creation, and
 * a layout that strands an objective is rejected fail-fast, mirroring how
 * the running game consults the checker.
 *
 * Run with:
 * java -cp "out;test_out" com.dungeonescape.game.LevelsReachabilityTest
 */
public class LevelsReachabilityTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        testRealLevelsPassValidation();
        testUnreachableKeyIsRejectedAtBuild();
        testUnreachableDoorIsRejectedAtBuild();
        testUnreachableChestAndPotionAreRejected();
        testRemovedObjectiveIsIgnored();
        testLevelWithoutObjectivesPasses();
        testValidationMessageIdentifiesTheObjective();

        System.out.println();
        System.out.println("Passed: " + passed + ", Failed: " + failed);

        if (failed > 0) {
            System.out.println("TESTS FAILED");
            System.exit(1);
        }

        System.out.println("All tests passed.");
        System.exit(0);
    }

    private static void testRealLevelsPassValidation() {
        // The genuine game layouts must validate cleanly —
        // Levels.create() itself calls this same validator.
        List<Level> levels = Levels.create();

        assertTrue("all three real levels build", levels.size() == 3);
        assertTrue("real levels contain no stranded objectives",
                levelsReachable(levels));
    }

    private static void testUnreachableKeyIsRejectedAtBuild() {
        // Column 2 is solid for both inner rows, so the alcove at column
        // 3 is a separate region never reachable from the start (1,1);
        // walkable tile-to-tile moves are 4-directional only.
        String[] layout = {
                "#####",
                "#.#.#",
                "#.#.#",
                "#####"
        };
        Level stranded = new Level("SealedKey",
                new Dungeon(layout, 1, 1),
                List.of(new GameObject(Kind.KEY, 3, 1)));

        assertTrue("unreachable key rejected",
                !levelsReachable(List.of(stranded)));
    }

    private static void testUnreachableDoorIsRejectedAtBuild() {
        // Column 3 is solid for the door's whole room, so tile (4,3) is
        // in a region sealed off from the start (1,1).
        String[] sealed = {
                "######",
                "#..#.#",
                "####.#",
                "#....#",
                "######"
        };
        Level doorLevel = new Level("SealedDoor",
                new Dungeon(sealed, 1, 1),
                List.of(new GameObject(Kind.DOOR, 4, 3)));

        assertTrue("door room separated by a solid wall column",
                !levelsReachable(List.of(doorLevel)));
    }

    private static void testUnreachableChestAndPotionAreRejected() {
        String[] layout = {
                "#####",
                "#.#.#",
                "#.#.#",
                "#####"
        };
        Level twoStranded = new Level("SealedItems",
                new Dungeon(layout, 1, 1),
                List.of(
                        new GameObject(Kind.CHEST, 3, 1),
                        new GameObject(Kind.POTION, 3, 2)));

        assertTrue("stranded chest and potion rejected",
                !levelsReachable(List.of(twoStranded)));
    }

    private static void testRemovedObjectiveIsIgnored() {
        String[] layout = {
                "#####",
                "#.#.#",
                "#...#",
                "#####"
        };
        GameObject collected = new GameObject(Kind.KEY, 3, 1);
        collected.remove();

        Level runAfterPickup = new Level("OpenRun",
                new Dungeon(layout, 1, 2),
                List.of(collected));

        assertTrue("an already-collected objective is not validated",
                levelsReachable(List.of(runAfterPickup)));
    }

    private static void testLevelWithoutObjectivesPasses() {
        String[] layout = {
                "###",
                "#.#",
                "###"
        };

        Level empty = new Level("Bare",
                new Dungeon(layout, 1, 1), List.of());

        assertTrue("level without objectives validates",
                levelsReachable(List.of(empty)));
    }

    private static void testValidationMessageIdentifiesTheObjective() {
        // The fail-fast gate reports the level, objective, and stranded
        // tile, matching the dungeon layout validator's error style.
        // Column 2 is solid for both inner rows: the alcove at (3,1) is
        // sealed off from the start (1,1).
        String[] layout = {
                "#####",
                "#.#.#",
                "#.#.#",
                "#####"
        };
        Level stranded = new Level("SealedKey",
                new Dungeon(layout, 1, 1),
                List.of(new GameObject(Kind.KEY, 3, 1)));

        try {
            Levels.validateReachableObjectives(List.of(stranded));
            assertTrue("stranded layout rejected, nothing thrown",
                    false);
        } catch (IllegalStateException e) {
            assertTrue("message names the level",
                    e.getMessage().contains("SealedKey"));
            assertTrue("message names the objective kind",
                    e.getMessage().contains("KEY"));
            assertTrue("message gives the stranded tile",
                    e.getMessage().contains("(3,1)"));
        }
    }

    /* --- helpers -------------------------------------------------- */

    /**
     * Returns true when {@link Levels#validateReachableObjectives(List)}
     * accepts every level (no stranded objective).
     */
    private static boolean levelsReachable(List<Level> levels) {
        try {
            Levels.validateReachableObjectives(levels);
            return true;
        } catch (IllegalStateException e) {
            System.out.println("  (validator: " + e.getMessage() + ")");
            return false;
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
}
