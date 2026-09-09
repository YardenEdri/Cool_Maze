package org.example;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;

/**
 * The initial settings screen.
 *
 * Shows the five render-config values fetched from the server, lets the user
 * refresh them, lets the user type a maze width/height, and has a GET MAZE
 * button (wired up in Step 6).
 *
 * Absolute positioning (setLayout(null) + setBounds), same style as the
 * course's MainScene / Menu / Scene panels.
 */
public class SettingsPanel extends JPanel {

    // Width/height validation bounds (Step 5). A field whose text is not a valid
    // integer, or is outside [MIN_SIZE, MAX_SIZE], falls back to DEFAULT_SIZE.
    static final int MIN_SIZE = 5;
    static final int MAX_SIZE = 100;
    static final int DEFAULT_SIZE = 30;

    // Layout for the color swatches drawn next to the color labels (Step 4
    // follow-up): a small filled square instead of printing the hex code.
    private static final int COLOR_SQUARE_SIZE = 14;
    private static final int COLOR_SQUARE_X = 150;

    private final JFrame frame;

    // The most recently fetched config. Passed on to the MazePanel in Step 6.
    private RenderConfig config;

    // Resolved maze size from Step 5 (kept here so Step 6 can read it).
    // Every later step must use these, never re-read the text fields.
    private int actualWidth;
    private int actualHeight;

    private final JLabel wallCellColorLabel = new JLabel();
    private final JLabel pathColorLabel = new JLabel();
    private final JLabel drawGridLabel = new JLabel();
    private final JLabel gridColorLabel = new JLabel();
    private final JLabel animationDelayLabel = new JLabel();
    private final JLabel statusLabel = new JLabel();

    // The color squares currently shown next to wallCellColorLabel /
    // pathColorLabel / gridColorLabel. Null until the first config loads.
    private JPanel wallCellColorSquare;
    private JPanel pathColorSquare;
    private JPanel gridColorSquare;

    private final JTextField widthField = new JTextField("30");
    private final JTextField heightField = new JTextField("30");

    private final JButton refreshButton = new JButton("Refresh Config");
    private final JButton getMazeButton = new JButton("GET MAZE");

    public SettingsPanel(JFrame frame) {
        this.frame = frame;
        setLayout(null);

        JLabel title = new JLabel("Render config (from server)");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
        title.setBounds(20, 10, 300, 24);
        add(title);

        wallCellColorLabel.setBounds(20, 40, 400, 20);
        pathColorLabel.setBounds(20, 62, 400, 20);
        drawGridLabel.setBounds(20, 84, 400, 20);
        gridColorLabel.setBounds(20, 106, 400, 20);
        animationDelayLabel.setBounds(20, 128, 400, 20);
        add(wallCellColorLabel);
        add(pathColorLabel);
        add(drawGridLabel);
        add(gridColorLabel);
        add(animationDelayLabel);

        // These three show a color swatch (added/updated in
        // updateColorSquares) instead of a hex code, so their own text is
        // just the field name and never changes again.
        wallCellColorLabel.setText("wallCellColor:");
        pathColorLabel.setText("pathColor:");
        gridColorLabel.setText("gridColor:");

        JLabel sizeTitle = new JLabel("Maze size");
        sizeTitle.setFont(sizeTitle.getFont().deriveFont(Font.BOLD, 14f));
        sizeTitle.setBounds(20, 165, 300, 24);
        add(sizeTitle);

        JLabel widthCaption = new JLabel("Width:");
        widthCaption.setBounds(20, 195, 60, 24);
        add(widthCaption);
        widthField.setBounds(85, 195, 80, 24);
        add(widthField);

        JLabel heightCaption = new JLabel("Height:");
        heightCaption.setBounds(20, 225, 60, 24);
        add(heightCaption);
        heightField.setBounds(85, 225, 80, 24);
        add(heightField);

        refreshButton.setBounds(20, 270, 150, 28);
        refreshButton.addActionListener(e -> loadConfig());
        add(refreshButton);

        getMazeButton.setBounds(185, 270, 120, 28);
        getMazeButton.addActionListener(e -> getMaze());
        add(getMazeButton);

        statusLabel.setForeground(new Color(120, 120, 120));
        statusLabel.setBounds(20, 310, 460, 20);
        add(statusLabel);

        // Initial config load, on a background thread.
        loadConfig();
    }

