package org.example;

import org.json.JSONObject;

import java.awt.Color;

/**
 * Model for the render settings fetched from GET /fm1/get-render-config.
 *
 * These values are always taken from the server. Nothing in the drawing /
 * animation code is allowed to hardcode them (see the spec's hard constraints).
 */
public class RenderConfig {

    public String wallCellColor;
    public String pathColor;
    public boolean drawGrid;
    public String gridColor;
    public int animationDelayMs;

    /**
     * Reads all five fields from a JSON object shaped like:
     * {
     *   "wallCellColor": "#222222",
     *   "pathColor": "#00AA00",
     *   "drawGrid": true,
     *   "gridColor": "#CCCCCC",
     *   "animationDelayMs": 80
     * }
     */
    public RenderConfig(JSONObject json) {
        this.wallCellColor = json.getString("wallCellColor");
        this.pathColor = json.getString("pathColor");
        this.drawGrid = json.getBoolean("drawGrid");
        this.gridColor = json.getString("gridColor");
        this.animationDelayMs = json.getInt("animationDelayMs");
    }

    /** Turns a "#RRGGBB" hex string into an AWT Color. */
    public static Color parseColor(String hex) {
        return Color.decode(hex);
    }

    public Color wallCellColorAsColor() {
        return parseColor(wallCellColor);
    }

    public Color pathColorAsColor() {
        return parseColor(pathColor);
    }

    public Color gridColorAsColor() {
        return parseColor(gridColor);
    }

    @Override
    public String toString() {
        return "RenderConfig{" +
                "wallCellColor='" + wallCellColor + '\'' +
                ", pathColor='" + pathColor + '\'' +
                ", drawGrid=" + drawGrid +
                ", gridColor='" + gridColor + '\'' +
                ", animationDelayMs=" + animationDelayMs +
                '}';
    }

    /** Quick manual test for Step 2 — construct from a sample JSON and read fields back. */
    public static void main(String[] args) {
        String sample = "{\"wallCellColor\":\"#222222\",\"pathColor\":\"#00AA00\","
                + "\"drawGrid\":true,\"gridColor\":\"#CCCCCC\",\"animationDelayMs\":80}";
        RenderConfig cfg = new RenderConfig(new JSONObject(sample));

        System.out.println(cfg);
        System.out.println("wallCellColor -> " + cfg.wallCellColorAsColor());
        System.out.println("pathColor     -> " + cfg.pathColorAsColor());
        System.out.println("gridColor     -> " + cfg.gridColorAsColor());

        boolean ok = cfg.wallCellColor.equals("#222222")
                && cfg.pathColor.equals("#00AA00")
                && cfg.drawGrid
                && cfg.gridColor.equals("#CCCCCC")
                && cfg.animationDelayMs == 80
                && cfg.wallCellColorAsColor().equals(new Color(0x22, 0x22, 0x22));
        System.out.println(ok ? "STEP 2 OK" : "STEP 2 FAILED");
    }
}
