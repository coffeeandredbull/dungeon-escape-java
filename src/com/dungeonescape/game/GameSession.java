
package com.dungeonescape.game;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Runs a play-through and resolves player and monster interactions.
 *
 * Pure game logic with no Swing dependency. Sound is triggered through a
 * Consumer so tests can record cues without an audio device.
 */
public class GameSession {

    public static final int START_LIVES = 3;
    public static final double INVULN_SECONDS = 1.0;
    public static final int CHEST_SCORE = 100;
    public static final int POTION_HEAL = 1;
    public static final double STEP_SECONDS = 0.3;
    public static final double DOOR_OPEN_SECONDS = 0.65;

    private static final double MONSTER_MOVE_SECONDS = 0.55;

    private final List<Level> levels;
    private final Consumer<String> sound;

    private int levelIndex;
    private int score;
    private int lives;

    private boolean hasKey;
    private boolean gameOver;
    private boolean victory;
    private boolean doorOpening;

    private double invuln;
    private double stepAccum;
    private double doorOpeningTime;
    private double monsterMoveAccum;

    private Player player;

    public GameSession(List<Level> levels, Consumer<String> sound) {
        this.levels = new ArrayList<>(levels);

        if (this.levels.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one level is required");
        }

        if (sound == null) {
            throw new IllegalArgumentException(
                    "Sound callback cannot be null");
        }

        this.sound = sound;
        startNewGame();
    }

    private void startNewGame() {
        levelIndex = 0;
        score = 0;
        lives = START_LIVES;

        hasKey = false;
        gameOver = false;
        victory = false;
        doorOpening = false;

        invuln = 0;
        stepAccum = 0;
        doorOpeningTime = 0;
        monsterMoveAccum = 0;

        Dungeon dungeon = levels.get(0).dungeon();
        player = new Player(dungeon.startX(), dungeon.startY());
    }

    public Level level() {
        return levels.get(levelIndex);
    }

    public int levelIndex() {
        return levelIndex;
    }

    public int levelCount() {
        return levels.size();
    }

    public int score() {
        return score;
    }

    public int lives() {
        return lives;
    }

