package com.mirage.commands;

import com.mirage.core.document.Document;
import com.mirage.core.document.Layer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class DocumentLayerStateCommandTest {

    @Test
    void undoesAndRedoesLayerChangesAlongsidePixelCommands() {
        Document document = new Document(1, 1);
        Layer original = document.getActiveLayer();
        original.getImage().setPixel(0, 0, 0xFFFF0000);
        CommandManager commands = new CommandManager();
        commands.execute(new SetPixelCommand(
                original.getImage(), 0, 0, 0xFF00FF00
        ));

        Document.StateSnapshot before = document.snapshotState();
        Layer added = document.addLayer("Added");
        Document.StateSnapshot after = document.snapshotState();
        commands.execute(new DocumentLayerStateCommand(
                document, before, after
        ));

        commands.undo();
        assertEquals(1, document.getLayers().size());
        assertSame(original, document.getActiveLayer());
        commands.undo();
        assertEquals(0xFFFF0000, original.getImage().getPixel(0, 0));

        commands.redo();
        assertEquals(0xFF00FF00, original.getImage().getPixel(0, 0));
        commands.redo();
        assertEquals(2, document.getLayers().size());
        assertSame(added, document.getActiveLayer());
    }

    @Test
    void undoRestoresLayerPropertiesAndOrder() {
        Document document = new Document(1, 1);
        Layer original = document.getActiveLayer();
        Layer added = document.addLayer("Added");
        Document.StateSnapshot before = document.snapshotState();

        added.setName("Renamed");
        added.setVisible(false);
        added.setLocked(true);
        added.setOpacity(0.25);
        document.moveLayer(added, 0);
        Document.StateSnapshot after = document.snapshotState();

        CommandManager commands = new CommandManager();
        commands.execute(new DocumentLayerStateCommand(
                document, before, after
        ));
        commands.undo();

        assertEquals("Added", added.getName());
        assertEquals(1.0, added.getOpacity());
        assertEquals(1, document.getLayers().indexOf(added));
        assertSame(added, document.getActiveLayer());

        commands.redo();
        assertEquals("Renamed", added.getName());
        assertEquals(0.25, added.getOpacity());
        assertEquals(0, document.getLayers().indexOf(added));
        assertSame(original, document.getLayers().get(1));
    }
}
