package com.dungeonescape.game;

/**
 * Held-key state for movement, combining WASD and arrow keys.
 *
 * <p>Equivalent keys are merged: holding W and Up both set the "up" flag, so
 * pressing both never makes the player move faster. Opposing keys cancel
 * because each axis collapses to a single -1/0/+1 value.</p>
 */
public class InputState {

    private boolean w;
    private boolean a;
    private boolean s;
    private boolean d;
    private boolean up;
    private boolean down;
    private boolean left;
    private boolean right;

    /** Sets the held state of one named key: w, a, s, d, up, down, left, right. */
    public void set(String name, boolean held) {
        switch (name) {
            case "w" -> w = held;
            case "a" -> a = held;
            case "s" -> s = held;
            case "d" -> d = held;
            case "up" -> up = held;
            case "down" -> down = held;
            case "left" -> left = held;
            case "right" -> right = held;
            default -> throw new IllegalArgumentException("Unknown key: " + name);
        }
    }

    /** True if up is requested by W or the up arrow. */
    public boolean up() {
        return w || up;
    }

    /** True if down is requested by S or the down arrow. */
    public boolean down() {
        return s || down;
    }

    /** True if left is requested by A or the left arrow. */
    public boolean left() {
        return a || left;
    }

    /** True if right is requested by D or the right arrow. */
    public boolean right() {
        return d || right;
    }

    /** Horizontal axis: -1 (left), 0, or +1 (right). */
    public double dx() {
        return (right() ? 1 : 0) - (left() ? 1 : 0);
    }

    /** Vertical axis: -1 (up), 0, or +1 (down). */
    public double dy() {
        return (down() ? 1 : 0) - (up() ? 1 : 0);
    }

    /** Clears every held key (used when the window loses focus). */
    public void clear() {
        w = a = s = d = up = down = left = right = false;
    }
}
