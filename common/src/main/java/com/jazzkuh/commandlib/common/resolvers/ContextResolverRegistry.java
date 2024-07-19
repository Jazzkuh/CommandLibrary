package com.jazzkuh.commandlib.common.resolvers;

import com.jazzkuh.commandlib.common.resolvers.context.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ContextResolverRegistry {
    private static final Map<Class<?>, ContextResolver<?>> resolvers = new HashMap<>();

    static {
        resolvers.put(String.class, new StringResolver());
        resolvers.put(Boolean.class, new BooleanResolver());
        resolvers.put(Integer.class, new IntegerResolver());
        resolvers.put(Double.class, new DoubleResolver());
        resolvers.put(Long.class, new LongResolver());
        resolvers.put(Float.class, new FloatResolver());
        resolvers.put(UUID.class, new UUIDResolver());
    }

    public static void registerResolver(Class<?> typeClass, ContextResolver<?> resolver) {
        resolvers.put(typeClass, resolver);
    }

    public static ContextResolver<?> getResolver(Class<?> typeClass) {
        if (typeClass.isArray()) {
            return resolvers.get(typeClass.getComponentType());
        }
        return resolvers.get(typeClass);
    }
}