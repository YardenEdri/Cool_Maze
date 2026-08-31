package org.example;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The maze screen. Draws the maze the program decoded (never the server PNG)
 * and owns the Check Solution button + solve animation.
 *
 * Drawing (Step 8): every cell is a filled {@code cellSize} square — white for
 * passable, {@code config.wallCellColor} for walls — with optional grid lines
 * in {@code config.gridColor} when {@code config.drawGrid} is set.
 *
 * Check Solution (Step 10): runs {@link MazeSolver}, then reveals the path one
 * cell at a time on a background thread, waiting {@code config.animationDelayMs}
 * between cells. A second click while an animation runs is ignored; a click
 * after one finished clears the previous path first.
 */
public class MazePanel extends JPanel {

    /** The one visual value the spec allows to be hardcoded. */
    private static final int CELL_SIZE = 20;

    private final JFrame frame;
    private final RenderConfig config;
    private final MazeData maze;

    // Colors resolved once from the fetched config (never hardcoded).
    private final Color wallColor;
    private final Color gridColor;
    private final Color pathColor;

    private final JButton checkButton = new JButton("Check Solution");

    // Which cells the animation has revealed so far. Mutated only on the EDT.
    private final boolean[][] revealed;

    // True while an animation thread is running; blocks overlapping runs.
    private final AtomicBoolean animating = new AtomicBoolean(false);

    // Counts how many reveal animations have actually started (test hook for the
    // Step 11 "no concurrent animation" check).
    private final AtomicInteger animationRuns = new AtomicInteger(0);

    public MazePanel(JFrame frame, RenderConfig config, MazeData maze) {
        this.frame = frame;
        this.config = config;
        this.maze = maze;
        this.wallColor = config.wallCellColorAsColor();
        this.gridColor = config.gridColorAsColor();
        this.pathColor = config.pathColorAsColor();
        this.revealed = new boolean[maze.getWidth()][maze.getHeight()];

        setLayout(null);
        setBackground(Color.WHITE);

        int mazePixelWidth = maze.getWidth() * CELL_SIZE;
        int mazePixelHeight = maze.getHeight() * CELL_SIZE;

        checkButton.setBounds(10, mazePixelHeight + 10, 150, 28);
        checkButton.addActionListener(e -> onCheckSolution());
        add(checkButton);

        setPreferredSize(new Dimension(
                Math.max(mazePixelWidth, 170) + 20,
                mazePixelHeight + 50));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        for (int x = 0; x < maze.getWidth(); x++) {
            for (int y = 0; y < maze.getHeight(); y++) {
                int px = x * CELL_SIZE;
                int py = y * CELL_SIZE;

                if (revealed[x][y]) {
                    g.setColor(pathColor);
                } else {
                    g.setColor(maze.isPassable(x, y) ? Color.WHITE : wallColor);
                }
                g.fillRect(px, py, CELL_SIZE, CELL_SIZE);

                if (config.drawGrid) {
                    g.setColor(gridColor);
                    g.drawRect(px, py, CELL_SIZE, CELL_SIZE);
                }
            }
        }
    }

    /**
     * Check Solution click handler. Ignored while an animation runs; otherwise
     * clears any previous path, solves, and either reports "no solution" or
     * starts the reveal animation.
     */
    private void onCheckSolution() {
        if (animating.get()) {
            return;
        }

        clearRevealed();
        repaint();

        List<Point> path = new MazeSolver().solve(maze);
        if (path.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No solution found",
                    "Check Solution", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        animating.set(true);
        checkButton.setEnabled(false);
        startRevealAnimation(path);
    }

    private void clearRevealed() {
        for (boolean[] column : revealed) {
            java.util.Arrays.fill(column, false);
        }
    }

    /**
     * Reveals the path one cell at a time on a background thread (same pattern
     * as the course's game loops). The {@code revealed} array and repaint are
     * touched only on the EDT via invokeLater; the worker thread just sleeps.
     */
    private void startRevealAnimation(List<Point> path) {
        animationRuns.incrementAndGet();
        Thread worker = new Thread(() -> {
            try {
                for (Point cell : path) {
                    SwingUtilities.invokeLater(() -> {
                        revealed[cell.x][cell.y] = true;
                        repaint();
                    });
                    Thread.sleep(config.animationDelayMs);
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } finally {
                SwingUtilities.invokeLater(() -> {
                    animating.set(false);
                    checkButton.setEnabled(true);
                });
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    // --- Package-private hooks for the Step 11 verification harness -----------

    /** Force the revealed path (skips the animation) for render tests. */
    void revealForTest(List<Point> path) {
        for (Point cell : path) {
            revealed[cell.x][cell.y] = true;
        }
    }

    void triggerCheckForTest() {
        onCheckSolution();
    }

    boolean isAnimating() {
        return animating.get();
    }

    int animationRuns() {
        return animationRuns.get();
    }

    int revealedCount() {
        int count = 0;
        for (boolean[] column : revealed) {
            for (boolean cell : column) {
                if (cell) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Quick manual test for Steps 8 + 10 — fetch a real config + maze, render the
     * unsolved panel and (if solvable) the fully revealed path to PNGs so the
     * drawing can be eyeballed. Pass an output directory as the first argument.
     */
    public static void main(String[] args) throws Exception {
        String dir = args.length > 0 ? args[0] : ".";
        int w = 20, h = 15;
        RenderConfig cfg = ApiClient.getRenderConfig();
        MazeData maze = new MazeData(ApiClient.getMazeImage(w, h), w, h);
        System.out.println("config: " + cfg);

        MazePanel panel = new MazePanel(null, cfg, maze);
        writePng(panel, new File(dir, "step8-maze.png"));

        List<Point> path = new MazeSolver().solve(maze);
        System.out.println("solver: " + (path.isEmpty() ? "no solution" : path.size() + " cells"));
        if (!path.isEmpty()) {
            panel.revealForTest(path);
            writePng(panel, new File(dir, "step10-solved.png"));
        }
        kong.unirest.Unirest.shutDown();
    }

    private static void writePng(MazePanel panel, File file) throws Exception {
        Dimension size = panel.getPreferredSize();
        panel.setSize(size);
        BufferedImage out = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
        Graphics g = out.getGraphics();
        panel.paint(g);
        g.dispose();
        ImageIO.write(out, "png", file);
        System.out.println("wrote " + file.getAbsolutePath());
    }
}
