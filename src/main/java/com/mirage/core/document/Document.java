package com.mirage.core.document;

import com.mirage.core.image.PixelImage;

/**
 * Initial document model. Layers, frames and palettes are added later.
 */
public final class Document {

    private final PixelImage image;
    private boolean modified;

    public Document(int width, int height) {
        this.image = new PixelImage(width, height);
    }

    public Document(PixelImage image) {
        this.image = image;
    }

    public PixelImage getImage() {
        return image;
    }

    public boolean isModified() {
        return modified;
    }

    public void setModified(boolean modified) {
        this.modified = modified;
    }
}
