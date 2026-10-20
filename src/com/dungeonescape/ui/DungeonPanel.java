package com.dungeonescape.ui;

import com.dungeonescape.asset.AssetLoader;

import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

/**
 * Fixed-size dungeon view: a tile-based grid drawn with Java2D.
 *
 * <p>The layout is a literal grid of characters: {@code '#'} is a wall,
 * {@code '.'} is walkable floor, and {@code 'T'} is a wall block carrying a
 * torch. Torch flames are animated with the existing
 * {@code walltorchflameani1..7.png} frames.</p>
 *
 * <p>All images are loaded once in the constructor via {@link AssetLoader};
 * {@link #paintComponent} only draws from the already-loaded images so
 * rendering stays responsive and never touches the file system.</p>
 */
public class DungeonPanel extends JPanel {

    private static final int TILE = 48;
    private static final int FRAME_MS = 90;
    private static final int TORCH_FRAMES = 7;

    // # = wall, . = floor, T = torch mounted on a wall block
    private static final String[] LAYOUT = {
            "################",
            "#..............#",
            "#..##......##..#",
            "#..#........#..#",
            "#..T........T..#",
            "#......##......#",
            "#......##......#",
            "#......##......#",
            "#..T........T..#",
            "#..#........#..#",
            "#..##......##..#",
            "################",
    };

    private static final Color FLOOR_BASE = new Color(26, 28, 36);
    private static final Color FLOOR_ALT = new Color(31, 34, 44);
    private static final Color FLOOR_GROUT = new Color(14, 15, 20);
    private static final Color WALL_DARK = new Color(18, 17, 20);
    private static final Color WALL_MID = new Color(70, 64, 58);
    private static final Color WALL_LIGHT = new Color(108, 100, 88);
    private static final Color BACKGROUND = new Color(8, 8, 12);

    private final int cols;
    private final int rows;
    private final BufferedImage[] torchFrames;
    private int frameIndex;
    private final Timer timer;

    /**
     * Creates the dungeon view and preloads its torch frames.
     *
     * @param loader the asset loader used to open images (never called during
     *               painting)
     * @throws com.dungeonescape.asset.AssetLoadException if a torch frame is
     *         missing or invalid
     */
    public DungeonPanel(AssetLoader loader) {
        validateLayout();
        this.cols = LAYOUT[0].length();
        this.rows = LAYOUT.length;

        this.torchFrames = new BufferedImage[TORCH_FRAMES];
        for (int i = 0; i < TORCH_FRAMES; i++) {
            torchFrames[i] = loader.loadImage("assets/walltorchflameani" + (i + 1) + ".png");
        }

        setBackground(BACKGROUND);
        setPreferredSize(new Dimension(cols * TILE, rows * TILE));

        this.timer = new Timer(FRAME_MS, e -> {
            frameIndex = (frameIndex + 1) % TORCH_FRAMES;
            repaint();
        });
        timer.start();
    }

