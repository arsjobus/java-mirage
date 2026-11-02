package com.mirage.commands;

import com.mirage.core.document.Document;
import com.mirage.core.document.Layer;
import com.mirage.core.document.PixelSelection;
import com.mirage.core.image.PixelImage;

import java.util.List;
import java.util.Objects;

public final class CanvasSizeDocumentCommand implements Command {

    private final Document document;
    private final int beforeWidth;
    private final int beforeHeight;
    private final int width;
    private final int height;
    private final List<PixelImage> beforeImages;
    private final List<PixelImage> afterImages;
    private final PixelSelection beforeSelection;
    private final PixelSelection afterSelection;

    public CanvasSizeDocumentCommand(
            Document document, int width, int height, Anchor anchor
    ) {
        this.document = Objects.requireNonNull(document, "document");
        Objects.requireNonNull(anchor, "anchor");
        validateDimensions(width, height);

        this.width = width;
        this.height = height;
        beforeWidth = document.getWidth();
        beforeHeight = document.getHeight();
        int offsetX = anchor.offsetX(beforeWidth, width);
        int offsetY = anchor.offsetY(beforeHeight, height);
        List<Layer> layers = document.getLayers();
        beforeImages = layers.stream()
                .map(layer -> layer.getImage().copy())
                .toList();
        afterImages = beforeImages.stream()
                .map(image -> reposition(image, width, height, offsetX, offsetY))
                .toList();
        beforeSelection = document.getSelection();
        afterSelection = reposition(
                beforeSelection, beforeWidth, beforeHeight,
                width, height, offsetX, offsetY
        );
    }

    @Override
    public void execute() {
        document.replaceLayerImages(width, height, afterImages, afterSelection);
    }

    @Override
    public void undo() {
        document.replaceLayerImages(
                beforeWidth, beforeHeight, beforeImages, beforeSelection
        );
    }

    private static PixelImage reposition(
            PixelImage source, int width, int height, int offsetX, int offsetY
    ) {
        PixelImage resized = new PixelImage(width, height);
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int targetX = x + offsetX;
                int targetY = y + offsetY;
                if (targetX >= 0 && targetX < width
                        && targetY >= 0 && targetY < height) {
                    resized.setPixel(
                            targetX, targetY, source.getPixel(x, y)
                    );
                }
            }
        }
        return resized;
    }

    private static PixelSelection reposition(
            PixelSelection selection,
            int beforeWidth,
            int beforeHeight,
            int width,
            int height,
            int offsetX,
            int offsetY
    ) {
        if (selection == null) {
            return null;
        }
        boolean[] mask = new boolean[width * height];
        for (int y = 0; y < beforeHeight; y++) {
            for (int x = 0; x < beforeWidth; x++) {
                int targetX = x + offsetX;
                int targetY = y + offsetY;
                if (selection.contains(x, y)
                        && targetX >= 0 && targetX < width
                        && targetY >= 0 && targetY < height) {
                    mask[targetY * width + targetX] = true;
                }
            }
        }
        return PixelSelection.fromMask(width, height, mask);
    }

    static void validateDimensions(int width, int height) {
        if (width <= 0 || height <= 0
                || (long) width * height > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "Dimensions must be positive and fit in a pixel buffer"
            );
        }
    }

    public enum Anchor {
        TOP_LEFT(0, 0, "Top Left"),
        TOP_CENTER(1, 0, "Top"),
        TOP_RIGHT(2, 0, "Top Right"),
        MIDDLE_LEFT(0, 1, "Left"),
        CENTER(1, 1, "Center"),
        MIDDLE_RIGHT(2, 1, "Right"),
        BOTTOM_LEFT(0, 2, "Bottom Left"),
        BOTTOM_CENTER(1, 2, "Bottom"),
        BOTTOM_RIGHT(2, 2, "Bottom Right");

        private final int horizontal;
        private final int vertical;
        private final String label;

        Anchor(int horizontal, int vertical, String label) {
            this.horizontal = horizontal;
            this.vertical = vertical;
            this.label = label;
        }

        private int offsetX(int oldWidth, int newWidth) {
            return (int) ((long) (newWidth - (long) oldWidth)
                    * horizontal / 2);
        }

        private int offsetY(int oldHeight, int newHeight) {
            return (int) ((long) (newHeight - (long) oldHeight)
                    * vertical / 2);
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
