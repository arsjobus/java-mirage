package com.mirage.icon;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.*;

class AppIconTest {

    @Test
    void drawsAnIconAtEverySupportedSize() {
        for (int size : AppIcon.SIZES) {
            var icon = AppIcon.defaultIcon(size);
            assertEquals(size, icon.getWidth());
            assertEquals(size, icon.getHeight());
            assertNotEquals(0, icon.getRGB(size / 2, size / 2));
        }
    }

    @Test
    void encodesIconAsPng() throws Exception {
        byte[] png = AppIcon.png(AppIcon.defaultIcon(128));
        var decoded = ImageIO.read(new ByteArrayInputStream(png));
        assertNotNull(decoded);
        assertEquals(128, decoded.getWidth());
        assertEquals(128, decoded.getHeight());
    }

    @Test
    void rejectsNonPositiveIconSizes() {
        assertThrows(IllegalArgumentException.class, () -> AppIcon.defaultIcon(0));
    }
}
