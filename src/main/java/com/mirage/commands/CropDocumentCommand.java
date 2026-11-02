package com.mirage.commands;

import com.mirage.core.document.Document;
import com.mirage.core.document.Layer;
import com.mirage.core.document.PixelSelection;
import com.mirage.core.image.PixelImage;

import java.util.List;
import java.util.Objects;

public final class CropDocumentCommand implements Command {

    private final Document document;
    private final int beforeWidth;
    private final int beforeHeight;
    private final List<Layer> layers;
    private final List<PixelImage> beforeImages;
    private final List<PixelImage> afterImages;
    private final PixelSelection beforeSelection;
    private final PixelSelection afterSelection;
    private final PixelSelection.Bounds bounds;

    public CropDocumentCommand(Document document) {
        this.document = Objects.requireNonNull(document, "document");
        beforeSelection = document.getSelection();
        if (beforeSelection == null) {
            throw new IllegalStateException(
                    "A selection is required to crop the document"
            );
        }
        bounds = beforeSelection.bounds().orElseThrow(() ->
                new IllegalStateException(
                        "The selection must contain at least one pixel"
                )
        );
        beforeWidth = document.getWidth();
        beforeHeight = document.getHeight();
        layers = document.getLayers();
        beforeImages = layers.stream()
                .map(layer -> layer.getImage().copy())
                .toList();
        afterImages = beforeImages.stream()
                .map(image -> crop(image, bounds))
                .toList();
        afterSelection = cropSelection(beforeSelection, bounds);
    }

    @Override
    public void execute() {
        document.replaceLayerImages(
                bounds.width(), bounds.height(), afterImages, afterSelection
        );
    }

    @Override
    public void undo() {
        document.replaceLayerImages(
                beforeWidth, beforeHeight, beforeImages, beforeSelection
        );
    }

    private static PixelImage crop(
            PixelImage source, PixelSelection.Bounds bounds
    ) {
        PixelImage cropped = new PixelImage(bounds.width(), bounds.height());
        for (int y = 0; y < bounds.height(); y++) {
            for (int x = 0; x < bounds.width(); x++) {
                cropped.setPixel(
                        x, y,
                        source.getPixel(bounds.x() + x, bounds.y() + y)
                );
            }
        }
        return cropped;
    }

    private static PixelSelection cropSelection(
            PixelSelection selection, PixelSelection.Bounds bounds
    ) {
        boolean[] cropped = new boolean[bounds.width() * bounds.height()];
        for (int y = 0; y < bounds.height(); y++) {
            for (int x = 0; x < bounds.width(); x++) {
                cropped[y * bounds.width() + x] =
                        selection.contains(bounds.x() + x, bounds.y() + y);
            }
        }
        return PixelSelection.fromMask(
                bounds.width(), bounds.height(), cropped
        );
    }
}
