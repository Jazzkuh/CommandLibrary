package com.jazzkuh.commandlib.common;

import com.jazzkuh.commandlib.common.annotations.*;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AnnotationCommandParser {
    public static AnnotationSubCommand parse(AnnotationCommandImpl baseCommand, Method method) {
        String name = null;
        List<String> aliases = new ArrayList<>();
        String permission = null;
        String description = "No description provided";
        String usage = "";

        if (method.isAnnotationPresent(Main.class)) name = baseCommand.getCommandName();
        else if (method.isAnnotationPresent(Subcommand.class)) name = method.getAnnotation(Subcommand.class).value();

        if (method.isAnnotationPresent(Alias.class)) {
            Alias alias = method.getAnnotation(Alias.class);
            if (alias.value().contains("|")) aliases = Arrays.asList(alias.value().split("\\|"));
            else if (!alias.value().isEmpty()) aliases.add(alias.value());
        }

        if (method.isAnnotationPresent(Description.class)) description = method.getAnnotation(Description.class).value();
        if (method.isAnnotationPresent(Permission.class)) permission = method.getAnnotation(Permission.class).value();
        if (method.isAnnotationPresent(Usage.class)) usage = method.getAnnotation(Usage.class).value();

        List<Parameter> parameters = Arrays.stream(method.getParameters()).toList();
        if (!method.isAnnotationPresent(Usage.class)) {
            StringBuilder paramUsage = new StringBuilder();
            for (int i = 1; i < parameters.size(); i++) {
                Parameter parameter = parameters.get(i);
                paramUsage.append("<").append(parameter.getName());
                if (parameter.getType().isArray()) {
                    paramUsage.append("...");
                }
                paramUsage.append(">");

                if (i != parameters.size() - 1) {
                    paramUsage.append(" ");
                }
            }

            usage = paramUsage.toString();
        }
        usage = !usage.isEmpty() ? " " + usage : "";
        return new AnnotationSubCommand(name, usage, aliases, description, permission, method);
    }
}
