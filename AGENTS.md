# Copilot instructions for PixelForge

## Build, run, and test

Requires JDK 25+ and Maven 3.9+.

- Run the JavaFX application: `mvn javafx:run`
- Run all tests: `mvn test`
- Run one test class: `mvn -Dtest=PixelImageTest test`
- Run one test method: `mvn -Dtest=PixelImageTest#rejectsInvalidDimensions test`
- Build and test the runnable JAR: `mvn clean package`
- Launch the packaged application: `java -jar target/pixelforge-0.1.0-SNAPSHOT.jar`

There is no separate lint command configured in `pom.xml`. Release builds run
`mvn --batch-mode clean package` on Java 25 when a `v*` tag is pushed.

## Architecture

- `core/image/PixelImage` is the JavaFX-independent image model: a flat `int[]`
  pixel buffer using `0xAARRGGBB`. `core/document/Document` currently wraps a
  single image. Keep model and editing logic independent of JavaFX so it can be
  tested without launching the application.
- `ui/MainWindow` owns the active document and editing state. It translates
  JavaFX input into pixel coordinates and routes changes through tools and the
  command manager.
- Edits use `commands/Command` implementations managed by
  `CommandManager`. Single-pixel changes use `SetPixelCommand`; multi-pixel
  actions use `SetPixelBatchCommand`, which retains prior colors for undo.
  Keep user-visible edits undoable and clear command history when replacing a
  document.
- `rendering/CanvasRenderer` converts the pixel buffer into a JavaFX image and
  draws it onto one `Canvas`, with checkerboard transparency, zoom/pan offsets,
  and an optional pixel grid. Do not create a JavaFX node per pixel.
- `tools/` contains editing operations over the image and command manager;
  tool selection and pointer-event handling remain in `MainWindow`.
- The optional stdio MCP server (`mcp/PixelForgeMcpServer`) is started by
  `App` with `--mcp`. MCP requests that read or edit the visible document are
  dispatched to the JavaFX application thread and use `MainWindow`'s MCP
  methods, so UI and MCP edits share the same document, renderer, and undo
  history. `PixelCanvasSession` handles MCP validation, rasterization,
  inspection, and PNG output. Keep edits from each draw call as one undoable
  batch, and sequence operations against the live canvas.

## Repository-specific conventions

- Use the `com.pixelforge` package hierarchy: image/document model in `core`,
  undoable changes in `commands`, canvas presentation in `rendering`, UI event
  coordination in `ui`, and MCP protocol/session code in `mcp`.
- Store colors as packed ARGB integers (`0xAARRGGBB`); zero is transparent.
- Core behavior is covered with JUnit 5 tests under the matching
  `src/test/java/com/pixelforge/...` package. Test model, command, tool, image
  I/O, and MCP session behavior without starting JavaFX where possible.
- MCP canvas inputs are deliberately bounded and validated in
  `PixelCanvasSession`: canvases are at most 1024 × 1024, pixel batches at
  most 10,000 changes, shape output at most 250,000 pixels, and inspection
  regions at most 64 × 64. Preserve whole-request validation before applying
  MCP changes.
- The workspace MCP registration is `.vscode/mcp.json`; it launches the
  packaged JAR with `--mcp` and writes output under `pixel-art-output/`.
