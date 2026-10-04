package com.pixelforge.tools;

import com.pixelforge.commands.CommandManager;
import com.pixelforge.commands.SetPixelBatchCommand;
import com.pixelforge.core.image.PixelImage;

import java.util.ArrayList;
import java.util.List;

public final class FillBucketTool implements Tool {

    @Override
    public String getName() {
        return "Fill Bucket";
    }

    public int fill(
            PixelImage image,
            CommandManager commands,
            int x,
            int y,
            int color
    ) {
        if (x < 0 || y < 0 || x >= image.getWidth() || y >= image.getHeight()) {
            throw new IllegalArgumentException(
                    "Fill start is outside canvas: (" + x + ", " + y + ")"
            );
        }

        int targetColor = image.getPixel(x, y);
        if (targetColor == color) {
            return 0;
        }

        int width = image.getWidth();
        int height = image.getHeight();
        boolean[] visited = new boolean[width * height];
        int[] queue = new int[width * height];
        List<SetPixelBatchCommand.PixelUpdate> updates = new ArrayList<>();

        int start = y * width + x;
        int queueEnd = 0;
        queue[queueEnd++] = start;
        visited[start] = true;

        for (int queueStart = 0; queueStart < queueEnd; queueStart++) {
            int index = queue[queueStart];
            int pixelX = index % width;
            int pixelY = index / width;
            updates.add(new SetPixelBatchCommand.PixelUpdate(
                    pixelX, pixelY, color
            ));

            if (pixelX > 0) {
                queueEnd = enqueueIfMatching(
                        image, index - 1, targetColor, visited, queue, queueEnd
                );
            }
            if (pixelX + 1 < width) {
                queueEnd = enqueueIfMatching(
                        image, index + 1, targetColor, visited, queue, queueEnd
                );
            }
            if (pixelY > 0) {
                queueEnd = enqueueIfMatching(
                        image, index - width, targetColor,
                        visited, queue, queueEnd
                );
            }
            if (pixelY + 1 < height) {
                queueEnd = enqueueIfMatching(
                        image, index + width, targetColor,
                        visited, queue, queueEnd
                );
            }
        }

        commands.execute(new SetPixelBatchCommand(image, updates));
        return updates.size();
    }

    private int enqueueIfMatching(
            PixelImage image,
            int index,
            int targetColor,
            boolean[] visited,
            int[] queue,
            int queueEnd
    ) {
        if (!visited[index]
                && image.getPixel(index % image.getWidth(),
                index / image.getWidth()) == targetColor) {
            visited[index] = true;
            queue[queueEnd++] = index;
        }
        return queueEnd;
    }
}
