package com.mirage.rendering;

import com.mirage.core.image.PixelImage;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.paint.Color;
import javafx.scene.image.WritableImage;

public final class CanvasRenderer {

    private WritableImage checkerboard;
    private int checkerboardWidth;
    private int checkerboardHeight;
    private WritableImage renderedImage;
    private int renderedWidth;
    private int renderedHeight;

    public void render(
            GraphicsContext gc,
            PixelImage image,
            double zoom,
            double offsetX,
            double offsetY,
            boolean showGrid,
            int gridCellWidth,
            int gridCellHeight
    ) {
        gc.setFill(Color.rgb(38, 38, 38));
        gc.fillRect(0, 0, gc.getCanvas().getWidth(), gc.getCanvas().getHeight());

        int width = image.getWidth();
        int height = image.getHeight();

        gc.setImageSmoothing(false);
        drawCheckerboard(gc, width, height, zoom, offsetX, offsetY);

        if (renderedImage == null
                || renderedWidth != width
                || renderedHeight != height) {
            renderedImage = new WritableImage(width, height);
            renderedWidth = width;
            renderedHeight = height;
        }
        PixelWriter writer = renderedImage.getPixelWriter();
        writer.setPixels(
                0, 0, width, height,
                PixelFormat.getIntArgbInstance(),
                image.copyPixels(), 0, width
        );

        gc.drawImage(
                renderedImage,
                0, 0, width, height,
                offsetX, offsetY,
                width * zoom, height * zoom
        );

        if (showGrid) {
            drawGrid(
                    gc, width, height, zoom, offsetX, offsetY,
                    gridCellWidth, gridCellHeight
            );
        }
    }

    private void drawCheckerboard(
            GraphicsContext gc,
            int width,
            int height,
            double zoom,
            double offsetX,
            double offsetY
    ) {
        if (checkerboard == null
                || checkerboardWidth != width
                || checkerboardHeight != height) {
            checkerboard = new WritableImage(width, height);
            int[] pixels = new int[width * height];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    pixels[y * width + x] = ((x + y) & 1) == 0
                            ? 0xFFD2D2D2
                            : 0xFFB9B9B9;
                }
            }
            checkerboard.getPixelWriter().setPixels(
                    0, 0, width, height,
                    PixelFormat.getIntArgbInstance(),
                    pixels, 0, width
            );
            checkerboardWidth = width;
            checkerboardHeight = height;
        }

        gc.drawImage(
                checkerboard,
                0, 0, width, height,
                offsetX, offsetY,
                width * zoom, height * zoom
        );
    }

    private void drawGrid(
            GraphicsContext gc,
            int width,
            int height,
            double zoom,
            double offsetX,
            double offsetY,
            int cellWidth,
            int cellHeight
    ) {
        gc.setStroke(Color.rgb(80, 80, 80, 0.65));
        gc.setLineWidth(1.0);

        for (int x = 0; x < width; x += cellWidth) {
            double sx = Math.round(offsetX + x * zoom) + 0.5;
            gc.strokeLine(sx, offsetY, sx, offsetY + height * zoom);
        }
        double right = Math.round(offsetX + width * zoom) + 0.5;
        gc.strokeLine(right, offsetY, right, offsetY + height * zoom);

        for (int y = 0; y < height; y += cellHeight) {
            double sy = Math.round(offsetY + y * zoom) + 0.5;
            gc.strokeLine(offsetX, sy, offsetX + width * zoom, sy);
        }
        double bottom = Math.round(offsetY + height * zoom) + 0.5;
        gc.strokeLine(offsetX, bottom, offsetX + width * zoom, bottom);
    }
}
