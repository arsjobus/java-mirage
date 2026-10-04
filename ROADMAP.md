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

- [ ] Layer model
- [ ] Layer visibility
- [ ] Layer locking
- [ ] Layer opacity
- [ ] Add/delete/rename/reorder layers
- [ ] Duplicate layer
- [ ] Rectangle selection
- [ ] Freeform selection
- [ ] Select all/deselect
- [ ] Copy/cut/paste
- [ ] Move selection
- [ ] Selection undo/redo

## v0.3 — Transformations and project format

- [ ] Flip horizontal/vertical
- [ ] Rotate 90 degrees
- [ ] Crop
- [ ] Resize canvas/image
- [ ] Versioned `.pxf` project format
- [ ] Save/load layers
- [ ] Save/load metadata
- [ ] Autosave
- [ ] Crash recovery

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
Project format
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
