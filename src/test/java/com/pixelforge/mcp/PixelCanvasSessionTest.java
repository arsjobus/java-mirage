package com.pixelforge.mcp;

import com.pixelforge.core.image.PixelImage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PixelCanvasSessionTest {

    @TempDir
    Path tempDirectory;

    @Test
    void exportsCurrentCanvasToTransparentPng() throws IOException {
        PixelImage image = new PixelImage(2, 2);
        image.setPixel(1, 0, 0xFFFF0000);
        image.setPixel(0, 1, 0x8000FF00);

        Path output = PixelCanvasSession.savePng(
                image, "sprite.png", tempDirectory
        );
        BufferedImage exported = ImageIO.read(output.toFile());

        assertEquals(2, exported.getWidth());
        assertEquals(2, exported.getHeight());
        assertEquals(0xFFFF0000, exported.getRGB(1, 0));
        assertEquals(0x8000FF00, exported.getRGB(0, 1));
        assertEquals(0x00000000, exported.getRGB(0, 0));
    }

    @Test
    void validatesWholePixelBatchBeforeUiAppliesIt() {
        PixelImage image = new PixelImage(2, 2);
        List<PixelCanvasSession.PixelChange> changes = List.of(
                new PixelCanvasSession.PixelChange(0, 0, 0xFFFF0000),
                new PixelCanvasSession.PixelChange(2, 0, 0xFF00FF00)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> PixelCanvasSession.validateChanges(image, changes)
        );
        assertEquals(0x00000000, image.getPixel(0, 0));
    }

    @Test
    void mirrorsPixelChangesAcrossOptionalSymmetryAxes() {
        PixelImage image = new PixelImage(8, 8);
        List<PixelCanvasSession.PixelChange> changes =
                PixelCanvasSession.symmetryChanges(
                        image,
                        List.of(new PixelCanvasSession.PixelChange(
                                1, 2, 0xFFFF0000
                        )),
                        new PixelCanvasSession.SymmetryOptions(
                                true, true, 4, 4, 1
                        )
                );

        assertEquals(Set.of("1,2", "6,2", "1,5", "6,5"),
                changes.stream()
                        .map(change -> change.x() + "," + change.y())
                        .collect(Collectors.toSet()));
        assertTrue(changes.stream().allMatch(
                change -> change.argb() == 0xFFFF0000
        ));
    }

    @Test
    void rotatesPixelChangesAroundDefaultCanvasCenter() {
        PixelImage image = new PixelImage(8, 8);
        List<PixelCanvasSession.PixelChange> changes =
                PixelCanvasSession.symmetryChanges(
                        image,
                        List.of(new PixelCanvasSession.PixelChange(
                                5, 3, 0xFF00FF00
                        )),
                        new PixelCanvasSession.SymmetryOptions(
                                false, false, null, null, 4
                        )
                );

        assertEquals(Set.of("5,3", "4,5", "2,4", "3,2"),
                changes.stream()
                        .map(change -> change.x() + "," + change.y())
                        .collect(Collectors.toSet()));
    }

    @Test
    void appliesSymmetryToRasterizedShapePixels() {
        PixelImage image = new PixelImage(8, 8);
        List<PixelCanvasSession.PixelChange> rectangle =
                PixelCanvasSession.shapeChanges(
                        image,
                        PixelCanvasSession.ShapeKind.RECTANGLE,
                        List.of(new PixelCanvasSession.Point(1, 1),
                                new PixelCanvasSession.Point(2, 2)),
                        0xFF0000FF,
                        true
                );

        List<PixelCanvasSession.PixelChange> changes =
                PixelCanvasSession.symmetryChanges(
                        image,
                        rectangle,
                        new PixelCanvasSession.SymmetryOptions(
                                true, false, 4, null, 1
                        )
                );

        assertEquals(Set.of(
                "1,1", "2,1", "1,2", "2,2",
                "5,1", "6,1", "5,2", "6,2"
        ), changes.stream()
                .map(change -> change.x() + "," + change.y())
                .collect(Collectors.toSet()));
    }

    @Test
    void appliesSymmetryToShapeBatchesLargerThanPixelToolLimit() {
        PixelImage image = new PixelImage(110, 100);
        List<PixelCanvasSession.PixelChange> rectangle =
                PixelCanvasSession.shapeChanges(
                        image,
                        PixelCanvasSession.ShapeKind.RECTANGLE,
                        List.of(new PixelCanvasSession.Point(0, 0),
                                new PixelCanvasSession.Point(109, 99)),
                        0xFF0000FF,
                        true
                );

        List<PixelCanvasSession.PixelChange> changes =
                PixelCanvasSession.symmetryChanges(
                        image, rectangle,
                        new PixelCanvasSession.SymmetryOptions(
                                false, false, null, null, 1
                        )
                );

        assertEquals(11_000, changes.size());
    }

    @Test
    void validatesSymmetryAxesAndRadialCopyLimit() {
        PixelImage image = new PixelImage(4, 4);
        List<PixelCanvasSession.PixelChange> changes = List.of(
                new PixelCanvasSession.PixelChange(1, 1, 0xFFFFFFFF)
        );

        assertThrows(IllegalArgumentException.class, () ->
                PixelCanvasSession.symmetryChanges(
                        image, changes,
                        new PixelCanvasSession.SymmetryOptions(
                                true, false, 5, null, 1
                        )
                )
        );
        assertThrows(IllegalArgumentException.class, () ->
                PixelCanvasSession.symmetryChanges(
                        image, changes,
                        new PixelCanvasSession.SymmetryOptions(
                                false, false, null, null,
                                PixelCanvasSession.MAX_RADIAL_COPIES + 1
                        )
                )
        );
    }

    @Test
    void rejectsUnsafeOutputFileNames() {
        PixelImage image = new PixelImage(1, 1);

        assertThrows(
                IllegalArgumentException.class,
                () -> PixelCanvasSession.savePng(
                        image, "../outside.png", tempDirectory
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> PixelCanvasSession.savePng(
                        image, "folder/sprite.png", tempDirectory
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> PixelCanvasSession.savePng(
                        image, "sprite.jpg", tempDirectory
                )
        );
    }

    @Test
    void limitsCanvasDimensions() {
        assertThrows(
                IllegalArgumentException.class,
                () -> PixelCanvasSession.validateDimensions(0, 1)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> PixelCanvasSession.validateDimensions(
                        PixelCanvasSession.MAX_DIMENSION + 1, 1
                )
        );
    }

    @Test
    void rasterizesFilledRectangleAndPolygonWithinCanvas() {
        PixelImage image = new PixelImage(6, 6);
        List<PixelCanvasSession.PixelChange> rectangle =
                PixelCanvasSession.shapeChanges(
                        image,
                        PixelCanvasSession.ShapeKind.RECTANGLE,
                        List.of(new PixelCanvasSession.Point(1, 1),
                                new PixelCanvasSession.Point(3, 3)),
                        0xFFFF0000,
                        true
                );
        assertEquals(9, rectangle.size());
        assertTrue(rectangle.stream().anyMatch(change ->
                change.x() == 2 && change.y() == 2));

        List<PixelCanvasSession.PixelChange> polygon =
                PixelCanvasSession.shapeChanges(
                        image,
                        PixelCanvasSession.ShapeKind.POLYGON,
                        List.of(new PixelCanvasSession.Point(1, 1),
                                new PixelCanvasSession.Point(4, 1),
                                new PixelCanvasSession.Point(2, 4)),
                        0xFF00FF00,
                        true
                );
        assertTrue(polygon.stream().anyMatch(change ->
                change.x() == 2 && change.y() == 2));
        assertTrue(polygon.stream().anyMatch(change ->
                change.x() == 2 && change.y() == 4));
        assertFalse(polygon.stream().anyMatch(change ->
                change.x() == 5 || change.y() == 5));
    }

    @Test
    void fillsPixelsTouchedBySlopedPolygonEdges() {
        PixelImage image = new PixelImage(32, 32);
        List<PixelCanvasSession.PixelChange> pot =
                PixelCanvasSession.shapeChanges(
                        image,
                        PixelCanvasSession.ShapeKind.POLYGON,
                        List.of(new PixelCanvasSession.Point(10, 18),
                                new PixelCanvasSession.Point(22, 18),
                                new PixelCanvasSession.Point(21, 24),
                                new PixelCanvasSession.Point(19, 29),
                                new PixelCanvasSession.Point(13, 29),
                                new PixelCanvasSession.Point(11, 24)),
                        0xFF54263D,
                        true
                );

        assertTrue(pot.stream().anyMatch(change ->
                change.x() == 20 && change.y() == 25));
    }

    @Test
    void rasterizesLineAndEllipseWithInclusiveEndpoints() {
        PixelImage image = new PixelImage(7, 7);
        List<PixelCanvasSession.PixelChange> line =
                PixelCanvasSession.shapeChanges(
                        image,
                        PixelCanvasSession.ShapeKind.LINE,
                        List.of(new PixelCanvasSession.Point(1, 1),
                                new PixelCanvasSession.Point(5, 5)),
                        0xFFFFFFFF,
                        false
                );
        assertEquals(5, line.size());
        assertTrue(line.stream().anyMatch(change ->
                change.x() == 5 && change.y() == 5));

        List<PixelCanvasSession.PixelChange> ellipse =
                PixelCanvasSession.shapeChanges(
                        image,
                        PixelCanvasSession.ShapeKind.ELLIPSE,
                        List.of(new PixelCanvasSession.Point(1, 1),
                                new PixelCanvasSession.Point(5, 5)),
                        0xFF0000FF,
                        true
                );
        assertTrue(ellipse.stream().anyMatch(change ->
                change.x() == 3 && change.y() == 3));
        assertTrue(ellipse.stream().anyMatch(change ->
                change.x() == 1 && change.y() == 3));
        assertFalse(ellipse.stream().anyMatch(change ->
                change.x() == 0 || change.y() == 0));

        List<PixelCanvasSession.PixelChange> oddSizedEllipse =
                PixelCanvasSession.shapeChanges(
                        image,
                        PixelCanvasSession.ShapeKind.ELLIPSE,
                        List.of(new PixelCanvasSession.Point(1, 1),
                                new PixelCanvasSession.Point(4, 4)),
                        0xFF0000FF,
                        true
                );
        Set<String> pixels = oddSizedEllipse.stream()
                .map(change -> change.x() + "," + change.y())
                .collect(Collectors.toSet());
        for (PixelCanvasSession.PixelChange change : oddSizedEllipse) {
            assertTrue(pixels.contains((5 - change.x()) + "," + change.y()));
            assertTrue(pixels.contains(change.x() + "," + (5 - change.y())));
        }
    }

    @Test
    void rejectsOutOfCanvasPointsAndInvalidShapePointCounts() {
        PixelImage image = new PixelImage(4, 4);
        assertThrows(
                IllegalArgumentException.class,
                () -> PixelCanvasSession.shapeChanges(
                        image,
                        PixelCanvasSession.ShapeKind.LINE,
                        List.of(new PixelCanvasSession.Point(0, 0),
                                new PixelCanvasSession.Point(4, 0)),
                        0xFFFFFFFF,
                        false
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> PixelCanvasSession.shapeChanges(
                        image,
                        PixelCanvasSession.ShapeKind.POLYGON,
                        List.of(new PixelCanvasSession.Point(0, 0),
                                new PixelCanvasSession.Point(3, 0)),
                        0xFFFFFFFF,
                        false
                )
        );
    }

    @Test
    void inspectsCanvasAsCoordinateLabeledColorGrid() {
        PixelImage image = new PixelImage(3, 2);
        image.setPixel(1, 0, 0xFFFF0000);
        image.setPixel(2, 1, 0x8000FF00);

        String inspection = PixelCanvasSession.inspectCanvas(
                image, null, null, null, null
        );

        assertTrue(inspection.contains("Canvas 3x2; region x=0, y=0, width=3, height=2"));
        assertTrue(inspection.contains("   0 .0."));
        assertTrue(inspection.contains("   1 ..1"));
        assertTrue(inspection.contains("0=#FFFF0000"));
        assertTrue(inspection.contains("1=#8000FF00"));
        assertTrue(inspection.contains(".=transparent"));
    }

    @Test
    void previewsEnlargedRegionWithPixelGridAndTransparencyCheckerboard()
            throws IOException {
        PixelImage image = new PixelImage(2, 1);
        image.setPixel(1, 0, 0xFFFF0000);

        byte[] png = PixelCanvasSession.previewRegionPng(
                image, 0, 0, 2, 1, 4
        );
        BufferedImage preview = ImageIO.read(
                new java.io.ByteArrayInputStream(png)
        );

        assertEquals(8, preview.getWidth());
        assertEquals(4, preview.getHeight());
        assertEquals(0xFFD8D8D8, preview.getRGB(2, 2));
        assertEquals(0xFFFF0000, preview.getRGB(6, 2));
        assertEquals(0x00000000, image.getPixel(0, 0));
        assertEquals(0xFFFF0000, image.getPixel(1, 0));
    }

    @Test
    void validatesRegionPreviewBoundsAndScale() {
        PixelImage image = new PixelImage(8, 8);

        assertThrows(IllegalArgumentException.class, () ->
                PixelCanvasSession.previewRegionPng(image, -1, 0, 2, 2, 8)
        );
        assertThrows(IllegalArgumentException.class, () ->
                PixelCanvasSession.previewRegionPng(image, 0, 0, 9, 2, 8)
        );
        assertThrows(IllegalArgumentException.class, () ->
                PixelCanvasSession.previewRegionPng(image, 0, 0, 2, 2, 17)
        );
    }

    @Test
    void requiresCompleteSmallInspectionRegion() {
        PixelImage image = new PixelImage(128, 128);
        assertThrows(
                IllegalArgumentException.class,
                () -> PixelCanvasSession.inspectCanvas(
                        image, null, null, null, null
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> PixelCanvasSession.inspectCanvas(
                        image, 0, 0, 64, null
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> PixelCanvasSession.inspectCanvas(
                        image, 100, 100, 32, 32
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> PixelCanvasSession.inspectCanvas(
                        image,
                        Integer.MAX_VALUE,
                        Integer.MAX_VALUE,
                        Integer.MAX_VALUE,
                        Integer.MAX_VALUE
                )
        );
    }
}
