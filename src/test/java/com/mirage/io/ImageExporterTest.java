package com.mirage.io;

import com.mirage.core.image.PixelImage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageExporterTest {

    @TempDir
    Path tempDirectory;

    @Test
    void writesPngWithTransparency() throws IOException {
        PixelImage image = new PixelImage(2, 1);
        image.setPixel(0, 0, 0x00000000);
        image.setPixel(1, 0, 0xFFFF0000);
        File file = tempDirectory.resolve("image.png").toFile();

        ImageExporter.write(image, file, "png");
        BufferedImage exported = ImageIO.read(file);

        assertEquals(2, exported.getWidth());
        assertEquals(0x00000000, exported.getRGB(0, 0));
        assertEquals(0xFFFF0000, exported.getRGB(1, 0));
    }

    @Test
    void encodesPngPreviewInMemoryWithTransparency() throws IOException {
        PixelImage image = new PixelImage(2, 1);
        image.setPixel(1, 0, 0x8000FF00);

        byte[] png = ImageExporter.toPngBytes(image);
        BufferedImage preview = ImageIO.read(new ByteArrayInputStream(png));

        assertEquals(2, preview.getWidth());
        assertEquals(1, preview.getHeight());
        assertEquals(0, preview.getRGB(0, 0));
        assertEquals(0x8000FF00, preview.getRGB(1, 0));
    }

    @Test
    void writesGifWithTransparency() throws IOException {
        PixelImage image = new PixelImage(2, 1);
        image.setPixel(0, 0, 0x00000000);
        image.setPixel(1, 0, 0xFFFF0000);
        File file = tempDirectory.resolve("image.gif").toFile();

        ImageExporter.write(image, file, "gif");
        BufferedImage exported = ImageIO.read(file);

        assertEquals(2, exported.getWidth());
        assertEquals(0, exported.getRGB(0, 0) >>> 24);
        assertEquals(0xFFFF0000, exported.getRGB(1, 0));
    }

    @Test
    void writesJpegWithTransparentPixelsCompositedOverWhite()
            throws IOException {
        PixelImage image = new PixelImage(1, 1);
        image.setPixel(0, 0, 0x00000000);
        File file = tempDirectory.resolve("image.jpg").toFile();

        ImageExporter.write(image, file, "jpg");
        BufferedImage exported = ImageIO.read(file);
        int pixel = exported.getRGB(0, 0);

        assertEquals(0xFF, pixel >>> 24);
        assertTrue(((pixel >>> 16) & 0xFF) >= 250);
        assertTrue(((pixel >>> 8) & 0xFF) >= 250);
        assertTrue((pixel & 0xFF) >= 250);
    }
}
