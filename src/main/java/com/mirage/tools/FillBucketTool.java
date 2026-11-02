package com.mirage.tools;

import com.mirage.commands.CommandManager;
import com.mirage.commands.SetPixelBatchCommand;
import com.mirage.core.document.PixelSelection;
import com.mirage.core.image.PixelImage;

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
        return fill(image, commands, x, y, color, null);
    }

    public int fill(
            PixelImage image,
            CommandManager commands,
            int x,
            int y,
            int color,
            PixelSelection selection
    ) {
        if (x < 0 || y < 0 || x >= image.getWidth() || y >= image.getHeight()) {
            throw new IllegalArgumentException(
                    "Fill start is outside canvas: (" + x + ", " + y + ")"
            );
        }
        if (selection != null && !selection.contains(x, y)) {
            return 0;
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
                        image, index - 1, targetColor, selection,
                        visited, queue, queueEnd
                );
            }
            if (pixelX + 1 < width) {
                queueEnd = enqueueIfMatching(
                        image, index + 1, targetColor, selection,
                        visited, queue, queueEnd
                );
            }
            if (pixelY > 0) {
                queueEnd = enqueueIfMatching(
                        image, index - width, targetColor,
                        selection, visited, queue, queueEnd
                );
            }
            if (pixelY + 1 < height) {
                queueEnd = enqueueIfMatching(
                        image, index + width, targetColor,
                        selection, visited, queue, queueEnd
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
            PixelSelection selection,
            boolean[] visited,
            int[] queue,
            int queueEnd
    ) {
        if (!visited[index]
                && (selection == null || selection.contains(
                    index % image.getWidth(), index / image.getWidth()
                ))
                && image.getPixel(index % image.getWidth(),
                index / image.getWidth()) == targetColor) {
            visited[index] = true;
            queue[queueEnd++] = index;
        }
        return queueEnd;
    }
}
