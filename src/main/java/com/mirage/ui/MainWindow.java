package com.mirage.ui;

import com.mirage.commands.CommandManager;
import com.mirage.commands.DocumentLayerStateCommand;
import com.mirage.commands.DocumentSelectionCommand;
import com.mirage.commands.SetPixelBatchCommand;
import com.mirage.core.document.Document;
import com.mirage.core.document.Layer;
import com.mirage.core.document.PixelSelection;
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
    private final ListView<Layer> layerList = new ListView<>();
    private final CheckBox layerVisible = new CheckBox("Visible");
    private final CheckBox layerLocked = new CheckBox("Locked");
    private final Slider layerOpacity = new Slider(0, 100, 100);
    private boolean syncingLayerControls;

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
    private PixelSelection previewSelection;
    private boolean creatingSelection;
    private boolean movingSelection;
    private int selectionStartX;
    private int selectionStartY;
    private int moveStartX;
    private int moveStartY;
    private int moveDeltaX;
    private int moveDeltaY;
    private List<PixelSelection.Point> lassoPoints = new ArrayList<>();
    private Clipboard clipboard;

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
        refreshLayersPanel();
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
        ensureActiveLayerEditable();
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
        ensureActiveLayerEditable();
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
                document.getCompositeImage(), x, y, width, height
        );
    }

    public byte[] previewPngFromMcp() throws IOException {
        return ImageExporter.toPngBytes(document.getCompositeImage());
    }

    public byte[] previewRegionPngFromMcp(
            int x,
            int y,
            int width,
            int height,
            int scale
    ) throws IOException {
        return PixelCanvasSession.previewRegionPng(
                document.getCompositeImage(), x, y, width, height, scale
        );
    }

    public int fillFromMcp(int x, int y, Integer color) {
        ensureActiveLayerEditable();
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
                document.getCompositeImage(), fileName, outputDirectory
        );
        document.setModified(false);
        return saved;
    }

    private void buildUi() {
        root.setTop(buildMenuBar());
        root.setLeft(buildToolbar());
        root.setCenter(buildCanvasArea());
        root.setRight(buildRightPanel());
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
            refreshLayersPanel();
            redraw();
        });

        MenuItem redo = new MenuItem("Redo");
        redo.setOnAction(e -> {
            commands.redo();
            refreshLayersPanel();
            redraw();
        });

        MenuItem selectAll = new MenuItem("Select All");
        selectAll.setOnAction(e -> selectAll());
        MenuItem deselect = new MenuItem("Deselect");
        deselect.setOnAction(e -> deselect());
        MenuItem copy = new MenuItem("Copy");
        copy.setOnAction(e -> copySelection());
        MenuItem cut = new MenuItem("Cut");
        cut.setOnAction(e -> cutSelection());
        MenuItem paste = new MenuItem("Paste");
        paste.setOnAction(e -> pasteSelection());

        edit.getItems().addAll(
                undo, redo, new SeparatorMenuItem(),
                selectAll, deselect, new SeparatorMenuItem(),
                copy, cut, paste
        );

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
        ToggleButton rectangleSelectButton = toolButton("Rectangle Select");
        ToggleButton freeformSelectButton = toolButton("Freeform Select");

        pencilButton.setSelected(true);

        box.getChildren().addAll(
                new Label("Tools"),
                pencilButton,
                eraserButton,
                eyedropperButton,
                fillBucketButton,
                rectangleSelectButton,
                freeformSelectButton
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
        pane.widthProperty().addListener(
                (observable, oldWidth, newWidth) ->
                        resizeCanvas(newWidth.doubleValue(), pane.getHeight())
        );
        pane.heightProperty().addListener(
                (observable, oldHeight, newHeight) ->
                        resizeCanvas(pane.getWidth(), newHeight.doubleValue())
        );
        return pane;
    }

    private void resizeCanvas(double width, double height) {
        if (canvas.getWidth() != width) {
            canvas.setWidth(width);
        }
        if (canvas.getHeight() != height) {
            canvas.setHeight(height);
        }
        redraw();
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

    private Node buildRightPanel() {
        Tab colorTab = new Tab("Color", buildColorPanel());
        Tab layersTab = new Tab("Layers", buildLayersPanel());
        colorTab.setClosable(false);
        layersTab.setClosable(false);
        TabPane tabs = new TabPane(colorTab, layersTab);
        tabs.setPrefWidth(230);
        return tabs;
    }

    private Node buildLayersPanel() {
        VBox panel = new VBox(8);
        panel.setPadding(new Insets(10));

        layerList.setCellFactory(ignored -> new ListCell<>() {
            @Override
            protected void updateItem(Layer layer, boolean empty) {
                super.updateItem(layer, empty);
                setText(empty || layer == null ? null : layer.getName());
            }
        });
        layerList.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldLayer, layer) -> {
                    if (layer != null) {
                        document.setActiveLayer(layer);
                        refreshLayerControls();
                        redraw();
                    }
                }
        );

        Button add = new Button("Add");
        Button duplicate = new Button("Duplicate");
        Button delete = new Button("Delete");
        Button rename = new Button("Rename");
        Button moveUp = new Button("Up");
        Button moveDown = new Button("Down");
        add.setOnAction(event -> {
            recordLayerChange(() ->
                    document.addLayer("Layer " + (document.getLayers().size() + 1))
            );
            refreshLayersPanel();
            redraw();
        });
        duplicate.setOnAction(event -> {
            recordLayerChange(() ->
                    document.duplicateLayer(document.getActiveLayer())
            );
            refreshLayersPanel();
            redraw();
        });
        delete.setOnAction(event -> {
            if (document.getLayers().size() == 1) {
                return;
            }
            recordLayerChange(() ->
                    document.deleteLayer(document.getActiveLayer())
            );
            refreshLayersPanel();
            redraw();
        });
        rename.setOnAction(event -> renameActiveLayer());
        moveUp.setOnAction(event -> moveActiveLayer(1));
        moveDown.setOnAction(event -> moveActiveLayer(-1));

        layerVisible.setOnAction(event -> {
            if (!syncingLayerControls) {
                boolean visible = layerVisible.isSelected();
                recordLayerChange(() ->
                        document.getActiveLayer().setVisible(visible)
                );
                redraw();
            }
        });
        layerLocked.setOnAction(event -> {
            if (!syncingLayerControls) {
                boolean locked = layerLocked.isSelected();
                recordLayerChange(() ->
                        document.getActiveLayer().setLocked(locked)
                );
            }
        });
        layerOpacity.valueProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (!syncingLayerControls) {
                        double opacity = newValue.doubleValue() / 100.0;
                        recordLayerChange(() ->
                                document.getActiveLayer().setOpacity(opacity)
                        );
                        redraw();
                    }
                }
        );

        layerOpacity.setShowTickMarks(true);
        layerOpacity.setMajorTickUnit(25);
        layerOpacity.setBlockIncrement(5);
        HBox actions = new HBox(4, add, duplicate, delete);
        HBox ordering = new HBox(4, rename, moveUp, moveDown);
        panel.getChildren().addAll(
                new Label("Layers"), layerList, actions, ordering,
                layerVisible, layerLocked,
                new Label("Opacity"), layerOpacity
        );
        VBox.setVgrow(layerList, Priority.ALWAYS);
        refreshLayersPanel();
        return panel;
    }

    private void renameActiveLayer() {
        TextInputDialog dialog = new TextInputDialog(
                document.getActiveLayer().getName()
        );
        dialog.setTitle("Rename Layer");
        dialog.setHeaderText("Enter a layer name");
        dialog.setContentText("Name:");
        dialog.showAndWait().ifPresent(name -> {
            if (name.isBlank()) {
                showError("Rename failed", "Layer name cannot be blank");
                return;
            }
            recordLayerChange(() ->
                    document.getActiveLayer().setName(name)
            );
            refreshLayersPanel();
        });
    }

    private void moveActiveLayer(int direction) {
        List<Layer> layers = document.getLayers();
        int index = layers.indexOf(document.getActiveLayer());
        int newIndex = index + direction;
        if (newIndex < 0 || newIndex >= layers.size()) {
            return;
        }
        Layer activeLayer = document.getActiveLayer();
        recordLayerChange(() -> document.moveLayer(activeLayer, newIndex));
        refreshLayersPanel();
        redraw();
    }

    private void recordLayerChange(Runnable change) {
        Document.StateSnapshot before = document.snapshotState();
        change.run();
        Document.StateSnapshot after = document.snapshotState();
        commands.execute(new DocumentLayerStateCommand(
                document, before, after
        ));
        document.setModified(true);
    }

    private void refreshLayersPanel() {
        if (layerList.getItems() == null) {
            return;
        }
        syncingLayerControls = true;
        layerList.getItems().setAll(document.getLayers());
        layerList.getSelectionModel().select(document.getActiveLayer());
        refreshLayerControls();
        syncingLayerControls = false;
    }

    private void refreshLayerControls() {
        Layer layer = document.getActiveLayer();
        syncingLayerControls = true;
        layerVisible.setSelected(layer.isVisible());
        layerLocked.setSelected(layer.isLocked());
        layerOpacity.setValue(layer.getOpacity() * 100.0);
        syncingLayerControls = false;
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
            } else if (event.getCode() == KeyCode.M) {
                selectTool("Rectangle Select");
            } else if (event.getCode() == KeyCode.L) {
                selectTool("Freeform Select");
            }

            boolean modifier = event.isControlDown() || event.isMetaDown();

            if (modifier && event.getCode() == KeyCode.A) {
                selectAll();
            } else if (modifier && event.getCode() == KeyCode.C) {
                copySelection();
            } else if (modifier && event.getCode() == KeyCode.X) {
                cutSelection();
            } else if (modifier && event.getCode() == KeyCode.V) {
                pasteSelection();
            } else if (event.getCode() == KeyCode.ESCAPE) {
                deselect();
            } else if (event.getCode() == KeyCode.DELETE
                    || event.getCode() == KeyCode.BACK_SPACE) {
                clearSelection();
            } else if (modifier && event.getCode() == KeyCode.Z) {
                commands.undo();
                refreshLayersPanel();
                redraw();
            } else if (modifier && event.getCode() == KeyCode.Y) {
                commands.redo();
                refreshLayersPanel();
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
                PixelSelection.Point point = canvasPoint(event.getX(), event.getY());
                if (isSelectionTool() && point != null
                        && document.getSelection() != null
                        && document.getSelection().contains(
                            point.x(), point.y()
                        )) {
                    movingSelection = true;
                    moveStartX = point.x();
                    moveStartY = point.y();
                    moveDeltaX = 0;
                    moveDeltaY = 0;
                    previewSelection = document.getSelection();
                } else if (isSelectionTool() && point != null) {
                    creatingSelection = true;
                    selectionStartX = point.x();
                    selectionStartY = point.y();
                    lassoPoints.clear();
                    if ("Freeform Select".equals(activeTool)) {
                        lassoPoints.add(point);
                    } else {
                        previewSelection = PixelSelection.rectangle(
                                document.getWidth(), document.getHeight(),
                                point.x(), point.y(), point.x(), point.y()
                        );
                    }
                    redraw();
                } else if ("Pencil".equals(activeTool)) {
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
                if (creatingSelection) {
                    finishSelection();
                } else if (movingSelection) {
                    finishSelectionMove();
                } else {
                    finishStroke();
                }
            }
        });

        canvas.setOnMouseDragged(event -> {
            if (event.isMiddleButtonDown()) {
                offsetX = originalOffsetX + event.getX() - panStartX;
                offsetY = originalOffsetY + event.getY() - panStartY;
                redraw();
            } else if (event.isPrimaryButtonDown()) {
                PixelSelection.Point point = canvasPoint(
                        event.getX(), event.getY()
                );
                if (creatingSelection && point != null) {
                    if ("Rectangle Select".equals(activeTool)) {
                        previewSelection = PixelSelection.rectangle(
                                document.getWidth(), document.getHeight(),
                                selectionStartX, selectionStartY,
                                point.x(), point.y()
                        );
                    } else if (lassoPoints.isEmpty()
                            || !lassoPoints.getLast().equals(point)) {
                        lassoPoints.add(point);
                    }
                    redraw();
                } else if (movingSelection && point != null) {
                    moveDeltaX = point.x() - moveStartX;
                    moveDeltaY = point.y() - moveStartY;
                    previewSelection = document.getSelection().translated(
                            moveDeltaX, moveDeltaY
                    );
                    redraw();
                } else if (!fillBucket.getName().equals(activeTool)) {
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

    private boolean isSelectionTool() {
        return "Rectangle Select".equals(activeTool)
                || "Freeform Select".equals(activeTool);
    }

    private PixelSelection.Point canvasPoint(double mouseX, double mouseY) {
        int x = (int) Math.floor((mouseX - offsetX) / zoom);
        int y = (int) Math.floor((mouseY - offsetY) / zoom);
        if (x < 0 || y < 0 || x >= document.getWidth()
                || y >= document.getHeight()) {
            return null;
        }
        return new PixelSelection.Point(x, y);
    }

    private void finishSelection() {
        PixelSelection selection = "Rectangle Select".equals(activeTool)
                ? previewSelection
                : PixelSelection.polygon(
                    document.getWidth(), document.getHeight(), lassoPoints
                );
        creatingSelection = false;
        previewSelection = null;
        lassoPoints.clear();
        if (selection != null && selection.bounds().isEmpty()) {
            selection = null;
        }
        if (selection != null) {
            commands.execute(new DocumentSelectionCommand(
                    document, document.getImage(), selection, List.of()
            ));
        }
        redraw();
    }

    private void finishSelectionMove() {
        PixelSelection before = document.getSelection();
        PixelSelection after = previewSelection;
        int dx = moveDeltaX;
        int dy = moveDeltaY;
        movingSelection = false;
        previewSelection = null;
        if ((dx == 0 && dy == 0) || document.getActiveLayer().isLocked()) {
            redraw();
            return;
        }

        PixelImage image = document.getImage();
        Map<SymmetryTransform.Point, Integer> movedPixels =
                new LinkedHashMap<>();
        for (int y = 0; y < document.getHeight(); y++) {
            for (int x = 0; x < document.getWidth(); x++) {
                if (before.contains(x, y)) {
                    movedPixels.put(
                            new SymmetryTransform.Point(x, y),
                            PixelColor.TRANSPARENT
                    );
                }
            }
        }
        for (int y = 0; y < document.getHeight(); y++) {
            for (int x = 0; x < document.getWidth(); x++) {
                if (after.contains(x, y)) {
                    movedPixels.put(
                            new SymmetryTransform.Point(x, y),
                            image.getPixel(x - dx, y - dy)
                    );
                }
            }
        }
        List<SetPixelBatchCommand.PixelUpdate> updates =
                movedPixels.entrySet().stream()
                        .map(entry -> new SetPixelBatchCommand.PixelUpdate(
                                entry.getKey().x(), entry.getKey().y(),
                                entry.getValue()
                        ))
                        .toList();
        commands.execute(new DocumentSelectionCommand(
                document, image, after, updates
        ));
        document.setModified(true);
        redraw();
    }

    private void selectAll() {
        PixelSelection selection = PixelSelection.all(
                document.getWidth(), document.getHeight()
        );
        commands.execute(new DocumentSelectionCommand(
                document, document.getImage(), selection, List.of()
        ));
        redraw();
    }

    private void deselect() {
        if (document.getSelection() == null) {
            return;
        }
        commands.execute(new DocumentSelectionCommand(
                document, document.getImage(), null, List.of()
        ));
        redraw();
    }

    private void copySelection() {
        PixelSelection selection = document.getSelection();
        if (selection == null) {
            return;
        }
        selection.bounds().ifPresent(bounds -> {
            PixelImage copied = new PixelImage(bounds.width(), bounds.height());
            boolean[] mask = new boolean[bounds.width() * bounds.height()];
            PixelImage source = document.getImage();
            for (int y = 0; y < bounds.height(); y++) {
                for (int x = 0; x < bounds.width(); x++) {
                    int sourceX = bounds.x() + x;
                    int sourceY = bounds.y() + y;
                    if (selection.contains(sourceX, sourceY)) {
                        mask[y * bounds.width() + x] = true;
                        copied.setPixel(
                                x, y, source.getPixel(sourceX, sourceY)
                        );
                    }
                }
            }
            clipboard = new Clipboard(
                    copied,
                    PixelSelection.fromMask(
                            bounds.width(), bounds.height(), mask
                    ),
                    bounds.x(), bounds.y()
            );
        });
    }

    private void cutSelection() {
        PixelSelection selection = document.getSelection();
        if (selection == null || document.getActiveLayer().isLocked()) {
            return;
        }
        copySelection();
        List<SetPixelBatchCommand.PixelUpdate> updates =
                selectedPixelUpdates(selection, PixelColor.TRANSPARENT);
        if (!updates.isEmpty()) {
            commands.execute(new DocumentSelectionCommand(
                    document, document.getImage(), selection, updates
            ));
            document.setModified(true);
            redraw();
        }
    }

    private void clearSelection() {
        PixelSelection selection = document.getSelection();
        if (selection == null || document.getActiveLayer().isLocked()) {
            return;
        }
        List<SetPixelBatchCommand.PixelUpdate> updates =
                selectedPixelUpdates(selection, PixelColor.TRANSPARENT);
        if (!updates.isEmpty()) {
            commands.execute(new DocumentSelectionCommand(
                    document, document.getImage(), selection, updates
            ));
            document.setModified(true);
            redraw();
        }
    }

    private List<SetPixelBatchCommand.PixelUpdate> selectedPixelUpdates(
            PixelSelection selection, int color
    ) {
        PixelImage image = document.getImage();
        List<SetPixelBatchCommand.PixelUpdate> updates = new ArrayList<>();
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (selection.contains(x, y)
                        && image.getPixel(x, y) != color) {
                    updates.add(new SetPixelBatchCommand.PixelUpdate(
                            x, y, color
                    ));
                }
            }
        }
        return updates;
    }

    private void pasteSelection() {
        if (clipboard == null || document.getActiveLayer().isLocked()) {
            return;
        }
        PixelImage destination = document.getImage();
        PixelImage source = clipboard.image();
        int x = Math.max(0, Math.min(
                clipboard.x() + 1, destination.getWidth() - source.getWidth()
        ));
        int y = Math.max(0, Math.min(
                clipboard.y() + 1, destination.getHeight() - source.getHeight()
        ));
        boolean[] mask = new boolean[
                destination.getWidth() * destination.getHeight()
        ];
        List<SetPixelBatchCommand.PixelUpdate> updates = new ArrayList<>();
        for (int sourceY = 0; sourceY < source.getHeight(); sourceY++) {
            for (int sourceX = 0; sourceX < source.getWidth(); sourceX++) {
                if (!clipboard.selection().contains(sourceX, sourceY)) {
                    continue;
                }
                int targetX = x + sourceX;
                int targetY = y + sourceY;
                mask[targetY * destination.getWidth() + targetX] = true;
                updates.add(new SetPixelBatchCommand.PixelUpdate(
                        targetX, targetY, source.getPixel(sourceX, sourceY)
                ));
            }
        }
        PixelSelection pastedSelection = PixelSelection.fromMask(
                destination.getWidth(), destination.getHeight(), mask
        );
        commands.execute(new DocumentSelectionCommand(
                document, destination, pastedSelection, updates
        ));
        document.setModified(true);
        redraw();
    }

    private void handleCanvasAction(double mouseX, double mouseY) {
        if (!"Eyedropper".equals(activeTool)
                && document.getActiveLayer().isLocked()) {
            return;
        }
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
                        eyedropper.sample(document.getCompositeImage(), x, y)
                );
                changed = false;
            }
            case "Fill Bucket" -> {
                changed = fillBucket.fill(
                        document.getImage(), commands, x, y, currentColor,
                        document.getSelection()
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
        if (document.getActiveLayer().isLocked()) {
            return false;
        }
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
        PixelSelection selection = document.getSelection();
        if (activeStroke != null) {
            for (SymmetryTransform.Point point : points) {
                if (selection != null
                        && !selection.contains(point.x(), point.y())) {
                    continue;
                }
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
                .filter(point -> selection == null
                        || selection.contains(point.x(), point.y()))
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

    private record Clipboard(
            PixelImage image,
            PixelSelection selection,
            int x,
            int y
    ) {
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
                document.getCompositeImage(),
                zoom,
                offsetX,
                offsetY,
                showGrid,
                gridCellWidth,
                gridCellHeight
        );
        drawSymmetryGuides(gc);
        drawSelection(gc);
    }

    private void drawSelection(GraphicsContext gc) {
        PixelSelection visibleSelection = previewSelection == null
                ? document.getSelection()
                : previewSelection;
        if (visibleSelection != null) {
            gc.save();
            gc.setLineWidth(Math.max(1, zoom * 0.08));
            gc.setLineDashes(4, 4);
            gc.setStroke(Color.WHITE);
            drawSelectionEdges(gc, visibleSelection);
            gc.setLineDashOffset(4);
            gc.setStroke(Color.BLACK);
            drawSelectionEdges(gc, visibleSelection);
            gc.restore();
        }

        if (creatingSelection && "Freeform Select".equals(activeTool)
                && lassoPoints.size() > 1) {
            gc.save();
            gc.setLineWidth(Math.max(1, zoom * 0.08));
            gc.setLineDashes(4, 4);
            gc.setStroke(Color.WHITE);
            gc.beginPath();
            gc.moveTo(
                    offsetX + (lassoPoints.getFirst().x() + 0.5) * zoom,
                    offsetY + (lassoPoints.getFirst().y() + 0.5) * zoom
            );
            for (int index = 1; index < lassoPoints.size(); index++) {
                PixelSelection.Point point = lassoPoints.get(index);
                gc.lineTo(
                        offsetX + (point.x() + 0.5) * zoom,
                        offsetY + (point.y() + 0.5) * zoom
                );
            }
            gc.stroke();
            gc.restore();
        }
    }

    private void drawSelectionEdges(
            GraphicsContext gc, PixelSelection selection
    ) {
        gc.beginPath();
        for (int y = 0; y < selection.getHeight(); y++) {
            for (int x = 0; x < selection.getWidth(); x++) {
                if (!selection.contains(x, y)) {
                    continue;
                }
                double left = offsetX + x * zoom;
                double top = offsetY + y * zoom;
                double right = left + zoom;
                double bottom = top + zoom;
                if (!selection.contains(x, y - 1)) {
                    gc.moveTo(left, top);
                    gc.lineTo(right, top);
                }
                if (!selection.contains(x + 1, y)) {
                    gc.moveTo(right, top);
                    gc.lineTo(right, bottom);
                }
                if (!selection.contains(x, y + 1)) {
                    gc.moveTo(right, bottom);
                    gc.lineTo(left, bottom);
                }
                if (!selection.contains(x - 1, y)) {
                    gc.moveTo(left, bottom);
                    gc.lineTo(left, top);
                }
            }
        }
        gc.stroke();
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
            refreshLayersPanel();
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
            refreshLayersPanel();
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
            ImageExporter.write(document.getCompositeImage(), file, format);
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

    private void ensureActiveLayerEditable() {
        if (document.getActiveLayer().isLocked()) {
            throw new IllegalStateException("The active layer is locked");
        }
    }
}
