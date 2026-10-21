package com.dungeonescape.game;

import java.util.List;

/**
 * Dependency-free tests for the move history: the hand-written LIFO stack
 * itself, the undo action on GameSession, tile-level recording rules
 * (blocked attempts are not recorded), boundary/collision safety, game
 * state that undo deliberately does not rewind, and history clearing on
 * level transitions.
 *
 * Run with:
 * java -cp "out;test_out" com.dungeonescape.game.MoveHistoryTest
 */
public class MoveHistoryTest {

    private static final double DT = 1.0 / 60.0;
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        testEmptyHistoryPopsNothing();
        testPushPopLIFOOrder();
        testFullStackKeepsMostRecentPositions();
        testClearEmptiesTheStack();
        testNoUndoAtStartWithoutMoves();
        testBlockedMovementEntersNoHistory();
        testNoHistoryRecordedUntilTheFirstTileCrossing();
        testWalkRightThenUndoRestoresPreviousTile();
        testMultipleMovesThenMultipleUndos();
        testUndoKeepsPlayerWalkableAndInside();
        testUndoDoesNotRewindScoreOrObjects();
        testLevelTransitionClearsHistoryAndRestartsRecording();
        testExistingMovementBehaviourUnchanged();

        System.out.println();
        System.out.println("Passed: " + passed + ", Failed: " + failed);

        if (failed > 0) {
            System.out.println("TESTS FAILED");
            System.exit(1);
        }

