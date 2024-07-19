package com.jazzkuh.commandlib.common.resolvers;

import java.util.HashMap;
import java.util.Map;

public class CompletionResolverRegistry {
    private static final Map<Class<?>, CompletionResolver<?>> typeResolvers = new HashMap<>();
    private static final Map<String, CompletionResolver<?>> completions = new HashMap<>();

    public static void registerCompletion(String name, CompletionResolver<?> resolver) {
        completions.put(name, resolver);
    }

    public static <T> CompletionResolver<T> getCompletion(String name) {
        name = name.startsWith("@") ? name.substring(1) : name;
        return (CompletionResolver<T>) completions.get(name);
    }

    public static void registerResolver(Class<?> typeClass, CompletionResolver<?> resolver) {
        typeResolvers.put(typeClass, resolver);
    }

    public static <T> CompletionResolver<T> getResolver(Class<?> typeClass) {
        return (CompletionResolver<T>) typeResolvers.get(typeClass);
    }
}