package com.mirage.commands;

import com.mirage.core.document.Document;
import com.mirage.core.document.PixelSelection;
import com.mirage.core.image.PixelImage;

import java.util.List;
import java.util.Objects;

/**
 * Applies pixel edits and a selection change as one undoable operation.
 */
public final class DocumentSelectionCommand implements Command {

    private final Document document;
    private final PixelImage image;
    private final PixelSelection before;
    private final PixelSelection after;
    private final List<SetPixelBatchCommand.PixelUpdate> updates;
    private final int[] oldColors;

    public DocumentSelectionCommand(
            Document document,
            PixelImage image,
            PixelSelection after,
            List<SetPixelBatchCommand.PixelUpdate> updates
    ) {
        this.document = Objects.requireNonNull(document, "document");
        this.image = Objects.requireNonNull(image, "image");
        this.before = document.getSelection();
        this.after = after;
        this.updates = List.copyOf(updates);
        if (image.getWidth() != document.getWidth()
                || image.getHeight() != document.getHeight()) {
            throw new IllegalArgumentException(
                    "Edited image dimensions must match the document"
            );
        }
        if (after != null
                && (after.getWidth() != document.getWidth()
                || after.getHeight() != document.getHeight())) {
            throw new IllegalArgumentException(
                    "Selection dimensions must match the document"
            );
        }
        oldColors = new int[this.updates.size()];
        for (int index = 0; index < this.updates.size(); index++) {
            SetPixelBatchCommand.PixelUpdate update = this.updates.get(index);
            oldColors[index] = image.getPixel(update.x(), update.y());
        }
    }

    @Override
    public void execute() {
        for (SetPixelBatchCommand.PixelUpdate update : updates) {
            image.setPixel(update.x(), update.y(), update.argb());
        }
        document.setSelection(after);
    }

    @Override
    public void undo() {
        for (int index = updates.size() - 1; index >= 0; index--) {
            SetPixelBatchCommand.PixelUpdate update = updates.get(index);
            image.setPixel(update.x(), update.y(), oldColors[index]);
        }
        document.setSelection(before);
    }
}
