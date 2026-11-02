package com.mirage.commands;

import com.mirage.core.document.Document;
import com.mirage.core.document.Layer;
import com.mirage.core.document.PixelSelection;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RotateDocumentCommandTest {

    @Test
    void rotatesEveryLayerClockwiseAndPreservesHistoryReferences() {
        Document document = new Document(3, 2);
        document.getImage().setPixel(0, 0, 1);
        document.getImage().setPixel(2, 1, 2);
        Layer second = document.addLayer("Second");
        second.getImage().setPixel(1, 0, 3);

        CommandManager commands = new CommandManager();
        commands.execute(new SetPixelCommand(
                document.getLayers().getFirst().getImage(), 1, 1, 4
        ));
        commands.execute(new RotateDocumentCommand(
                document, RotateDocumentCommand.Direction.CLOCKWISE
        ));

        assertEquals(2, document.getWidth());
        assertEquals(3, document.getHeight());
        assertArrayEquals(new int[]{0, 1, 4, 0, 2, 0},
                document.getLayers().getFirst().getImage().copyPixels());
        assertArrayEquals(new int[]{0, 0, 0, 3, 0, 0},
                second.getImage().copyPixels());

        commands.undo();
        assertEquals(3, document.getWidth());
        assertEquals(2, document.getHeight());
        commands.undo();
        assertEquals(
                0, document.getLayers().getFirst().getImage().getPixel(1, 1)
        );

        commands.redo();
        commands.redo();
        assertEquals(2, document.getWidth());
        assertArrayEquals(new int[]{0, 1, 4, 0, 2, 0},
                document.getLayers().getFirst().getImage().copyPixels());
    }

    @Test
    void rotatesCounterclockwiseAndTransformsSelectionAcrossUndoRedo() {
        Document document = new Document(3, 2);
        document.getImage().setPixel(0, 0, 1);
        document.setSelection(PixelSelection.fromMask(
                3, 2, new boolean[]{
                        true, false, false,
                        false, false, true
                }
        ));

        CommandManager commands = new CommandManager();
        commands.execute(new RotateDocumentCommand(
                document, RotateDocumentCommand.Direction.COUNTERCLOCKWISE
        ));

        assertEquals(2, document.getWidth());
        assertEquals(3, document.getHeight());
        assertArrayEquals(new int[]{0, 0, 0, 0, 1, 0},
                document.getImage().copyPixels());
        assertTrue(document.getSelection().contains(0, 2));
        assertTrue(document.getSelection().contains(1, 0));
        assertFalse(document.getSelection().contains(0, 0));
        assertFalse(document.getSelection().contains(1, 1));

        commands.undo();
        assertTrue(document.getSelection().contains(0, 0));
        assertTrue(document.getSelection().contains(2, 1));
        commands.redo();
        assertTrue(document.getSelection().contains(0, 2));
        assertTrue(document.getSelection().contains(1, 0));
    }
}
