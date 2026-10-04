package com.pixelforge.core.image;

import java.util.Arrays;

/**
 * JavaFX-independent pixel image.
 * Pixels use 0xAARRGGBB.
 */
public final class PixelImage {

    private final int width;
    private final int height;
    private final int[] pixels;

    public PixelImage(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Image dimensions must be positive");
        }

        this.width = width;
        this.height = height;
        this.pixels = new int[width * height];
    }

    public PixelImage(PixelImage source) {
        this.width = source.width;
        this.height = source.height;
        this.pixels = source.pixels.clone();
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }

    public int getPixel(int x, int y) {
        checkBounds(x, y);
        return pixels[y * width + x];
    }

    public void setPixel(int x, int y, int color) {
        checkBounds(x, y);
        pixels[y * width + x] = color;
    }

    public void clear() {
        Arrays.fill(pixels, PixelColor.TRANSPARENT);
    }

    public int[] copyPixels() {
        return pixels.clone();
    }

    public PixelImage copy() {
        return new PixelImage(this);
    }

    private void checkBounds(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            throw new IndexOutOfBoundsException(
                    "Pixel outside image: (" + x + ", " + y + ")"
            );
        }
    }
}
