
package com.dungeonescape.game;

/**
 * Player position, movement, and animation state.
 *
 * Movement is resolved one axis at a time against the dungeon collision map.
 * Binary-search collision resolution prevents the player from snapping to
 * incorrect positions when approaching walls or wall corners.
 */
public class Player {

    /** Movement speed in pixels per second. */
    public static final double SPEED = 150.0;

    /** Half the player's collision box size (pixels). */
    public static final double HALF_SIZE = 14.0;

    /** Seconds per walking animation frame. */
    public static final double FRAME_SECONDS = 0.09;

    /** Number of walking frames in the sprite sheet. */
    public static final int RUN_FRAMES = 6;

    private static final int COLLISION_SEARCH_STEPS = 14;

    private double x;
    private double y;
    private boolean facingLeft;
    private boolean moving;
    private int runFrame;
    private double animAccum;

    /**
     * Creates a player centered at the supplied pixel position.
     */
    public Player(double startX, double startY) {
        this.x = startX;
        this.y = startY;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public boolean isMoving() {
        return moving;
    }

    public boolean facingLeft() {
        return facingLeft;
    }

    /** Current walking frame index (zero when idle). */
    public int runFrame() {
        return runFrame;
    }

    /**
     * Advances the player using the current input.
     */
    public void update(double dt, InputState input, Dungeon dungeon) {
        double dx = input.dx();
        double dy = input.dy();

        // Keep diagonal movement at the same speed as straight movement.
        if (dx != 0 && dy != 0) {
            double normalization = Math.sqrt(0.5);
            dx *= normalization;
            dy *= normalization;
        }

        moving = dx != 0 || dy != 0;

        if (dx < 0) {
            facingLeft = true;
        } else if (dx > 0) {
            facingLeft = false;
        }

        double step = SPEED * dt;

        // Resolve each axis independently to allow sliding along walls.
        if (dx != 0) {
            moveHorizontally(dx * step, dungeon);
        }

        if (dy != 0) {
            moveVertically(dy * step, dungeon);
        }

        updateAnimation(dt);
    }

    /**
     * Moves horizontally as far as possible without entering a solid tile.
     */
    private void moveHorizontally(double amount, Dungeon dungeon) {
        double targetX = x + amount;

        if (!collides(targetX, y, dungeon)) {
            x = targetX;
            return;
        }

        // Find the furthest safe position between the current and target X.
        double low = 0.0;
        double high = 1.0;

        for (int i = 0; i < COLLISION_SEARCH_STEPS; i++) {
            double middle = (low + high) / 2.0;
            double candidateX = x + amount * middle;

            if (collides(candidateX, y, dungeon)) {
                high = middle;
            } else {
                low = middle;
            }
        }

        x += amount * low;
    }

    /**
     * Moves vertically as far as possible without entering a solid tile.
     */
    private void moveVertically(double amount, Dungeon dungeon) {
        double targetY = y + amount;

        if (!collides(x, targetY, dungeon)) {
            y = targetY;
            return;
        }

        // Find the furthest safe position between the current and target Y.
        double low = 0.0;
        double high = 1.0;

        for (int i = 0; i < COLLISION_SEARCH_STEPS; i++) {
            double middle = (low + high) / 2.0;
            double candidateY = y + amount * middle;

            if (collides(x, candidateY, dungeon)) {
                high = middle;
            } else {
                low = middle;
            }
        }

        y += amount * low;
    }

    private void updateAnimation(double dt) {
        if (moving) {
            animAccum += dt;

            while (animAccum >= FRAME_SECONDS) {
                runFrame = (runFrame + 1) % RUN_FRAMES;
                animAccum -= FRAME_SECONDS;
            }
        } else {
            runFrame = 0;
            animAccum = 0;
        }
    }

    /** True if the player's collision box overlaps a solid tile. */
    public boolean isColliding(Dungeon dungeon) {
        return collides(x, y, dungeon);
    }

    private boolean collides(double cx, double cy, Dungeon dungeon) {
        int c0 = tileIndex(cx - HALF_SIZE);
        int c1 = tileIndex(cx + HALF_SIZE);
        int r0 = tileIndex(cy - HALF_SIZE);
        int r1 = tileIndex(cy + HALF_SIZE);

        for (int row = r0; row <= r1; row++) {
            for (int col = c0; col <= c1; col++) {
                if (dungeon.isSolid(col, row)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static int tileIndex(double pixel) {
        return (int) Math.floor(pixel / Dungeon.TILE);
    }
}
