package com.mirage.commands;

import com.mirage.commands.CanvasSizeDocumentCommand.Anchor;
import com.mirage.core.document.Document;
import com.mirage.core.document.Layer;
import com.mirage.core.document.PixelSelection;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentTransformCommandsTest {

    @Test
    void canvasSizeUsesEveryAnchorAndTransformsArtworkAndSelectionTogether() {
        for (Anchor anchor : Anchor.values()) {
            Document document = new Document(2, 2);
            document.getImage().setPixel(0, 0, 1);
            document.setSelection(PixelSelection.fromMask(
                    2, 2, new boolean[]{true, false, false, false}
            ));

            new CanvasSizeDocumentCommand(document, 4, 4, anchor).execute();

            int offsetX = switch (anchor) {
                case TOP_LEFT, MIDDLE_LEFT, BOTTOM_LEFT -> 0;
                case TOP_CENTER, CENTER, BOTTOM_CENTER -> 1;
                case TOP_RIGHT, MIDDLE_RIGHT, BOTTOM_RIGHT -> 2;
            };
            int offsetY = switch (anchor) {
                case TOP_LEFT, TOP_CENTER, TOP_RIGHT -> 0;
                case MIDDLE_LEFT, CENTER, MIDDLE_RIGHT -> 1;
                case BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT -> 2;
            };
            assertEquals(1, document.getImage().getPixel(offsetX, offsetY));
            assertTrue(document.getSelection().contains(offsetX, offsetY));
        }
    }

    @Test
    void canvasSizeClipsAndUndoRedoPreservesLayersAndSelection() {
        Document document = new Document(4, 3);
        Layer bottom = document.getActiveLayer();
        bottom.setName("Background");
        bottom.setVisible(false);
        bottom.setLocked(true);
        bottom.setOpacity(0.35);
        bottom.getImage().setPixel(1, 0, 11);
        bottom.getImage().setPixel(3, 2, 12);
        Layer top = document.addLayer("Foreground");
        top.setOpacity(0.65);
        top.getImage().setPixel(2, 1, 21);
        document.setActiveLayer(bottom);
        PixelSelection selection = PixelSelection.fromMask(
                4, 3, new boolean[]{
                        true, false, false, false,
                        false, true, true, false,
                        false, false, false, true
                }
        );
        document.setSelection(selection);
        int[] beforeBottom = bottom.getImage().copyPixels();
        int[] beforeTop = top.getImage().copyPixels();
        CommandManager commands = new CommandManager();
        commands.execute(new CanvasSizeDocumentCommand(
                document, 2, 2, Anchor.CENTER
        ));

        assertEquals(2, document.getWidth());
        assertEquals(2, document.getHeight());
        assertArrayEquals(new int[]{11, 0, 0, 0}, bottom.getImage().copyPixels());
        assertArrayEquals(new int[]{0, 0, 0, 21}, top.getImage().copyPixels());
        assertFalse(document.getSelection().contains(0, 0));
        assertTrue(document.getSelection().contains(0, 1));
        assertTrue(document.getSelection().contains(1, 1));
        assertSame(bottom, document.getActiveLayer());
        assertEquals("Background", bottom.getName());
        assertFalse(bottom.isVisible());
        assertTrue(bottom.isLocked());
        assertEquals(0.35, bottom.getOpacity());
        assertEquals("Foreground", top.getName());
        assertTrue(top.isVisible());
        assertFalse(top.isLocked());
        assertEquals(0.65, top.getOpacity());

        commands.undo();
        assertEquals(4, document.getWidth());
        assertEquals(3, document.getHeight());
        assertArrayEquals(beforeBottom, bottom.getImage().copyPixels());
        assertArrayEquals(beforeTop, top.getImage().copyPixels());
        assertSame(selection, document.getSelection());
        assertSame(bottom, document.getActiveLayer());
        commands.redo();
        assertArrayEquals(new int[]{11, 0, 0, 0}, bottom.getImage().copyPixels());
        assertTrue(document.getSelection().contains(0, 1));
        assertTrue(document.getSelection().contains(1, 1));
        assertSame(bottom, document.getActiveLayer());
    }

    @Test
    void resizeImageUsesSameNearestNeighborMappingForPixelsAndMask() {
        Document document = new Document(3, 2);
        for (int y = 0; y < 2; y++) {
            for (int x = 0; x < 3; x++) {
                document.getImage().setPixel(x, y, 10 * y + x + 1);
            }
        }
        Layer hiddenLocked = document.addLayer("Hidden locked");
        hiddenLocked.setVisible(false);
        hiddenLocked.setLocked(true);
        hiddenLocked.setOpacity(0.4);
        hiddenLocked.getImage().setPixel(1, 1, 99);
        document.setActiveLayer(document.getLayers().getFirst());
        PixelSelection selection = PixelSelection.fromMask(
                3, 2, new boolean[]{
                        false, true, false,
                        false, false, true
                }
        );
        document.setSelection(selection);
        int[] beforeBase = document.getLayers().getFirst()
                .getImage().copyPixels();
        int[] beforeHidden = hiddenLocked.getImage().copyPixels();
        CommandManager commands = new CommandManager();
        commands.execute(new ResizeImageDocumentCommand(document, 2, 3));

        assertEquals(2, document.getWidth());
        assertEquals(3, document.getHeight());
        assertArrayEquals(new int[]{
                1, 2,
                1, 2,
                11, 12
        }, document.getLayers().getFirst().getImage().copyPixels());
        assertArrayEquals(new int[]{0, 0, 0, 0, 0, 99},
                hiddenLocked.getImage().copyPixels());
        assertFalse(document.getSelection().contains(0, 0));
        assertTrue(document.getSelection().contains(1, 0));
        assertTrue(document.getSelection().contains(1, 1));
        assertFalse(document.getSelection().contains(1, 2));
        assertSame(document.getLayers().getFirst(), document.getActiveLayer());
        assertEquals("Hidden locked", hiddenLocked.getName());
        assertFalse(hiddenLocked.isVisible());
        assertTrue(hiddenLocked.isLocked());
        assertEquals(0.4, hiddenLocked.getOpacity());

        commands.undo();
        assertEquals(3, document.getWidth());
        assertEquals(2, document.getHeight());
        assertArrayEquals(beforeBase,
                document.getLayers().getFirst().getImage().copyPixels());
        assertArrayEquals(beforeHidden, hiddenLocked.getImage().copyPixels());
        assertSame(selection, document.getSelection());
        commands.redo();
        assertEquals(2, document.getWidth());
        assertEquals(3, document.getHeight());
        assertArrayEquals(new int[]{
                1, 2,
                1, 2,
                11, 12
        }, document.getLayers().getFirst().getImage().copyPixels());
        assertTrue(document.getSelection().contains(1, 0));
        assertFalse(document.getSelection().contains(1, 2));
    }

    @Test
    void transformationsKeepAnAbsentSelectionAbsent() {
        Document document = new Document(2, 2);

        CanvasSizeDocumentCommand canvasSize =
                new CanvasSizeDocumentCommand(document, 3, 3, Anchor.CENTER);
        canvasSize.execute();
        assertNull(document.getSelection());
        canvasSize.undo();
        assertNull(document.getSelection());

        ResizeImageDocumentCommand resize =
                new ResizeImageDocumentCommand(document, 3, 3);
        resize.execute();
        assertNull(document.getSelection());
        resize.undo();
        assertNull(document.getSelection());
    }

    @Test
    void resizeImageUpscalingMapsLastDestinationPixelToLastSourcePixel() {
        Document document = new Document(2, 2);
        document.getImage().setPixel(0, 0, 1);
        document.getImage().setPixel(1, 0, 2);
        document.getImage().setPixel(0, 1, 3);
        document.getImage().setPixel(1, 1, 4);
        document.setSelection(PixelSelection.fromMask(
                2, 2, new boolean[]{false, false, false, true}
        ));

        new ResizeImageDocumentCommand(document, 3, 3).execute();

        assertArrayEquals(new int[]{
                1, 1, 2,
                1, 1, 2,
                3, 3, 4
        }, document.getImage().copyPixels());
        assertFalse(document.getSelection().contains(1, 1));
        assertTrue(document.getSelection().contains(2, 2));
    }

    @Test
    void transformationsRejectNonPositiveAndUnrepresentableDimensions() {
        Document document = new Document(2, 2);
        assertThrows(IllegalArgumentException.class,
                () -> new CanvasSizeDocumentCommand(
                        document, 0, 2, Anchor.CENTER
                ));
        assertThrows(IllegalArgumentException.class,
                () -> new ResizeImageDocumentCommand(document, 2, -1));
        assertThrows(IllegalArgumentException.class,
                () -> new ResizeImageDocumentCommand(
                        document, Integer.MAX_VALUE, 2
                ));
    }
}
