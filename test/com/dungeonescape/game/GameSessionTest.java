
package com.dungeonescape.game;

import java.util.ArrayList;
import java.util.List;

/**
 * Dependency-free tests for GameSession: object interactions, damage,
 * level advancement, victory, and sound cues.
 *
 * Run with:
 * java -cp "out;test_out" com.dungeonescape.game.GameSessionTest
 */
public class GameSessionTest {

    private static final double DT = 1.0 / 60.0;
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        testChestAddsScoreAndPlaysSound();
        testPotionIsConsumed();
        testKeyPickupSetsFlag();
        testDoorWithoutKeyDoesNothing();
        testDoorWithKeyCompletesSingleLevel();
        testDoorWithKeyAdvancesToNextLevel();
        testMonsterDamageAndInvulnerability();
        testGameOverAfterThreeHits();
        testFootstepCuesWhileMoving();
        testLevelsStructure();
        testCustomDungeonLayout();

        System.out.println();
        System.out.println("Passed: " + passed + ", Failed: " + failed);

        if (failed > 0) {
            System.out.println("TESTS FAILED");
            System.exit(1);
        }

        System.out.println("All tests passed.");
        System.exit(0);
    }

    private static void testChestAddsScoreAndPlaysSound() {
        List<String> sounds = new ArrayList<>();
        Level level = levelWith(
                new GameObject(GameObject.Kind.CHEST, 5, 5));

        GameSession session = new GameSession(List.of(level), sounds::add);
        session.update(DT, new InputState());

        assertEquals("chest adds score",
                GameSession.CHEST_SCORE, session.score());
        assertTrue("chest removed after opening",
                level.objects().get(0).removed());
        assertTrue("chest sound played", sounds.contains("chest"));
    }

    private static void testPotionIsConsumed() {
        List<String> sounds = new ArrayList<>();
        Level level = levelWith(
                new GameObject(GameObject.Kind.POTION, 5, 5));

        GameSession session = new GameSession(List.of(level), sounds::add);
        session.update(DT, new InputState());

        assertTrue("potion removed after drinking",
                level.objects().get(0).removed());
        assertEquals("potion at full health keeps 3 lives",
                GameSession.START_LIVES, session.lives());
        assertTrue("potion sound played", sounds.contains("potion"));
    }

    private static void testKeyPickupSetsFlag() {
        List<String> sounds = new ArrayList<>();
        Level level = levelWith(
                new GameObject(GameObject.Kind.KEY, 5, 5));

        GameSession session = new GameSession(List.of(level), sounds::add);
        session.update(DT, new InputState());

        assertTrue("key collected sets hasKey", session.hasKey());
        assertTrue("key removed after pickup",
                level.objects().get(0).removed());
        assertTrue("key sound played", sounds.contains("key"));
    }

    private static void testDoorWithoutKeyDoesNothing() {
        List<String> sounds = new ArrayList<>();
        Level level = levelWith(
                new GameObject(GameObject.Kind.DOOR, 5, 5));

        GameSession session = new GameSession(List.of(level), sounds::add);
        session.update(DT, new InputState());

        assertFalse("door stays closed without a key",
                level.objects().get(0).removed());
        assertFalse("no victory without a key", session.isVictory());
        assertFalse("no door sound without a key",
                sounds.contains("door"));
    }

    private static void testDoorWithKeyCompletesSingleLevel() {
        List<String> sounds = new ArrayList<>();

        Level level = new Level("Level 1", new Dungeon(), List.of(
                new GameObject(GameObject.Kind.KEY, 5, 5),
                new GameObject(GameObject.Kind.DOOR, 5, 5)));

        GameSession session = new GameSession(List.of(level), sounds::add);
        session.update(DT, new InputState());

        assertTrue("key collected", session.hasKey());
        assertTrue("door removed for open-door animation",
                level.objects().get(1).removed());
        assertFalse("victory waits for door animation",
                session.isVictory());
        assertTrue("door sound played", sounds.contains("door"));

        session.update(
                GameSession.DOOR_OPEN_SECONDS + DT,
                new InputState());

        assertTrue("single level completes as victory",
                session.isVictory());
        assertTrue("victory sound played", sounds.contains("victory"));
    }

    private static void testDoorWithKeyAdvancesToNextLevel() {
        List<String> sounds = new ArrayList<>();

        Level first = new Level("Level 1", new Dungeon(), List.of(
                new GameObject(GameObject.Kind.KEY, 5, 5),
                new GameObject(GameObject.Kind.DOOR, 5, 5)));

        Level second = new Level("Level 2", new Dungeon(), List.of(
                new GameObject(GameObject.Kind.CHEST, 5, 5)));

        GameSession session = new GameSession(
                List.of(first, second), sounds::add);

        session.update(DT, new InputState());

        assertEquals("level stays during door animation",
                0, session.levelIndex());
        assertFalse("victory has not occurred",
                session.isVictory());

        session.update(
                GameSession.DOOR_OPEN_SECONDS + DT,
                new InputState());

        assertEquals("advanced to the second level",
                1, session.levelIndex());
        assertEquals("current level is Level 2",
                "Level 2", session.level().name());
        assertFalse("not victory yet", session.isVictory());
    }

    private static void testMonsterDamageAndInvulnerability() {
        List<String> sounds = new ArrayList<>();
        Level level = levelWith(
                new GameObject(GameObject.Kind.MONSTER, 5, 5));

        GameSession session = new GameSession(List.of(level), sounds::add);
        session.update(DT, new InputState());

        assertEquals("first hit removes one life",
                GameSession.START_LIVES - 1, session.lives());

        session.update(DT, new InputState());

        assertEquals("invulnerability blocks a second hit",
                GameSession.START_LIVES - 1, session.lives());

        session.update(
                GameSession.INVULN_SECONDS + 0.1,
                new InputState());

        assertEquals("hit lands after invulnerability expires",
                GameSession.START_LIVES - 2, session.lives());
        assertTrue("monster sound played", sounds.contains("monster"));
    }

    private static void testGameOverAfterThreeHits() {
        List<String> sounds = new ArrayList<>();
        Level level = levelWith(
                new GameObject(GameObject.Kind.MONSTER, 5, 5));

        GameSession session = new GameSession(List.of(level), sounds::add);

        session.update(DT, new InputState());
        session.update(
                GameSession.INVULN_SECONDS + 0.1,
                new InputState());
        session.update(
                GameSession.INVULN_SECONDS + 0.1,
                new InputState());

        assertTrue("game over after three hits", session.isGameOver());
        assertEquals("lives clamp to zero", 0, session.lives());
        assertTrue("game over sound played",
                sounds.contains("gameover"));
    }

    private static void testFootstepCuesWhileMoving() {
        List<String> sounds = new ArrayList<>();
        Level level = new Level("Level 1", new Dungeon(), List.of());

        GameSession session = new GameSession(List.of(level), sounds::add);
        InputState input = new InputState();
        input.set("d", true);

        for (int i = 0; i < 60; i++) {
            session.update(DT, input);
        }

        assertTrue("footstep cue while walking",
                sounds.contains("footstep"));
    }

    private static void testLevelsStructure() {
        List<Level> levels = Levels.create();

        assertTrue("at least one level is defined", !levels.isEmpty());

        Level first = levels.get(0);

        assertEquals("level 1 name", "Level 1", first.name());
        assertTrue("level 1 has a dungeon", first.dungeon() != null);

        boolean hasMonster = false;
        boolean hasChest = false;
        boolean hasDoor = false;

        for (GameObject object : first.objects()) {
            hasMonster |= object.kind() == GameObject.Kind.MONSTER;
            hasChest |= object.kind() == GameObject.Kind.CHEST;
            hasDoor |= object.kind() == GameObject.Kind.DOOR;
        }

        assertTrue("level 1 has a monster", hasMonster);
        assertTrue("level 1 has a chest", hasChest);
        assertTrue("level 1 has a door", hasDoor);
    }

    private static void testCustomDungeonLayout() {
        String[] layout = {
                "#####",
                "#...#",
                "#...#",
                "#####"
        };

        Dungeon dungeon = new Dungeon(layout, 2, 1);

        assertEquals("custom columns", 5, dungeon.cols());
        assertEquals("custom rows", 4, dungeon.rows());
        assertTrue("custom start is walkable",
                dungeon.isWalkable(2, 1));
        assertTrue("custom wall is solid",
                dungeon.isSolid(0, 0));
        assertEquals("custom start x",
                2 * Dungeon.TILE + Dungeon.TILE / 2.0,
                dungeon.startX(), 1e-9);
    }

    private static Level levelWith(GameObject object) {
        return new Level("Test", new Dungeon(), List.of(object));
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
            String message,
            double expected,
            double actual,
            double epsilon) {
        assertTrue(message + " (expected " + expected
                + ", got " + actual + ")",
                Math.abs(expected - actual) <= epsilon);
    }

    private static void assertEquals(
            String message, String expected, String actual) {
        assertTrue(message + " (expected '" + expected
                + "', got '" + actual + "')",
                expected.equals(actual));
    }
}
