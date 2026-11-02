package com.mirage.commands;

import com.mirage.core.document.Document;
import com.mirage.core.document.Layer;
import com.mirage.core.document.PixelSelection;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CropDocumentCommandTest {

    @Test
    void cropsEveryLayerAndSelectionAndSupportsUndoRedo() {
        Document document = new Document(4, 3);
        Layer bottom = document.getActiveLayer();
        bottom.getImage().setPixel(0, 0, 1);
        bottom.getImage().setPixel(1, 1, 2);
        bottom.getImage().setPixel(2, 1, 3);
        bottom.getImage().setPixel(2, 2, 4);
        bottom.setVisible(false);
        Layer top = document.addLayer("Top");
        top.getImage().setPixel(1, 1, 5);
        top.getImage().setPixel(3, 2, 6);
        top.setOpacity(0.5);
        PixelSelection selection = PixelSelection.fromMask(
                4, 3, new boolean[]{
                        false, false, false, false,
                        false, true, false, false,
                        false, false, true, false
                }
        );
        document.setSelection(selection);
        document.setActiveLayer(bottom);
        int[] beforeBottom = bottom.getImage().copyPixels();
        int[] beforeTop = top.getImage().copyPixels();

        CommandManager commands = new CommandManager();
        commands.execute(new CropDocumentCommand(document));

        assertEquals(2, document.getWidth());
        assertEquals(2, document.getHeight());
        assertArrayEquals(new int[]{2, 3, 0, 4},
                bottom.getImage().copyPixels());
        assertArrayEquals(new int[]{5, 0, 0, 0},
                top.getImage().copyPixels());
        assertSame(bottom, document.getActiveLayer());
        assertFalse(bottom.isVisible());
        assertEquals(0.5, top.getOpacity());
        assertTrue(document.getSelection().contains(0, 0));
        assertTrue(document.getSelection().contains(1, 1));
        assertFalse(document.getSelection().contains(1, 0));

        commands.undo();
        assertEquals(4, document.getWidth());
        assertEquals(3, document.getHeight());
        assertArrayEquals(beforeBottom, bottom.getImage().copyPixels());
        assertArrayEquals(beforeTop, top.getImage().copyPixels());
        assertSame(selection, document.getSelection());

        commands.redo();
        assertEquals(2, document.getWidth());
        assertArrayEquals(new int[]{2, 3, 0, 4},
                bottom.getImage().copyPixels());
    }

    @Test
    void rejectsMissingOrEmptySelection() {
        Document document = new Document(2, 2);
        assertThrows(IllegalStateException.class,
                () -> new CropDocumentCommand(document));

        document.setSelection(PixelSelection.fromMask(
                2, 2, new boolean[4]
        ));
        assertThrows(IllegalStateException.class,
                () -> new CropDocumentCommand(document));
    }
}
