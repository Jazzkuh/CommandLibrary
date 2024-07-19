package com.jazzkuh.commandlib.common;

public interface AnnotationCommandImpl {
    default String getCommandName() {
        return this.getClass().getName().toLowerCase();
    }
}
