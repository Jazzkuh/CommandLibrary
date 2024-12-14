package com.jazzkuh.commandlib.jda.framework;

import lombok.Getter;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

public record CommandParameter(@Getter String name, @Getter String description, @Getter boolean optional, @Getter Class<?> type, Parameter parameter) {
}
