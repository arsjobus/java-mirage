# Mirage Development Roadmap

## Phase 0 — Foundation

- [x] Java 25 project
- [x] Maven build
- [x] JavaFX application
- [x] Core/UI separation
- [x] JUnit setup
- [x] README and architecture documentation

## v0.1 — Pixel canvas

- [x] Pixel image buffer
- [x] Pixel renderer
- [x] Zoom
- [x] Pan
- [x] Pixel grid
- [x] Pencil
- [x] Eraser
- [x] Eyedropper
- [x] Fill bucket
- [x] Color selection
- [x] New document
- [x] PNG open/save
- [x] Basic command architecture
- [x] Undo/redo
- [x] Pixel model tests

## v0.2 — Layers and selections

- [x] Layer model
- [x] Layer visibility
- [x] Layer locking
- [x] Layer opacity
- [x] Add/delete/rename/reorder layers
- [x] Duplicate layer
- [x] Rectangle selection
- [x] Freeform selection
- [x] Select all/deselect
- [x] Copy/cut/paste
- [x] Move selection
- [x] Selection undo/redo

## v0.3 — Transformations and project format

### Transformations

- [x] Flip the whole document horizontally/vertically
- [x] Rotate the whole document 90 degrees clockwise/counterclockwise
- [x] Crop the whole document to the selection bounds
- [ ] Resize canvas without scaling artwork
  - [ ] Set width/height and choose an anchor
  - [ ] Keep existing pixels in place relative to the anchor; expose transparent pixels
- [ ] Resize image, scaling artwork with nearest-neighbor sampling
- [ ] Preserve layer order/properties and transform the selection with every operation
- [ ] Make each operation one undoable command; verify undo/redo restores pixels, dimensions, and selection

All document-wide transformations apply consistently to every layer. The initial resize implementation is nearest-neighbor only; other resampling modes are out of scope for pixel art.

### Native project format

- [ ] Specify and document `.mpa` format version 1 before implementation
- [ ] Store the project as a ZIP container with a JSON manifest and lossless RGBA layer images
- [ ] Save/load canvas dimensions, layer pixel data, order, names, visibility, lock state, opacity, and active layer
- [ ] Record format version and application version; reject unsupported major versions with a useful error
- [ ] Validate the complete archive and manifest before replacing the open document
- [ ] Save atomically so an interrupted save does not corrupt the previous project
- [ ] Round-trip test transparent pixels, multiple layers, metadata, and malformed/unsupported files

Version 1 is for editable still-image documents. Selection and undo history are transient and are not saved. Keep the manifest extensible for future frames, palettes, and tiles; do not serialize Java objects or make future fields mandatory for version 1 readers.

### Recovery

- [ ] Add autosave only after native save/load is reliable
- [ ] Offer crash recovery without overwriting the last explicitly saved project
- [ ] Clear recovered state after a successful explicit save or user dismissal

## v0.4 — Animation

- [ ] Frames
- [ ] Timeline
- [ ] Add/delete/duplicate/move frame
- [ ] Frame duration
- [ ] FPS
- [ ] Playback
- [ ] Looping
- [ ] Onion skin
- [ ] Animated GIF export
- [ ] Sprite-sheet export

## v0.5 — Palette system

- [ ] Palette model
- [ ] Palette panel
- [ ] Add/remove/reorder colors
- [ ] Palette swapping
- [ ] Recent colors
- [ ] Indexed color mode
- [ ] Palette import/export
- [ ] Palette presets

Potential presets:

```text
NES
Game Boy
C64
Amiga
Custom
```

## v0.6 — Tiles and tile maps

- [ ] 8x8 tile mode
- [ ] 16x16 tile mode
- [ ] 32x32 tile mode
- [ ] TileSet
- [ ] Tile palette
- [ ] Tile selection
- [ ] TileMap
- [ ] Tile placement
- [ ] Tile duplication
- [ ] Tile flipping
- [ ] Tile-map export
- [ ] Tile grid overlays

## v0.7 — Sprite/retro workflows

- [ ] Sprite-sheet importer
- [ ] Automatic tile slicing
- [ ] Tile rearrangement
- [ ] Original-to-edited tile mapping
- [ ] Sprite assembly
- [ ] Sprite extraction
- [ ] Mapping export
- [ ] Retro palette constraints
- [ ] NES-oriented workflow
- [ ] Configurable tile dimensions

Example:

```text
Original       Edited

0 1 2 3        5 2 0 7
4 5 6 7        1 6 3 4
```

The mapping should be retained so external ROM/build tools can consume it.

## v0.8 — Advanced editing

- [ ] Line
- [ ] Rectangle
- [ ] Ellipse
- [x] Fill
- [ ] Gradient
- [ ] Replace color
- [ ] Color ramps
- [ ] Symmetry/mirror mode
- [ ] Pixel-perfect line mode
- [ ] Brush sizes
- [ ] Custom brushes
- [ ] Dithering
- [ ] Tile-aware drawing

## v0.9 — Application polish

- [ ] Multiple documents
- [ ] Document tabs
- [ ] Dockable panels
- [ ] Resizable panels
- [ ] Dark/light themes
- [ ] Preferences
- [ ] Shortcut editor
- [ ] Recent files
- [ ] Autosave/recovery
- [ ] Fullscreen
- [ ] Status bar
- [ ] Drag/drop files
- [ ] Clipboard integration
- [ ] Application icons
- [ ] Performance profiling
- [ ] Large-canvas testing

## v1.0 — Stable release

### Core

- [ ] Pixel-perfect rendering
- [ ] Layers
- [ ] Selections
- [ ] Transformations
- [ ] Undo/redo
- [ ] Animation
- [ ] Palettes
- [ ] Tiles
- [ ] Tile maps

### Tools

- [ ] Pencil
- [ ] Eraser
- [ ] Eyedropper
- [ ] Line
- [ ] Rectangle
- [ ] Ellipse
- [ ] Fill
- [ ] Gradient
- [ ] Selection
- [ ] Move
- [ ] Crop
- [ ] Zoom

### Formats

- [ ] PNG
- [ ] GIF
- [ ] BMP
- [ ] PXF
- [ ] Sprite-sheet export
- [ ] Palette import/export
- [ ] Optional Aseprite `.ase/.aseprite` compatibility

### Platforms

- [ ] Windows
- [ ] macOS
- [ ] Linux
- [ ] Bundled Java runtime
- [ ] Native application packaging

## Recommended implementation order

```text
PixelImage
    |
    v
CanvasRenderer
    |
    v
Mouse -> pixel coordinate conversion
    |
    v
Pencil / Eraser / Eyedropper
    |
    v
Undo / Redo
    |
    v
PNG import/export
    |
    v
Layers
    |
    v
Selections
    |
    v
Transformations
    |
    v
Project format specification
    |
    v
Project save/load and round-trip tests
    |
    v
Autosave/recovery
    |
    v
Animation
    |
    v
Palettes
    |
    v
Tiles
    |
    v
Sprite/tile workflows
    |
    v
Advanced tools
    |
    v
UI polish
    |
    v
1.0
```

## Architectural rules

1. Keep JavaFX out of core model classes.
2. Do not represent each pixel as a JavaFX Node.
3. Use a compact pixel buffer.
4. Make editing operations commands so they can be undone.
5. Version the project file format from its first release.
6. Keep tiles reusable by sprite sheets, tile maps, animation, and export.
7. Keep rendering separate from document storage.
8. Keep the core testable without launching the GUI.
