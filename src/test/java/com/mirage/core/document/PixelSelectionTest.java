package com.mirage.core.document;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PixelSelectionTest {

    @Test
    void createsInclusiveClippedRectanglesAndReportsBounds() {
        PixelSelection selection =
                PixelSelection.rectangle(5, 4, 3, 2, 1, 1);

        assertTrue(selection.contains(1, 1));
        assertTrue(selection.contains(3, 2));
        assertFalse(selection.contains(4, 2));
        assertEquals(
                new PixelSelection.Bounds(1, 1, 3, 2),
                selection.bounds().orElseThrow()
        );
    }

    @Test
    void rasterizesPolygonAndTranslationKeepsMaskInsideCanvas() {
        PixelSelection polygon = PixelSelection.polygon(
                5, 5,
                List.of(
                        new PixelSelection.Point(0, 0),
                        new PixelSelection.Point(4, 0),
                        new PixelSelection.Point(0, 4)
                )
        );

        assertTrue(polygon.contains(0, 0));
        assertTrue(polygon.contains(1, 1));
        assertFalse(polygon.contains(4, 4));

        PixelSelection translated = polygon.translated(2, 1);
        assertTrue(translated.contains(2, 1));
        assertTrue(translated.contains(3, 2));
        assertFalse(translated.contains(4, 2));
        assertFalse(translated.contains(0, 0));
    }
}
