package com.jazzkuh.commandlib.common;

import lombok.Getter;

public record AnnotationCommandSender<T>(@Getter T sender) {
}