    /** Fetches the render config on a background thread and updates the labels. */
    private void loadConfig() {
        setBusy(true, "Loading config...");
        new Thread(() -> {
            try {
                RenderConfig fetched = ApiClient.getRenderConfig();
                SwingUtilities.invokeLater(() -> {
                    this.config = fetched;
                    applyConfigToLabels(fetched);
                    setBusy(false, "Config loaded.");
                });
            } catch (ApiClient.ApiException ex) {
                SwingUtilities.invokeLater(() -> {
                    setBusy(false, "Config load failed.");
                    JOptionPane.showMessageDialog(this, ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }

    private void applyConfigToLabels(RenderConfig cfg) {
        drawGridLabel.setText("drawGrid: " + cfg.drawGrid);
        animationDelayLabel.setText("animationDelayMs: " + cfg.animationDelayMs);
        updateColorSquares(cfg);
    }

    /**
     * Updates the wallCellColor / pathColor / gridColor swatches shown next
     * to their labels. Split out of applyConfigToLabels so each piece stays
     * simple: this one only deals with the three colors that come back from
     * the server.
     */
    private void updateColorSquares(RenderConfig cfg) {
        wallCellColorSquare = placeColorSquare(wallCellColorSquare,
                cfg.wallCellColorAsColor(), COLOR_SQUARE_X, 40);
        pathColorSquare = placeColorSquare(pathColorSquare,
                cfg.pathColorAsColor(), COLOR_SQUARE_X, 62);
        gridColorSquare = placeColorSquare(gridColorSquare,
                cfg.gridColorAsColor(), COLOR_SQUARE_X, 106);
        revalidate();
        repaint();
    }

    /**
     * Removes the previously shown square (if any) and adds a freshly built
     * one at the given position, so repeated Refresh Config clicks never
     * leave a stale square sitting behind the new one.
     */
    private JPanel placeColorSquare(JPanel previousSquare, Color color, int x, int y) {
        if (previousSquare != null) {
            remove(previousSquare);
        }
        JPanel square = createColorSquare(color);
        square.setBounds(x, y, COLOR_SQUARE_SIZE, COLOR_SQUARE_SIZE);
        add(square);
        return square;
    }

    /**
     * Converts a color into a small filled square component — this is what
     * gets shown instead of printing the color's hex code as plain text.
     */
    private JPanel createColorSquare(Color color) {
        JPanel square = new JPanel();
        square.setBackground(color);
        square.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        return square;
    }

    private void setBusy(boolean busy, String status) {
        refreshButton.setEnabled(!busy);
        getMazeButton.setEnabled(!busy);
        statusLabel.setText(status);
    }

    // --- Step 6: GET MAZE behaviour ------------------------------------------

    /**
     * Resolves width/height (Step 5), fetches the maze image on a background
     * thread, decodes it into a {@link MazeData} (Step 7) and swaps the JFrame
     * content over to the {@link MazePanel}. A network failure shows an error
     * dialog and leaves the settings screen in place.
     */
    private void getMaze() {
        if (config == null) {
            JOptionPane.showMessageDialog(this,
                    "Render config has not loaded yet. Try Refresh Config first.",
                    "Config missing", JOptionPane.WARNING_MESSAGE);
            return;
        }

        resolveSizes();
        int w = actualWidth;
        int h = actualHeight;
        RenderConfig cfg = config;

        setBusy(true, "Fetching maze " + w + " x " + h + "...");
        new Thread(() -> {
            try {
                BufferedImage image = ApiClient.getMazeImage(w, h);
                MazeData maze = new MazeData(image, w, h);
                SwingUtilities.invokeLater(() -> {
                    MazePanel mazePanel = new MazePanel(frame, cfg, maze);
                    JScrollPane scrollPane = new JScrollPane(mazePanel);
                    frame.getContentPane().removeAll();
                    frame.getContentPane().add(scrollPane);
                    // Resize the window so the whole maze is visible, but never
                    // bigger than the screen: for large mazes (up to the allowed
                    // 100x100) the scroll pane lets the user reach every cell
                    // instead of the window running off the edges of the display.
                    frame.pack();
                    java.awt.Rectangle screenBounds =
                            java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment()
                                    .getMaximumWindowBounds();
                    int width = Math.min(frame.getWidth(), screenBounds.width);
                    int height = Math.min(frame.getHeight(), screenBounds.height);
                    frame.setSize(width, height);
                    frame.setLocationRelativeTo(null);
                    frame.revalidate();
                    frame.repaint();
                });
            } catch (RuntimeException ex) {
                SwingUtilities.invokeLater(() -> {
                    setBusy(false, "Maze fetch failed.");
                    JOptionPane.showMessageDialog(this, ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }

    // --- Step 5: width/height validation --------------------------------------

    /**
     * Resolves one size text field to the value the program will actually use:
     * the parsed integer if it is valid and within [MIN_SIZE, MAX_SIZE],
     * otherwise DEFAULT_SIZE. Applied independently per field.
     */
    static int resolveDimension(String text) {
        try {
            int value = Integer.parseInt(text.trim());
            if (value < MIN_SIZE || value > MAX_SIZE) {
                return DEFAULT_SIZE;
            }
            return value;
        } catch (NumberFormatException ex) {
            return DEFAULT_SIZE;
        }
    }

    /**
     * Reads both text fields, resolves them (Step 5) and stores the results in
     * actualWidth / actualHeight. Called when GET MAZE is clicked (Step 6);
     * later steps read the stored values, not the fields.
     */
    void resolveSizes() {
        actualWidth = resolveDimension(widthField.getText());
        actualHeight = resolveDimension(heightField.getText());
    }

    int getActualWidth() {
        return actualWidth;
    }

    int getActualHeight() {
        return actualHeight;
    }

    /** Test hook for the Step 11 harness: set the raw width/height field text. */
    void setSizeFieldsForTest(String width, String height) {
        widthField.setText(width);
        heightField.setText(height);
    }

    // --- Placeholder hooks used by later steps ---------------------------------

    RenderConfig getConfig() {
        return config;
    }

    /**
     * Quick manual test for Step 4 — shows the settings screen.
     * Run with the argument "step5" instead to run the Step 5 validation
     * self-test on the console (no window).
     */
    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("step5")) {
            runStep5SelfTest();
            return;
        }
        // The settings screen is what the app opens on, so this is just the app.
        Main.main(args);
    }

    /**
     * Step 5 self-test: each field's text resolves independently, falling back
     * to 30 on a non-integer, an empty value, or a value outside [5, 100].
     */
    private static void runStep5SelfTest() {
        String[][] cases = {
                // text, expected
                {"30", "30"},
                {"40", "40"},
                {"5", "5"},
                {"100", "100"},
                {"200", "30"},
                {"2", "30"},
                {"0", "30"},
                {"-7", "30"},
                {"abc", "30"},
                {"", "30"},
                {"  ", "30"},
                {" 25 ", "25"},
                {"12.5", "30"},
        };
        boolean ok = true;
        for (String[] c : cases) {
            int actual = resolveDimension(c[0]);
            int expected = Integer.parseInt(c[1]);
            boolean pass = actual == expected;
            ok &= pass;
            System.out.printf("resolveDimension(%-6s) -> %-3d  (expected %-3d)  %s%n",
                    "\"" + c[0] + "\"", actual, expected, pass ? "OK" : "FAIL");
        }

        // Independence: an invalid width must not force height to 30.
        int w = resolveDimension("abc");
        int h = resolveDimension("42");
        boolean independent = w == 30 && h == 42;
        System.out.println("independent width/height: " + (independent ? "OK" : "FAIL"));
        ok &= independent;

        System.out.println(ok ? "STEP 5 OK" : "STEP 5 FAILED");
    }
}
