
package com.dungeonescape.ui;

import com.dungeonescape.asset.AssetLoader;
import com.dungeonescape.game.Dungeon;
import com.dungeonescape.game.GameObject;
import com.dungeonescape.game.GameSession;
import com.dungeonescape.game.InputState;
import com.dungeonescape.game.Player;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

public class DungeonPanel extends JPanel {

    private static final int TICK_MS = 16;
    private static final int TORCH_FRAMES = 7;
    private static final int MONSTER_FRAMES = 6;
    private static final double TORCH_FRAME_SECONDS = 0.09;
    private static final double MONSTER_FRAME_SECONDS = 0.12;
    private static final double MAX_DT_SECONDS = 0.1;

    private static final Color BACKGROUND = new Color(8, 8, 12);

    private static final Color FLOOR_BASE = new Color(26, 28, 36);
    private static final Color FLOOR_ALT = new Color(31, 34, 44);
    private static final Color FLOOR_GROUT = new Color(14, 15, 20);
    private static final Color WALL_DARK = new Color(18, 17, 20);
    private static final Color WALL_MID = new Color(70, 64, 58);
    private static final Color WALL_LIGHT = new Color(108, 100, 88);

    private static final Color LEVEL2_FLOOR_BASE = new Color(17, 31, 37);
    private static final Color LEVEL2_FLOOR_ALT = new Color(22, 39, 46);
    private static final Color LEVEL2_FLOOR_GROUT = new Color(8, 19, 25);
    private static final Color LEVEL2_WALL_DARK = new Color(9, 21, 29);
    private static final Color LEVEL2_WALL_MID = new Color(39, 72, 80);
    private static final Color LEVEL2_WALL_LIGHT = new Color(81, 130, 132);

    private static final Color LEVEL3_FLOOR_BASE = new Color(48, 20, 27);
    private static final Color LEVEL3_FLOOR_ALT = new Color(62, 24, 32);
    private static final Color LEVEL3_FLOOR_GROUT = new Color(25, 10, 16);
    private static final Color LEVEL3_WALL_DARK = new Color(25, 9, 14);
    private static final Color LEVEL3_WALL_MID = new Color(105, 35, 43);
    private static final Color LEVEL3_WALL_LIGHT = new Color(166, 65, 67);

    private final GameSession session;
    private final HudPanel hud;
    private final InputState input = new InputState();

    private final BufferedImage idleFrame;
    private final BufferedImage[] runFrames;
    private final BufferedImage[] torchFrames;
    private final BufferedImage[] greenTorchFrames;
    private final BufferedImage[] redTorchFrames;
    private final BufferedImage monsterImage;
    private final BufferedImage[] monsterFrames;

    private final BufferedImage chestClosed;
    private final BufferedImage chestOpen;
    private final BufferedImage doorClosed;
    private final BufferedImage doorOpen;
    private final BufferedImage keyImage;
    private final BufferedImage potionImage;

    private final int tile;
    private final Timer timer;

    private int torchFrame;
    private int monsterFrame;
    private double torchAccum;
    private double monsterAccum;
    private long lastNanos = -1;

    private GameFlowListener flow;
    private boolean notifiedGameOver;
    private boolean notifiedVictory;
    private boolean stopped;

    public DungeonPanel(
            AssetLoader loader,
            HudPanel hud,
            GameSession session) {

        this.session = session;
        this.hud = hud;
        this.tile = session.level().dungeon().tileSize();

        idleFrame = loader.loadImage("assets/Idle-frame0.png");

        runFrames = new BufferedImage[Player.RUN_FRAMES];
        for (int i = 0; i < runFrames.length; i++) {
            runFrames[i] = loader.loadImage(
                    "assets/Run_" + String.format("%03d", i) + ".png");
        }

        monsterImage = loader.loadImage("assets/monster_tentackle.png");

        monsterFrames = new BufferedImage[MONSTER_FRAMES];
        for (int i = 0; i < monsterFrames.length; i++) {
            monsterFrames[i] = loader.loadImage(
                    "assets/RunMONSTER_000 (" + (i + 1) + ").png");
        }

        torchFrames = new BufferedImage[TORCH_FRAMES];
        greenTorchFrames = new BufferedImage[TORCH_FRAMES];
        redTorchFrames = new BufferedImage[TORCH_FRAMES];

        for (int i = 0; i < TORCH_FRAMES; i++) {
            int frame = i + 1;

            torchFrames[i] = loader.loadImage(
                    "assets/walltorchflameani" + frame + ".png");

            greenTorchFrames[i] = loader.loadImage(
                    "assets/greengoblet2flameani" + frame + ".png");

            redTorchFrames[i] = loader.loadImage(
                    "assets/purplegoblet1flameani" + frame + ".png");
        }

        chestClosed = loader.loadImage("assets/chest_closed.png");
        chestOpen = loader.loadImage("assets/chest_open_full.png");
        doorClosed = loader.loadImage("assets/door_closed.png");
        doorOpen = loader.loadImage("assets/door_open.png");
        keyImage = loader.loadImage("assets/key.png");
        potionImage = loader.loadImage("assets/potion.png");

        setBackground(BACKGROUND);
        setPreferredSize(new Dimension(
                session.level().dungeon().cols() * tile,
                session.level().dungeon().rows() * tile));

        installKeyBindings();

        timer = new Timer(TICK_MS, e -> tick());
        timer.setCoalesce(true);
        timer.start();
    }

