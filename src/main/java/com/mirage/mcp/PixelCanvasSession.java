package com.mirage.mcp;

import com.mirage.core.image.PixelImage;
import com.mirage.io.ImageExporter;
import com.mirage.tools.SymmetryTransform;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class PixelCanvasSession {

    public static final int MAX_DIMENSION = 1024;
    public static final int MAX_PIXELS_PER_CALL = 10_000;
    public static final int MAX_SHAPE_PIXELS = 250_000;
    public static final int MAX_RADIAL_COPIES = 12;
    public static final int MAX_INSPECT_DIMENSION = 64;
    private static final String INSPECT_SYMBOLS =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    private PixelCanvasSession() {
    }

    public static void validateDimensions(int width, int height) {
        if (width <= 0 || width > MAX_DIMENSION
                || height <= 0 || height > MAX_DIMENSION) {
            throw new IllegalArgumentException(
                    "Canvas dimensions must be between 1 and "
                            + MAX_DIMENSION + " pixels"
            );
        }
    }

    public static void validateChanges(
            PixelImage image,
            List<PixelChange> changes
    ) {
        if (changes.isEmpty() || changes.size() > MAX_PIXELS_PER_CALL) {
            throw new IllegalArgumentException(
                    "Provide between 1 and " + MAX_PIXELS_PER_CALL
                            + " pixel changes"
            );
        }
        validateCoordinates(image, changes);
    }

    private static void validateCoordinates(
            PixelImage image, List<PixelChange> changes
    ) {
        for (PixelChange change : changes) {
            if (change.x() < 0 || change.x() >= image.getWidth()
                    || change.y() < 0 || change.y() >= image.getHeight()) {
                throw new IllegalArgumentException(
                        "Pixel outside canvas: (" + change.x() + ", "
                                + change.y() + ")"
                );
            }
        }
    }

    public static List<PixelChange> shapeChanges(
            PixelImage image,
            ShapeKind shape,
            List<Point> points,
            int argb,
            boolean filled
    ) {
        int requiredPoints = shape == ShapeKind.POLYGON ? -1 : 2;
        if ((requiredPoints >= 0 && points.size() != requiredPoints)
                || (shape == ShapeKind.POLYGON
                && (points.size() < 3 || points.size() > 64))) {
            throw new IllegalArgumentException(
                    shape == ShapeKind.POLYGON
                            ? "A polygon needs between 3 and 64 points"
                            : shape.name().toLowerCase(Locale.ROOT)
                            + " needs exactly 2 points"
            );
        }
        for (Point point : points) {
            if (point.x() < 0 || point.x() >= image.getWidth()
                    || point.y() < 0 || point.y() >= image.getHeight()) {
                throw new IllegalArgumentException(
                        "Point outside canvas: (" + point.x() + ", "
                                + point.y() + ")"
                );
            }
        }

        Raster raster = new Raster(image.getWidth(), argb);
        switch (shape) {
            case LINE -> raster.line(points.get(0), points.get(1));
            case RECTANGLE -> {
                Bounds bounds = Bounds.of(points.get(0), points.get(1));
                if (filled) raster.fillRectangle(bounds);
                else raster.rectangle(bounds);
            }
            case ELLIPSE -> {
                Bounds bounds = Bounds.of(points.get(0), points.get(1));
                raster.ellipse(bounds, filled);
            }
            case POLYGON -> {
                if (filled) raster.fillPolygon(points);
                raster.polygon(points);
            }
        }
        return raster.changes();
    }

    public static List<PixelChange> symmetryChanges(
            PixelImage image,
            List<PixelChange> changes,
            SymmetryOptions options
    ) {
        if (changes.isEmpty() || changes.size() > MAX_SHAPE_PIXELS) {
            throw new IllegalArgumentException(
                    "Provide between 1 and " + MAX_SHAPE_PIXELS
                            + " source pixels"
            );
        }
        validateCoordinates(image, changes);
        if (options == null) {
            return changes;
        }
        int centerX = options.centerX() == null
                ? image.getWidth() / 2 : options.centerX();
        int centerY = options.centerY() == null
                ? image.getHeight() / 2 : options.centerY();
        if (centerX < 0 || centerX > image.getWidth()
                || centerY < 0 || centerY > image.getHeight()) {
            throw new IllegalArgumentException(
                    "Symmetry axes must be within the canvas"
            );
        }
        if (options.radialCopies() < 1
                || options.radialCopies() > MAX_RADIAL_COPIES) {
            throw new IllegalArgumentException(
                    "Radial copies must be between 1 and "
                            + MAX_RADIAL_COPIES
            );
        }

        Map<Integer, PixelChange> transformed = new LinkedHashMap<>();
        for (PixelChange change : changes) {
            for (SymmetryTransform.Point point : SymmetryTransform.pointsFor(
                    change.x(), change.y(),
                    image.getWidth(), image.getHeight(),
                    options.mirrorVertical(),
                    options.mirrorHorizontal(),
                    centerX, centerY, options.radialCopies()
            )) {
                int key = point.y() * image.getWidth() + point.x();
                transformed.put(key, new PixelChange(
                        point.x(), point.y(), change.argb()
                ));
                if (transformed.size() > MAX_SHAPE_PIXELS) {
                    throw new IllegalArgumentException(
                            "Symmetry output exceeds the limit of "
                                    + MAX_SHAPE_PIXELS + " pixels"
                    );
                }
            }
        }
        return List.copyOf(transformed.values());
    }

    public static String inspectCanvas(
            PixelImage image,
            Integer x,
            Integer y,
            Integer width,
            Integer height
    ) {
        boolean anyRegionValue = x != null || y != null
                || width != null || height != null;
        if (anyRegionValue && (x == null || y == null
                || width == null || height == null)) {
            throw new IllegalArgumentException(
                    "Provide x, y, width, and height together"
            );
        }

        int regionX = x == null ? 0 : x;
        int regionY = y == null ? 0 : y;
        int regionWidth = width == null ? image.getWidth() : width;
        int regionHeight = height == null ? image.getHeight() : height;
        if (regionX < 0 || regionY < 0 || regionWidth <= 0
                || regionHeight <= 0
                || regionX >= image.getWidth()
                || regionY >= image.getHeight()
                || regionWidth > image.getWidth() - regionX
                || regionHeight > image.getHeight() - regionY) {
            throw new IllegalArgumentException(
                    "Inspection region must fit inside the canvas"
            );
        }
        if (regionWidth > MAX_INSPECT_DIMENSION
                || regionHeight > MAX_INSPECT_DIMENSION) {
            throw new IllegalArgumentException(
                    "Inspection is limited to "
                            + MAX_INSPECT_DIMENSION + "x"
                            + MAX_INSPECT_DIMENSION
                            + " pixels; request a smaller region"
            );
        }

        Map<Integer, Character> palette = new LinkedHashMap<>();
        for (int row = 0; row < regionHeight; row++) {
            for (int column = 0; column < regionWidth; column++) {
                int color = image.getPixel(regionX + column, regionY + row);
                if (color != 0 && !palette.containsKey(color)) {
                    if (palette.size() == INSPECT_SYMBOLS.length()) {
                        throw new IllegalArgumentException(
                                "Inspection region contains more than "
                                        + INSPECT_SYMBOLS.length()
                                        + " colors; request a smaller region"
                        );
                    }
                    palette.put(
                            color,
                            INSPECT_SYMBOLS.charAt(palette.size())
                    );
                }
            }
        }

        StringBuilder result = new StringBuilder()
                .append("Canvas ").append(image.getWidth()).append('x')
                .append(image.getHeight()).append("; region x=")
                .append(regionX).append(", y=").append(regionY)
                .append(", width=").append(regionWidth)
                .append(", height=").append(regionHeight)
                .append("\n     ");
        for (int column = 0; column < regionWidth; column++) {
            result.append((regionX + column) % 10);
        }
        for (int row = 0; row < regionHeight; row++) {
            result.append('\n');
            String rowLabel = Integer.toString(regionY + row);
            result.append(" ".repeat(Math.max(0, 4 - rowLabel.length())))
                    .append(rowLabel).append(' ');
            for (int column = 0; column < regionWidth; column++) {
                int color = image.getPixel(regionX + column, regionY + row);
                result.append(color == 0 ? '.'
                        : palette.get(color));
            }
        }
        result.append("\nLegend: .=transparent");
        palette.entrySet().stream()
                .sorted(Comparator.comparingInt(entry ->
                        INSPECT_SYMBOLS.indexOf(entry.getValue())))
                .forEach(entry -> result.append(", ")
                        .append(entry.getValue()).append('=')
                        .append(String.format(
                                Locale.ROOT, "#%08X", entry.getKey()
                        )));
        return result.toString();
    }

    public static byte[] previewRegionPng(
            PixelImage image,
            int x,
            int y,
            int width,
            int height,
            int scale
    ) throws IOException {
        validatePreviewRegion(image, x, y, width, height, scale);

        BufferedImage preview = new BufferedImage(
                width * scale, height * scale, BufferedImage.TYPE_INT_RGB
        );
        for (int sourceY = 0; sourceY < height; sourceY++) {
            for (int sourceX = 0; sourceX < width; sourceX++) {
                int color = image.getPixel(x + sourceX, y + sourceY);
                int checker = ((sourceX + sourceY) & 1) == 0
                        ? 0xFFD8D8D8 : 0xFFBEBEBE;
                int composited = compositeOver(color, checker);
                for (int pixelY = 0; pixelY < scale; pixelY++) {
                    for (int pixelX = 0; pixelX < scale; pixelX++) {
                        int outputColor = pixelX == 0 || pixelY == 0
                                ? compositeOver(0x50000000, composited)
                                : composited;
                        preview.setRGB(
                                sourceX * scale + pixelX,
                                sourceY * scale + pixelY,
                                outputColor
                        );
                    }
                }
            }
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        if (!ImageIO.write(preview, "png", output)) {
            throw new IOException("No PNG image writer is available");
        }
        return output.toByteArray();
    }

    private static void validatePreviewRegion(
            PixelImage image,
            int x,
            int y,
            int width,
            int height,
            int scale
    ) {
        if (x < 0 || y < 0 || width <= 0 || height <= 0
                || x >= image.getWidth() || y >= image.getHeight()
                || width > image.getWidth() - x
                || height > image.getHeight() - y) {
            throw new IllegalArgumentException(
                    "Preview region must fit inside the canvas"
            );
        }
        if (width > MAX_INSPECT_DIMENSION
                || height > MAX_INSPECT_DIMENSION) {
            throw new IllegalArgumentException(
                    "Region preview is limited to "
                            + MAX_INSPECT_DIMENSION + "x"
                            + MAX_INSPECT_DIMENSION + " source pixels"
            );
        }
        if (scale < 1 || scale > 16) {
            throw new IllegalArgumentException(
                    "Preview scale must be between 1 and 16"
            );
        }
    }

    private static int compositeOver(int color, int background) {
        int alpha = color >>> 24;
        if (alpha == 255) return color;
        int inverseAlpha = 255 - alpha;
        int red = (((color >>> 16) & 0xFF) * alpha
                + ((background >>> 16) & 0xFF) * inverseAlpha + 127) / 255;
        int green = (((color >>> 8) & 0xFF) * alpha
                + ((background >>> 8) & 0xFF) * inverseAlpha + 127) / 255;
        int blue = ((color & 0xFF) * alpha
                + (background & 0xFF) * inverseAlpha + 127) / 255;
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }

    public static Path savePng(
            PixelImage image,
            String fileName,
            Path outputDirectory
    ) throws IOException {
        if (fileName.isBlank()
                || fileName.contains("/")
                || fileName.contains("\\")
                || !fileName.toLowerCase(Locale.ROOT).endsWith(".png")) {
            throw new IllegalArgumentException(
                    "Provide a PNG file name without a directory path"
            );
        }

        Path normalizedDirectory = outputDirectory.toAbsolutePath().normalize();
        Path outputFile = normalizedDirectory.resolve(fileName).normalize();
        if (!normalizedDirectory.equals(outputFile.getParent())) {
            throw new IllegalArgumentException(
                    "Output file must stay inside the configured output directory"
            );
        }

        Files.createDirectories(normalizedDirectory);
        ImageExporter.write(image, outputFile.toFile(), "png");
        return outputFile;
    }

    public record PixelChange(int x, int y, int argb) {
    }

    public record SymmetryOptions(
            boolean mirrorVertical,
            boolean mirrorHorizontal,
            Integer centerX,
            Integer centerY,
            int radialCopies
    ) {
    }

    public record Point(int x, int y) {
    }

    public enum ShapeKind {
        LINE,
        RECTANGLE,
        ELLIPSE,
        POLYGON
    }

    private record Bounds(int left, int top, int right, int bottom) {

        private static Bounds of(Point first, Point second) {
            return new Bounds(
                    Math.min(first.x(), second.x()),
                    Math.min(first.y(), second.y()),
                    Math.max(first.x(), second.x()),
                    Math.max(first.y(), second.y())
            );
        }
    }

    private static final class Raster {

        private final int imageWidth;
        private final int argb;
        private final Map<Integer, PixelChange> changes = new LinkedHashMap<>();

        private Raster(int imageWidth, int argb) {
            this.imageWidth = imageWidth;
            this.argb = argb;
        }

        private void pixel(int x, int y) {
            int key = y * imageWidth + x;
            if (!changes.containsKey(key)) {
                if (changes.size() == MAX_SHAPE_PIXELS) {
                    throw new IllegalArgumentException(
                            "Shape exceeds the limit of "
                                    + MAX_SHAPE_PIXELS + " pixels"
                    );
                }
                changes.put(key, new PixelChange(x, y, argb));
            }
        }

        private void line(Point start, Point end) {
            int x = start.x(), y = start.y();
            int dx = Math.abs(end.x() - x), sx = x < end.x() ? 1 : -1;
            int dy = -Math.abs(end.y() - y), sy = y < end.y() ? 1 : -1;
            int error = dx + dy;
            while (true) {
                pixel(x, y);
                if (x == end.x() && y == end.y()) return;
                int twiceError = 2 * error;
                if (twiceError >= dy) {
                    error += dy;
                    x += sx;
                }
                if (twiceError <= dx) {
                    error += dx;
                    y += sy;
                }
            }
        }

        private void rectangle(Bounds bounds) {
            line(new Point(bounds.left(), bounds.top()),
                    new Point(bounds.right(), bounds.top()));
            line(new Point(bounds.right(), bounds.top()),
                    new Point(bounds.right(), bounds.bottom()));
            line(new Point(bounds.right(), bounds.bottom()),
                    new Point(bounds.left(), bounds.bottom()));
            line(new Point(bounds.left(), bounds.bottom()),
                    new Point(bounds.left(), bounds.top()));
        }

        private void fillRectangle(Bounds bounds) {
            for (int y = bounds.top(); y <= bounds.bottom(); y++) {
                for (int x = bounds.left(); x <= bounds.right(); x++) {
                    pixel(x, y);
                }
            }
        }

        private void ellipse(Bounds bounds, boolean filled) {
            int radiusXTimesTwo = bounds.right() - bounds.left();
            int radiusYTimesTwo = bounds.bottom() - bounds.top();
            if (radiusXTimesTwo == 0 || radiusYTimesTwo == 0) {
                rectangle(bounds);
                return;
            }

            for (int y = bounds.top(); y <= bounds.bottom(); y++) {
                for (int x = bounds.left(); x <= bounds.right(); x++) {
                    if (!insideEllipse(x, y, bounds)) continue;
                    if (filled
                            || !insideEllipse(x - 1, y, bounds)
                            || !insideEllipse(x + 1, y, bounds)
                            || !insideEllipse(x, y - 1, bounds)
                            || !insideEllipse(x, y + 1, bounds)) {
                        pixel(x, y);
                    }
                }
            }
        }

        private boolean insideEllipse(int x, int y, Bounds bounds) {
            long radiusX = bounds.right() - bounds.left() + 1L;
            long radiusY = bounds.bottom() - bounds.top() + 1L;
            long dx = 2L * x - bounds.left() - bounds.right();
            long dy = 2L * y - bounds.top() - bounds.bottom();
            return dx * dx * radiusY * radiusY
                    + dy * dy * radiusX * radiusX
                    <= radiusX * radiusX * radiusY * radiusY;
        }

        private void polygon(List<Point> points) {
            for (int index = 0; index < points.size(); index++) {
                line(points.get(index),
                        points.get((index + 1) % points.size()));
            }
        }

        private void fillPolygon(List<Point> points) {
            int minY = points.stream().mapToInt(Point::y).min().orElseThrow();
            int maxY = points.stream().mapToInt(Point::y).max().orElseThrow();
            for (int y = minY; y <= maxY; y++) {
                double scanY = y + 0.5;
                List<Double> intersections = new ArrayList<>();
                for (int index = 0; index < points.size(); index++) {
                    Point first = points.get(index);
                    Point second = points.get((index + 1) % points.size());
                    if ((first.y() <= scanY && second.y() > scanY)
                            || (second.y() <= scanY && first.y() > scanY)) {
                        intersections.add(first.x()
                                + (scanY - first.y())
                                * (second.x() - first.x())
                                / (second.y() - first.y()));
                    }
                }
                intersections.sort(Double::compareTo);
                for (int index = 0; index + 1 < intersections.size();
                     index += 2) {
                    int left = (int) Math.floor(intersections.get(index));
                    int right = (int) Math.ceil(
                            intersections.get(index + 1)
                    ) - 1;
                    for (int x = left; x <= right; x++) pixel(x, y);
                }
            }
        }

        private List<PixelChange> changes() {
            return List.copyOf(changes.values());
        }
    }
}
