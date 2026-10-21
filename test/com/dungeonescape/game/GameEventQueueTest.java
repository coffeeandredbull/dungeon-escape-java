package com.dungeonescape.game;

import java.util.ArrayList;
import java.util.List;

/**
 * Dependency-free tests for the hand-written FIFO game-event queue: the
 * circular-buffer mechanics (empty dequeue, FIFO order, interleaving,
 * wraparound, full-queue policy, size/empty accuracy, repeat cycles) and
 * the GameSession integration that delivers audio cues in FIFO order.
 *
 * Run with:
 * java -cp "out;test_out" com.dungeonescape.game.GameEventQueueTest
 */
public class GameEventQueueTest {

    private static final double DT = 1.0 / 60.0;
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        testNewQueueIsEmptyAndDequeuesNull();
        testFIFOOrder();
        testInterleavedEnqueueDequeue();
        testCircularWraparound();
        testFullQueueRejectsWithoutCorruption();
        testSizeAndEmptyStateAccuracy();
        testRejectsNullAndBlankCues();
        testRepeatedOperationsAvoidCorruption();
        testGameSessionDeliversCuesInFIFOOrder();
        testGameOverAndVictoryEventsAreDelivered();

        System.out.println();
        System.out.println("Passed: " + passed + ", Failed: " + failed);

        if (failed > 0) {
            System.out.println("TESTS FAILED");
            System.exit(1);
        }

