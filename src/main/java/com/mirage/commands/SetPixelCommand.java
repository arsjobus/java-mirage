package com.mirage.commands;

import com.mirage.core.image.PixelImage;

public final class SetPixelCommand implements Command {

    private final PixelImage image;
    private final int x;
    private final int y;
    private final int newColor;
    private final int oldColor;

    public SetPixelCommand(PixelImage image, int x, int y, int newColor) {
        this.image = image;
        this.x = x;
        this.y = y;
        this.newColor = newColor;
        this.oldColor = image.getPixel(x, y);
    }

    @Override
    public void execute() {
        image.setPixel(x, y, newColor);
    }

    @Override
    public void undo() {
        image.setPixel(x, y, oldColor);
    }
}
