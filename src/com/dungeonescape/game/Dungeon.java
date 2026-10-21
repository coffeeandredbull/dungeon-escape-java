package com.dungeonescape.game;

/**
 * The dungeon tile map: layout, dimensions, and collision queries.
 *
 * <p>Pure game data with no Swing dependencies, so it can be tested headlessly
 * and shared by both rendering and movement code. The layout is a literal grid
 * of characters: {@code '#'} wall, {@code '.'} walkable floor, and {@code 'T'}
 * a wall block carrying a torch.</p>
 */
public class Dungeon {

    /** Pixel size of one tile; shared by rendering and collision. */
    public static final int TILE = 48;

    /** Default map, used when no explicit layout is supplied. */
    public static final String[] DEFAULT_LAYOUT = {
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

    /** Player start tile for the default layout. */
    public static final int DEFAULT_START_COL = 5;
    public static final int DEFAULT_START_ROW = 5;

    private final String[] layout;
    private final int startCol;
    private final int startRow;
    private final int cols;
    private final int rows;

    /** Creates the default dungeon. */
    public Dungeon() {
        this(DEFAULT_LAYOUT, DEFAULT_START_COL, DEFAULT_START_ROW);
    }

    /**
     * Creates a dungeon from an explicit layout.
     *
     * @param layout   rectangular grid of {@code #} wall, {@code .} floor,
     *                 {@code T} torch tiles
     * @param startCol player start column (must be walkable)
     * @param startRow player start row (must be walkable)
     */
    public Dungeon(String[] layout, int startCol, int startRow) {
        validateLayout(layout, startCol, startRow);
        this.layout = layout.clone();
        this.startCol = startCol;
        this.startRow = startRow;
        this.cols = layout[0].length();
        this.rows = layout.length;
    }

    /** Number of tile columns. */
    public int cols() {
        return cols;
    }

    /** Number of tile rows. */
    public int rows() {
        return rows;
    }

    /** Pixel size of one tile. */
    public int tileSize() {
        return TILE;
    }

    /** True if the tile is solid (wall/torch) or outside the map. */
    public boolean isSolid(int col, int row) {
        if (col < 0 || row < 0 || col >= cols || row >= rows) {
            return true;
        }
        char cell = layout[row].charAt(col);
        return cell == '#' || cell == 'T';
    }

    /** True if the tile holds an animated torch. */
    public boolean isTorch(int col, int row) {
        if (col < 0 || row < 0 || col >= cols || row >= rows) {
            return false;
        }
        return layout[row].charAt(col) == 'T';
    }

    /** True if the tile can be walked on. */
    public boolean isWalkable(int col, int row) {
        return !isSolid(col, row);
    }

    /** Player start X in pixels (center of the start tile). */
    public double startX() {
        return startCol * TILE + TILE / 2.0;
    }

    /** Player start Y in pixels (center of the start tile). */
    public double startY() {
        return startRow * TILE + TILE / 2.0;
    }

    private static void validateLayout(String[] layout, int startCol, int startRow) {
        if (layout == null || layout.length == 0 || layout[0].isEmpty()) {
            throw new IllegalStateException("Dungeon layout is empty");
        }
        int width = layout[0].length();
        for (int r = 0; r < layout.length; r++) {
            if (layout[r].length() != width) {
                throw new IllegalStateException("Dungeon layout row " + r
                        + " has width " + layout[r].length() + ", expected " + width);
            }
        }
        if (startCol < 0 || startRow < 0 || startCol >= width || startRow >= layout.length
                || layout[startRow].charAt(startCol) != '.') {
            throw new IllegalStateException("Player start tile ("
                    + startCol + "," + startRow + ") is not walkable floor");
        }
    }
}
