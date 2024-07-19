package com.jazzkuh.commandlib.common;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

public record CommandArgument(@Getter String arguments, @Getter String description, @Getter String permission) {
    public CommandArgument(String arguments, String description, @Nullable String permission) {
        this.arguments = arguments;
        this.description = description;
        this.permission = permission;
    }
}
