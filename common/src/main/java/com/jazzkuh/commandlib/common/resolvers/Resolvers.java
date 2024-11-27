package com.jazzkuh.commandlib.common.resolvers;

import com.jazzkuh.commandlib.common.resolvers.context.*;
import lombok.experimental.UtilityClass;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@UtilityClass
public class Resolvers {
    private static final Map<Class<?>, ContextResolver<?>> contextResolvers = new HashMap<>();
    private static final Map<Class<?>, CompletionResolver<?>> completionResolvers = new HashMap<>();
    private static final Map<String, CompletionResolver<?>> completionStrings = new HashMap<>();

    static {
        contextResolvers.put(String.class, new StringResolver());
        contextResolvers.put(Boolean.class, new BooleanResolver());
        contextResolvers.put(Integer.class, new IntegerResolver());
        contextResolvers.put(Double.class, new DoubleResolver());
        contextResolvers.put(Long.class, new LongResolver());
        contextResolvers.put(Float.class, new FloatResolver());
        contextResolvers.put(UUID.class, new UUIDResolver());
    }

    public static void register(Class<?> typeClass, Object resolver, String... completions) {
        if (resolver instanceof ContextResolver) {
            contextResolvers.put(typeClass, (ContextResolver<?>) resolver);
        }

        if (resolver instanceof CompletionResolver) {
            completionResolvers.put(typeClass, (CompletionResolver<?>) resolver);
        }

        for (String completion : completions) {
            completionStrings.put(completion, (CompletionResolver<?>) resolver);
        }
    }

    public static void register(Object resolver, String... completions) {
        for (String completion : completions) {
            completionStrings.put(completion, (CompletionResolver<?>) resolver);
        }
    }

    public static ContextResolver<?> context(Class<?> typeClass) {
        if (typeClass.isArray()) {
            return contextResolvers.get(typeClass.getComponentType());
        }
        return contextResolvers.get(typeClass);
    }

    public static <T> CompletionResolver<T> completion(Class<?> typeClass) {
        return (CompletionResolver<T>) completionResolvers.get(typeClass);
    }

    public static <T> CompletionResolver<T> completion(String name) {
        name = name.startsWith("@") ? name.substring(1) : name;
        return (CompletionResolver<T>) completionStrings.get(name);
    }
}