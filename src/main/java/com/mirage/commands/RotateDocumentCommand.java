package com.mirage.commands;

import com.mirage.core.document.Document;
import com.mirage.core.document.Layer;
import com.mirage.core.document.PixelSelection;
import com.mirage.core.image.PixelImage;

import java.util.List;
import java.util.Objects;

public final class RotateDocumentCommand implements Command {

    private final Document document;
    private final int beforeWidth;
    private final int beforeHeight;
    private final List<PixelImage> beforeImages;
    private final List<PixelImage> afterImages;
    private final PixelSelection beforeSelection;
    private final PixelSelection afterSelection;

    public RotateDocumentCommand(Document document, Direction direction) {
        this.document = Objects.requireNonNull(document, "document");
        Objects.requireNonNull(direction, "direction");
        beforeWidth = document.getWidth();
        beforeHeight = document.getHeight();
        beforeImages = document.getLayers().stream()
                .map(layer -> layer.getImage().copy())
                .toList();
        afterImages = beforeImages.stream()
                .map(image -> rotate(image, direction))
                .toList();
        beforeSelection = document.getSelection();
        afterSelection = rotateSelection(
                beforeSelection, beforeWidth, beforeHeight, direction
        );
    }

    @Override
    public void execute() {
        document.replaceLayerImages(
                beforeHeight, beforeWidth, afterImages, afterSelection
        );
    }

    @Override
    public void undo() {
        document.replaceLayerImages(
                beforeWidth, beforeHeight, beforeImages, beforeSelection
        );
    }

    private static PixelImage rotate(PixelImage source, Direction direction) {
        PixelImage rotated = new PixelImage(
                source.getHeight(), source.getWidth()
        );
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int targetX = direction == Direction.CLOCKWISE
                        ? source.getHeight() - 1 - y : y;
                int targetY = direction == Direction.CLOCKWISE
                        ? x : source.getWidth() - 1 - x;
                rotated.setPixel(
                        targetX, targetY, source.getPixel(x, y)
                );
            }
        }
        return rotated;
    }

    private static PixelSelection rotateSelection(
            PixelSelection selection,
            int width,
            int height,
            Direction direction
    ) {
        if (selection == null) {
            return null;
        }
        boolean[] source = selection.copyMask();
        boolean[] rotated = new boolean[source.length];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int targetX = direction == Direction.CLOCKWISE
                        ? height - 1 - y : y;
                int targetY = direction == Direction.CLOCKWISE
                        ? x : width - 1 - x;
                rotated[targetY * height + targetX] =
                        source[y * width + x];
            }
        }
        return PixelSelection.fromMask(height, width, rotated);
    }

    public enum Direction {
        CLOCKWISE,
        COUNTERCLOCKWISE
    }
}