        System.out.println("All tests passed.");
        System.exit(0);
    }

    /* --- MoveHistory unit tests ------------------------------------- */

    private static void testEmptyHistoryPopsNothing() {
        MoveHistory history = new MoveHistory();

        assertTrue("new history is empty", history.isEmpty());
        assertEquals("new history has size zero", 0, history.size());
        assertTrue("pop from empty history is null",
                history.pop() == null);
    }

    private static void testPushPopLIFOOrder() {
        MoveHistory history = new MoveHistory();

        history.push(10, 100);
        history.push(20, 200);
        history.push(30, 300);

        assertEquals("three pushes stored", 3, history.size());

        MoveHistory.Position first = history.pop();
        assertEquals("newest position popped first", 30, (int) first.x());
        assertEquals("newest Y popped first", 300, (int) first.y());

        MoveHistory.Position second = history.pop();
        assertEquals("then the middle entry", 20, (int) second.x());

        MoveHistory.Position third = history.pop();
        assertEquals("oldest entry popped last", 10, (int) third.x());

        assertTrue("stack empty after three pops", history.isEmpty());
        assertTrue("fourth pop is still a safe null", history.pop() == null);
    }

    private static void testFullStackKeepsMostRecentPositions() {
        MoveHistory history = new MoveHistory(3);

        history.push(1, 10);
        history.push(2, 20);
        history.push(3, 30);
        history.push(4, 40);

        assertEquals("capacity respected", 3, history.size());
        assertEquals("oldest entry dropped", 4, (int) history.pop().x());
        assertEquals("then third", 3, (int) history.pop().x());
        assertEquals("then second", 2, (int) history.pop().x());
        assertTrue("first entry was never stored", history.pop() == null);
    }

    private static void testClearEmptiesTheStack() {
        MoveHistory history = new MoveHistory();

        history.push(5, 50);
        history.push(6, 60);
        history.clear();

        assertEquals("clear resets size", 0, history.size());
        assertTrue("clear empties the stack", history.isEmpty());
        assertTrue("pop after clear is null", history.pop() == null);
    }

    /* --- GameSession integration tests ------------------------------ */

    private static void testNoUndoAtStartWithoutMoves() {
        GameSession session = sessionIn(corridor());

        assertTrue("no moves recorded at the start position",
                !session.undoLastMove());
        assertEquals("start x unchanged",
                corridorStartX(), session.player().x(), 1e-9);
        assertEquals("start y unchanged",
                corridorStartY(), session.player().y(), 1e-9);
    }

    private static void testBlockedMovementEntersNoHistory() {
        Dungeon dungeon = corridor();
        GameSession session = sessionIn(dungeon);
        InputState up = input("w");

        // Row 0 is a solid wall, so holding up stays blocked.
        for (int i = 0; i < 90; i++) {
            session.update(DT, up);
        }

        assertTrue("blocked pressing records nothing",
                !session.undoLastMove());
        // Bisection slides the player against the wall (y clamps to just
        // above row 0), but the tile itself never changes, and tile-level
        // history ignores movement inside a single tile.
        assertEquals("player still on the same tile row", 1,
                tileOf(session.player().y()));
        assertTrue("player is not colliding",
                !session.player().isColliding(dungeon));
    }

    private static void testNoHistoryRecordedUntilTheFirstTileCrossing() {
        GameSession session = sessionIn(corridor());
        InputState right = input("d");

        // One step is SPEED * DT = 2.5 px per tick; the player's center
        // reaches the second tile (x >= 96) on the tenth update, so the
        // first nine updates walk inside one tile and record nothing.
        for (int i = 0; i < 9; i++) {
            session.update(DT, right);
        }

        assertTrue("moving within one tile records nothing",
                !session.undoLastMove());

        session.update(DT, right);

        assertTrue("the tile-crossing tick records exactly one move",
                session.undoLastMove());
    }

    private static void testWalkRightThenUndoRestoresPreviousTile() {
        Dungeon dungeon = corridor();
        GameSession session = sessionIn(dungeon);
        int startCol = dungeon.startCol();

        assertTrue("walked one tile right",
                walkUntilColumnChanges(session, input("d"), startCol));

        assertTrue("undo restored a recorded position",
                session.undoLastMove());
        assertEquals("player is back on the previous tile column",
                startCol, tileOf(session.player().x()));
        assertEquals("player keeps the same row", 1,
                tileOf(session.player().y()));
        assertTrue("restored position is collision-free",
                !session.player().isColliding(dungeon));
        assertTrue("history is empty after undoing",
                !session.undoLastMove());
    }

    private static void testMultipleMovesThenMultipleUndos() {
        Dungeon dungeon = corridor();
        GameSession session = sessionIn(dungeon);

        // Walk the full corridor: tiles 1 -> 2 -> 3 -> 4 -> 5, which
        // crosses four tile borders and records four positions.
        InputState right = input("d");
        for (int i = 0; i < 100; i++) {
            session.update(DT, right);
        }

        int undos = 0;
        while (session.undoLastMove()) {
            undos++;
        }

        assertEquals("every tile crossing undoes exactly once", 4, undos);
        assertEquals("final undo returns to the first column", 1,
                tileOf(session.player().x()));
        assertTrue("history empty after the full unwind",
                !session.undoLastMove());
        assertTrue("start position still collision-free",
                !session.player().isColliding(dungeon));
    }

    private static void testUndoKeepsPlayerWalkableAndInside() {
        Dungeon dungeon = corridor();
        GameSession session = sessionIn(dungeon);

        InputState right = input("d");
        for (int i = 0; i < 100; i++) {
            session.update(DT, right);
        }

        session.undoLastMove();
        session.undoLastMove();

        double x = session.player().x();
        double y = session.player().y();

        assertTrue("undo stays inside the horizontal bounds",
                x >= Player.HALF_SIZE
                        && x <= dungeon.cols() * Dungeon.TILE - Player.HALF_SIZE);
        assertTrue("undo stays inside the vertical bounds",
                y >= Player.HALF_SIZE
                        && y <= dungeon.rows() * Dungeon.TILE - Player.HALF_SIZE);
        assertTrue("undo position is not inside a wall",
                !session.player().isColliding(dungeon));
    }

    private static void testUndoDoesNotRewindScoreOrObjects() {
        Dungeon dungeon = corridor();
        GameObject chest = new GameObject(GameObject.Kind.CHEST, 2, 1);
        GameSession session = sessionIn(dungeon, chest);

        assertTrue("walked onto the chest tile",
                walkUntilColumnChanges(session, input("d"),
                        dungeon.startCol()));

        assertEquals("chest collected", GameSession.CHEST_SCORE,
                session.score());
        assertTrue("chest removed from the level",
                session.level().objects().get(0).removed());

        session.undoLastMove();

        assertEquals("undo does not rewind score", GameSession.CHEST_SCORE,
                session.score());
        assertTrue("chest stays collected after undo",
                session.level().objects().get(0).removed());
    }

    private static void testLevelTransitionClearsHistoryAndRestartsRecording() {
        Level first = new Level("Level 1", corridor(), List.of(
                new GameObject(GameObject.Kind.KEY, 2, 1),
                new GameObject(GameObject.Kind.DOOR, 3, 1)));
        Level second = new Level("Level 2", corridor(), List.of());

        GameSession session = new GameSession(
                List.of(first, second), sound -> {
        });

        // Two tile crossings (into the key tile and the door tile) are
        // recorded before the door tile opens the exit animation.
        InputState right = input("d");
        for (int i = 0; i < 40
                && !session.level().objects().get(1).removed(); i++) {
            session.update(DT, right);
        }

        assertTrue("key collected on the way", session.hasKey());
        assertTrue("door removed for the open-door animation",
                session.level().objects().get(1).removed());

        assertTrue("undo is blocked during the door animation",
                !session.undoLastMove());

        session.update(GameSession.DOOR_OPEN_SECONDS + DT, new InputState());

        assertEquals("level transition happened", 1, session.levelIndex());
        assertTrue("stale level-1 entries cannot be undone",
                !session.undoLastMove());

        assertTrue("fresh walk recorded on the new level",
                walkUntilColumnChanges(session, input("d"), 1));
        assertTrue("undo works again on the new level",
                session.undoLastMove());
    }

    private static void testExistingMovementBehaviourUnchanged() {
        GameSession session = sessionIn(corridor());
        InputState right = input("d");

        session.update(DT, right);

        assertEquals("one tick still moves one speed step",
                2.5, session.player().x() - corridorStartX(), 1e-9);

        double before = session.player().x();
        InputState opposite = new InputState();
        opposite.set("a", true);
        opposite.set("d", true);
        for (int i = 0; i < 10; i++) {
            session.update(DT, opposite);
        }

        assertEquals("opposing inputs still cancel movement",
                before, session.player().x(), 1e-9);
    }

    /* --- helpers ----------------------------------------------------- */

    /** Open 7 x 3 corridor: floor is row 1, tiles 1..5. */
    private static Dungeon corridor() {
        String[] layout = {
                "#######",
                "#.....#",
                "#######"
        };
        return new Dungeon(layout, 1, 1);
    }

    private static double corridorStartX() {
        return Dungeon.TILE + Dungeon.TILE / 2.0;
    }

    private static double corridorStartY() {
        return Dungeon.TILE + Dungeon.TILE / 2.0;
    }

    private static GameSession sessionIn(Dungeon dungeon, GameObject... objects) {
        List<Level> levels = List.of(
                new Level("Test", dungeon, List.of(objects)));

        return new GameSession(levels, sound -> {
        });
    }

    private static InputState input(String key) {
        InputState input = new InputState();
        input.set(key, true);
        return input;
    }

    /**
     * Walks with the supplied input until the player's center tile column
     * differs from {@code fromCol}. Exactly one history entry is pushed
     * per tile crossing, so a change followed by an undo is one move.
     */
    private static boolean walkUntilColumnChanges(
            GameSession session, InputState input, int fromCol) {

        for (int i = 0; i < 200; i++) {
            session.update(DT, input);

            if (tileOf(session.player().x()) != fromCol) {
                return true;
            }
        }

        return false;
    }

    private static int tileOf(double pixel) {
        return (int) Math.floor(pixel / Dungeon.TILE);
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
            String message, double expected, double actual, double epsilon) {
        assertTrue(message + " (expected " + expected
                + ", got " + actual + ")",
                Math.abs(expected - actual) <= epsilon);
    }
}