    public void setGameFlowListener(GameFlowListener flow) {
        this.flow = flow;
    }

    public void clearInput() {
        input.clear();
    }

    /**
     * Stops this panel's game loop before its game view is removed.
     * Safe to call more than once.
     */
    public void stopGameLoop() {
        if (stopped) {
            return;
        }

        stopped = true;
        timer.stop();
        input.clear();
    }

    InputState inputState() {
        return input;
    }

    private void tick() {
        if (stopped) {
            return;
        }

        long now = System.nanoTime();

        if (lastNanos < 0) {
            lastNanos = now;
        }

        double dt = (now - lastNanos) / 1_000_000_000.0;
        lastNanos = now;
        dt = Math.min(Math.max(dt, 0.0), MAX_DT_SECONDS);

        session.update(dt, input);

        monsterAccum += dt;
        while (monsterAccum >= MONSTER_FRAME_SECONDS) {
            monsterFrame = (monsterFrame + 1) % MONSTER_FRAMES;
            monsterAccum -= MONSTER_FRAME_SECONDS;
        }

        torchAccum += dt;
        while (torchAccum >= TORCH_FRAME_SECONDS) {
            torchFrame = (torchFrame + 1) % TORCH_FRAMES;
            torchAccum -= TORCH_FRAME_SECONDS;
        }

        hud.setLevel(session.level().name());
        hud.setScore(session.score());
        hud.setLives(session.lives());
        hud.setKey(session.hasKey());

        if (flow != null) {
            if (session.isGameOver() && !notifiedGameOver) {
                notifiedGameOver = true;
                stopGameLoop();
                flow.onGameOver();
                return;
            }

            if (session.isVictory() && !notifiedVictory) {
                notifiedVictory = true;
                stopGameLoop();
                flow.onVictory();
                return;
            }
        }

        repaint();
    }

    private void installKeyBindings() {
        setFocusable(true);

        bindKey(java.awt.event.KeyEvent.VK_W, "w");
        bindKey(java.awt.event.KeyEvent.VK_A, "a");
        bindKey(java.awt.event.KeyEvent.VK_S, "s");
        bindKey(java.awt.event.KeyEvent.VK_D, "d");

        bindKey(java.awt.event.KeyEvent.VK_UP, "up");
        bindKey(java.awt.event.KeyEvent.VK_DOWN, "down");
        bindKey(java.awt.event.KeyEvent.VK_LEFT, "left");
        bindKey(java.awt.event.KeyEvent.VK_RIGHT, "right");
    }

    private void bindKey(int keyCode, String name) {
        String pressName = "press-" + name;
        String releaseName = "release-" + name;

        InputMap inputMap =
                getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);

        inputMap.put(
                KeyStroke.getKeyStroke(keyCode, 0, false),
                pressName);

        inputMap.put(
                KeyStroke.getKeyStroke(keyCode, 0, true),
                releaseName);