    public boolean hasKey() {
        return hasKey;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public boolean isVictory() {
        return victory;
    }

    public Player player() {
        return player;
    }

    /**
     * Advances one game tick.
     */
    public void update(double dt, InputState input) {
        if (gameOver || victory) {
            return;
        }

        if (dt < 0 || !Double.isFinite(dt)) {
            throw new IllegalArgumentException(
                    "Delta time must be finite and non-negative");
        }

        if (input == null) {
            throw new IllegalArgumentException(
                    "Input state cannot be null");
        }

        // Freeze gameplay while the open-door image is displayed.
        if (doorOpening) {
            doorOpeningTime -= dt;

            if (doorOpeningTime <= 0) {
                doorOpening = false;
                advanceLevel();
            }

            return;
        }

        if (invuln > 0) {
            invuln = Math.max(0, invuln - dt);
        }

        handleMonsterContact();

        if (gameOver) {
            return;
        }

        moveMonsters(dt);

        player.update(dt, input, level().dungeon());

        if (player.isMoving()) {
            stepAccum += dt;

            while (stepAccum >= STEP_SECONDS) {
                stepAccum -= STEP_SECONDS;
                sound.accept("footstep");
            }
        } else {
            stepAccum = 0;
        }

        handleObjects();
    }

    /**
     * Checks contact before monsters move.
     */
    private void handleMonsterContact() {
        int playerCol = tileOf(player.x());
        int playerRow = tileOf(player.y());

        for (GameObject object : level().objects()) {
            if (object.removed()
                    || object.kind() != GameObject.Kind.MONSTER) {
                continue;
            }

            if (object.col() == playerCol
                    && object.row() == playerRow) {
                hitMonster();

                if (gameOver) {
                    return;
                }

                // The player has respawned. Stop checking the old position.
                return;
            }
        }
    }

    /**
     * Moves each monster along its configured patrol axis.
     * A monster reverses when its next tile is blocked.
     */
    private void moveMonsters(double dt) {
        monsterMoveAccum += dt;

        if (monsterMoveAccum < MONSTER_MOVE_SECONDS) {
            return;
        }

        monsterMoveAccum %= MONSTER_MOVE_SECONDS;

        Dungeon dungeon = level().dungeon();

        int playerCol = tileOf(player.x());
        int playerRow = tileOf(player.y());

        for (GameObject monster : level().objects()) {
            if (monster.removed()
                    || monster.kind() != GameObject.Kind.MONSTER) {
                continue;
            }

            // Do not move a monster away from an occupied player tile.
            if (monster.col() == playerCol
                    && monster.row() == playerRow) {
                continue;
            }

            int nextCol = monster.col();
            int nextRow = monster.row();

            if (monster.patrolAxis()
                    == GameObject.PatrolAxis.HORIZONTAL) {
                nextCol += monster.patrolDirection();
            } else {
                nextRow += monster.patrolDirection();
            }

            if (!canMonsterOccupy(
                    nextCol, nextRow, monster, dungeon)) {

                monster.reversePatrolDirection();

                nextCol = monster.col();
                nextRow = monster.row();

                if (monster.patrolAxis()
                        == GameObject.PatrolAxis.HORIZONTAL) {
                    nextCol += monster.patrolDirection();
                } else {
                    nextRow += monster.patrolDirection();
                }
            }

            if (canMonsterOccupy(
                    nextCol, nextRow, monster, dungeon)) {
                monster.moveTo(nextCol, nextRow);
            }
        }
    }

    /**
     * Monsters cannot cross solid tiles, other monsters,
     * or interactive objects.
     */
    private boolean canMonsterOccupy(
            int col,
            int row,
            GameObject movingMonster,
            Dungeon dungeon) {

        if (!dungeon.isWalkable(col, row)) {
            return false;
        }

        for (GameObject other : level().objects()) {
            if (other == movingMonster || other.removed()) {
                continue;
            }

            if (other.col() == col && other.row() == row) {
                return false;
            }
        }

        return true;
    }

    /**
     * Resolves objects on the player's current tile.
     */
    private void handleObjects() {
        int playerCol = tileOf(player.x());
        int playerRow = tileOf(player.y());

        for (GameObject object : level().objects()) {
            if (object.removed()
                    || object.col() != playerCol
                    || object.row() != playerRow) {
                continue;
            }

            switch (object.kind()) {
                case MONSTER -> hitMonster();

                case CHEST -> {
                    object.remove();
                    score += CHEST_SCORE;
                    sound.accept("chest");
                }

                case KEY -> {
                    object.remove();
                    hasKey = true;
                    sound.accept("key");
                }

                case POTION -> {
                    object.remove();
                    lives = Math.min(
                            START_LIVES,
                            lives + POTION_HEAL);
                    sound.accept("potion");
                }

                case DOOR -> {
                    if (hasKey) {
                        object.remove();
                        doorOpening = true;
                        doorOpeningTime = DOOR_OPEN_SECONDS;
                        sound.accept("door");
                        return;
                    }
                }

                default -> {
                    // No interaction.
                }
            }
        }
    }

    /**
     * Applies damage and respawns the player when a life is lost.
     */
    private void hitMonster() {
        if (invuln > 0 || gameOver) {
            return;
        }

        lives--;
        invuln = INVULN_SECONDS;

        if (lives <= 0) {
            lives = 0;
            gameOver = true;
            sound.accept("gameover");
        } else {
            Dungeon dungeon = level().dungeon();

            // Return the adventurer to this level's starting position.
            player = new Player(dungeon.startX(), dungeon.startY());

            // Prevent leftover movement time from triggering footstep sounds.
            stepAccum = 0;

            sound.accept("monster");
        }
    }

    /**
     * Advances to the next level or completes the game.
     */
    private void advanceLevel() {
        if (levelIndex + 1 < levels.size()) {
            levelIndex++;
            hasKey = false;

            Dungeon dungeon = level().dungeon();
            player = new Player(dungeon.startX(), dungeon.startY());

            invuln = 0;
            stepAccum = 0;
            monsterMoveAccum = 0;
            doorOpeningTime = 0;
        } else {
            victory = true;
            sound.accept("victory");
        }
    }

    private static int tileOf(double pixel) {
        return (int) Math.floor(pixel / Dungeon.TILE);
    }
}
