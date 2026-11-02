package com.mirage.core.document;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * An immutable mask of selected pixels in document coordinates.
 */
public final class PixelSelection {

    private final int width;
    private final int height;
    private final boolean[] selected;

    private PixelSelection(int width, int height, boolean[] selected) {
        if (selected.length != checkedArea(width, height)) {
            throw new IllegalArgumentException("Invalid selection dimensions");
        }
        this.width = width;
        this.height = height;
        this.selected = selected.clone();
    }

    public static PixelSelection all(int width, int height) {
        boolean[] selected = new boolean[checkedArea(width, height)];
        java.util.Arrays.fill(selected, true);
        return new PixelSelection(width, height, selected);
    }

    public static PixelSelection rectangle(
            int width, int height, int x1, int y1, int x2, int y2
    ) {
        boolean[] selected = new boolean[checkedArea(width, height)];
        int left = Math.max(0, Math.min(x1, x2));
        int top = Math.max(0, Math.min(y1, y2));
        int right = Math.min(width - 1, Math.max(x1, x2));
        int bottom = Math.min(height - 1, Math.max(y1, y2));
        for (int y = top; y <= bottom; y++) {
            for (int x = left; x <= right; x++) {
                selected[y * width + x] = true;
            }
        }
        return new PixelSelection(width, height, selected);
    }

    public static PixelSelection polygon(
            int width, int height, List<Point> points
    ) {
        Objects.requireNonNull(points, "points");
        boolean[] selected = new boolean[checkedArea(width, height)];
        if (points.size() < 3) {
            return new PixelSelection(width, height, selected);
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (containsPoint(points, x + 0.5, y + 0.5)) {
                    selected[y * width + x] = true;
                }
            }
        }
        return new PixelSelection(width, height, selected);
    }

    public static PixelSelection fromMask(
            int width, int height, boolean[] selected
    ) {
        Objects.requireNonNull(selected, "selected");
        return new PixelSelection(width, height, selected);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean contains(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height
                && selected[y * width + x];
    }

    public boolean[] copyMask() {
        return selected.clone();
    }

    public Optional<Bounds> bounds() {
        int left = width;
        int top = height;
        int right = -1;
        int bottom = -1;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (contains(x, y)) {
                    left = Math.min(left, x);
                    top = Math.min(top, y);
                    right = Math.max(right, x);
                    bottom = Math.max(bottom, y);
                }
            }
        }
        return right < left
                ? Optional.empty()
                : Optional.of(new Bounds(
                        left, top, right - left + 1, bottom - top + 1
                ));
    }

    public PixelSelection translated(int dx, int dy) {
        boolean[] translated = new boolean[selected.length];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int targetX = x + dx;
                int targetY = y + dy;
                if (contains(x, y)
                        && targetX >= 0 && targetX < width
                        && targetY >= 0 && targetY < height) {
                    translated[targetY * width + targetX] = true;
                }
            }
        }
        return new PixelSelection(width, height, translated);
    }

    private static boolean containsPoint(
            List<Point> points, double x, double y
    ) {
        boolean inside = false;
        for (int current = 0, previous = points.size() - 1;
             current < points.size(); previous = current++) {
            Point a = points.get(current);
            Point b = points.get(previous);
            if ((a.y() > y) != (b.y() > y)
                    && x < (b.x() - a.x()) * (y - a.y())
                    / (b.y() - a.y()) + a.x()) {
                inside = !inside;
            }
        }
        return inside;
    }

    private static int checkedArea(int width, int height) {
        if (width <= 0 || height <= 0
                || (long) width * height > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Invalid selection dimensions");
        }
        return width * height;
    }

    public record Point(int x, int y) {
    }

    public record Bounds(int x, int y, int width, int height) {
    }
}
