package com.mirage.core.document;

import com.mirage.core.image.PixelImage;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A pixel document with layers ordered from bottom to top.
 */
public final class Document {

    private final int width;
    private final int height;
    private final List<Layer> layers = new ArrayList<>();
    private Layer activeLayer;
    private boolean modified;

    public Document(int width, int height) {
        this(new PixelImage(width, height));
    }

    public Document(PixelImage image) {
        Objects.requireNonNull(image, "image");
        width = image.getWidth();
        height = image.getHeight();
        activeLayer = new Layer("Layer 1", image);
        layers.add(activeLayer);
    }

    /**
     * Returns the active layer's image for editing.
     */
    public PixelImage getImage() {
        return activeLayer.getImage();
    }

    public PixelImage getCompositeImage() {
        PixelImage composite = new PixelImage(width, height);
        for (Layer layer : layers) {
            if (!layer.isVisible() || layer.getOpacity() == 0.0) {
                continue;
            }
            compositeOver(composite, layer);
        }
        return composite;
    }

    public List<Layer> getLayers() {
        return List.copyOf(layers);
    }

    public Layer getActiveLayer() {
        return activeLayer;
    }

    public void setActiveLayer(Layer layer) {
        if (!layers.contains(layer)) {
            throw new IllegalArgumentException(
                    "Active layer must belong to this document"
            );
        }
        activeLayer = layer;
    }

    public Layer addLayer(String name) {
        Layer layer = new Layer(name, new PixelImage(width, height));
        layers.add(layer);
        activeLayer = layer;
        return layer;
    }

    public Layer duplicateLayer(Layer source) {
        int sourceIndex = indexOf(source);
        Layer duplicate = new Layer(
                source.getName() + " Copy", source.getImage().copy()
        );
        duplicate.setVisible(source.isVisible());
        duplicate.setLocked(source.isLocked());
        duplicate.setOpacity(source.getOpacity());
        layers.add(sourceIndex + 1, duplicate);
        activeLayer = duplicate;
        return duplicate;
    }

    public void deleteLayer(Layer layer) {
        int index = indexOf(layer);
        if (layers.size() == 1) {
            throw new IllegalStateException(
                    "A document must contain at least one layer"
            );
        }
        layers.remove(index);
        if (activeLayer == layer) {
            activeLayer = layers.get(Math.min(index, layers.size() - 1));
        }
    }

    public void moveLayer(Layer layer, int newIndex) {
        int oldIndex = indexOf(layer);
        if (newIndex < 0 || newIndex >= layers.size()) {
            throw new IndexOutOfBoundsException(
                    "Layer index outside document: " + newIndex
            );
        }
        layers.remove(oldIndex);
        layers.add(newIndex, layer);
    }

    public StateSnapshot snapshotState() {
        List<LayerState> states = layers.stream()
                .map(layer -> new LayerState(
                        layer, layer.getName(), layer.isVisible(),
                        layer.isLocked(), layer.getOpacity()
                ))
                .toList();
        return new StateSnapshot(states, activeLayer);
    }

    public void restoreState(StateSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        for (LayerState state : snapshot.layers) {
            Layer layer = state.layer();
            if (layer.getImage().getWidth() != width
                    || layer.getImage().getHeight() != height) {
                throw new IllegalArgumentException(
                        "Layer dimensions must match the document"
                );
            }
        }
        if (snapshot.layers.isEmpty()
                || snapshot.layers.stream().noneMatch(
                    state -> state.layer() == snapshot.activeLayer
                )) {
            throw new IllegalArgumentException(
                "Document state must contain its active layer"
            );
        }

        layers.clear();
        for (LayerState state : snapshot.layers) {
            Layer layer = state.layer();
            layer.setName(state.name());
            layer.setVisible(state.visible());
            layer.setLocked(state.locked());
            layer.setOpacity(state.opacity());
            layers.add(layer);
        }
        activeLayer = snapshot.activeLayer;
    }

    public static final class StateSnapshot {
        private final List<LayerState> layers;
        private final Layer activeLayer;

        private StateSnapshot(
                List<LayerState> layers,
                Layer activeLayer
        ) {
            this.layers = List.copyOf(layers);
            this.activeLayer = activeLayer;
        }
    }

    private record LayerState(
            Layer layer,
            String name,
            boolean visible,
            boolean locked,
            double opacity
    ) {
    }

    private int indexOf(Layer layer) {
        int index = layers.indexOf(layer);
        if (index < 0) {
            throw new IllegalArgumentException(
                    "Layer must belong to this document"
            );
        }
        return index;
    }

    private static void compositeOver(PixelImage destination, Layer source) {
        PixelImage sourceImage = source.getImage();
        double opacity = source.getOpacity();
        for (int y = 0; y < destination.getHeight(); y++) {
            for (int x = 0; x < destination.getWidth(); x++) {
                int sourceArgb = sourceImage.getPixel(x, y);
                double sourceAlpha = ((sourceArgb >>> 24) & 0xFF)
                        / 255.0 * opacity;
                if (sourceAlpha == 0.0) {
                    continue;
                }

                int destinationArgb = destination.getPixel(x, y);
                double destinationAlpha =
                        ((destinationArgb >>> 24) & 0xFF) / 255.0;
                double outputAlpha = sourceAlpha
                        + destinationAlpha * (1.0 - sourceAlpha);
                int red = compositeChannel(
                        (sourceArgb >>> 16) & 0xFF,
                        (destinationArgb >>> 16) & 0xFF,
                        sourceAlpha, destinationAlpha, outputAlpha
                );
                int green = compositeChannel(
                        (sourceArgb >>> 8) & 0xFF,
                        (destinationArgb >>> 8) & 0xFF,
                        sourceAlpha, destinationAlpha, outputAlpha
                );
                int blue = compositeChannel(
                        sourceArgb & 0xFF,
                        destinationArgb & 0xFF,
                        sourceAlpha, destinationAlpha, outputAlpha
                );
                int alpha = (int) Math.round(outputAlpha * 255.0);
                destination.setPixel(
                        x, y,
                        (alpha << 24) | (red << 16) | (green << 8) | blue
                );
            }
        }
    }

    private static int compositeChannel(
            int source,
            int destination,
            double sourceAlpha,
            double destinationAlpha,
            double outputAlpha
    ) {
        double output = (source * sourceAlpha
                + destination * destinationAlpha * (1.0 - sourceAlpha))
                / outputAlpha;
        return (int) Math.round(output);
    }

    public boolean isModified() {
        return modified;
    }

    public void setModified(boolean modified) {
        this.modified = modified;
    }
}
