package com.mirage.commands;

public interface Command {
    void execute();
    void undo();

    default String description() {
        return getClass().getSimpleName();
    }
}
