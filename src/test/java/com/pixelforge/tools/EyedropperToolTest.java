package com.pixelforge.tools;

import com.pixelforge.core.image.PixelColor;
import com.pixelforge.core.image.PixelImage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EyedropperToolTest {

    @Test
    void samplesPixelColorIncludingAlpha() {
        PixelImage image = new PixelImage(2, 2);
        int color = PixelColor.argb(128, 12, 34, 56);
        image.setPixel(1, 0, color);

        assertEquals(color, new EyedropperTool().sample(image, 1, 0));
    }

    @Test
    void samplesTransparentPixels() {
        PixelImage image = new PixelImage(1, 1);

        assertEquals(
                PixelColor.TRANSPARENT,
                new EyedropperTool().sample(image, 0, 0)
        );
    }
}
