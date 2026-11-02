package com.mirage.commands;

import com.mirage.core.document.Document;
import com.mirage.core.document.PixelSelection;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentSelectionCommandTest {

    @Test
    void undoAndRedoRestoreSelectionAndPixelChangesTogether() {
        Document document = new Document(2, 1);
        document.getImage().setPixel(0, 0, 0xFFFF0000);
        PixelSelection selection =
                PixelSelection.rectangle(2, 1, 0, 0, 0, 0);
        CommandManager commands = new CommandManager();
        DocumentSelectionCommand cut = new DocumentSelectionCommand(
                document,
                document.getImage(),
                selection,
                List.of(new SetPixelBatchCommand.PixelUpdate(0, 0, 0))
        );

        commands.execute(cut);
        assertEquals(0, document.getImage().getPixel(0, 0));
        assertTrue(document.getSelection().contains(0, 0));

        commands.undo();
        assertEquals(0xFFFF0000, document.getImage().getPixel(0, 0));
        assertNull(document.getSelection());

        commands.redo();
        assertEquals(0, document.getImage().getPixel(0, 0));
        assertTrue(document.getSelection().contains(0, 0));
    }
}
