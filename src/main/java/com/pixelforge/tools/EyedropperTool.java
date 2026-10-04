package com.pixelforge.tools;

import com.pixelforge.core.image.PixelImage;

public final class EyedropperTool implements Tool {

    @Override
    public String getName() {
        return "Eyedropper";
    }

    public int sample(PixelImage image, int x, int y) {
        if (x < 0 || y < 0 ||
                x >= image.getWidth() || y >= image.getHeight()) {
            return 0;
        }

        return image.getPixel(x, y);
    }
}
