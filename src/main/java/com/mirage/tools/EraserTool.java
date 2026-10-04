package com.mirage.tools;

import com.mirage.commands.CommandManager;
import com.mirage.commands.SetPixelCommand;
import com.mirage.core.image.PixelColor;
import com.mirage.core.image.PixelImage;

public final class EraserTool implements Tool {

    @Override
    public String getName() {
        return "Eraser";
    }

    public void erase(PixelImage image, CommandManager commands,
                      int x, int y) {
        if (x < 0 || y < 0 ||
                x >= image.getWidth() || y >= image.getHeight()) {
            return;
        }

        if (image.getPixel(x, y) != PixelColor.TRANSPARENT) {
            commands.execute(new SetPixelCommand(
                    image, x, y, PixelColor.TRANSPARENT
            ));
        }
    }
}
