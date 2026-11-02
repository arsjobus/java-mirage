package com.mirage.commands;

import com.mirage.core.document.Document;
import com.mirage.core.document.Layer;
import com.mirage.core.document.PixelSelection;
import com.mirage.core.image.PixelImage;

import java.util.List;
import java.util.Objects;

public final class ResizeImageDocumentCommand implements Command {

    private final Document document;
    private final int beforeWidth;
    private final int beforeHeight;
    private final int width;
    private final int height;
    private final List<PixelImage> beforeImages;
    private final List<PixelImage> afterImages;
    private final PixelSelection beforeSelection;
    private final PixelSelection afterSelection;

    public ResizeImageDocumentCommand(
            Document document, int width, int height
    ) {
        this.document = Objects.requireNonNull(document, "document");
        CanvasSizeDocumentCommand.validateDimensions(width, height);
        beforeWidth = document.getWidth();
        beforeHeight = document.getHeight();
        this.width = width;
        this.height = height;
        List<Layer> layers = document.getLayers();
        beforeImages = layers.stream()
                .map(layer -> layer.getImage().copy())
                .toList();
        afterImages = beforeImages.stream()
                .map(image -> resize(image, width, height))
                .toList();
        beforeSelection = document.getSelection();
        afterSelection = resize(
                beforeSelection, beforeWidth, beforeHeight, width, height
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

    private static PixelImage resize(PixelImage source, int width, int height) {
        PixelImage resized = new PixelImage(width, height);
        for (int y = 0; y < height; y++) {
            int sourceY = (int) ((long) y * source.getHeight() / height);
            for (int x = 0; x < width; x++) {
                int sourceX = (int) ((long) x * source.getWidth() / width);
                resized.setPixel(
                        x, y, source.getPixel(sourceX, sourceY)
                );
            }
        }
        return resized;
    }

    private static PixelSelection resize(
            PixelSelection selection,
            int sourceWidth,
            int sourceHeight,
            int width,
            int height
    ) {
        if (selection == null) {
            return null;
        }
        boolean[] mask = new boolean[width * height];
        for (int y = 0; y < height; y++) {
            int sourceY = (int) ((long) y * sourceHeight / height);
            for (int x = 0; x < width; x++) {
                int sourceX = (int) ((long) x * sourceWidth / width);
                mask[y * width + x] = selection.contains(sourceX, sourceY);
            }
        }
        return PixelSelection.fromMask(width, height, mask);
    }
}