    /** Ensures the layout is rectangular so rendering never reads out of bounds. */
    private static void validateLayout() {
        if (LAYOUT.length == 0 || LAYOUT[0].isEmpty()) {
            throw new IllegalArgumentException("Dungeon layout is empty");
        }
        int width = LAYOUT[0].length();
        for (int r = 0; r < LAYOUT.length; r++) {
            if (LAYOUT[r].length() != width) {
                throw new IllegalArgumentException("Dungeon layout row " + r
                        + " has width " + LAYOUT[r].length() + ", expected " + width);
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                char cell = LAYOUT[row].charAt(col);
                int x = col * TILE;
                int y = row * TILE;
                if (cell == '#' || cell == 'T') {
                    drawWall(g2, x, y, row, col);
                } else {
                    drawFloor(g2, x, y, row, col);
                }
            }
        }

        // Torches are drawn last so their glow overlays the stone.
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                if (LAYOUT[row].charAt(col) == 'T') {
                    drawTorch(g2, col * TILE, row * TILE);
                }
            }
        }

        drawVignette(g2, getWidth(), getHeight());
        g2.dispose();
    }

    /** Deep, slightly varied stone floor with grout lines. */
    private void drawFloor(Graphics2D g2, int x, int y, int row, int col) {
        Color base = ((row + col) & 1) == 0 ? FLOOR_BASE : FLOOR_ALT;
        int variation = ((row * 7 + col * 13) % 5) - 2;
        g2.setColor(shift(base, variation));
        g2.fillRect(x, y, TILE, TILE);

        g2.setColor(FLOOR_GROUT);
        g2.drawRect(x, y, TILE, TILE);

        g2.setColor(new Color(255, 255, 255, 9));
        g2.drawLine(x + 2, y + 2, x + TILE - 3, y + 2);
    }

    /** Warm, beveled stone block that clearly contrasts with the floor. */
    private void drawWall(Graphics2D g2, int x, int y, int row, int col) {
        int variation = ((row * 5 + col * 3) % 5) - 2;

        g2.setColor(WALL_DARK);
        g2.fillRect(x, y, TILE, TILE);

        g2.setColor(shift(WALL_MID, variation));
        g2.fillRect(x + 2, y + 2, TILE - 4, TILE - 4);

        g2.setColor(WALL_LIGHT);
        g2.fillRect(x + 2, y + 2, TILE - 4, 6);
        g2.fillRect(x + 2, y + 2, 6, TILE - 4);

        g2.setColor(WALL_DARK);
        g2.fillRect(x + 2, y + TILE - 8, TILE - 4, 6);
        g2.fillRect(x + TILE - 8, y + 2, 6, TILE - 4);

        g2.setColor(new Color(255, 255, 255, 12));
        g2.drawLine(x + TILE / 2, y + 5, x + TILE / 2, y + TILE - 5);

        g2.setColor(new Color(0, 0, 0, 140));
        g2.drawRect(x, y, TILE, TILE);
    }

    /** Warm radial glow, a small mount, and the current flame frame. */
    private void drawTorch(Graphics2D g2, int x, int y) {
        float cx = x + TILE / 2f;
        float cy = y + TILE / 2f;
        float radius = TILE * 1.6f;

        Paint previous = g2.getPaint();
        g2.setPaint(new RadialGradientPaint(
                new Point2D.Float(cx, cy), radius,
                new float[] {0f, 0.35f, 1f},
                new Color[] {
                        new Color(255, 190, 90, 90),
                        new Color(255, 150, 40, 35),
                        new Color(255, 140, 30, 0)
                }));
        g2.fillRect(x - TILE, y - TILE, TILE * 3, TILE * 3);
        g2.setPaint(previous);

        g2.setColor(new Color(40, 30, 20));
        g2.fillRect((int) cx - 4, y + TILE - 14, 8, 12);

        BufferedImage frame = torchFrames[frameIndex];
        int size = (int) (TILE * 0.85);
        g2.drawImage(frame, (int) (cx - size / 2.0), y + TILE / 2 - size / 2 - 2,
                size, size, null);
    }

    /** Subtle darkened corners for a moody look. */
    private void drawVignette(Graphics2D g2, int width, int height) {
        float cx = width / 2f;
        float cy = height / 2f;
        float radius = (float) Math.hypot(width, height) / 2f;

        Paint previous = g2.getPaint();
        g2.setPaint(new RadialGradientPaint(
                new Point2D.Float(cx, cy), radius,
                new float[] {0.55f, 1f},
                new Color[] {new Color(0, 0, 0, 0), new Color(0, 0, 0, 110)}));
        g2.fillRect(0, 0, width, height);
        g2.setPaint(previous);
    }

    private static Color shift(Color c, int delta) {
        return new Color(
                clamp(c.getRed() + delta),
                clamp(c.getGreen() + delta),
                clamp(c.getBlue() + delta),
                c.getAlpha());
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