        getActionMap().put(pressName, inputAction(name, true));
        getActionMap().put(releaseName, inputAction(name, false));
    }

    private Action inputAction(String name, boolean held) {
        return new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!stopped) {
                    input.set(name, held);
                }
            }
        };
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();

        try {
            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            Dungeon dungeon = session.level().dungeon();

            for (int row = 0; row < dungeon.rows(); row++) {
                for (int col = 0; col < dungeon.cols(); col++) {
                    int x = col * tile;
                    int y = row * tile;

                    if (dungeon.isSolid(col, row)) {
                        drawWall(g2, x, y, row, col);
                    } else {
                        drawFloor(g2, x, y, row, col);
                    }
                }
            }

            for (int row = 0; row < dungeon.rows(); row++) {
                for (int col = 0; col < dungeon.cols(); col++) {
                    if (dungeon.isTorch(col, row)) {
                        drawTorch(g2, col * tile, row * tile);
                    }
                }
            }

            drawObjects(g2);
            drawPlayer(g2);
            drawVignette(g2, getWidth(), getHeight());
        } finally {
            g2.dispose();
        }
    }

    private boolean isLevel2() {
        return session.levelIndex() == 1;
    }

    private boolean isLevel3() {
        return session.levelIndex() == 2;
    }

    private void drawObjects(Graphics2D g2) {
        for (GameObject object : session.level().objects()) {
            switch (object.kind()) {
                case MONSTER ->
                        drawMonster(g2, object.col(), object.row());

                case CHEST -> {
                    BufferedImage sprite =
                            object.removed() ? chestOpen : chestClosed;

                    drawObjectSprite(
                            g2, sprite, object.col(), object.row(), 40, 40);
                }

                case DOOR -> {
                    BufferedImage sprite =
                            object.removed() ? doorOpen : doorClosed;

                    drawObjectSprite(
                            g2, sprite, object.col(), object.row(), 48, 48);
                }

                case KEY -> {
                    if (!object.removed()) {
                        drawObjectSprite(
                                g2, keyImage,
                                object.col(), object.row(), 22, 44);
                    }
                }

                case POTION -> {
                    if (!object.removed()) {
                        drawObjectSprite(
                                g2, potionImage,
                                object.col(), object.row(), 28, 34);
                    }
                }

                default -> {
                    // No sprite is required for this object.
                }
            }
        }
    }

    private void drawMonster(Graphics2D g2, int col, int row) {
        int[] bob = {0, -1, -2, -1, 0, 1};
        int yOffset = bob[monsterFrame];

        BufferedImage sprite = monsterImage;

        if (isLevel3() && isValidImage(monsterFrames[monsterFrame])) {
            sprite = monsterFrames[monsterFrame];
        }

        int maxSize = (int) (tile * 0.92);
        int centerX = col * tile + tile / 2;
        int centerY = row * tile + tile - 7;

        g2.setColor(new Color(0, 0, 0, 75));
        g2.fillOval(centerX - 13, centerY - 3, 26, 7);

        drawObjectSprite(
                g2, sprite, col, row, maxSize, maxSize, yOffset);
    }

    private void drawObjectSprite(
            Graphics2D g2,
            BufferedImage image,
            int col,
            int row,
            int maxWidth,
            int maxHeight) {

        drawObjectSprite(
                g2, image, col, row, maxWidth, maxHeight, 0);
    }

    private void drawObjectSprite(
            Graphics2D g2,
            BufferedImage image,
            int col,
            int row,
            int maxWidth,
            int maxHeight,
            int yOffset) {

        if (!isValidImage(image) || maxWidth <= 0 || maxHeight <= 0) {
            return;
        }

        double scale = Math.min(
                (double) maxWidth / image.getWidth(),
                (double) maxHeight / image.getHeight());

        int w = Math.max(1, (int) Math.round(image.getWidth() * scale));
        int h = Math.max(1, (int) Math.round(image.getHeight() * scale));

        int x = col * tile + (tile - w) / 2;
        int y = row * tile + (tile - h) / 2 + yOffset;

        g2.drawImage(image, x, y, w, h, null);
    }

    private static boolean isValidImage(BufferedImage image) {
        return image != null
                && image.getWidth() > 0
                && image.getHeight() > 0;
    }

    private void drawFloor(
            Graphics2D g2, int x, int y, int row, int col) {

        Color base;

        if (isLevel3()) {
            base = ((row + col) & 1) == 0
                    ? LEVEL3_FLOOR_BASE : LEVEL3_FLOOR_ALT;
        } else if (isLevel2()) {
            base = ((row + col) & 1) == 0
                    ? LEVEL2_FLOOR_BASE : LEVEL2_FLOOR_ALT;
        } else {
            base = ((row + col) & 1) == 0
                    ? FLOOR_BASE : FLOOR_ALT;
        }

        int variation = ((row * 7 + col * 13) % 5) - 2;

        g2.setColor(shift(base, variation));
        g2.fillRect(x, y, tile, tile);

        Color grout = isLevel3()
                ? LEVEL3_FLOOR_GROUT
                : isLevel2() ? LEVEL2_FLOOR_GROUT : FLOOR_GROUT;

        g2.setColor(grout);
        g2.drawRect(x, y, tile, tile);

        g2.setColor(new Color(255, 255, 255, 9));
        g2.drawLine(x + 2, y + 2, x + tile - 3, y + 2);
    }

    private void drawWall(
            Graphics2D g2, int x, int y, int row, int col) {

        int variation = ((row * 5 + col * 3) % 5) - 2;

        Color dark;
        Color mid;
        Color light;

        if (isLevel3()) {
            dark = LEVEL3_WALL_DARK;
            mid = LEVEL3_WALL_MID;
            light = LEVEL3_WALL_LIGHT;
        } else if (isLevel2()) {
            dark = LEVEL2_WALL_DARK;
            mid = LEVEL2_WALL_MID;
            light = LEVEL2_WALL_LIGHT;
        } else {
            dark = WALL_DARK;
            mid = WALL_MID;
            light = WALL_LIGHT;
        }

        g2.setColor(dark);
        g2.fillRect(x, y, tile, tile);

        g2.setColor(shift(mid, variation));
        g2.fillRect(x + 2, y + 2, tile - 4, tile - 4);

        g2.setColor(light);
        g2.fillRect(x + 2, y + 2, tile - 4, 6);
        g2.fillRect(x + 2, y + 2, 6, tile - 4);

        g2.setColor(dark);
        g2.fillRect(x + 2, y + tile - 8, tile - 4, 6);
        g2.fillRect(x + tile - 8, y + 2, 6, tile - 4);

        g2.setColor(new Color(255, 255, 255, 12));
        g2.drawLine(
                x + tile / 2, y + 5,
                x + tile / 2, y + tile - 5);

        g2.setColor(new Color(0, 0, 0, 140));
        g2.drawRect(x, y, tile, tile);
    }

    private void drawTorch(Graphics2D g2, int x, int y) {
        float cx = x + tile / 2f;
        float cy = y + tile / 2f;
        float radius = tile * 1.6f;

        Color inner;
        Color middle;
        Color outer;
        Color holder;

        if (isLevel3()) {
            inner = new Color(255, 75, 100, 105);
            middle = new Color(230, 35, 75, 45);
            outer = new Color(170, 20, 55, 0);
            holder = new Color(48, 17, 25);
        } else if (isLevel2()) {
            inner = new Color(95, 255, 135, 95);
            middle = new Color(50, 220, 110, 38);
            outer = new Color(30, 180, 90, 0);
            holder = new Color(22, 42, 31);
        } else {
            inner = new Color(255, 190, 90, 90);
            middle = new Color(255, 150, 40, 35);
            outer = new Color(255, 140, 30, 0);
            holder = new Color(40, 30, 20);
        }

        Paint previous = g2.getPaint();

        g2.setPaint(new RadialGradientPaint(
                new Point2D.Float(cx, cy),
                radius,
                new float[]{0f, 0.35f, 1f},
                new Color[]{inner, middle, outer}));

        g2.fillRect(
                x - tile, y - tile,
                tile * 3, tile * 3);

        g2.setPaint(previous);

        g2.setColor(holder);
        g2.fillRect((int) cx - 4, y + tile - 14, 8, 12);

        BufferedImage[] frames;

        if (isLevel3()) {
            frames = redTorchFrames;
        } else if (isLevel2()) {
            frames = greenTorchFrames;
        } else {
            frames = torchFrames;
        }

        BufferedImage frame = frames[torchFrame];
        int size = (int) (tile * 0.85);

        if (isValidImage(frame)) {
            g2.drawImage(
                    frame,
                    (int) (cx - size / 2.0),
                    y + tile / 2 - size / 2 - 2,
                    size, size, null);
        }
    }

    private void drawPlayer(Graphics2D g2) {
        Player player = session.player();

        BufferedImage image = player.isMoving()
                ? runFrames[player.runFrame()]
                : idleFrame;

        if (!isValidImage(image)) {
            return;
        }

        int size = tile;
        int drawX = (int) Math.round(player.x() - size / 2.0);
        int drawY = (int) Math.round(player.y() - size / 2.0);

        if (player.facingLeft()) {
            g2.drawImage(
                    image,
                    drawX + size, drawY,
                    drawX, drawY + size,
                    0, 0, image.getWidth(), image.getHeight(),
                    null);
        } else {
            g2.drawImage(image, drawX, drawY, size, size, null);
        }
    }

    private void drawVignette(Graphics2D g2, int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }

        float cx = width / 2f;
        float cy = height / 2f;
        float radius = Math.max(1f,
                (float) Math.hypot(width, height) / 2f);

        Paint previous = g2.getPaint();

        g2.setPaint(new RadialGradientPaint(
                new Point2D.Float(cx, cy),
                radius,
                new float[]{0.55f, 1f},
                new Color[]{
                        new Color(0, 0, 0, 0),
                        new Color(0, 0, 0, 110)
                }));

        g2.fillRect(0, 0, width, height);
        g2.setPaint(previous);
    }

    private static Color shift(Color color, int delta) {
        return new Color(
                clamp(color.getRed() + delta),
                clamp(color.getGreen() + delta),
                clamp(color.getBlue() + delta),
                color.getAlpha());
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}