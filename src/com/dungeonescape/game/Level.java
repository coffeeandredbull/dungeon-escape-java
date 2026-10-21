package com.dungeonescape.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One playable level: a name, its {@link Dungeon}, and the objects on it.
 *
 * <p>Pure game data with no Swing dependency.</p>
 */
public class Level {

    private final String name;
    private final Dungeon dungeon;
    private final List<GameObject> objects;

    /**
     * Creates a level.
     *
     * @param name    display name, e.g. {@code "Level 1"}
     * @param dungeon tile map
     * @param objects interactive objects (the list is copied)
     */
    public Level(String name, Dungeon dungeon, List<GameObject> objects) {
        this.name = name;
        this.dungeon = dungeon;
        this.objects = new ArrayList<>(objects);
    }

    public String name() {
        return name;
    }

    public Dungeon dungeon() {
        return dungeon;
    }

    /** The level's interactive objects. */
    public List<GameObject> objects() {
        return Collections.unmodifiableList(objects);
    }
}
