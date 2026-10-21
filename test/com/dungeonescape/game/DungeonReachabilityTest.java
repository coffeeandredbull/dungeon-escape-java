
package com.dungeonescape.game;

import java.util.List;

/**
 * Dependency-free tests for recursive dungeon reachability.
 *
 * Run with:
 * java -cp "out;test_out" com.dungeonescape.game.DungeonReachabilityTest
 */
public class DungeonReachabilityTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        testOpenGridIsReachable();
        testWallBlocksDirectRoute();
        testReachableWithObstacleDetour();
        testInvalidTargetsAreRejected();
        testRecursiveSearchHandlesCycles();
        testVisitedTilesAreNotCountedTwice();
        testTorchTilesAreSolid();
        testRealLevelObjectivesAreReachable();
        testRealLevelsAreFullyConnected();

        System.out.println();
        System.out.println("Passed: " + passed + ", Failed: " + failed);

        if (failed > 0) {
            System.out.println("TESTS FAILED");
            System.exit(1);
        }

        System.out.println("All tests passed.");
        System.exit(0);
    }

    private static void testOpenGridIsReachable() {
        Dungeon dungeon = new Dungeon(new String[] {
                "#####",
                "#...#",
                "#...#",
                "#...#",
                "#####"
        }, 1, 1);

        assertTrue("nearby floor is reachable",
                DungeonReachability.isReachable(dungeon, 2, 1));
        assertTrue("distant floor is reachable",
                DungeonReachability.isReachable(dungeon, 3, 3));
    }

    private static void testWallBlocksDirectRoute() {
        Dungeon dungeon = new Dungeon(new String[] {
                "#######",
                "#..#..#",
                "#..#..#",
                "#..#..#",
                "#######"
        }, 1, 1);

        assertFalse("floor behind solid wall is unreachable",
                DungeonReachability.isReachable(dungeon, 4, 1));
    }

    private static void testReachableWithObstacleDetour() {
        Dungeon dungeon = new Dungeon(new String[] {
                "#######",
                "#..#..#",
                "#.....#",
                "#..#..#",
                "#######"
        }, 1, 1);

        assertTrue("target is reachable around the wall",
                DungeonReachability.isReachable(dungeon, 5, 1));
    }

    private static void testInvalidTargetsAreRejected() {
        Dungeon dungeon = new Dungeon(new String[] {
                "#####",
                "#...#",
                "#...#",
                "#####"
        }, 1, 1);

        assertFalse("negative column rejected",
                DungeonReachability.isReachable(dungeon, -1, 1));
        assertFalse("negative row rejected",
                DungeonReachability.isReachable(dungeon, 1, -1));
        assertFalse("column beyond grid rejected",
                DungeonReachability.isReachable(dungeon, 5, 1));
        assertFalse("row beyond grid rejected",
                DungeonReachability.isReachable(dungeon, 1, 4));
        assertFalse("wall target rejected",
                DungeonReachability.isReachable(dungeon, 0, 0));
        assertFalse("null dungeon rejected",
                DungeonReachability.isReachable(null, 1, 1));
    }

    private static void testRecursiveSearchHandlesCycles() {
        Dungeon dungeon = new Dungeon(new String[] {
                "######",
                "#....#",
                "#....#",
                "#....#",
                "#....#",
                "######"
        }, 1, 1);

        assertTrue("recursive search terminates on connected loops",
                DungeonReachability.isFullyConnected(dungeon));
        assertTrue("target in loop remains reachable",
                DungeonReachability.isReachable(dungeon, 4, 4));
    }

    private static void testVisitedTilesAreNotCountedTwice() {
        Dungeon dungeon = new Dungeon(new String[] {
                "######",
                "#....#",
                "#....#",
                "######"
        }, 1, 1);

        boolean[][] visited = new boolean[dungeon.rows()][dungeon.cols()];

        int firstCount = DungeonReachability.floodFill(
                dungeon, visited, dungeon.startCol(), dungeon.startRow());

        int secondCount = DungeonReachability.floodFill(
                dungeon, visited, dungeon.startCol(), dungeon.startRow());

        assertEquals("first flood visits all eight floor tiles",
                8, firstCount);
        assertEquals("second flood does not count visited tiles again",
                0, secondCount);
    }

    private static void testTorchTilesAreSolid() {
        Dungeon dungeon = new Dungeon(new String[] {
                "#####",
                "#.T.#",
                "#...#",
                "#####"
        }, 1, 1);

        assertFalse("torch tile cannot be reached",
                DungeonReachability.isReachable(dungeon, 2, 1));
        assertTrue("floor beyond torch is reachable by another route",
                DungeonReachability.isReachable(dungeon, 3, 1));
    }

    private static void testRealLevelObjectivesAreReachable() {
        List<Level> levels = Levels.create();

        for (Level level : levels) {
            GameObject key = findObject(level, GameObject.Kind.KEY);
            GameObject door = findObject(level, GameObject.Kind.DOOR);

            assertTrue(level.name() + " has a key", key != null);
            assertTrue(level.name() + " has a door", door != null);

            if (key != null) {
                assertTrue(level.name() + " key is reachable",
                        DungeonReachability.isReachable(
                                level.dungeon(), key.col(), key.row()));
            }

            if (door != null) {
                assertTrue(level.name() + " door is reachable",
                        DungeonReachability.isReachable(
                                level.dungeon(), door.col(), door.row()));
            }
        }
    }

    private static void testRealLevelsAreFullyConnected() {
        for (Level level : Levels.create()) {
            assertTrue(level.name() + " walkable area is connected",
                    DungeonReachability.isFullyConnected(level.dungeon()));
        }
    }

    private static GameObject findObject(Level level, GameObject.Kind kind) {
        for (GameObject object : level.objects()) {
            if (object.kind() == kind) {
                return object;
            }
        }

        return null;
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
}
