package com.jazzkuh.commandlib.common.resolvers.context;

import com.jazzkuh.commandlib.common.resolvers.ContextResolver;

public final class LongResolver implements ContextResolver<Long> {
    @Override
    public Long resolve(String args) {
        try {
            return Long.parseLong(args);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}