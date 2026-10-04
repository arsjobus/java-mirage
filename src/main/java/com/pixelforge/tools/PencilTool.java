package com.pixelforge.tools;

import com.pixelforge.commands.CommandManager;
import com.pixelforge.commands.SetPixelCommand;
import com.pixelforge.core.image.PixelImage;

public final class PencilTool implements Tool {

    @Override
    public String getName() {
        return "Pencil";
    }

    public void draw(PixelImage image, CommandManager commands,
                     int x, int y, int color) {
        if (x < 0 || y < 0 ||
                x >= image.getWidth() || y >= image.getHeight()) {
            return;
        }

        if (image.getPixel(x, y) != color) {
            commands.execute(new SetPixelCommand(image, x, y, color));
        }
    }
}
