package com.pixelforge.core.image;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PixelImageTest {

    @Test
    void createsCorrectDimensions() {
        PixelImage image = new PixelImage(16, 32);
        assertEquals(16, image.getWidth());
        assertEquals(32, image.getHeight());
    }

    @Test
    void newPixelsAreTransparent() {
        PixelImage image = new PixelImage(4, 4);
        assertEquals(PixelColor.TRANSPARENT, image.getPixel(0, 0));
    }

    @Test
    void setAndGetPixel() {
        PixelImage image = new PixelImage(4, 4);
        image.setPixel(2, 3, PixelColor.WHITE);
        assertEquals(PixelColor.WHITE, image.getPixel(2, 3));
    }

    @Test
    void copyIsIndependent() {
        PixelImage image = new PixelImage(4, 4);
        image.setPixel(1, 1, PixelColor.WHITE);

        PixelImage copy = image.copy();
        copy.setPixel(1, 1, PixelColor.BLACK);

        assertEquals(PixelColor.WHITE, image.getPixel(1, 1));
        assertEquals(PixelColor.BLACK, copy.getPixel(1, 1));
    }

    @Test
    void rejectsInvalidDimensions() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new PixelImage(0, 4)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new PixelImage(4, 0)
        );
    }

    @Test
    void rejectsOutOfBoundsPixels() {
        PixelImage image = new PixelImage(4, 4);

        assertThrows(
                IndexOutOfBoundsException.class,
                () -> image.getPixel(4, 0)
        );

        assertThrows(
                IndexOutOfBoundsException.class,
                () -> image.setPixel(-1, 0, PixelColor.WHITE)
        );
    }

    @Test
    void clearMakesPixelsTransparent() {
        PixelImage image = new PixelImage(4, 4);

        image.setPixel(0, 0, PixelColor.WHITE);
        image.setPixel(3, 3, PixelColor.BLACK);
        image.clear();

        assertEquals(PixelColor.TRANSPARENT, image.getPixel(0, 0));
        assertEquals(PixelColor.TRANSPARENT, image.getPixel(3, 3));
    }
}
