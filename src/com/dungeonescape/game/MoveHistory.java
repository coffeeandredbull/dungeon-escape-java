package com.dungeonescape.game;

/**
 * Hand-written LIFO stack of the player's previous tile-crossing positions.
 *
 * <p>Backed by one plain fixed-capacity array with an explicit top index;
 * deliberately not {@code java.util.Stack} and not a list wrapper, so the
 * stack operations are visible in project source. {@code push} and
 * {@code pop} run in O(1). When the array is full, the oldest entry falls
 * out of the bottom of the stack: entries are shifted one slot toward
 * index 0 by an explicit loop, so the most recent positions are always the
 * ones kept.</p>
 *
 * <p>Entries hold pixel coordinates the player actually occupied during
 * normal play, which are by construction collision-free and inside the
 * dungeon, so restoring one can never place the player inside a wall.
 * Pure game logic with no Swing dependency.</p>
 */
public class MoveHistory {

    /** Default number of positions the history remembers. */
    public static final int CAPACITY = 64;

    /** One recorded player position. Immutable once created. */
    public static final class Position {

        private final double x;
        private final double y;

        private Position(double x, double y) {
            this.x = x;
            this.y = y;
        }

        /** Player center X in pixels when the position was recorded. */
        public double x() {
            return x;
        }

        /** Player center Y in pixels when the position was recorded. */
        public double y() {
            return y;
        }
    }

    private final Position[] entries;
    private int top;

    /** Creates a history with the default capacity. */
    public MoveHistory() {
        this(CAPACITY);
    }

    /**
     * Creates a history with an explicit capacity (used by tests).
     *
     * @throws IllegalArgumentException when capacity is not positive
     */
    public MoveHistory(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException(
                    "Move history capacity must be positive");
        }

        this.entries = new Position[capacity];
        this.top = 0;
    }

    /**
     * Pushes one position as the newest stack entry.
     */
    public void push(double x, double y) {
        if (top == entries.length) {
            shiftOutOldest();
        }

        entries[top] = new Position(x, y);
        top++;
    }

    /**
     * Pops the most recent position, or {@code null} when the stack is
     * empty. The null answer keeps an undo on empty history safe: no
     * crash and no unintended movement.
     */
    public Position pop() {
        if (top == 0) {
            return null;
        }

        top--;
        Position newest = entries[top];
        entries[top] = null;
        return newest;
    }

    /** Number of positions currently on the stack. */
    public int size() {
        return top;
    }

    /** True when no position can be undone. */
    public boolean isEmpty() {
        return top == 0;
    }

    /** Removes every entry; used when a level transition invalidates them. */
    public void clear() {
        for (int i = 0; i < top; i++) {
            entries[i] = null;
        }

        top = 0;
    }

    /**
     * Makes room at the newest end when the array is full: the oldest
     * entry (slot 0) is dropped and every remaining entry is shifted one
     * slot toward index 0 by an explicit loop, opening the last slot for
     * the entry push is about to store.
     */
    private void shiftOutOldest() {
        entries[0] = null;

        for (int i = 1; i < entries.length; i++) {
            entries[i - 1] = entries[i];
        }

        top = entries.length - 1;
    }
}
