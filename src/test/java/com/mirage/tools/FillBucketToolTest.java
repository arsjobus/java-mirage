package com.mirage.tools;

import com.mirage.commands.CommandManager;
import com.mirage.core.image.PixelImage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FillBucketToolTest {

    @Test
    void fillsOnlyTheFourConnectedRegionAndSupportsUndoRedo() {
        PixelImage image = new PixelImage(5, 3);
        int wall = 0xFF000000;
        for (int y = 0; y < image.getHeight(); y++) {
            image.setPixel(2, y, wall);
        }
        CommandManager commands = new CommandManager();

        int filled = new FillBucketTool().fill(
                image, commands, 0, 0, 0xFFFF0000
        );

        assertEquals(6, filled);
        assertEquals(0xFFFF0000, image.getPixel(0, 0));
        assertEquals(0xFFFF0000, image.getPixel(1, 2));
        assertEquals(wall, image.getPixel(2, 1));
        assertEquals(0, image.getPixel(3, 1));

        commands.undo();
        assertEquals(0, image.getPixel(0, 0));
        assertEquals(wall, image.getPixel(2, 1));
        assertEquals(0, image.getPixel(3, 1));

        commands.redo();
        assertEquals(0xFFFF0000, image.getPixel(0, 0));
    }

    @Test
    void doesNothingWhenRegionAlreadyHasSelectedColor() {
        PixelImage image = new PixelImage(2, 2);
        CommandManager commands = new CommandManager();
        image.setPixel(0, 0, 0xFF00FF00);

        int filled = new FillBucketTool().fill(
                image, commands, 0, 0, 0xFF00FF00
        );

        assertEquals(0, filled);
        assertFalse(commands.canUndo());
    }

    @Test
    void rejectsStartOutsideImage() {
        PixelImage image = new PixelImage(2, 2);

        assertThrows(
                IllegalArgumentException.class,
                () -> new FillBucketTool().fill(
                        image, new CommandManager(), 2, 0, 0xFFFF0000
                )
        );
    }
}
