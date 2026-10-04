package com.pixelforge.commands;

import java.util.ArrayDeque;
import java.util.Deque;

public final class CommandManager {

    private final Deque<Command> undoStack = new ArrayDeque<>();
    private final Deque<Command> redoStack = new ArrayDeque<>();

    public void execute(Command command) {
        command.execute();
        undoStack.push(command);
        redoStack.clear();
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public void undo() {
        if (!canUndo()) return;
        Command command = undoStack.pop();
        command.undo();
        redoStack.push(command);
    }

    public void redo() {
        if (!canRedo()) return;
        Command command = redoStack.pop();
        command.execute();
        undoStack.push(command);
    }

    public void clear() {
        undoStack.clear();
        redoStack.clear();
    }
}
