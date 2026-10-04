package com.pixelforge.io;

import com.pixelforge.core.image.PixelImage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Set;

public final class ImageExporter {

    private static final Set<String> SUPPORTED_FORMATS =
            Set.of("png", "jpg", "jpeg", "gif");

    private ImageExporter() {
    }

    public static void write(PixelImage image, File file, String format)
            throws IOException {
        String normalizedFormat = format.toLowerCase(Locale.ROOT);
        if (!SUPPORTED_FORMATS.contains(normalizedFormat)) {
            throw new IllegalArgumentException(
                    "Unsupported image format: " + format
            );
        }

        BufferedImage output = toBufferedImage(image, normalizedFormat);
        String writerFormat = normalizedFormat.equals("jpeg")
                ? "jpg"
                : normalizedFormat;
        if (!ImageIO.write(output, writerFormat, file)) {
            throw new IOException(
                    "No image writer is available for " + writerFormat
            );
        }
    }

    public static byte[] toPngBytes(PixelImage image) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        if (!ImageIO.write(toBufferedImage(image, "png"), "png", output)) {
            throw new IOException("No PNG image writer is available");
        }
        return output.toByteArray();
    }

    private static BufferedImage toBufferedImage(
            PixelImage image,
            String format
    ) {
        int width = image.getWidth();
        int height = image.getHeight();

        if (format.equals("jpg") || format.equals("jpeg")) {
            BufferedImage output =
                    new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    output.setRGB(x, y, compositeOverWhite(
                            image.getPixel(x, y)
                    ));
                }
            }
            return output;
        }

        BufferedImage output =
                new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                output.setRGB(x, y, image.getPixel(x, y));
            }
        }
        return output;
    }

    private static int compositeOverWhite(int argb) {
        int alpha = (argb >>> 24) & 0xFF;
        int red = compositeChannel((argb >>> 16) & 0xFF, alpha);
        int green = compositeChannel((argb >>> 8) & 0xFF, alpha);
        int blue = compositeChannel(argb & 0xFF, alpha);
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }

    private static int compositeChannel(int channel, int alpha) {
        return (channel * alpha + 255 * (255 - alpha) + 127) / 255;
    }
}
