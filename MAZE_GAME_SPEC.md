# Maze Game — Java Swing Exercise

## 1. Overview

Build a Java Swing desktop application that displays a maze fetched from a
remote API as an image, decodes the maze structure from the image's pixels,
draws the maze itself (never the raw image), and can find and animate a
solution path through it.

The program has two screens inside a single `JFrame` (no separate windows):

1. **Settings screen** — shown on startup. Displays render settings fetched
   from the server, lets the user refresh those settings, lets the user pick
   a maze width/height, and has a `GET MAZE` button.
2. **Maze screen** — shown after `GET MAZE` is clicked. Displays the maze
   drawn by the program (not the server's raw PNG) and has a
   `Check Solution` button that finds a path from the top-left cell to the
   bottom-right cell and animates it if one exists.

The two screens replace each other's content inside the same `JFrame`
(`removeAll()` / `add()` / `revalidate()` / `repaint()`), matching the style
already used in this course's Swing exercises (see `Scene.java`,
`MainScene.java` in the JavaClass reference material) — do not use
`CardLayout` or any UI framework not already used in the course material.

## 2. Confirmed API details

Both endpoints below were tested live and confirmed working. **Use this
exact base URL for both calls** — an older/incorrect base URL
(`backend-qcf9.onrender.com`) appears once in the original spec but returns
404; ignore it.

### 2.1 Render config

```
GET https://shaitest-production-3066.up.railway.app/fm1/get-render-config
```

Returns a JSON object, e.g.:

```json
{
  "wallCellColor": "#222222",
  "pathColor": "#00AA00",
  "drawGrid": true,
  "gridColor": "#CCCCCC",
  "animationDelayMs": 80
}
```

| Field | Meaning |
|---|---|
| `wallCellColor` | Hex color to paint wall cells in the maze the program draws. May differ from the wall color in the source image. |
| `pathColor` | Hex color used to paint the solution path during the animation. |
| `drawGrid` | `true`/`false` — whether to draw grid lines between cells. |
| `gridColor` | Hex color for grid lines, used only if `drawGrid` is `true`. |
| `animationDelayMs` | Milliseconds to wait between coloring one path cell and the next during the solution animation. |

Fetch this once on startup, and again every time the user clicks
`Refresh Config`.

### 2.2 Maze image

```
GET https://shaitest-production-3066.up.railway.app/fm1/get-maze-image?width={width}&height={height}
```

Returns a PNG image that represents a maze grid of exactly
`width` columns by `height` rows (the values the user asked for). **Each
cell is drawn as a square block of pixels, not a single pixel.** The block
size is not fixed by the spec — the program must compute it from the image
it actually receives:

```
cellPixels = imageWidth  / width      // e.g. 480 / 30 = 16
           = imageHeight / height      // must come out the same on both axes
```

(The live server currently uses 16×16 pixel blocks, but the code must rely
on the division above, not on the literal `16`.)

To read cell `(x, y)`, sample a pixel inside its block — use the block
centre: `image.getRGB(x * cellPixels + cellPixels / 2, y * cellPixels + cellPixels / 2)`.
A **white** sample means a passable cell; **any non-white** sample means a
wall cell. The wall color in this source image is random and is *not* the
color to render with — it is only used to detect "this is a wall".

Only called once, when the user clicks `GET MAZE`, using the actual
width/height the program is using (see validation rules below).

## 3. Tech stack (must match course conventions)

- **Maven** project, Java 17, package `org.example` — same layout as the
  other exercises in the JavaClass folder.
- **HTTP calls**: `com.konghq:unirest-java` (`kong.unirest.Unirest`) —
  same library already used in `folder/untitled1` and `Main.java` (fixer.io
  example). Use `.asString()` for the JSON config call and `.asBytes()` for
  the image call.
- **JSON parsing**: `org.json` (`org.json.JSONObject`) — same library
  already used in those same examples.
- **Swing**: `JFrame` + `JPanel`, absolute positioning
  (`setLayout(null)` + `setBounds(...)`), `JButton` with lambda
  `addActionListener`, custom `paintComponent(Graphics)` — same style as
  `MainScene.java`, `Menu.java`, `Scene.java`, `Player.java`.
- **Background work / animation**: a raw `Thread` that sleeps between
  steps — same pattern as `Scene.mainGameLoop()`, `TraficLight.changeColor()`,
  `Philosopher.startFlow()`. When a background thread needs to update the
  UI (labels, repaint), wrap that update in `SwingUtilities.invokeLater(...)`
  so it runs safely on the Event Dispatch Thread.
- Colors are parsed from hex strings (`"#RRGGBB"`) with
  `java.awt.Color.decode(hex)`.

## 4. Suggested project structure

```
MazeGame/
 ├─ pom.xml                                  (unirest-java 3.14.5, org.json)
 └─ src/main/java/org/example/
     ├─ Main.java              — entry point, creates the JFrame
     ├─ RenderConfig.java      — model: wallCellColor, pathColor, drawGrid, gridColor, animationDelayMs
     ├─ ApiClient.java         — the two HTTP calls
     ├─ MazeData.java          — boolean[][] passable + the actual width/height in use
     ├─ MazeSolver.java        — BFS pathfinding
     ├─ SettingsPanel.java     — the initial settings screen
     └─ MazePanel.java         — draws the maze, owns the Check Solution button + animation
```

