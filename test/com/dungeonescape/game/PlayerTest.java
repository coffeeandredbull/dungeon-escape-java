package com.dungeonescape.game;

/**
 * Dependency-free tests for player movement, collision, map boundaries, and
 * simultaneous key handling.
 *
 * <p>Run with: {@code java -cp out;test_out com.dungeonescape.game.PlayerTest}
 * Exit code 0 means every assertion passed.</p>
 */
public class PlayerTest {

    private static final double DT = 1.0 / 60.0;
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        testMoveRight();
        testMoveLeftFlipsFacing();
        testDiagonalIsNormalized();
        testSimultaneousEquivalentKeys();
        testOpposingKeysCancel();
        testWallCollisionStops();
        testSlidingAlongWall();
        testMapBoundary();
        testAnimationAdvancesWhileWalking();
        testAnimationResetsWhenIdle();

        System.out.println();
        System.out.println("Passed: " + passed + ", Failed: " + failed);
        if (failed > 0) {
            System.out.println("TESTS FAILED");
            System.exit(1);
        }
        System.out.println("All tests passed.");
    }

    private static void testMoveRight() {
        Dungeon d = new Dungeon();
        Player p = new Player(d.startX(), d.startY());
        InputState in = new InputState();
        in.set("d", true);
        p.update(DT, in, d);
        assertEquals("move right changes x", Player.SPEED * DT, p.x() - d.startX(), 1e-9);
        assertEquals("move right keeps y", 0.0, p.y() - d.startY(), 1e-9);
        assertTrue("moving flag set", p.isMoving());
        assertTrue("facing right", !p.facingLeft());
    }

    private static void testMoveLeftFlipsFacing() {
        Dungeon d = new Dungeon();
        Player p = new Player(d.startX(), d.startY());
        InputState in = new InputState();
        in.set("a", true);
        p.update(DT, in, d);
        assertEquals("move left changes x", -Player.SPEED * DT, p.x() - d.startX(), 1e-9);
        assertTrue("facing left", p.facingLeft());
    }

    private static void testDiagonalIsNormalized() {
        Dungeon d = new Dungeon();
        Player p = new Player(d.startX(), d.startY());
        InputState in = new InputState();
        in.set("d", true);
        in.set("s", true);
        p.update(DT, in, d);
        double dx = p.x() - d.startX();
        double dy = p.y() - d.startY();
        assertEquals("diagonal speed equals straight speed",
                Player.SPEED * DT, Math.hypot(dx, dy), 1e-6);
    }

    private static void testSimultaneousEquivalentKeys() {
        Dungeon d = new Dungeon();
        Player upAlone = new Player(d.startX(), d.startY());
        InputState upOnly = new InputState();
        upOnly.set("w", true);
        upAlone.update(DT, upOnly, d);

        Player both = new Player(d.startX(), d.startY());
        InputState wAndUp = new InputState();
        wAndUp.set("w", true);
        wAndUp.set("up", true);
        both.update(DT, wAndUp, d);

        assertEquals("W+Up matches W alone (y)", upAlone.y(), both.y(), 1e-9);
        assertEquals("W+Up matches W alone (x)", upAlone.x(), both.x(), 1e-9);
        assertTrue("equivalent keys do not double speed",
                Math.abs(both.y() - d.startY()) <= Player.SPEED * DT + 1e-9);
    }

    private static void testOpposingKeysCancel() {
        Dungeon d = new Dungeon();
        Player p = new Player(d.startX(), d.startY());
        InputState in = new InputState();
        in.set("a", true);
        in.set("d", true);
        p.update(DT, in, d);
        assertEquals("left+right cancels x", 0.0, p.x() - d.startX(), 1e-9);
        assertTrue("opposing keys mean not moving", !p.isMoving());
    }

    private static void testWallCollisionStops() {
        Dungeon d = new Dungeon();
        double x = 8 * Dungeon.TILE + Dungeon.TILE / 2.0;
        double y = 1 * Dungeon.TILE + Dungeon.TILE / 2.0;
        Player p = new Player(x, y);
        InputState in = new InputState();
        in.set("w", true);
        for (int i = 0; i < 120; i++) {
            p.update(DT, in, d);
        }
        assertTrue("player not inside wall", !p.isColliding(d));
        double expectedY = Dungeon.TILE + Player.HALF_SIZE + 0.001;
        assertEquals("clamped against top wall", expectedY, p.y(), 0.02);
    }

    private static void testSlidingAlongWall() {
        Dungeon d = new Dungeon();
        // Row 1 is floor; row 0 is the top wall. Moving up is blocked while
        // moving right is free, so the player must slide along the wall.
        double x = 8 * Dungeon.TILE + Dungeon.TILE / 2.0;
        double y = 1 * Dungeon.TILE + Dungeon.TILE / 2.0;
        Player p = new Player(x, y);
        InputState in = new InputState();
        in.set("d", true);
        in.set("w", true);
        for (int i = 0; i < 30; i++) {
            p.update(DT, in, d);
        }
        double wallLimit = Dungeon.TILE + Player.HALF_SIZE + 0.001;
        assertEquals("vertical blocked at top wall", wallLimit, p.y(), 0.02);
        assertTrue("slid horizontally along wall", p.x() > x + 1.0);
        assertTrue("not colliding while sliding", !p.isColliding(d));
    }

    private static void testMapBoundary() {
        Dungeon d = new Dungeon();
        Player p = new Player(d.startX(), d.startY());
        InputState in = new InputState();

        in.set("w", true);
        run(p, in, d, 200);
        assertTrue("up: inside map", !p.isColliding(d));
        in.set("w", false);

        in.set("a", true);
        run(p, in, d, 200);
        assertTrue("left: inside map", !p.isColliding(d));
        in.set("a", false);

        in.set("s", true);
        run(p, in, d, 200);
        assertTrue("down: inside map", !p.isColliding(d));
        in.set("s", false);

        in.set("d", true);
        run(p, in, d, 200);
        assertTrue("right: inside map", !p.isColliding(d));

        double maxX = d.cols() * Dungeon.TILE - Player.HALF_SIZE;
        double maxY = d.rows() * Dungeon.TILE - Player.HALF_SIZE;
        assertTrue("within horizontal bounds", p.x() >= Player.HALF_SIZE && p.x() <= maxX + 0.01);
        assertTrue("within vertical bounds", p.y() >= Player.HALF_SIZE && p.y() <= maxY + 0.01);
    }

    private static void testAnimationAdvancesWhileWalking() {
        Dungeon d = new Dungeon();
        Player p = new Player(d.startX(), d.startY());
        InputState in = new InputState();
        in.set("d", true);
        for (int i = 0; i < 30; i++) {
            p.update(DT, in, d);
        }
        assertTrue("walking animation advances", p.runFrame() > 0);
    }

    private static void testAnimationResetsWhenIdle() {
        Dungeon d = new Dungeon();
        Player p = new Player(d.startX(), d.startY());
        InputState in = new InputState();
        in.set("d", true);
        for (int i = 0; i < 20; i++) {
            p.update(DT, in, d);
        }
        in.clear();
        p.update(DT, in, d);
        assertEquals("idle resets frame", 0.0, (double) p.runFrame(), 1e-9);
        assertTrue("not moving when idle", !p.isMoving());
    }

    private static void run(Player p, InputState in, Dungeon d, int frames) {
        for (int i = 0; i < frames; i++) {
            p.update(DT, in, d);
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

    private static void assertEquals(String message, double expected, double actual, double epsilon) {
        if (Math.abs(expected - actual) <= epsilon) {
            passed++;
            System.out.println("PASS: " + message);
        } else {
            failed++;
            System.out.println("FAIL: " + message
                    + " (expected " + expected + ", got " + actual + ")");
        }
    }
}
