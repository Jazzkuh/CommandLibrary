package com.jazzkuh.commandlib.common.resolvers.context;

import com.jazzkuh.commandlib.common.resolvers.ContextResolver;

public final class BooleanResolver implements ContextResolver<Boolean> {
    @Override
    public Boolean resolve(String args) {
        try {
            return Boolean.parseBoolean(args);
        } catch (Exception e) {
            return null;
        }
    }
}