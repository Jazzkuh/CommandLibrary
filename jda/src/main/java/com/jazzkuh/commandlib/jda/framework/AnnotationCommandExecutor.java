package com.jazzkuh.commandlib.jda.framework;

import com.jazzkuh.commandlib.common.AnnotationCommandImpl;
import com.jazzkuh.commandlib.common.AnnotationCommandSender;
import com.jazzkuh.commandlib.common.annotations.*;
import com.jazzkuh.commandlib.common.resolvers.ContextResolver;
import com.jazzkuh.commandlib.common.resolvers.ContextResolverRegistry;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;

public record AnnotationCommandExecutor<T>(AnnotationSubCommand subCommand, AnnotationCommandImpl annotationCommand) {
    public CommandResult execute(AnnotationCommandSender<T> sender, String[] args) {
        Method method = this.subCommand.getMethod();
        List<Parameter> parameters = Arrays.stream(this.subCommand.getMethod().getParameters()).toList();

        Object[] resolvedParameters = new Object[parameters.size()];
        resolvedParameters[0] = sender.getSender();

        int size = parameters.stream().filter(parameter -> !parameter.isAnnotationPresent(Optional.class)).toList().size();
        int paramSize = method.isAnnotationPresent(Main.class) ? size - 1 : size;

        if (args.length < paramSize) return CommandResult.NOT_ENOUGH_ARGUMENTS;
        if (!method.getParameterTypes()[0].isInstance(sender.getSender())) return CommandResult.NOT_ALLOWED;

        for (int i = 1; i < parameters.size(); i++) {
            Parameter parameter = parameters.get(i);
            Class<?> paramClass = parameter.getType();
            ContextResolver<?> contextResolver = ContextResolverRegistry.getResolver(paramClass);
            if (contextResolver == null) return CommandResult.CONTEXT_RESOLVER_NOT_FOUND;

            int argumentIndex = method.isAnnotationPresent(Main.class) ? i - 1 : i;
            if (args.length <= argumentIndex && parameter.isAnnotationPresent(Optional.class)) {
                resolvedParameters[i] = null;
            } else {
                if (parameter.isAnnotationPresent(Greedy.class) && paramClass == String.class) {
                    String[] array = Arrays.copyOfRange(args, argumentIndex, args.length);
                    resolvedParameters[i] = String.join(" ", array);
                    continue;
                }

                Object resolvedObject = contextResolver.resolve(method.isAnnotationPresent(Main.class) ? args[i - 1] : args[i]);
                if (resolvedObject == null) return CommandResult.PARAMETER_INVALID;
                resolvedParameters[i] = resolvedObject;
            }
        }

        try {
            method.setAccessible(true);
            method.invoke(this.annotationCommand, resolvedParameters);
            return CommandResult.SUCCESS;
        } catch (Exception exception) {
            exception.printStackTrace();
            return CommandResult.ERROR;
        }
    }

    public enum CommandResult {
        SUCCESS,
        NOT_ENOUGH_ARGUMENTS,
        NOT_ALLOWED,
        CONTEXT_RESOLVER_NOT_FOUND,
        PARAMETER_INVALID,
        ERROR
    }
}
