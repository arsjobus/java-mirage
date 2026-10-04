package com.mirage.core.image;

public final class PixelColor {

    private PixelColor() {}

    public static int argb(int alpha, int red, int green, int blue) {
        return ((alpha & 0xFF) << 24)
                | ((red & 0xFF) << 16)
                | ((green & 0xFF) << 8)
                | (blue & 0xFF);
    }

    public static final int TRANSPARENT = 0x00000000;
    public static final int BLACK = 0xFF000000;
    public static final int WHITE = 0xFFFFFFFF;
}
