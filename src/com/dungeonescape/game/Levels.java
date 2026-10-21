
package com.dungeonescape.game;

import java.util.ArrayList;
import java.util.List;

import static com.dungeonescape.game.GameObject.Kind;
import static com.dungeonescape.game.GameObject.PatrolAxis;

/**
 * Builds the ordered playable levels.
 */
public final class Levels {

    private Levels() {
    }

    /**
     * Creates all playable levels in progression order.
     */
    public static List<Level> create() {
        List<Level> levels = new ArrayList<>();

        levels.add(level1());
        levels.add(level2());
        levels.add(level3());

        return levels;
    }

    /**
     * Level 1: warm stone, orange flames, and three monsters.
     */
    private static Level level1() {
        Dungeon dungeon = new Dungeon();
        List<GameObject> objects = new ArrayList<>();

        // Guard the key approach.
        objects.add(new GameObject(
                Kind.MONSTER, 2, 5, PatrolAxis.VERTICAL));

        // Patrol near the door approach.
        objects.add(new GameObject(
                Kind.MONSTER, 8, 3, PatrolAxis.VERTICAL));

        // Patrol the upper corridor near the chest.
        objects.add(new GameObject(
                Kind.MONSTER, 11, 1, PatrolAxis.HORIZONTAL));

        objects.add(new GameObject(Kind.CHEST, 13, 1));
        objects.add(new GameObject(Kind.KEY, 2, 2));
        objects.add(new GameObject(Kind.DOOR, 8, 1));

        return new Level("Level 1", dungeon, objects);
    }

    /**
     * Level 2: cool stone, green flames, and five monsters.
     */
    private static Level level2() {
        String[] layout = {
                "################",
                "#..............#",
                "#.##....##.....#",
                "#..#....#......#",
                "#..T....#..T...#",
                "#......##......#",
                "#.##...#...##..#",
                "#......#.......#",
                "#..T...#...T...#",
                "#..#....#..#...#",
                "#..............#",
                "################"
        };

        Dungeon dungeon = new Dungeon(layout, 1, 1);
        List<GameObject> objects = new ArrayList<>();

        // Guard the upper route near the key.
        objects.add(new GameObject(
                Kind.MONSTER, 5, 1, PatrolAxis.HORIZONTAL));

        // Patrol near the chest.
        objects.add(new GameObject(
                Kind.MONSTER, 11, 1, PatrolAxis.HORIZONTAL));

        // Patrol vertically through the central area.
        objects.add(new GameObject(
                Kind.MONSTER, 5, 6, PatrolAxis.VERTICAL));

        // Guard the potion approach.
        objects.add(new GameObject(
                Kind.MONSTER, 1, 8, PatrolAxis.VERTICAL));

        // Patrol the exit approach.
        objects.add(new GameObject(
                Kind.MONSTER, 11, 10, PatrolAxis.HORIZONTAL));

        objects.add(new GameObject(Kind.KEY, 2, 8));
        objects.add(new GameObject(Kind.CHEST, 13, 1));
        objects.add(new GameObject(Kind.POTION, 1, 10));
        objects.add(new GameObject(Kind.DOOR, 14, 10));

        return new Level("Level 2", dungeon, objects);
    }

    /**
     * Level 3: the hardest dungeon, with dangerous corridors
     * and six monsters patrolling horizontally and vertically.
     */
    private static Level level3() {
        String[] layout = {
                "################",
                "#..............#",
                "#.##...##...#..#",
                "#......#.......#",
                "#.T....#....T..#",
                "#......#.......#",
                "#..##.....##...#",
                "#..............#",
                "#.T...#....T...#",
                "#......#.......#",
                "#..............#",
                "################"
        };

        Dungeon dungeon = new Dungeon(layout, 1, 1);
        List<GameObject> objects = new ArrayList<>();

        // Patrol across the upper route.
        objects.add(new GameObject(
                Kind.MONSTER, 5, 1, PatrolAxis.HORIZONTAL));

        // Vertical patrol through the upper-right area.
        objects.add(new GameObject(
                Kind.MONSTER, 10, 3, PatrolAxis.VERTICAL));

        // Patrol the central corridor.
        objects.add(new GameObject(
                Kind.MONSTER, 4, 4, PatrolAxis.VERTICAL));

        // Patrol the lower-left route.
        objects.add(new GameObject(
                Kind.MONSTER, 3, 7, PatrolAxis.HORIZONTAL));

        // Patrol the lower-right route.
        objects.add(new GameObject(
                Kind.MONSTER, 13, 8, PatrolAxis.VERTICAL));

        // Guard the approach to the exit.
        objects.add(new GameObject(
                Kind.MONSTER, 7, 10, PatrolAxis.HORIZONTAL));

        // Level 3 objectives.
        objects.add(new GameObject(Kind.KEY, 10, 1));
        objects.add(new GameObject(Kind.CHEST, 13, 7));
        objects.add(new GameObject(Kind.POTION, 1, 10));
        objects.add(new GameObject(Kind.DOOR, 14, 10));

        return new Level("Level 3", dungeon, objects);
    }
}
