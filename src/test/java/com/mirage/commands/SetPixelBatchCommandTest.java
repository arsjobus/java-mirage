package com.mirage.commands;

import com.mirage.core.image.PixelImage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SetPixelBatchCommandTest {

    @Test
    void executesUndoesAndRedoesBatchAsOneCommand() {
        PixelImage image = new PixelImage(2, 1);
        CommandManager commands = new CommandManager();
        SetPixelBatchCommand batch = new SetPixelBatchCommand(
                image,
                List.of(
                        new SetPixelBatchCommand.PixelUpdate(0, 0, 0xFFFF0000),
                        new SetPixelBatchCommand.PixelUpdate(1, 0, 0xFF00FF00)
                )
        );

        commands.execute(batch);
        assertEquals(0xFFFF0000, image.getPixel(0, 0));
        assertEquals(0xFF00FF00, image.getPixel(1, 0));

        commands.undo();
        assertEquals(0, image.getPixel(0, 0));
        assertEquals(0, image.getPixel(1, 0));

        commands.redo();
        assertEquals(0xFFFF0000, image.getPixel(0, 0));
        assertEquals(0xFF00FF00, image.getPixel(1, 0));
    }

    @Test
    void recordsAndUndoesPixelsAlreadyAppliedDuringAStroke() {
        PixelImage image = new PixelImage(2, 1);
        CommandManager commands = new CommandManager();
        image.setPixel(0, 0, 0xFFFF0000);
        image.setPixel(1, 0, 0xFF00FF00);

        commands.execute(new SetPixelBatchCommand(
                image,
                List.of(
                        new SetPixelBatchCommand.PixelUpdate(0, 0, 0xFFFF0000),
                        new SetPixelBatchCommand.PixelUpdate(1, 0, 0xFF00FF00)
                ),
                new int[]{0, 0}
        ));

        commands.undo();
        assertEquals(0, image.getPixel(0, 0));
        assertEquals(0, image.getPixel(1, 0));

        commands.redo();
        assertEquals(0xFFFF0000, image.getPixel(0, 0));
        assertEquals(0xFF00FF00, image.getPixel(1, 0));
    }
}
