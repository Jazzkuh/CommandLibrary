package com.jazzkuh.commandlib.common.resolvers;

public interface ContextResolver<T> {
    T resolve(String arg);
}
