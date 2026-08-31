package org.example;

import java.awt.image.BufferedImage;

/**
 * The decoded maze structure.
 *
 * Indexing convention (fixed for the whole program): {@code passable[x][y]}
 * where {@code x} is the column (0 .. width-1, left to right) and {@code y} is
 * the row (0 .. height-1, top to bottom).
 *
 * Value convention: {@code true} means a passable cell, {@code false} means a
 * wall cell.
 *
 * The source PNG draws every cell as a square block of pixels. The block size
 * is not fixed by the spec; it is derived here as image size / requested size
 * (see spec section 2.2). A white sample at the block centre means passable;
 * any non-white sample means a wall.
 */
public class MazeData {

    private final int width;
    private final int height;
    private final boolean[][] passable;

    public MazeData(BufferedImage image, int width, int height) {
        this.width = width;
        this.height = height;

        int cellPixels = image.getWidth() / width;
        int cellPixelsY = image.getHeight() / height;
        if (cellPixels != cellPixelsY || cellPixels < 1) {
            throw new IllegalArgumentException(
                    "Image " + image.getWidth() + "x" + image.getHeight()
                            + " does not divide evenly into a " + width + "x" + height
                            + " grid (got " + cellPixels + " vs " + cellPixelsY + " px per cell)");
        }

        this.passable = new boolean[width][height];
        int half = cellPixels / 2;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int rgb = image.getRGB(x * cellPixels + half, y * cellPixels + half);
                passable[x][y] = isWhite(rgb);
            }
        }
    }

    /**
     * Builds a maze directly from a {@code [x][y]} passable grid. Used by tests
     * that need a known layout without going through an image.
     */
    MazeData(boolean[][] passable) {
        this.width = passable.length;
        this.height = passable[0].length;
        this.passable = passable;
    }

    /** True when the RGB value is pure white, ignoring the alpha byte. */
    private static boolean isWhite(int rgb) {
        return (rgb & 0xFFFFFF) == 0xFFFFFF;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    /** {@code true} = passable, {@code false} = wall. Indexed {@code [x][y]}. */
    public boolean isPassable(int x, int y) {
        return passable[x][y];
    }

    /** Renders the grid as text ('.' passable, '#' wall) for manual verification. */
    public String toGridString() {
        StringBuilder sb = new StringBuilder();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                sb.append(passable[x][y] ? '.' : '#');
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    /** Quick manual test for Step 7 — fetch a small maze and print its grid. */
    public static void main(String[] args) {
        int w = 5, h = 5;
        BufferedImage image = ApiClient.getMazeImage(w, h);
        MazeData maze = new MazeData(image, w, h);
        System.out.println("Fetched " + image.getWidth() + "x" + image.getHeight()
                + " image for a " + w + "x" + h + " maze:");
        System.out.print(maze.toGridString());
        System.out.println("(compare this against the fetched image)");
        kong.unirest.Unirest.shutDown();
    }
}
