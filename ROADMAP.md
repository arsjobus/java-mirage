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

- [x] Flip every layer and the selection horizontally/vertically
- [x] Rotate every layer and the selection 90 degrees clockwise/counterclockwise
- [x] Crop every layer and the selection to the selection bounds
- [x] Add **Canvas Size**: set positive width/height and choose one of nine anchors (center by default)
- [x] Keep artwork at the same anchor-relative position when changing canvas size; clip pixels outside smaller bounds and leave newly exposed pixels transparent
- [x] Add **Resize Image**: set positive width/height and scale every layer with nearest-neighbor sampling
- [x] Use the same pixel-coordinate mapping for layer pixels and the selection mask when scaling; specify edge cases in tests
- [x] Preserve layer order, names, visibility, lock state, opacity, and active layer through every transformation
- [x] Make each operation a single undoable command; undo/redo restores exact pixels, dimensions, and selection
- [x] Test transforms on multiple layers, including hidden/locked/partially transparent layers, and on selections at canvas edges

Canvas Size changes the document bounds without scaling artwork. Resize Image scales artwork to the requested dimensions. Both apply to the entire document; additional resampling modes are out of scope for pixel art.

### Native project format

- [ ] Write the `.mpa` version 1 specification before implementing save/load
- [ ] Define a ZIP layout with one UTF-8 JSON manifest and one lossless RGBA PNG per layer; keep layer pixels separate rather than flattening
- [ ] Specify manifest fields for format/application versions, canvas dimensions, ordered layer entries, layer names and properties, and active-layer identity
- [ ] Define required versus optional fields, defaults for optional fields, and compatibility rules for unknown fields
- [ ] Save and load the complete editable document: dimensions, pixel data, layer order/properties, and active layer
- [ ] Keep selection and undo/redo history transient; do not serialize Java objects
- [ ] Validate the full archive, manifest, layer references, dimensions, and decoded images before changing the open document
- [ ] Reject unsupported major format versions and invalid or resource-excessive inputs with a useful error
- [ ] Save through a temporary file and replace the destination only after a successful write, preserving an existing project if saving fails
- [ ] Round-trip test transparent pixels, multiple layers, metadata, and active layer; test malformed archives, missing/duplicate entries, invalid dimensions, and unsupported versions

Version 1 targets editable still-image documents. Keep the manifest extensible for future frames, palettes, and tiles without making those future fields mandatory for version 1 readers.

### Recovery

- [ ] Add autosave only after native save/load and atomic-save behavior are reliable
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
