package com.jazzkuh.commandlib.common;

import com.jazzkuh.commandlib.common.annotations.Completion;
import com.jazzkuh.commandlib.common.annotations.Greedy;
import com.jazzkuh.commandlib.common.annotations.Main;
import com.jazzkuh.commandlib.common.annotations.Optional;
import com.jazzkuh.commandlib.common.resolvers.CompletionResolver;
import com.jazzkuh.commandlib.common.resolvers.CompletionResolverRegistry;
import com.jazzkuh.commandlib.common.resolvers.ContextResolver;
import com.jazzkuh.commandlib.common.resolvers.ContextResolverRegistry;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.stream.StreamSupport;

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

    public List<String> complete(AnnotationCommandSender<T> sender, String[] args) {
        Method method = this.subCommand.method();
        List<Parameter> parameters = Arrays.stream(this.subCommand.method().getParameters()).toList();
        List<String> options = new ArrayList<>(Collections.emptyList());

        int paramSize = method.isAnnotationPresent(Main.class) ? parameters.size() - 1 : parameters.size();
        if (args.length > paramSize) return options;
        int index = args.length < 1 ? 0 : args.length - 1;

        int paramIndex = method.isAnnotationPresent(Main.class) ? index + 1 : index;
        if (paramIndex >= parameters.size() && paramIndex - 1 >= parameters.size()) return options;

        Parameter parameter = parameters.get(paramIndex >= parameters.size() ? paramIndex - 1 : paramIndex);
        Class<?> paramClass = parameter.getType();
        if (args.length < index + 1) return options;
        String arg = args[index];

        if (parameter.isAnnotationPresent(Completion.class)) {
            Completion completion = parameter.getAnnotation(Completion.class);
            CompletionResolver<T> resolver = CompletionResolverRegistry.getCompletion(completion.value());
            if (resolver != null) {
                return copyPartialMatches(arg, resolver.resolve(sender, arg), new ArrayList<>(resolver.resolve(sender, arg).size()));
            }
        }

        CompletionResolver<T> completionResolver = CompletionResolverRegistry.getResolver(paramClass);
        if (completionResolver != null) {
            return copyPartialMatches(arg, completionResolver.resolve(sender, arg), new ArrayList<>(completionResolver.resolve(sender, arg).size()));
        }

        return options;
    }

    private <S extends Collection<? super String>> S copyPartialMatches(final String token, final Iterable<String> originals, final S collection) throws UnsupportedOperationException, IllegalArgumentException {
        if (token == null) {
            collection.addAll(StreamSupport.stream(originals.spliterator(), false).toList());
            return collection;
        }

        for (String string : originals) {
            if (startsWithIgnoreCase(string, token)) {
                collection.add(string);
            }
        }

        return collection;
    }

    private boolean startsWithIgnoreCase(final String string, final String prefix) throws IllegalArgumentException, NullPointerException {
        if (string.length() < prefix.length()) {
            return false;
        }
        return string.regionMatches(true, 0, prefix, 0, prefix.length());
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
