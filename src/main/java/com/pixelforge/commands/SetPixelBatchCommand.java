package com.pixelforge.commands;

import com.pixelforge.core.image.PixelImage;

import java.util.List;

public final class SetPixelBatchCommand implements Command {

    private final PixelImage image;
    private final List<PixelUpdate> updates;
    private final int[] oldColors;

    public SetPixelBatchCommand(PixelImage image, List<PixelUpdate> updates) {
        this(image, updates, captureOldColors(image, updates));
    }

    public SetPixelBatchCommand(
            PixelImage image, List<PixelUpdate> updates, int[] oldColors
    ) {
        this.image = image;
        this.updates = List.copyOf(updates);
        if (oldColors.length != updates.size()) {
            throw new IllegalArgumentException(
                    "Each pixel update must have one previous color"
            );
        }
        this.oldColors = oldColors.clone();
    }

    private static int[] captureOldColors(
            PixelImage image, List<PixelUpdate> updates
    ) {
        int[] colors = new int[updates.size()];
        for (int index = 0; index < updates.size(); index++) {
            PixelUpdate update = updates.get(index);
            colors[index] = image.getPixel(update.x(), update.y());
        }
        return colors;
    }

    @Override
    public void execute() {
        for (PixelUpdate update : updates) {
            image.setPixel(update.x(), update.y(), update.argb());
        }
    }

    @Override
    public void undo() {
        for (int index = updates.size() - 1; index >= 0; index--) {
            PixelUpdate update = updates.get(index);
            image.setPixel(update.x(), update.y(), oldColors[index]);
        }
    }

    public record PixelUpdate(int x, int y, int argb) {
    }
}
