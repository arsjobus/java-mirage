package com.pixelforge.mcp;

import com.pixelforge.ui.MainWindow;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.ImageContent;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import javafx.application.Platform;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class PixelForgeMcpServer implements AutoCloseable {

    private static final long UI_ACTION_TIMEOUT_SECONDS = 30;

    private final McpSyncServer server;

    private PixelForgeMcpServer(McpSyncServer server) {
        this.server = server;
    }

    public static PixelForgeMcpServer start(
            MainWindow window,
            Path outputDirectory
    ) {
        StdioServerTransportProvider transport =
                new StdioServerTransportProvider(McpJsonDefaults.getMapper());

        McpSyncServer server = McpServer.sync(transport)
                .serverInfo("mirage", "0.1.0")
                .capabilities(ServerCapabilities.builder().tools(true).build())
                .toolCall(tool(
                                "create_canvas",
                                "Create a new transparent canvas in the open "
                                        + "Mirage window. Dimensions are "
                                        + "limited to 1024 by 1024.",
                                schema(
                                        Map.of("width", integerProperty(
                                                "Canvas width in pixels"
                                        ), "height", integerProperty(
                                                "Canvas height in pixels"
                                        )),
                                        List.of("width", "height")
                                )
                        ),
                        (exchange, request) -> respond(request, arguments ->
                                onFxThread(() -> {
                                    int width = integer(arguments, "width");
                                    int height = integer(arguments, "height");
                                    window.createCanvasFromMcp(width, height);
                                    return "Created transparent " + width + "x"
                                            + height
                                            + " canvas in Mirage.";
                                })))
                .toolCall(tool(
                                "draw_pixels",
                                "Draw pixels on the canvas shown in the open "
                                        + "Mirage window. Colors use "
                                        + "#RRGGBB or #AARRGGBB; six-digit "
                                        + "colors are opaque. Send as many "
                                        + "pixels as possible in each call "
                                        + "(up to 10,000); each call is one "
                                        + "undoable batch. Optionally provide "
                                        + "symmetry settings to mirror or "
                                        + "radially repeat the pixels.",
                                schema(
                                        Map.of(
                                                "pixels", Map.of(
                                                        "type", "array",
                                                        "description", "Pixels to set",
                                                        "minItems", 1,
                                                        "maxItems",
                                                        PixelCanvasSession.MAX_PIXELS_PER_CALL,
                                                        "items", Map.of(
                                                                "type", "object",
                                                                "properties", Map.of(
                                                                        "x", integerProperty(
                                                                                "Zero-based x coordinate"
                                                                        ),
                                                                        "y", integerProperty(
                                                                                "Zero-based y coordinate"
                                                                        ),
                                                                        "color", Map.of(
                                                                                "type", "string",
                                                                                "pattern", "^#([0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})$",
                                                                                "description", "ARGB or RGB hex color"
                                                                        )
                                                                ),
                                                                "required", List.of(
                                                                        "x", "y", "color"
                                                                ),
                                                                "additionalProperties", false
                                                        )
                                                ),
                                                "symmetry", symmetryProperty()
                                        ),
                                        List.of("pixels")
                                )
                        ),
                        (exchange, request) -> respond(request, arguments -> {
                            List<PixelCanvasSession.PixelChange> changes =
                                    pixelChanges(arguments);
                            PixelCanvasSession.SymmetryOptions symmetry =
                                    symmetryOptions(arguments);
                            return onFxThread(() -> {
                                int count = window.drawPixelsFromMcp(
                                        changes, symmetry
                                );
                                return "Drew " + count
                                        + " pixel(s) in Mirage.";
                            });
                        }))
                .toolCall(tool(
                                "draw_shape",
                                "Draw a line, rectangle, ellipse, or polygon "
                                        + "using pixel coordinates and one "
                                        + "solid color. Points outside the "
                                        + "canvas are rejected. Each shape is "
                                        + "one undoable action. Rectangles and "
                                        + "ellipses use two opposite corner "
                                        + "points; lines use two endpoints; "
                                        + "polygons use 3 to 64 vertices. A "
                                        + "shape may affect at most "
                                        + PixelCanvasSession.MAX_SHAPE_PIXELS
                                        + " pixels. An optional symmetry object "
                                        + "mirrors or radially repeats the "
                                        + "rasterized shape.",
                                schema(
                                        Map.of(
                                                "shape", Map.of(
                                                        "type", "string",
                                                        "enum", List.of(
                                                                "line",
                                                                "rectangle",
                                                                "ellipse",
                                                                "polygon"
                                                        ),
                                                        "description", "Shape to rasterize"
                                                ),
                                                "points", Map.of(
                                                        "type", "array",
                                                        "description", "Zero-based points: two for line/rectangle/ellipse; 3 to 64 vertices for polygon",
                                                        "minItems", 2,
                                                        "maxItems", 64,
                                                        "items", Map.of(
                                                                "type", "object",
                                                                "properties", Map.of(
                                                                        "x", integerProperty("Zero-based x coordinate"),
                                                                        "y", integerProperty("Zero-based y coordinate")
                                                                ),
                                                                "required", List.of("x", "y"),
                                                                "additionalProperties", false
                                                        )
                                                ),
                                                "color", Map.of(
                                                        "type", "string",
                                                        "pattern", "^#([0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})$",
                                                        "description", "ARGB or RGB hex color"
                                                ),
                                                "filled", Map.of(
                                                        "type", "boolean",
                                                        "description", "Fill a rectangle, ellipse, or polygon; defaults to false"
                                                ),
                                                "symmetry", symmetryProperty()
                                        ),
                                        List.of("shape", "points", "color")
                                )
                        ),
                        (exchange, request) -> respond(request, arguments -> {
                            PixelCanvasSession.ShapeKind shape;
                            try {
                                shape = PixelCanvasSession.ShapeKind.valueOf(
                                        string(arguments, "shape")
                                                .toUpperCase(Locale.ROOT)
                                );
                            } catch (IllegalArgumentException ex) {
                                throw new IllegalArgumentException(
                                        "Shape must be line, rectangle, "
                                                + "ellipse, or polygon",
                                        ex
                                );
                            }
                            List<PixelCanvasSession.Point> points =
                                    shapePoints(arguments);
                            int color = parseColor(string(arguments, "color"));
                            boolean filled = optionalBoolean(
                                    arguments, "filled", false
                            );
                            PixelCanvasSession.SymmetryOptions symmetry =
                                    symmetryOptions(arguments);
                            return onFxThread(() -> {
                                int count = window.drawShapeFromMcp(
                                        shape, points, color, filled, symmetry
                                );
                                return "Drew " + shape.name().toLowerCase(
                                        Locale.ROOT
                                ) + " using " + count
                                        + " pixel(s) in Mirage.";
                            });
                        }))
                .toolCall(tool(
                                "inspect_canvas",
                                "Inspect exact canvas pixels as a compact "
                                        + "character grid. Transparent pixels "
                                        + "are '.', and the legend maps each "
                                        + "other character to an exact ARGB "
                                        + "color. Can inspect up to 64x64 "
                                        + "pixels; larger canvases need an "
                                        + "explicit x, y, width, and height "
                                        + "region.",
                                schema(
                                        Map.of(
                                                "x", integerProperty("Zero-based region x coordinate"),
                                                "y", integerProperty("Zero-based region y coordinate"),
                                                "width", integerProperty("Region width in pixels"),
                                                "height", integerProperty("Region height in pixels")
                                        ),
                                        List.of()
                                )
                        ),
                        (exchange, request) -> respond(request, arguments -> {
                            boolean anyRegionValue = arguments.containsKey("x")
                                    || arguments.containsKey("y")
                                    || arguments.containsKey("width")
                                    || arguments.containsKey("height");
                            Integer x = anyRegionValue
                                    ? integer(arguments, "x") : null;
                            Integer y = anyRegionValue
                                    ? integer(arguments, "y") : null;
                            Integer width = anyRegionValue
                                    ? integer(arguments, "width") : null;
                            Integer height = anyRegionValue
                                    ? integer(arguments, "height") : null;
                            return onFxThread(() -> window.inspectCanvasFromMcp(
                                    x, y, width, height
                            ));
                        }))
                .toolCall(tool(
                                "preview_canvas",
                                "Return the current Mirage canvas as an "
                                        + "in-memory PNG image so the drawing "
                                        + "can be visually reviewed without "
                                        + "saving or changing the document.",
                                schema(Map.of(), List.of())
                        ),
                        (exchange, request) -> preview(window))
                .toolCall(tool(
                                "preview_region",
                                "Return an enlarged crop of the current "
                                        + "canvas with a visible pixel grid. "
                                        + "Use this to inspect and refine "
                                        + "small details without changing the "
                                        + "document. Regions are limited to "
                                        + PixelCanvasSession.MAX_INSPECT_DIMENSION
                                        + "x"
                                        + PixelCanvasSession.MAX_INSPECT_DIMENSION
                                        + " source pixels; scale defaults to "
                                        + "8 and can be set from 1 to 16.",
                                schema(
                                        Map.of(
                                                "x", integerProperty(
                                                        "Zero-based region x coordinate"
                                                ),
                                                "y", integerProperty(
                                                        "Zero-based region y coordinate"
                                                ),
                                                "width", integerProperty(
                                                        "Region width in source pixels"
                                                ),
                                                "height", integerProperty(
                                                        "Region height in source pixels"
                                                ),
                                                "scale", Map.of(
                                                        "type", "integer",
                                                        "minimum", 1,
                                                        "maximum", 16,
                                                        "description", "Output pixels per source pixel; defaults to 8"
                                                )
                                        ),
                                        List.of("x", "y", "width", "height")
                                )
                        ),
                        (exchange, request) ->
                                previewRegion(window, request.arguments()))
                .toolCall(tool(
                                "bucket_fill",
                                "Fill the four-connected area containing the "
                                        + "canvas pixel at x, y. Uses the "
                                        + "currently selected Mirage color "
                                        + "unless an optional color is supplied "
                                        + "as #RRGGBB or #AARRGGBB. The fill is "
                                        + "one undoable action.",
                                schema(
                                        Map.of(
                                                "x", integerProperty(
                                                        "Zero-based x coordinate to fill"
                                                ),
                                                "y", integerProperty(
                                                        "Zero-based y coordinate to fill"
                                                ),
                                                "color", Map.of(
                                                        "type", "string",
                                                        "pattern", "^#([0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})$",
                                                        "description", "Optional ARGB or RGB fill color; defaults to the selected editor color"
                                                )
                                        ),
                                        List.of("x", "y")
                                )
                        ),
                        (exchange, request) -> respond(request, arguments -> {
                            int x = integer(arguments, "x");
                            int y = integer(arguments, "y");
                            Integer color = arguments.containsKey("color")
                                    ? parseColor(string(arguments, "color"))
                                    : null;
                            return onFxThread(() -> {
                                int filled = window.fillFromMcp(x, y, color);
                                return "Filled " + filled
                                        + " pixel(s) in Mirage.";
                            });
                        }))
                .toolCall(tool(
                                "save_png",
                                "Export the currently visible Mirage "
                                        + "canvas to a PNG in the configured "
                                        + "output directory. Supply a file name "
                                        + "only, not a path.",
                                schema(
                                        Map.of("file_name", Map.of(
                                                "type", "string",
                                                "description", "PNG file name, e.g. sprite.png",
                                                "pattern", "^[^/\\\\]+\\.png$"
                                        )),
                                        List.of("file_name")
                                )
                        ),
                        (exchange, request) -> respond(request, arguments ->
                                onFxThread(() -> {
                                    Path saved = window.savePngFromMcp(
                                            string(arguments, "file_name"),
                                            outputDirectory
                                    );
                                    return "Saved Mirage canvas: " + saved;
                                })))
                .build();
        return new PixelForgeMcpServer(server);
    }

    @Override
    public void close() {
        server.close();
    }

    private static Tool tool(
            String name,
            String description,
            Map<String, Object> inputSchema
    ) {
        return Tool.builder(name, inputSchema)
                .description(description)
                .build();
    }

    private static Map<String, Object> schema(
            Map<String, Object> properties,
            List<String> required
    ) {
        return Map.of(
                "type", "object",
                "properties", properties,
                "required", required,
                "additionalProperties", false
        );
    }

    private static Map<String, Object> integerProperty(String description) {
        return Map.of("type", "integer", "description", description);
    }

    private static CallToolResult respond(
            CallToolRequest request,
            ToolAction action
    ) {
        try {
            return CallToolResult.builder()
                    .addTextContent(action.run(request.arguments()))
                    .build();
        } catch (IllegalArgumentException | IllegalStateException
                 | IOException | TimeoutException ex) {
            return CallToolResult.builder()
                    .addTextContent(ex.getMessage())
                    .isError(true)
                    .build();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return CallToolResult.builder()
                    .addTextContent("Interrupted while updating Mirage.")
                    .isError(true)
                    .build();
        }
    }

    private static <T> T onFxThread(Callable<T> action)
            throws IOException, InterruptedException, TimeoutException {
        if (Platform.isFxApplicationThread()) {
            try {
                return action.call();
            } catch (IOException | RuntimeException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new IllegalStateException(
                        "Could not update the Mirage window", ex
                );
            }
        }

        FutureTask<T> task = new FutureTask<>(action);
        Platform.runLater(task);
        try {
            return task.get(UI_ACTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof IOException ioException) {
                throw ioException;
            }
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException(
                    "Could not update the Mirage window", cause
            );
        }
    }

    private static CallToolResult preview(MainWindow window) {
        try {
            byte[] png = onFxThread(window::previewPngFromMcp);
            return CallToolResult.builder()
                    .addTextContent("Current Mirage canvas preview.")
                    .addContent(ImageContent.builder(
                            Base64.getEncoder().encodeToString(png),
                            "image/png"
                    ).build())
                    .build();
        } catch (IllegalArgumentException | IllegalStateException
                 | IOException | TimeoutException ex) {
            return CallToolResult.builder()
                    .addTextContent(ex.getMessage())
                    .isError(true)
                    .build();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return CallToolResult.builder()
                    .addTextContent("Interrupted while previewing Mirage.")
                    .isError(true)
                    .build();
        }
    }

    private static CallToolResult previewRegion(
            MainWindow window,
            Map<String, Object> arguments
    ) {
        try {
            int x = integer(arguments, "x");
            int y = integer(arguments, "y");
            int width = integer(arguments, "width");
            int height = integer(arguments, "height");
            int scale = arguments.containsKey("scale")
                    ? integer(arguments, "scale") : 8;
            byte[] png = onFxThread(() ->
                    window.previewRegionPngFromMcp(
                            x, y, width, height, scale
                    ));
            return CallToolResult.builder()
                    .addTextContent(
                            "Enlarged canvas region x=" + x + ", y=" + y
                                    + ", width=" + width + ", height="
                                    + height + ", scale=" + scale + "."
                    )
                    .addContent(ImageContent.builder(
                            Base64.getEncoder().encodeToString(png),
                            "image/png"
                    ).build())
                    .build();
        } catch (IllegalArgumentException | IllegalStateException
                 | IOException | TimeoutException ex) {
            return CallToolResult.builder()
                    .addTextContent(ex.getMessage())
                    .isError(true)
                    .build();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return CallToolResult.builder()
                    .addTextContent(
                            "Interrupted while previewing Mirage region."
                    )
                    .isError(true)
                    .build();
        }
    }

    private static List<PixelCanvasSession.PixelChange> pixelChanges(
            Map<String, Object> arguments
    ) {
        Object value = arguments.get("pixels");
        if (!(value instanceof List<?> pixels)) {
            throw new IllegalArgumentException(
                    "Argument 'pixels' must be an array"
            );
        }
        if (pixels.size() > PixelCanvasSession.MAX_PIXELS_PER_CALL) {
            throw new IllegalArgumentException(
                    "A draw_pixels call can set at most "
                            + PixelCanvasSession.MAX_PIXELS_PER_CALL + " pixels"
            );
        }

        List<PixelCanvasSession.PixelChange> changes =
                new ArrayList<>(pixels.size());
        for (int index = 0; index < pixels.size(); index++) {
            Object item = pixels.get(index);
            if (!(item instanceof Map<?, ?> pixel)) {
                throw new IllegalArgumentException(
                        "Pixel at index " + index + " must be an object"
                );
            }
            int x = integer(pixel, "x");
            int y = integer(pixel, "y");
            int color = parseColor(string(pixel, "color"));
            changes.add(new PixelCanvasSession.PixelChange(x, y, color));
        }
        return changes;
    }

    private static List<PixelCanvasSession.Point> shapePoints(
            Map<String, Object> arguments
    ) {
        Object value = arguments.get("points");
        if (!(value instanceof List<?> values)) {
            throw new IllegalArgumentException(
                    "Argument 'points' must be an array"
            );
        }
        List<PixelCanvasSession.Point> points = new ArrayList<>(values.size());
        for (int index = 0; index < values.size(); index++) {
            Object item = values.get(index);
            if (!(item instanceof Map<?, ?> point)) {
                throw new IllegalArgumentException(
                        "Point at index " + index + " must be an object"
                );
            }
            points.add(new PixelCanvasSession.Point(
                    integer(point, "x"), integer(point, "y")
            ));
        }
        return points;
    }

    private static boolean optionalBoolean(
            Map<?, ?> arguments,
            String name,
            boolean defaultValue
    ) {
        Object value = arguments.get(name);
        if (value == null) return defaultValue;
        if (value instanceof Boolean booleanValue) return booleanValue;
        throw new IllegalArgumentException(
                "Argument '" + name + "' must be a boolean"
        );
    }

    private static Integer optionalInteger(
            Map<?, ?> arguments, String name
    ) {
        if (!arguments.containsKey(name)) return null;
        return integer(arguments, name);
    }

    private static PixelCanvasSession.SymmetryOptions symmetryOptions(
            Map<String, Object> arguments
    ) {
        Object value = arguments.get("symmetry");
        if (value == null) return null;
        if (!(value instanceof Map<?, ?> symmetry)) {
            throw new IllegalArgumentException(
                    "Argument 'symmetry' must be an object"
            );
        }
        Integer radialCopies = optionalInteger(symmetry, "radial_copies");
        return new PixelCanvasSession.SymmetryOptions(
                optionalBoolean(symmetry, "mirror_vertical", false),
                optionalBoolean(symmetry, "mirror_horizontal", false),
                optionalInteger(symmetry, "center_x"),
                optionalInteger(symmetry, "center_y"),
                radialCopies == null ? 1 : radialCopies
        );
    }

    private static Map<String, Object> symmetryProperty() {
        return Map.of(
                "type", "object",
                "description", "Optional drawing symmetry. Axes default to "
                        + "the canvas center; radial_copies defaults to 1.",
                "properties", Map.of(
                        "mirror_vertical", Map.of(
                                "type", "boolean",
                                "description", "Mirror across the vertical axis"
                        ),
                        "mirror_horizontal", Map.of(
                                "type", "boolean",
                                "description", "Mirror across the horizontal axis"
                        ),
                        "center_x", Map.of(
                                "type", "integer",
                                "minimum", 0,
                                "maximum", PixelCanvasSession.MAX_DIMENSION,
                                "description", "Vertical symmetry axis gridline"
                        ),
                        "center_y", Map.of(
                                "type", "integer",
                                "minimum", 0,
                                "maximum", PixelCanvasSession.MAX_DIMENSION,
                                "description", "Horizontal symmetry axis gridline"
                        ),
                        "radial_copies", Map.of(
                                "type", "integer",
                                "minimum", 1,
                                "maximum", PixelCanvasSession.MAX_RADIAL_COPIES,
                                "description", "Number of rotated copies"
                        )
                ),
                "additionalProperties", false
        );
    }

    private static int integer(Map<?, ?> arguments, String name) {
        Object value = arguments.get(name);
        if (value instanceof Number number) {
            double numericValue = number.doubleValue();
            int integerValue = number.intValue();
            if (Double.isFinite(numericValue) && numericValue == integerValue) {
                return integerValue;
            }
        }
        throw new IllegalArgumentException(
                "Argument '" + name + "' must be an integer"
        );
    }

    private static String string(Map<?, ?> arguments, String name) {
        Object value = arguments.get(name);
        if (value instanceof String string) {
            return string;
        }
        throw new IllegalArgumentException(
                "Argument '" + name + "' must be a string"
        );
    }

    private static int parseColor(String color) {
        if ((color.length() != 7 && color.length() != 9)
                || color.charAt(0) != '#') {
            throw new IllegalArgumentException(
                    "Color must use #RRGGBB or #AARRGGBB format"
            );
        }
        try {
            long parsed = Long.parseLong(color.substring(1), 16);
            return color.length() == 7
                    ? (int) (0xFF000000L | parsed)
                    : (int) parsed;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(
                    "Color must use #RRGGBB or #AARRGGBB format",
                    ex
            );
        }
    }

    @FunctionalInterface
    private interface ToolAction {
        String run(Map<String, Object> arguments)
                throws IOException, InterruptedException, TimeoutException;
    }
}
