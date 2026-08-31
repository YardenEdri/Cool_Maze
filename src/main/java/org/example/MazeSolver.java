package org.example;

import java.awt.Point;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/**
 * Breadth-first search over the maze grid.
 *
 * Coordinates follow the {@link MazeData} convention: {@code Point(x, y)} where
 * {@code x} is the column and {@code y} is the row. Movement is 4-directional
 * (no diagonals) and only into in-bounds passable cells.
 */
public class MazeSolver {

    // Up, down, left, right.
    private static final int[][] DIRECTIONS = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};

    /**
     * Finds a shortest path from the top-left cell (0, 0) to the bottom-right
     * cell (width-1, height-1).
     *
     * @return the ordered list of cells from start to end, or an empty list if
     *         there is no path (including when the start or end cell is a wall).
     */
    public List<Point> solve(MazeData maze) {
        int width = maze.getWidth();
        int height = maze.getHeight();

        int endX = width - 1;
        int endY = height - 1;

        if (!maze.isPassable(0, 0) || !maze.isPassable(endX, endY)) {
            return new ArrayList<>();
        }

        boolean[][] visited = new boolean[width][height];
        // parent[x][y] holds the cell we reached (x, y) from; null at the start.
        Point[][] parent = new Point[width][height];

        Deque<Point> queue = new ArrayDeque<>();
        queue.add(new Point(0, 0));
        visited[0][0] = true;

        while (!queue.isEmpty()) {
            Point current = queue.poll();

            if (current.x == endX && current.y == endY) {
                return reconstructPath(parent, current);
            }

            for (int[] dir : DIRECTIONS) {
                int nx = current.x + dir[0];
                int ny = current.y + dir[1];

                if (nx < 0 || nx >= width || ny < 0 || ny >= height) {
                    continue;
                }
                if (visited[nx][ny] || !maze.isPassable(nx, ny)) {
                    continue;
                }

                visited[nx][ny] = true;
                parent[nx][ny] = current;
                queue.add(new Point(nx, ny));
            }
        }

        return new ArrayList<>();
    }

    /** Walks parent pointers back from the end cell, then reverses. */
    private List<Point> reconstructPath(Point[][] parent, Point end) {
        List<Point> path = new ArrayList<>();
        for (Point p = end; p != null; p = parent[p.x][p.y]) {
            path.add(p);
        }
        Collections.reverse(path);
        return path;
    }

    /** Quick manual test for Step 9 — one solvable maze and one with no path. */
    public static void main(String[] args) {
        MazeSolver solver = new MazeSolver();

        // 5x5, '.' passable, '#' wall. A path snakes from (0,0) to (4,4).
        boolean[][] solvable = grid(
                ".....",
                ".###.",
                ".#...",
                ".#.#.",
                "...#."
        );
        List<Point> path = solver.solve(new MazeData(solvable));
        System.out.println("solvable -> " + path.size() + " cells: " + path);
        boolean ok1 = !path.isEmpty()
                && path.get(0).equals(new Point(0, 0))
                && path.get(path.size() - 1).equals(new Point(4, 4));

        // Same layout but the start cell is walled off.
        boolean[][] blocked = grid(
                "#....",
                ".###.",
                ".#...",
                ".#.#.",
                "...#."
        );
        List<Point> none = solver.solve(new MazeData(blocked));
        System.out.println("blocked  -> " + none.size() + " cells");
        boolean ok2 = none.isEmpty();

        // End cell is open but sealed off from the rest of the maze.
        boolean[][] noRoute = grid(
                ".....",
                ".....",
                ".....",
                "....#",
                "...#."
        );
        List<Point> noRouteResult = solver.solve(new MazeData(noRoute));
        System.out.println("no route -> " + noRouteResult.size() + " cells");
        boolean ok3 = noRouteResult.isEmpty();

        System.out.println((ok1 && ok2 && ok3) ? "STEP 9 OK" : "STEP 9 FAILED");
    }

    /** Builds a {@code [x][y]} passable grid from rows of '.' and '#'. */
    private static boolean[][] grid(String... rows) {
        int height = rows.length;
        int width = rows[0].length();
        boolean[][] passable = new boolean[width][height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                passable[x][y] = rows[y].charAt(x) == '.';
            }
        }
        return passable;
    }
}