        System.out.println("All tests passed.");
        System.exit(0);
    }

    /* --- Queue mechanics --------------------------------------------- */

    private static void testNewQueueIsEmptyAndDequeuesNull() {
        GameEventQueue queue = new GameEventQueue();

        assertTrue("new queue is empty", queue.isEmpty());
        assertEquals("new queue has size zero", 0, queue.size());
        assertTrue("dequeue from empty queue is null",
                queue.dequeue() == null);
    }

    private static void testFIFOOrder() {
        GameEventQueue queue = new GameEventQueue();

        assertTrue("first enqueued", queue.enqueue("chest"));
        assertTrue("second enqueued", queue.enqueue("key"));
        assertTrue("third enqueued", queue.enqueue("door"));

        assertEquals("first out is the first in", "chest",
                queue.dequeue());
        assertEquals("second out is the second in", "key",
                queue.dequeue());
        assertEquals("third out is the third in", "door",
                queue.dequeue());
        assertTrue("queue empty after draining", queue.isEmpty());
    }

    private static void testInterleavedEnqueueDequeue() {
        GameEventQueue queue = new GameEventQueue();

        queue.enqueue("a");
        assertEquals("interleave step 1", "a", queue.dequeue());

        queue.enqueue("b");
        queue.enqueue("c");
        assertEquals("interleave step 2", "b", queue.dequeue());

        queue.enqueue("d");
        assertEquals("interleave step 3", "c", queue.dequeue());
        assertEquals("interleave step 4", "d", queue.dequeue());

        assertTrue("queue empty after interleaving", queue.isEmpty());
        assertTrue("interleaved empty dequeue is null",
                queue.dequeue() == null);
    }

    private static void testCircularWraparound() {
        GameEventQueue queue = new GameEventQueue(3);

        queue.enqueue("a");
        queue.enqueue("b");
        queue.enqueue("c");
        assertEquals(3, queue.size());

        // Freeing two slots moves front forward; the next enqueues must
        // wrap around to the recycled slots instead of running off the
        // end of the array.
        assertEquals("a", queue.dequeue());
        assertEquals("b", queue.dequeue());

        assertTrue("enqueue wraps to a freed slot", queue.enqueue("d"));
        assertTrue("second wraparound enqueue", queue.enqueue("e"));

        assertEquals(3, queue.size());
        assertEquals("c", queue.dequeue());
        assertEquals("d kept its FIFO place", "d", queue.dequeue());
        assertEquals("e kept its FIFO place", "e", queue.dequeue());
        assertTrue("queue empty after wrapping", queue.isEmpty());
    }

    private static void testFullQueueRejectsWithoutCorruption() {
        GameEventQueue queue = new GameEventQueue(3);

        assertTrue(queue.enqueue("x"));
        assertTrue(queue.enqueue("y"));
        assertTrue(queue.enqueue("z"));
        assertEquals(3, queue.size());

        assertTrue("overflow is rejected, not stored",
                !queue.enqueue("w"));
        assertEquals("size unchanged after rejection", 3, queue.size());

        assertEquals("oldest entry intact after rejection", "x",
                queue.dequeue());
        assertEquals("second entry intact", "y", queue.dequeue());
        assertEquals("third entry intact", "z", queue.dequeue());

        assertTrue("freed slot accepts a later event",
                queue.enqueue("w"));
        assertEquals("later event kept its cue", "w", queue.dequeue());
        assertTrue(queue.isEmpty());
    }

    private static void testSizeAndEmptyStateAccuracy() {
        GameEventQueue queue = new GameEventQueue();

        assertTrue("starts empty", queue.isEmpty());
        queue.enqueue("p");
        queue.enqueue("q");
        assertEquals("two enqueued", 2, queue.size());
        assertTrue("not empty while entries wait", !queue.isEmpty());

        queue.dequeue();
        assertEquals("one left after one dequeue", 1, queue.size());
        assertTrue("still not empty", !queue.isEmpty());

        queue.dequeue();
        assertEquals("back to zero", 0, queue.size());
        assertTrue("empty again", queue.isEmpty());
        assertTrue("empty dequeue still safe after reuse",
                queue.dequeue() == null);
    }

    private static void testRejectsNullAndBlankCues() {
        GameEventQueue queue = new GameEventQueue();

        assertTrue("null cue rejected", !queue.enqueue(null));
        assertTrue("blank cue rejected", !queue.enqueue("   "));
        assertEquals(0, queue.size());

        assertTrue("valid cue still accepted", queue.enqueue("key"));
        assertEquals("valid cue stored whole", "key", queue.dequeue());
    }

    private static void testRepeatedOperationsAvoidCorruption() {
        GameEventQueue queue = new GameEventQueue(4);

        // Every cycle wraps rear and front around the four slots, so a
        // thousand cycles stress the circular indexing thoroughly.
        int mismatches = 0;

        for (int i = 0; i < 1000; i++) {
            String label = "e" + i;
            boolean enqueued = queue.enqueue(label);
            String picked = queue.dequeue();

            if (!enqueued || !label.equals(picked)) {
                mismatches++;
            }
        }

        assertEquals("no corruption in 1000 enqueue-dequeue cycles", 0,
                mismatches);
        assertTrue("queue empty after the cycles", queue.isEmpty());
    }

    /* --- GameSession integration -------------------------------------- */

    private static void testGameSessionDeliversCuesInFIFOOrder() {
        // Walking onto a start tile holding chest, key, and door produces
        // three cues in one tick; the queue must deliver them in the same
        // order they were recorded.
        List<String> sounds = new ArrayList<>();
        Level level = new Level("Level 1", new Dungeon(), List.of(
                new GameObject(GameObject.Kind.CHEST, 5, 5),
                new GameObject(GameObject.Kind.KEY, 5, 5),
                new GameObject(GameObject.Kind.DOOR, 5, 5)));

        GameSession session = new GameSession(List.of(level), sounds::add);
        session.update(DT, new InputState());

        assertEquals("chest cue delivered first", "chest",
                sounds.get(0));
        assertEquals("key cue delivered second", "key", sounds.get(1));
        assertEquals("door cue delivered third", "door", sounds.get(2));
        assertEquals("no extra cues recorded", 3, sounds.size());
    }

    private static void testGameOverAndVictoryEventsAreDelivered() {
        // Non-fatal monster hit: the monster cue is delivered in the same
        // tick that recorded it.
        List<String> sounds = new ArrayList<>();
        Level lethal = new Level("Test", new Dungeon(), List.of(
                new GameObject(GameObject.Kind.MONSTER, 5, 5)));
        GameSession session = new GameSession(
                List.of(lethal), sounds::add);

        session.update(DT, new InputState());

        assertTrue("monster cue delivered on the hit tick",
                sounds.contains("monster"));
        assertTrue("non-fatal hit did not end the run",
                !session.isGameOver());

        // Two more hits (invulnerability must lapse between them) end the
        // run; the game-over cue is delivered on the fatal tick.
        session.update(GameSession.INVULN_SECONDS + 0.1, new InputState());
        session.update(GameSession.INVULN_SECONDS + 0.1, new InputState());

        assertTrue("run ended", session.isGameOver());
        assertTrue("gameover cue delivered", sounds.contains("gameover"));
        assertTrue("monster cue precedes gameover cue",
                sounds.indexOf("monster") < sounds.indexOf("gameover"));

        // Victory: a single level whose key and door share the start tile.
        List<String> victorySounds = new ArrayList<>();
        Level finish = new Level("Final", new Dungeon(), List.of(
                new GameObject(GameObject.Kind.KEY, 5, 5),
                new GameObject(GameObject.Kind.DOOR, 5, 5)));
        GameSession winner = new GameSession(
                List.of(finish), victorySounds::add);

        winner.update(DT, new InputState());
        winner.update(GameSession.DOOR_OPEN_SECONDS + DT, new InputState());

        assertTrue("game completed", winner.isVictory());
        assertEquals("key delivered before door", "key",
                victorySounds.get(0));
        assertEquals("door delivered before victory", "door",
                victorySounds.get(1));
        assertEquals("victory delivered last", "victory",
                victorySounds.get(2));
    }

    /* --- helpers ------------------------------------------------------ */

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

    private static void assertEquals(int expected, int actual) {
        assertTrue("expected " + expected + ", got " + actual,
                expected == actual);
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
