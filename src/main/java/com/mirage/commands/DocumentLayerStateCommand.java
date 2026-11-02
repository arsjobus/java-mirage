package com.mirage.commands;

import com.mirage.core.document.Document;

import java.util.Objects;

public final class DocumentLayerStateCommand implements Command {

    private final Document document;
    private final Document.StateSnapshot before;
    private final Document.StateSnapshot after;

    public DocumentLayerStateCommand(
            Document document,
            Document.StateSnapshot before,
            Document.StateSnapshot after
    ) {
        this.document = Objects.requireNonNull(document, "document");
        this.before = Objects.requireNonNull(before, "before");
        this.after = Objects.requireNonNull(after, "after");
    }

    @Override
    public void execute() {
        document.restoreState(after);
    }

    @Override
    public void undo() {
        document.restoreState(before);
    }
}
