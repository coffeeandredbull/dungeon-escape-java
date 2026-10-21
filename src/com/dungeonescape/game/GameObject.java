
package com.dungeonescape.game;

/**
 * Represents an object placed on a dungeon tile.
 */
public class GameObject {

    public enum Kind {
        MONSTER, CHEST, DOOR, KEY, POTION
    }

    public enum PatrolAxis {
        HORIZONTAL,
        VERTICAL
    }

    private final Kind kind;
    private final PatrolAxis patrolAxis;

    private int col;
    private int row;
    private int patrolDirection = 1;
    private boolean removed;

    /**
     * Creates an object. Monsters use horizontal patrols by default
     * to preserve compatibility with existing code and tests.
     */
    public GameObject(Kind kind, int col, int row) {
        this(kind, col, row, PatrolAxis.HORIZONTAL);
    }

    /**
     * Creates an object with an explicitly selected monster patrol axis.
     */
    public GameObject(
            Kind kind,
            int col,
            int row,
            PatrolAxis patrolAxis) {

        if (kind == null) {
            throw new IllegalArgumentException("Object kind cannot be null");
        }

        if (patrolAxis == null) {
            throw new IllegalArgumentException("Patrol axis cannot be null");
        }

        this.kind = kind;
        this.col = col;
        this.row = row;
        this.patrolAxis = patrolAxis;
    }

    public Kind kind() {
        return kind;
    }

    public int col() {
        return col;
    }

    public int row() {
        return row;
    }

    public boolean removed() {
        return removed;
    }

    public void remove() {
        removed = true;
    }

    public PatrolAxis patrolAxis() {
        return patrolAxis;
    }

    public int patrolDirection() {
        return patrolDirection;
    }

    public void reversePatrolDirection() {
        patrolDirection = -patrolDirection;
    }

    /**
     * Moves a monster to a new tile.
     */
    public void moveTo(int newCol, int newRow) {
        if (kind != Kind.MONSTER) {
            throw new IllegalStateException(
                    "Only monsters can move");
        }

        col = newCol;
        row = newRow;
    }
}
