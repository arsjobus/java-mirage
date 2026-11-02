package com.mirage.core.document;

import com.mirage.core.image.PixelImage;

import java.util.Objects;

public final class Layer {

    private String name;
    private final PixelImage image;
    private boolean visible = true;
    private boolean locked;
    private double opacity = 1.0;

    public Layer(String name, PixelImage image) {
        this.name = validateName(name);
        this.image = Objects.requireNonNull(image, "image");
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = validateName(name);
    }

    public PixelImage getImage() {
        return image;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public double getOpacity() {
        return opacity;
    }

    public void setOpacity(double opacity) {
        if (!Double.isFinite(opacity) || opacity < 0.0 || opacity > 1.0) {
            throw new IllegalArgumentException(
                    "Layer opacity must be between 0.0 and 1.0"
            );
        }
        this.opacity = opacity;
    }

    private static String validateName(String name) {
        Objects.requireNonNull(name, "name");
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Layer name cannot be blank");
        }
        return trimmed;
    }

    @Override
    public String toString() {
        return name;
    }
}
