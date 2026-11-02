package com.mirage.core.document;

import com.mirage.core.image.PixelImage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentTest {

    @Test
    void compositesVisibleLayersFromBottomToTopWithOpacity() {
        Document document = new Document(1, 1);
        document.getImage().setPixel(0, 0, 0xFF0000FF);
        Layer top = document.addLayer("Top");
        top.getImage().setPixel(0, 0, 0xFFFF0000);
        top.setOpacity(0.5);

        assertEquals(0xFF800080,
                document.getCompositeImage().getPixel(0, 0));

        top.setVisible(false);
        assertEquals(0xFF0000FF,
                document.getCompositeImage().getPixel(0, 0));
    }

    @Test
    void duplicatesLayerPixelsAndPropertiesWithoutSharingImage() {
        Document document = new Document(2, 1);
        Layer source = document.getActiveLayer();
        source.setName("Background");
        source.setVisible(false);
        source.setLocked(true);
        source.setOpacity(0.4);
        source.getImage().setPixel(1, 0, 0xFF123456);

        Layer duplicate = document.duplicateLayer(source);
        assertEquals("Background Copy", duplicate.getName());
        assertFalse(duplicate.isVisible());
        assertTrue(duplicate.isLocked());
        assertEquals(0.4, duplicate.getOpacity());
        assertEquals(0xFF123456, duplicate.getImage().getPixel(1, 0));
        assertNotSame(source.getImage(), duplicate.getImage());

        duplicate.getImage().setPixel(1, 0, 0);
        assertEquals(0xFF123456, source.getImage().getPixel(1, 0));
        assertSame(duplicate, document.getActiveLayer());
    }

    @Test
    void keepsOneLayerAndSelectsNeighborAfterDeletingActiveLayer() {
        Document document = new Document(1, 1);
        Layer first = document.getActiveLayer();
        Layer second = document.addLayer("Second");

        document.deleteLayer(second);
        assertSame(first, document.getActiveLayer());
        assertThrows(IllegalStateException.class,
                () -> document.deleteLayer(first));
    }

    @Test
    void reordersLayersAndUsesNewCompositeOrder() {
        PixelImage baseImage = new PixelImage(1, 1);
        baseImage.setPixel(0, 0, 0xFFFF0000);
        Document document = new Document(baseImage);
        Layer top = document.addLayer("Top");
        top.getImage().setPixel(0, 0, 0xFF0000FF);

        document.moveLayer(top, 0);
        assertEquals(0xFFFF0000,
                document.getCompositeImage().getPixel(0, 0));
        assertSame(top, document.getActiveLayer());
    }

    @Test
    void rejectsInvalidLayerNamesOpacityAndForeignLayers() {
        Document document = new Document(1, 1);
        Layer layer = document.getActiveLayer();

        assertThrows(IllegalArgumentException.class,
                () -> layer.setName("  "));
        assertThrows(IllegalArgumentException.class,
                () -> layer.setOpacity(Double.NaN));
        assertThrows(IllegalArgumentException.class,
                () -> layer.setOpacity(1.1));
        assertThrows(IllegalArgumentException.class,
                () -> document.setActiveLayer(new Layer("Other", new PixelImage(1, 1))));
    }
}
