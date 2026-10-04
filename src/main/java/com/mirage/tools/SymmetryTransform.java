package com.mirage.tools;

import java.util.LinkedHashSet;
import java.util.Set;

public final class SymmetryTransform {

    private SymmetryTransform() {
    }

    public static Set<Point> pointsFor(
            int x,
            int y,
            int width,
            int height,
            boolean mirrorVertical,
            boolean mirrorHorizontal,
            int centerX,
            int centerY,
            int radialCopies
    ) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException(
                    "Image dimensions must be positive"
            );
        }
        if (radialCopies < 1) {
            throw new IllegalArgumentException(
                    "Radial copies must be at least one"
            );
        }
        if (centerX < 0 || centerX > width
                || centerY < 0 || centerY > height) {
            throw new IllegalArgumentException(
                    "Symmetry center must be within the image"
            );
        }
        if (x < 0 || x >= width || y < 0 || y >= height) {
            throw new IllegalArgumentException("Point must be inside image");
        }

        Set<Point> points = new LinkedHashSet<>();
        int horizontalVariants = mirrorVertical ? 2 : 1;
        int verticalVariants = mirrorHorizontal ? 2 : 1;

        for (int horizontal = 0; horizontal < horizontalVariants;
             horizontal++) {
            int mirroredX = horizontal == 0 ? x : 2 * centerX - x - 1;
            for (int vertical = 0; vertical < verticalVariants; vertical++) {
                int mirroredY = vertical == 0 ? y : 2 * centerY - y - 1;
                double axisX = centerX;
                double axisY = centerY;
                double dx = mirroredX + 0.5 - axisX;
                double dy = mirroredY + 0.5 - axisY;
                for (int copy = 0; copy < radialCopies; copy++) {
                    double angle = 2.0 * Math.PI * copy / radialCopies;
                    int transformedX = (int) Math.round(
                            axisX + dx * Math.cos(angle)
                                    - dy * Math.sin(angle) - 0.5
                    );
                    int transformedY = (int) Math.round(
                            axisY + dx * Math.sin(angle)
                                    + dy * Math.cos(angle) - 0.5
                    );
                    if (transformedX >= 0 && transformedX < width
                            && transformedY >= 0 && transformedY < height) {
                        points.add(new Point(transformedX, transformedY));
                    }
                }
            }
        }
        return points;
    }

    public record Point(int x, int y) {
    }
}
