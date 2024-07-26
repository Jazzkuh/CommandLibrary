package com.jazzkuh.commandlib.jda.framework;

import lombok.Getter;

public record CommandParameter(@Getter String name, @Getter String description, @Getter boolean optional, @Getter Class<?> type) {
}
