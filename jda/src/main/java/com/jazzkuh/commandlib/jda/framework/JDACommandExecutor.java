package com.jazzkuh.commandlib.jda.framework;

import com.jazzkuh.commandlib.common.AnnotationCommandImpl;
import com.jazzkuh.commandlib.common.AnnotationCommandSender;
import com.jazzkuh.commandlib.common.annotations.*;
import com.jazzkuh.commandlib.common.exception.*;
import com.jazzkuh.commandlib.common.resolvers.ContextResolver;
import com.jazzkuh.commandlib.common.resolvers.Resolvers;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;

public record JDACommandExecutor<T>(JDASubCommand subCommand, AnnotationCommandImpl annotationCommand) {
    public void execute(AnnotationCommandSender<T> sender, String[] args) throws CommandException {
        Method method = this.subCommand.getMethod();
        List<Parameter> parameters = Arrays.stream(this.subCommand.getMethod().getParameters()).toList();

        Object[] resolvedParameters = new Object[parameters.size()];
        resolvedParameters[0] = sender.getSender();

        int size = parameters.stream().filter(parameter -> !parameter.isAnnotationPresent(Optional.class)).toList().size();
        int paramSize = method.isAnnotationPresent(Main.class) ? size - 1 : size;

        if (args.length < paramSize) throw new ArgumentException();
        if (!method.getParameterTypes()[0].isInstance(sender.getSender())) throw new PermissionException("You are not allowed to execute this command.");

        for (int i = 1; i < parameters.size(); i++) {
            Parameter parameter = parameters.get(i);
            Class<?> paramClass = parameter.getType();
            ContextResolver<?> contextResolver = Resolvers.context(paramClass);
            if (contextResolver == null) {
                if (!paramClass.isEnum()) throw new ContextResolverException(paramClass.getName());

                int argumentIndex = method.isAnnotationPresent(Main.class) ? i - 1 : i;
                if (args.length <= argumentIndex && parameter.isAnnotationPresent(Optional.class)) {
                    resolvedParameters[i] = null;
                } else {
                    Object resolvedObject;
                    try {
                        resolvedObject = Enum.valueOf((Class<? extends Enum>) paramClass, method.isAnnotationPresent(Main.class) ? args[i - 1].toUpperCase() : args[i].toUpperCase());
                    } catch (Exception exception) {
                        throw new ParameterException("Cannot resolver parameter " + (method.isAnnotationPresent(Main.class) ? args[i - 1] : args[i]) + " for type " + paramClass.getSimpleName());
                    }

                    resolvedParameters[i] = resolvedObject;
                }
                continue;
            }

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
                if (resolvedObject == null) {
                    throw new ParameterException("Cannot resolver parameter " + (method.isAnnotationPresent(Main.class) ? args[i - 1] : args[i]) + " for type " + paramClass.getSimpleName());
                }
                resolvedParameters[i] = resolvedObject;
            }
        }

        try {
            method.setAccessible(true);
            method.invoke(this.annotationCommand, resolvedParameters);
        } catch (Exception exception) {
            exception.printStackTrace();
            throw new ErrorException(exception.getMessage());
        }
    }
}
