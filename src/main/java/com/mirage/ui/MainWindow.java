package com.mirage.ui;

import com.mirage.commands.CommandManager;
import com.mirage.commands.SetPixelBatchCommand;
import com.mirage.core.document.Document;
import com.mirage.core.image.PixelColor;
import com.mirage.core.image.PixelImage;
import com.mirage.io.ImageExporter;
import com.mirage.mcp.PixelCanvasSession;
import com.mirage.rendering.CanvasRenderer;
import com.mirage.tools.EyedropperTool;
import com.mirage.tools.FillBucketTool;
import com.mirage.tools.SymmetryTransform;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MainWindow {

    private final BorderPane root = new BorderPane();
    private final Canvas canvas = new Canvas(760, 620);
    private final CanvasRenderer renderer = new CanvasRenderer();
    private final CommandManager commands = new CommandManager();
    private final ToggleGroup toolGroup = new ToggleGroup();
    private final ColorPicker colorPicker = new ColorPicker(Color.BLACK);
    private final CheckBox mirrorVertical = new CheckBox("Mirror left/right");
    private final CheckBox mirrorHorizontal = new CheckBox("Mirror top/bottom");
    private final Spinner<Integer> symmetryCenterX =
            new Spinner<>(0, 1024, 16);
    private final Spinner<Integer> symmetryCenterY =
            new Spinner<>(0, 1024, 16);
    private final Spinner<Integer> radialCopies = new Spinner<>(1, 12, 1);

    private final EyedropperTool eyedropper = new EyedropperTool();
    private final FillBucketTool fillBucket = new FillBucketTool();

    private Document document = new Document(32, 32);
    private double zoom = 16.0;
    private double offsetX = 80;
    private double offsetY = 50;
    private int currentColor = PixelColor.BLACK;
    private String activeTool = "Pencil";
    private boolean showGrid;
    private int gridCellWidth = 16;
    private int gridCellHeight = 16;

    private double panStartX, panStartY;
    private double originalOffsetX, originalOffsetY;
    private Map<SymmetryTransform.Point, StrokePixel> activeStroke;
    private int strokeColor;

    public MainWindow() {
        buildUi();
        installCanvasHandlers();
        installKeyboardShortcuts();
        redraw();
    }

    public BorderPane getRoot() {
        return root;
    }

    public void createCanvasFromMcp(int width, int height) {
        PixelCanvasSession.validateDimensions(width, height);
        document = new Document(width, height);
        document.setModified(true);
        commands.clear();
        resetSymmetryCenter();
        zoom = Math.max(1, Math.min(16, 512.0 / Math.max(width, height)));
        offsetX = 80;
        offsetY = 50;
        redraw();
    }

    public int drawPixelsFromMcp(
            List<PixelCanvasSession.PixelChange> changes
    ) {
        return drawPixelsFromMcp(changes, null);
    }

    public int drawPixelsFromMcp(
            List<PixelCanvasSession.PixelChange> changes,
            PixelCanvasSession.SymmetryOptions symmetry
    ) {
        PixelImage image = document.getImage();
        PixelCanvasSession.validateChanges(image, changes);
        List<PixelCanvasSession.PixelChange> output =
                PixelCanvasSession.symmetryChanges(image, changes, symmetry);
        List<SetPixelBatchCommand.PixelUpdate> updates = output.stream()
                .map(change -> new SetPixelBatchCommand.PixelUpdate(
                        change.x(), change.y(), change.argb()
                ))
                .toList();
        commands.execute(new SetPixelBatchCommand(image, updates));
        document.setModified(true);
        redraw();
        return updates.size();
    }

    public int drawShapeFromMcp(
            PixelCanvasSession.ShapeKind shape,
            List<PixelCanvasSession.Point> points,
            int color,
            boolean filled
    ) {
        return drawShapeFromMcp(shape, points, color, filled, null);
    }

    public int drawShapeFromMcp(
            PixelCanvasSession.ShapeKind shape,
            List<PixelCanvasSession.Point> points,
            int color,
            boolean filled,
            PixelCanvasSession.SymmetryOptions symmetry
    ) {
        List<PixelCanvasSession.PixelChange> changes =
                PixelCanvasSession.shapeChanges(
                        document.getImage(), shape, points, color, filled
                );
        changes = PixelCanvasSession.symmetryChanges(
                document.getImage(), changes, symmetry
        );
        List<SetPixelBatchCommand.PixelUpdate> updates = changes.stream()
                .map(change -> new SetPixelBatchCommand.PixelUpdate(
                        change.x(), change.y(), change.argb()
                ))
                .toList();
        commands.execute(new SetPixelBatchCommand(
                document.getImage(), updates
        ));
        document.setModified(true);
        redraw();
        return changes.size();
    }

    public String inspectCanvasFromMcp(
            Integer x,
            Integer y,
            Integer width,
            Integer height
    ) {
        return PixelCanvasSession.inspectCanvas(
                document.getImage(), x, y, width, height
        );
    }

    public byte[] previewPngFromMcp() throws IOException {
        return ImageExporter.toPngBytes(document.getImage());
    }

    public byte[] previewRegionPngFromMcp(
            int x,
            int y,
            int width,
            int height,
            int scale
    ) throws IOException {
        return PixelCanvasSession.previewRegionPng(
                document.getImage(), x, y, width, height, scale
        );
    }

    public int fillFromMcp(int x, int y, Integer color) {
        int filled = fillBucket.fill(
                document.getImage(), commands, x, y,
                color == null ? currentColor : color
        );
        if (filled > 0) {
            document.setModified(true);
            redraw();
        }
        return filled;
    }

    public Path savePngFromMcp(String fileName, Path outputDirectory)
            throws IOException {
        Path saved = PixelCanvasSession.savePng(
                document.getImage(), fileName, outputDirectory
        );
        document.setModified(false);
        return saved;
    }

    private void buildUi() {
        root.setTop(buildMenuBar());
        root.setLeft(buildToolbar());
        root.setCenter(buildCanvasArea());
        root.setRight(buildColorPanel());
        root.setBottom(buildStatusBar());
        root.setPadding(new Insets(4));
    }

    private MenuBar buildMenuBar() {
        Menu file = new Menu("File");

        MenuItem newItem = new MenuItem("New");
        newItem.setOnAction(e -> newDocument());

        MenuItem openItem = new MenuItem("Open...");
        openItem.setOnAction(e -> openImage());

        MenuItem saveItem = new MenuItem("Save as...");
        saveItem.setOnAction(e -> saveAs());

        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction(e -> Platform.exit());

        file.getItems().addAll(
                newItem, openItem, saveItem,
                new SeparatorMenuItem(), exitItem
        );

        Menu edit = new Menu("Edit");

        MenuItem undo = new MenuItem("Undo");
        undo.setOnAction(e -> {
            commands.undo();
            redraw();
        });

        MenuItem redo = new MenuItem("Redo");
        redo.setOnAction(e -> {
            commands.redo();
            redraw();
        });

        edit.getItems().addAll(undo, redo);

        Menu view = new Menu("View");
        CheckMenuItem grid = new CheckMenuItem("Grid");
        grid.setSelected(showGrid);
        grid.setOnAction(e -> {
            showGrid = grid.isSelected();
            redraw();
        });
        MenuItem gridSettings = new MenuItem("Grid Settings...");
        gridSettings.setOnAction(e -> showGridSettings());
        view.getItems().addAll(grid, gridSettings);

        return new MenuBar(file, edit, view);
    }

    private void showGridSettings() {
        Spinner<Integer> width = new Spinner<>(1, 1024, gridCellWidth);
        Spinner<Integer> height = new Spinner<>(1, 1024, gridCellHeight);
        width.setPrefWidth(100);
        height.setPrefWidth(100);

        GridPane content = new GridPane();
        content.setHgap(8);
        content.setVgap(8);
        content.setPadding(new Insets(10));
        content.addRow(0, new Label("Cell width"), width);
        content.addRow(1, new Label("Cell height"), height);

        ButtonType apply = new ButtonType("Apply", ButtonBar.ButtonData.OK_DONE);
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Grid Settings");
        dialog.setHeaderText("Set the grid cell size");
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(
                apply, ButtonType.CANCEL
        );

        dialog.showAndWait().filter(apply::equals).ifPresent(result -> {
            gridCellWidth = width.getValue();
            gridCellHeight = height.getValue();
            redraw();
        });
    }

    private Node buildToolbar() {
        VBox box = new VBox(8);
        box.setPadding(new Insets(8));
        box.setPrefWidth(100);

        ToggleButton pencilButton = toolButton("Pencil");
        ToggleButton eraserButton = toolButton("Eraser");
        ToggleButton eyedropperButton = toolButton("Eyedropper");
        ToggleButton fillBucketButton = toolButton(fillBucket.getName());
        fillBucketButton.setTooltip(
                new Tooltip("Fill the connected area with the selected color")
        );

        pencilButton.setSelected(true);

        box.getChildren().addAll(
                new Label("Tools"),
                pencilButton,
                eraserButton,
                eyedropperButton,
                fillBucketButton
        );

        return box;
    }

    private ToggleButton toolButton(String name) {
        ToggleButton button = new ToggleButton(name);
        button.setToggleGroup(toolGroup);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(e -> activeTool = name);
        return button;
    }

    private Node buildCanvasArea() {
        StackPane pane = new StackPane(canvas);
        pane.setAlignment(Pos.TOP_LEFT);
        pane.setStyle("-fx-background-color: #262626;");
        return pane;
    }

    private Node buildColorPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10));
        panel.setPrefWidth(180);

        Label title = new Label("Color");
        colorPicker.setMaxWidth(Double.MAX_VALUE);

        colorPicker.setOnAction(e -> {
            Color c = colorPicker.getValue();
            currentColor = PixelColor.argb(
                    (int) Math.round(c.getOpacity() * 255),
                    (int) Math.round(c.getRed() * 255),
                    (int) Math.round(c.getGreen() * 255),
                    (int) Math.round(c.getBlue() * 255)
            );
        });

        symmetryCenterX.setEditable(true);
        symmetryCenterY.setEditable(true);
        radialCopies.setEditable(true);
        mirrorVertical.setOnAction(e -> redraw());
        mirrorHorizontal.setOnAction(e -> redraw());
        symmetryCenterX.valueProperty().addListener(
                (observable, oldValue, newValue) -> redraw()
        );
        symmetryCenterY.valueProperty().addListener(
                (observable, oldValue, newValue) -> redraw()
        );
        radialCopies.valueProperty().addListener(
                (observable, oldValue, newValue) -> redraw()
        );

        Label symmetryTitle = new Label("Symmetry");
        Label centerLabel = new Label("Axis position (gridline)");
        Label radialLabel = new Label("Radial copies");
        symmetryCenterX.setTooltip(new Tooltip("Vertical axis position"));
        symmetryCenterY.setTooltip(new Tooltip("Horizontal axis position"));
        radialCopies.setTooltip(
                new Tooltip("Number of rotated copies; 1 disables radial symmetry")
        );

        panel.getChildren().addAll(
                title, colorPicker,
                new Separator(),
                symmetryTitle,
                mirrorVertical,
                mirrorHorizontal,
                centerLabel,
                new HBox(4, new Label("X"), symmetryCenterX),
                new HBox(4, new Label("Y"), symmetryCenterY),
                radialLabel,
                radialCopies
        );
        return panel;
    }

    private Node buildStatusBar() {
        Label status = new Label("Mirage 0.1.0");
        status.setPadding(new Insets(4, 8, 4, 8));
        return status;
    }

    private void installKeyboardShortcuts() {
        root.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.P) {
                selectTool("Pencil");
            } else if (event.getCode() == KeyCode.E) {
                selectTool("Eraser");
            } else if (event.getCode() == KeyCode.I) {
                selectTool("Eyedropper");
            } else if (event.getCode() == KeyCode.F) {
                selectTool(fillBucket.getName());
            }

            boolean modifier = event.isControlDown() || event.isMetaDown();

            if (modifier && event.getCode() == KeyCode.Z) {
                commands.undo();
                redraw();
            } else if (modifier && event.getCode() == KeyCode.Y) {
                commands.redo();
                redraw();
            } else if (modifier && event.getCode() == KeyCode.S) {
                saveAs();
            }
        });

        root.setFocusTraversable(true);
        root.requestFocus();
    }

    private void selectTool(String name) {
        activeTool = name;
        for (Toggle toggle : toolGroup.getToggles()) {
            if (toggle instanceof ToggleButton button
                    && name.equals(button.getText())) {
                button.setSelected(true);
                return;
            }
        }
    }

    private void installCanvasHandlers() {
        canvas.setOnMousePressed(event -> {
            root.requestFocus();

            if (event.getButton() == MouseButton.MIDDLE) {
                panStartX = event.getX();
                panStartY = event.getY();
                originalOffsetX = offsetX;
                originalOffsetY = offsetY;
                return;
            }

            if (event.getButton() == MouseButton.PRIMARY) {
                if ("Pencil".equals(activeTool)) {
                    activeStroke = new LinkedHashMap<>();
                    strokeColor = currentColor;
                } else if ("Eraser".equals(activeTool)) {
                    activeStroke = new LinkedHashMap<>();
                    strokeColor = PixelColor.TRANSPARENT;
                }
                handleCanvasAction(event.getX(), event.getY());
            }
        });

        canvas.setOnMouseReleased(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                finishStroke();
            }
        });

        canvas.setOnMouseDragged(event -> {
            if (event.isMiddleButtonDown()) {
                offsetX = originalOffsetX + event.getX() - panStartX;
                offsetY = originalOffsetY + event.getY() - panStartY;
                redraw();
            } else if (event.isPrimaryButtonDown()) {
                if (!fillBucket.getName().equals(activeTool)) {
                    handleCanvasAction(event.getX(), event.getY());
                }
            }
        });

        canvas.setOnScroll(event -> {
            double oldZoom = zoom;

            zoom = event.getDeltaY() > 0
                    ? Math.min(64, zoom * 2)
                    : Math.max(1, zoom / 2);

            if (zoom != oldZoom) {
                double imageX = (event.getX() - offsetX) / oldZoom;
                double imageY = (event.getY() - offsetY) / oldZoom;

                offsetX = event.getX() - imageX * zoom;
                offsetY = event.getY() - imageY * zoom;

                redraw();
            }
        });
    }

    private void handleCanvasAction(double mouseX, double mouseY) {
        int x = (int) Math.floor((mouseX - offsetX) / zoom);
        int y = (int) Math.floor((mouseY - offsetY) / zoom);

        if (x < 0 || y < 0 ||
                x >= document.getImage().getWidth() ||
                y >= document.getImage().getHeight()) {
            return;
        }

        if (activeStroke != null) {
            boolean changed = drawSymmetricPixel(x, y, strokeColor);
            if (changed) {
                redraw();
            }
            return;
        }

        boolean changed;
        switch (activeTool) {
            case "Pencil" -> changed = drawSymmetricPixel(
                    x, y, currentColor
            );
            case "Eraser" -> changed = drawSymmetricPixel(
                    x, y, PixelColor.TRANSPARENT
            );
            case "Eyedropper" -> {
                setCurrentColor(
                        eyedropper.sample(document.getImage(), x, y)
                );
                changed = false;
            }
            case "Fill Bucket" -> {
                changed = fillBucket.fill(
                        document.getImage(), commands, x, y, currentColor
                ) > 0;
            }
            default -> {
                return;
            }
        }

        if (changed) {
            if (activeStroke == null) {
                document.setModified(true);
            }
            redraw();
        }
    }

    private boolean drawSymmetricPixel(int x, int y, int color) {
        PixelImage image = document.getImage();
        int centerX = Math.min(symmetryCenterX.getValue(), image.getWidth());
        int centerY = Math.min(symmetryCenterY.getValue(), image.getHeight());
        boolean changed = false;
        List<SymmetryTransform.Point> points = List.copyOf(
                SymmetryTransform.pointsFor(
                        x, y,
                        image.getWidth(), image.getHeight(),
                        mirrorVertical.isSelected(),
                        mirrorHorizontal.isSelected(),
                        centerX, centerY, radialCopies.getValue()
                )
        );
        if (activeStroke != null) {
            for (SymmetryTransform.Point point : points) {
                int previousColor = image.getPixel(point.x(), point.y());
                if (previousColor == color) {
                    continue;
                }
                activeStroke.computeIfAbsent(
                        point, ignored -> new StrokePixel(previousColor, color)
                );
                StrokePixel strokePixel = activeStroke.get(point);
                activeStroke.put(
                        point,
                        new StrokePixel(strokePixel.oldColor(), color)
                );
                image.setPixel(point.x(), point.y(), color);
                changed = true;
            }
            return changed;
        }

        List<SetPixelBatchCommand.PixelUpdate> updates = points.stream()
                .filter(point -> image.getPixel(point.x(), point.y()) != color)
                .map(point -> new SetPixelBatchCommand.PixelUpdate(
                        point.x(), point.y(), color
                ))
                .toList();
        if (updates.isEmpty()) return false;
        commands.execute(new SetPixelBatchCommand(image, updates));
        return true;
    }

    private void finishStroke() {
        if (activeStroke == null) {
            return;
        }
        List<SetPixelBatchCommand.PixelUpdate> updates = new ArrayList<>();
        List<Integer> previousColors = new ArrayList<>();
        for (Map.Entry<SymmetryTransform.Point, StrokePixel> entry
                : activeStroke.entrySet()) {
            StrokePixel pixel = entry.getValue();
            if (pixel.oldColor() == pixel.newColor()) {
                continue;
            }
            SymmetryTransform.Point point = entry.getKey();
            updates.add(new SetPixelBatchCommand.PixelUpdate(
                    point.x(), point.y(), pixel.newColor()
            ));
            previousColors.add(pixel.oldColor());
        }
        activeStroke = null;
        if (updates.isEmpty()) {
            return;
        }
        int[] oldColors = previousColors.stream()
                .mapToInt(Integer::intValue)
                .toArray();
        commands.execute(new SetPixelBatchCommand(
                document.getImage(), updates, oldColors
        ));
        document.setModified(true);
        redraw();
    }

    private record StrokePixel(int oldColor, int newColor) {
    }

    private void setCurrentColor(int color) {
        currentColor = color;
        colorPicker.setValue(Color.rgb(
                (color >>> 16) & 0xFF,
                (color >>> 8) & 0xFF,
                color & 0xFF,
                ((color >>> 24) & 0xFF) / 255.0
        ));
    }

    private void redraw() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        renderer.render(
                gc,
                document.getImage(),
                zoom,
                offsetX,
                offsetY,
                showGrid,
                gridCellWidth,
                gridCellHeight
        );
        drawSymmetryGuides(gc);
    }

    private void drawSymmetryGuides(GraphicsContext gc) {
        boolean showVertical = mirrorVertical.isSelected();
        boolean showHorizontal = mirrorHorizontal.isSelected();
        int copies = radialCopies.getValue();
        if (!showVertical && !showHorizontal && copies <= 1) {
            return;
        }

        PixelImage image = document.getImage();
        int centerX = Math.min(symmetryCenterX.getValue(), image.getWidth());
        int centerY = Math.min(symmetryCenterY.getValue(), image.getHeight());
        double imageCenterX = offsetX + centerX * zoom;
        double imageCenterY = offsetY + centerY * zoom;

        gc.save();
        gc.beginPath();
        gc.rect(
                offsetX, offsetY,
                image.getWidth() * zoom, image.getHeight() * zoom
        );
        gc.closePath();
        gc.clip();
        gc.setStroke(Color.rgb(100, 230, 255, 0.85));
        gc.setLineWidth(Math.max(1, zoom < 4 ? 1 : 1.5));
        gc.setLineDashes(5, 4);

        if (showVertical) {
            double guideX = imageCenterX;
            gc.strokeLine(
                    guideX, offsetY,
                    guideX, offsetY + image.getHeight() * zoom
            );
        }
        if (showHorizontal) {
            double guideY = imageCenterY;
            gc.strokeLine(
                    offsetX, guideY,
                    offsetX + image.getWidth() * zoom, guideY
            );
        }
        for (int copy = 0; copy < copies; copy++) {
            double angle = 2.0 * Math.PI * copy / copies;
            double dx = Math.cos(angle);
            double dy = Math.sin(angle);
            double distance = Math.min(
                    rayDistanceToEdge(
                            centerX, centerY,
                            dx, dy, image.getWidth(), image.getHeight()
                    ),
                    Math.hypot(image.getWidth(), image.getHeight())
            );
            if (copies > 1) {
                gc.strokeLine(
                        imageCenterX, imageCenterY,
                        imageCenterX + dx * distance * zoom,
                        imageCenterY + dy * distance * zoom
                );
            }
        }
        gc.setLineDashes();
        gc.restore();
    }

    private double rayDistanceToEdge(
            double x, double y, double dx, double dy, int width, int height
    ) {
        double distanceX = dx > 0 ? (width - x) / dx
                : dx < 0 ? -x / dx : Double.POSITIVE_INFINITY;
        double distanceY = dy > 0 ? (height - y) / dy
                : dy < 0 ? -y / dy : Double.POSITIVE_INFINITY;
        return Math.max(0, Math.min(distanceX, distanceY));
    }

    private void resetSymmetryCenter() {
        symmetryCenterX.getValueFactory().setValue(
                document.getImage().getWidth() / 2
        );
        symmetryCenterY.getValueFactory().setValue(
                document.getImage().getHeight() / 2
        );
    }

    private void newDocument() {
        ChoiceDialog<Integer> dialog =
                new ChoiceDialog<>(32, 8, 16, 32, 64, 128, 256, 512);

        dialog.setTitle("New Mirage Document");
        dialog.setHeaderText("Choose canvas size");
        dialog.setContentText("Size:");

        dialog.showAndWait().ifPresent(size -> {
            document = new Document(size, size);
            commands.clear();
            resetSymmetryCenter();
            zoom = size <= 32 ? 16 : 4;
            offsetX = 80;
            offsetY = 50;
            redraw();
        });
    }

    private void openImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open Image");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        "Image Files (*.png, *.jpg, *.jpeg, *.gif)",
                        "*.png", "*.jpg", "*.jpeg", "*.gif"
                )
        );

        File file = chooser.showOpenDialog(root.getScene().getWindow());
        if (file == null) return;

        try {
            BufferedImage input = ImageIO.read(file);
            if (input == null) throw new IOException("Could not read image");

            Document loaded = new Document(
                    input.getWidth(), input.getHeight()
            );

            for (int y = 0; y < input.getHeight(); y++) {
                for (int x = 0; x < input.getWidth(); x++) {
                    loaded.getImage().setPixel(x, y, input.getRGB(x, y));
                }
            }

            document = loaded;
            commands.clear();
            resetSymmetryCenter();
            zoom = Math.max(
                    1,
                    Math.min(
                            16,
                            512.0 / Math.max(
                                    input.getWidth(), input.getHeight()
                            )
                    )
            );
            offsetX = 80;
            offsetY = 50;
            redraw();

        } catch (IOException ex) {
            showError("Open failed", ex.getMessage());
        }
    }

    private void saveAs() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save As");
        FileChooser.ExtensionFilter pngFilter =
                new FileChooser.ExtensionFilter("PNG Image (*.png)", "*.png");
        FileChooser.ExtensionFilter jpegFilter =
                new FileChooser.ExtensionFilter(
                        "JPEG Image (*.jpg, *.jpeg)", "*.jpg", "*.jpeg"
                );
        FileChooser.ExtensionFilter gifFilter =
                new FileChooser.ExtensionFilter("GIF Image (*.gif)", "*.gif");
        chooser.getExtensionFilters().addAll(pngFilter, jpegFilter, gifFilter);

        File file = chooser.showSaveDialog(root.getScene().getWindow());
        if (file == null) return;

        String format = formatFor(file, chooser.getSelectedExtensionFilter());
        file = withExtension(file, format);

        try {
            ImageExporter.write(document.getImage(), file, format);
            document.setModified(false);

        } catch (IOException ex) {
            showError("Save failed", ex.getMessage());
        }
    }

    private String formatFor(
            File file,
            FileChooser.ExtensionFilter selectedFilter
    ) {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".png")) return "png";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "jpg";
        if (name.endsWith(".gif")) return "gif";

        if (selectedFilter != null &&
                selectedFilter.getExtensions().contains("*.jpg")) {
            return "jpg";
        }
        if (selectedFilter != null &&
                selectedFilter.getExtensions().contains("*.gif")) {
            return "gif";
        }
        return "png";
    }

    private File withExtension(File file, String format) {
        String name = file.getName();
        String lowerName = name.toLowerCase();
        String expectedExtension = "." + format;
        if (format.equals("jpg") &&
                (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg"))) {
            return file;
        }
        if (lowerName.endsWith(expectedExtension)) return file;

        int extensionStart = name.lastIndexOf('.');
        if (extensionStart > 0) {
            name = name.substring(0, extensionStart);
        }
        return new File(file.getParentFile(), name + expectedExtension);
    }

    private void showError(String title, String message) {
        new Alert(
                Alert.AlertType.ERROR,
                message,
                ButtonType.OK
        ).showAndWait();
    }
}
