package com.mirage.tools;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SymmetryTransformTest {

    @Test
    void mirrorsAcrossSelectedVerticalAndHorizontalAxes() {
        Set<SymmetryTransform.Point> points = SymmetryTransform.pointsFor(
                2, 1, 8, 8, true, true, 3, 2, 1
        );

        assertEquals(Set.of(
                new SymmetryTransform.Point(2, 1),
                new SymmetryTransform.Point(3, 1),
                new SymmetryTransform.Point(2, 2),
                new SymmetryTransform.Point(3, 2)
        ), points);
    }

    @Test
    void radialCopiesRotateAroundSelectedCenter() {
        Set<SymmetryTransform.Point> points = SymmetryTransform.pointsFor(
                4, 3, 9, 9, false, false, 4, 4, 4
        );

        assertEquals(Set.of(
                new SymmetryTransform.Point(4, 3),
                new SymmetryTransform.Point(4, 4),
                new SymmetryTransform.Point(3, 4),
                new SymmetryTransform.Point(3, 3)
        ), points);
    }

    @Test
    void clipsTransformedPointsOutsideImage() {
        Set<SymmetryTransform.Point> points = SymmetryTransform.pointsFor(
                0, 2, 5, 5, true, false, 2, 2, 1
        );

        assertEquals(Set.of(
                new SymmetryTransform.Point(0, 2),
                new SymmetryTransform.Point(3, 2)
        ), points);
    }

    @Test
    void rejectsInvalidPointsAndRadialCopyCounts() {
        assertThrows(IllegalArgumentException.class, () ->
                SymmetryTransform.pointsFor(
                        2, 0, 2, 2, false, false, 0, 0, 1
                )
        );
        assertThrows(IllegalArgumentException.class, () ->
                SymmetryTransform.pointsFor(
                        0, 0, 2, 2, false, false, 0, 0, 0
                )
        );
        assertThrows(IllegalArgumentException.class, () ->
                SymmetryTransform.pointsFor(
                        0, 0, 2, 2, false, false, 3, 0, 1
                )
        );
    }
}
