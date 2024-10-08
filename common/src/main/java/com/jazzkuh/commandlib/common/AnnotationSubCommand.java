package com.jazzkuh.commandlib.common;

import lombok.Getter;

import java.lang.reflect.Method;
import java.util.List;

public record AnnotationSubCommand(@Getter String name, @Getter String usage, @Getter List<String> aliases,
                                   @Getter String description, @Getter String permission, @Getter Method method) {
}
