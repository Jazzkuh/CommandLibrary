package com.jazzkuh.commandlib.common.resolvers.context;

import com.jazzkuh.commandlib.common.resolvers.ContextResolver;

public final class StringResolver implements ContextResolver<String> {
    @Override
    public String resolve(String args) {
        return args;
    }
}