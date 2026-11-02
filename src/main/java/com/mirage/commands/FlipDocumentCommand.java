package com.mirage.commands;

import com.mirage.core.document.Document;
import com.mirage.core.document.Layer;
import com.mirage.core.document.PixelSelection;
import com.mirage.core.image.PixelImage;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class FlipDocumentCommand implements Command {

    private final Document document;
    private final List<Layer> layers;
    private final List<int[]> beforePixels;
    private final List<int[]> afterPixels;
    private final PixelSelection beforeSelection;
    private final PixelSelection afterSelection;

    public FlipDocumentCommand(Document document, Direction direction) {
        this.document = Objects.requireNonNull(document, "document");
        Objects.requireNonNull(direction, "direction");
        layers = document.getLayers();
        beforePixels = new ArrayList<>(layers.size());
        afterPixels = new ArrayList<>(layers.size());
        for (Layer layer : layers) {
            int[] pixels = layer.getImage().copyPixels();
            beforePixels.add(pixels);
            afterPixels.add(flip(
                    pixels, document.getWidth(), document.getHeight(),
                    direction
            ));
        }
        beforeSelection = document.getSelection();
        afterSelection = flipSelection(
                beforeSelection, document.getWidth(), document.getHeight(),
                direction
        );
    }

    @Override
    public void execute() {
        apply(afterPixels, afterSelection);
    }

    @Override
    public void undo() {
        apply(beforePixels, beforeSelection);
    }

    private void apply(List<int[]> pixels, PixelSelection selection) {
        for (int layerIndex = 0; layerIndex < layers.size(); layerIndex++) {
            PixelImage image = layers.get(layerIndex).getImage();
            int[] layerPixels = pixels.get(layerIndex);
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    image.setPixel(x, y, layerPixels[y * image.getWidth() + x]);
                }
            }
        }
        document.setSelection(selection);
    }

    private static int[] flip(
            int[] pixels, int width, int height, Direction direction
    ) {
        int[] flipped = new int[pixels.length];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int sourceX = direction == Direction.HORIZONTAL
                        ? width - 1 - x : x;
                int sourceY = direction == Direction.VERTICAL
                        ? height - 1 - y : y;
                flipped[y * width + x] = pixels[sourceY * width + sourceX];
            }
        }
        return flipped;
    }

    private static PixelSelection flipSelection(
            PixelSelection selection,
            int width,
            int height,
            Direction direction
    ) {
        if (selection == null) {
            return null;
        }
        boolean[] mask = selection.copyMask();
        boolean[] flipped = new boolean[mask.length];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int sourceX = direction == Direction.HORIZONTAL
                        ? width - 1 - x : x;
                int sourceY = direction == Direction.VERTICAL
                        ? height - 1 - y : y;
                flipped[y * width + x] =
                        mask[sourceY * width + sourceX];
            }
        }
        return PixelSelection.fromMask(width, height, flipped);
    }

    public enum Direction {
        HORIZONTAL,
        VERTICAL
    }
}