## 5. Step-by-step build instructions

Follow these steps **in order**. Each step lists exactly what must be true
before moving to the next one.

### Step 1 — Set up the Maven project

- Create `pom.xml` with dependencies `com.konghq:unirest-java` (version
  `3.14.5`) and `org.json:json`, Java source/target `17`,
  `UTF-8` source encoding (copy the pattern from `folder/untitled1/pom.xml`
  in the course material).
- Create the standard `src/main/java/org/example/` folder structure.
- **Done when**: the project builds with `mvn compile` and produces no errors.

### Step 2 — `RenderConfig` model class

- Fields: `String wallCellColor`, `String pathColor`, `boolean drawGrid`,
  `String gridColor`, `int animationDelayMs`.
- Constructor takes a `JSONObject` and reads all five fields from it.
- Add a helper (either here or in a small color-utils method) that turns a
  hex string into `java.awt.Color` via `Color.decode(hex)`.
- **Done when**: given a sample JSON object matching section 2.1, a
  `RenderConfig` instance can be constructed and its fields read back
  correctly (write a quick manual test in `main` or a JUnit test).

### Step 3 — `ApiClient` class

- `RenderConfig getRenderConfig()`:
  `Unirest.get(CONFIG_URL).asString()` → `new JSONObject(response.getBody())`
  → `new RenderConfig(json)`.
- `BufferedImage getMazeImage(int width, int height)`:
  `Unirest.get(IMAGE_URL).queryString("width", width).queryString("height", height).asBytes()`
  → `ImageIO.read(new ByteArrayInputStream(response.getBody()))`.
