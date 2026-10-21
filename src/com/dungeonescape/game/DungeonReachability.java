
package com.dungeonescape.game;

/**
 * Checks which dungeon tiles can be reached from the player's start.
 *
 * Uses recursive depth-first search (DFS) to visit connected walkable
 * tiles. Walls and torch tiles are treated as solid obstacles.
 */
public final class DungeonReachability {

    private DungeonReachability() {
        // Utility class; no instances are required.
    }

    /**
     * Checks whether a target tile can be reached from the dungeon start.
     *
     * @param dungeon dungeon to explore
     * @param targetCol target column
     * @param targetRow target row
     * @return true if the target is walkable and reachable
     */
    public static boolean isReachable(
            Dungeon dungeon, int targetCol, int targetRow) {

        if (dungeon == null
                || !isInside(dungeon, targetCol, targetRow)
                || dungeon.isSolid(targetCol, targetRow)
                || dungeon.isSolid(dungeon.startCol(), dungeon.startRow())) {
            return false;
        }

        boolean[][] visited = new boolean[dungeon.rows()][dungeon.cols()];

        floodFill(
                dungeon,
                visited,
                dungeon.startCol(),
                dungeon.startRow());

        return visited[targetRow][targetCol];
    }

    /**
     * Checks whether every walkable tile belongs to the same connected area.
     *
     * @param dungeon dungeon to explore
     * @return true if all walkable tiles are connected to the start
     */
    public static boolean isFullyConnected(Dungeon dungeon) {
        if (dungeon == null
                || dungeon.isSolid(dungeon.startCol(), dungeon.startRow())) {
            return false;
        }

        boolean[][] visited = new boolean[dungeon.rows()][dungeon.cols()];

        int reachableCount = floodFill(
                dungeon,
                visited,
                dungeon.startCol(),
                dungeon.startRow());

        int walkableCount = 0;

        for (int row = 0; row < dungeon.rows(); row++) {
            for (int col = 0; col < dungeon.cols(); col++) {
                if (dungeon.isWalkable(col, row)) {
                    walkableCount++;
                }
            }
        }

        return reachableCount == walkableCount;
    }

    /**
     * Recursively visits walkable tiles connected to the current tile.
     *
     * The visited matrix prevents revisiting tiles and stops infinite
     * recursion when the dungeon contains loops.
     *
     * @return number of newly visited walkable tiles
     */
    static int floodFill(
            Dungeon dungeon,
            boolean[][] visited,
            int col,
            int row) {

        // Base case: stop outside the dungeon.
        if (!isInside(dungeon, col, row)) {
            return 0;
        }

        // Base case: stop at a tile already explored.
        if (visited[row][col]) {
            return 0;
        }

        // Base case: walls and torches cannot be crossed.
        if (dungeon.isSolid(col, row)) {
            return 0;
        }

        // Mark the tile before exploring neighbours to avoid cycles.
        visited[row][col] = true;

        int count = 1;

        // Recursive case: explore the four orthogonal neighbours.
        count += floodFill(dungeon, visited, col + 1, row);
        count += floodFill(dungeon, visited, col - 1, row);
        count += floodFill(dungeon, visited, col, row + 1);
        count += floodFill(dungeon, visited, col, row - 1);

        return count;
    }

    private static boolean isInside(
            Dungeon dungeon, int col, int row) {
        return dungeon != null
                && col >= 0
                && row >= 0
                && col < dungeon.cols()
                && row < dungeon.rows();
    }
}
