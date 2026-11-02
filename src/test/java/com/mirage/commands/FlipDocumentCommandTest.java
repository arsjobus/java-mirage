package com.mirage.commands;

import com.mirage.core.document.Document;
import com.mirage.core.document.Layer;
import com.mirage.core.document.PixelSelection;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlipDocumentCommandTest {

    @Test
    void flipsEveryLayerHorizontallyAndSupportsUndoRedo() {
        Document document = new Document(3, 2);
        document.getImage().setPixel(0, 0, 1);
        document.getImage().setPixel(2, 1, 2);
        Layer second = document.addLayer("Second");
        second.getImage().setPixel(1, 0, 3);

        CommandManager commands = new CommandManager();
        commands.execute(new FlipDocumentCommand(
                document, FlipDocumentCommand.Direction.HORIZONTAL
        ));

        assertArrayEquals(new int[]{0, 0, 1, 2, 0, 0},
                document.getLayers().getFirst().getImage().copyPixels());
        assertArrayEquals(new int[]{0, 3, 0, 0, 0, 0},
                second.getImage().copyPixels());

        commands.undo();
        assertArrayEquals(new int[]{1, 0, 0, 0, 0, 2},
                document.getLayers().getFirst().getImage().copyPixels());
        assertArrayEquals(new int[]{0, 3, 0, 0, 0, 0},
                second.getImage().copyPixels());

        commands.redo();
        assertArrayEquals(new int[]{0, 0, 1, 2, 0, 0},
                document.getLayers().getFirst().getImage().copyPixels());
    }

    @Test
    void flipsVerticallyAndPreservesSelectionAcrossUndoRedo() {
        Document document = new Document(2, 3);
        document.getImage().setPixel(0, 0, 1);
        document.getImage().setPixel(1, 2, 2);
        boolean[] mask = {
                true, false,
                false, false,
                false, true
        };
        PixelSelection selection = PixelSelection.fromMask(2, 3, mask);
        document.setSelection(selection);

        CommandManager commands = new CommandManager();
        commands.execute(new FlipDocumentCommand(
                document, FlipDocumentCommand.Direction.VERTICAL
        ));

        assertArrayEquals(new int[]{0, 2, 0, 0, 1, 0},
                document.getImage().copyPixels());
        assertFalse(document.getSelection().contains(0, 0));
        assertTrue(document.getSelection().contains(0, 2));
        assertTrue(document.getSelection().contains(1, 0));

        commands.undo();
        assertArrayEquals(new int[]{1, 0, 0, 0, 0, 2},
                document.getImage().copyPixels());
        assertTrue(document.getSelection().contains(0, 0));
        assertTrue(document.getSelection().contains(1, 2));

        commands.redo();
        assertTrue(document.getSelection().contains(0, 2));
        assertTrue(document.getSelection().contains(1, 0));
    }
}