- Wrap both calls in try/catch; on failure, surface the error to the caller
  (don't swallow it silently) so the UI layer can show a message dialog.
- **Done when**: both methods can be called from a scratch `main` and
  return real data from the live server.

### Step 4 — `SettingsPanel` — layout and initial config load

- On construction (or right after the `JFrame` becomes visible), call
  `ApiClient.getRenderConfig()` **on a background thread** (network call
  must not block the UI thread), then update the on-screen labels via
  `SwingUtilities.invokeLater(...)`.
- Display all five config values as simple labels — no need for fancy
  styling, just make the values readable.
- Add a `Refresh Config` button: re-calls `getRenderConfig()` (background
  thread again) and updates the same labels. **It must not touch the maze
  in any way** — at this point no maze exists yet.
- Add two `JTextField`s for `width` and `height`, both pre-filled with
  `"30"`.
- Add a `GET MAZE` button (wired up in Step 6).
- **Done when**: running the app shows the settings screen with real values
  from the server, and clicking Refresh Config visibly updates them
  (confirm by refreshing a few times and observing the values change,
  since the server returns randomized colors).

### Step 5 — Width/height validation

- When `GET MAZE` is clicked, read both text fields and compute the actual
  values to use:
  - Try `Integer.parseInt` on the field's text.
  - If parsing fails, **or** the parsed value is outside `[5, 100]`,
    treat that field's value as `30`.
  - Do this independently per field (an invalid `width` does not force
    `height` to 30 too, and vice versa).
- Store the resulting `actualWidth` / `actualHeight` — every later step
  (image request, pixel decoding, drawing, solving) must use these stored
  values, not re-read the text fields.
- **Done when**: typing `"200"`/`"2"`/`"abc"`/empty into either field and
  clicking GET MAZE results in that field falling back to 30, verified by
  checking the maze dimensions actually produced.

### Step 6 — `GET MAZE` button behavior

- On click: validate/resolve width & height (Step 5), call
  `ApiClient.getMazeImage(actualWidth, actualHeight)`.
- Once the image is received, decode it into a `MazeData` (Step 7), then
  swap the `JFrame`'s content from `SettingsPanel` to `MazePanel`
  (`removeAll()`, `add(mazePanel)`, `revalidate()`, `repaint()`).
- If the request fails (network error), show a `JOptionPane` error message
  and stay on the settings screen.
- **Done when**: clicking GET MAZE with valid values transitions the window
  to a maze screen, matching the requested width/height.

### Step 7 — `MazeData` — decode pixels into a maze structure

- Compute `cellPixels = image.getWidth() / actualWidth` (and assert it
  equals `image.getHeight() / actualHeight`). This is the pixel size of one
  cell's block in the received image (see section 2.2).
- Iterate every cell `(x, y)`, `x` in `[0, actualWidth)`, `y` in
  `[0, actualHeight)`.
- Sample the block centre:
  `image.getRGB(x * cellPixels + cellPixels / 2, y * cellPixels + cellPixels / 2)`.
  If the sample is white (`0xFFFFFF`, ignoring alpha), the cell is passable;
  any other color means a wall.
- Store the result in `boolean[][] passable`, indexed consistently (decide
  once whether it's `passable[x][y]` or `passable[y][x]` and use that
  convention everywhere — document it with a comment at the top of the
  class since this is a common source of bugs).
- `true` = passable cell (document this convention explicitly).
- **Done when**: for a small maze (e.g. 5×5), printing the boolean grid to
  the console visually matches a screenshot of the fetched image.

### Step 8 — `MazePanel` — draw the maze

- `cellSize` is a hardcoded constant (e.g. `20`) — this is the **only**
  visual value allowed to be hardcoded.
- In `paintComponent(Graphics g)`, for every cell `(x, y)`:
  - Draw a filled rectangle at `(x * cellSize, y * cellSize, cellSize, cellSize)`.
  - Passable cell → **white**.
  - Wall cell → `Color.decode(config.wallCellColor)`.
  - If `config.drawGrid` is `true`, also draw grid lines around the cell
    in `Color.decode(config.gridColor)`.
- Add a `Check Solution` button to this panel (wired up in Step 10).
- **Do not** hardcode any wall/path/grid color or the `drawGrid` decision
  or the animation delay anywhere — all of those must come from the
  `RenderConfig` that was fetched from the server.
- **Done when**: the maze screen visually renders a correct maze — white
  passages, wall-colored walls, and a grid if `drawGrid` was true for that
  fetch (test by comparing a few Refresh Config cycles where `drawGrid`
  differs).

### Step 9 — `MazeSolver` — BFS pathfinding

- Signature roughly: `List<Point> solve(MazeData maze)` — returns the
  ordered list of cells from start to end, or an empty
  list/`null` if there is no solution.
- Start cell: `(0, 0)` (top-left). End cell: `(width - 1, height - 1)`
  (bottom-right).
- If the start or end cell is a wall, return "no solution" immediately
  without running BFS.
- Use a queue (`java.util.ArrayDeque<Point>` or `LinkedList`), a
  `visited[][]` array, and a way to reconstruct the path (store a
  `parent` reference/coordinate per visited cell).
- Only 4-directional movement (up/down/left/right) — no diagonals. Only
  move into passable cells that are within bounds.
- **Done when**: tested against at least one maze known to have a solution
  and one known not to (e.g. force a wall at `(0,0)` for the no-solution
  case), returning correct results both times.

### Step 10 — `Check Solution` button + animation

- Clicking the button while an animation is already running must do
  **nothing** (guard with a boolean flag or `AtomicBoolean`, and also
  `checkButton.setEnabled(false)` while running, re-enabled when the
  animation finishes).
- If this is a repeat click after a previous animation finished, first
  reset the maze's drawn state back to its original (un-solved) look,
  then proceed exactly as a fresh click would.
- Run `MazeSolver.solve(...)`.
  - No solution → show `JOptionPane` with a message like
    `"No solution found"`. Re-enable the button.
  - Solution found → animate it on a **background thread** (same pattern
    as `Scene.mainGameLoop()`): for each cell in the path, in order,
    mark that cell as "part of the revealed solution" in a field
    `MazePanel` reads from during `paintComponent`, trigger a repaint via
    `SwingUtilities.invokeLater(panel::repaint)`, then
    `Thread.sleep(config.animationDelayMs)` before moving to the next
    cell. After the last cell, re-enable the button.
- **Done when**: clicking Check Solution on a solvable maze visibly colors
  the path one cell at a time at the configured delay, clicking again
  mid-animation has no effect, and clicking again after it finishes resets
  and replays correctly.

### Step 11 — Manual verification pass

Before considering this done, manually verify every case below:

- [ ] Valid width/height (e.g. 40×25) produces a maze of exactly that size.
- [ ] Invalid width/height (non-numeric, empty, `0`, `500`) falls back to
      `30` for the affected field only.
- [ ] `Refresh Config` before `GET MAZE` updates the displayed values and
      does not error.
- [ ] `Refresh Config` is not clickable/visible from the maze screen (or,
      if it is, confirm it never touches the loaded maze).
- [ ] A maze where `(0,0)` or `(width-1, height-1)` is a wall correctly
      shows "no solution" (try a small size like 5×5 a few times, or force
      it in a quick test).
- [ ] A solvable maze animates correctly at the configured
      `animationDelayMs`, using the configured `pathColor`.
- [ ] Rapid repeated clicks on `Check Solution` never start a second
      concurrent animation.
- [ ] A fetch where `drawGrid` is `false` renders with no grid lines; a
      fetch where it's `true` renders with grid lines in `gridColor`.
- [ ] Boundary sizes `5×5` and `100×100` both work without errors or
      unreasonable slowdown.
- [ ] The raw PNG from the server is never displayed directly anywhere in
      the UI — only the program's own Swing drawing is shown.

## 6. Hard constraints (do not violate)

- Never hardcode `wallCellColor`, `pathColor`, `gridColor`, the
  `drawGrid` boolean, or `animationDelayMs` — always read them from the
  `RenderConfig` fetched from the server.
- `cellSize` is the one exception — it is fine to hardcode it (e.g. `20`).
- Never display the raw maze PNG image as-is; it is only ever used as
  input for pixel decoding.
- All comments and identifiers in the code should be in English.
