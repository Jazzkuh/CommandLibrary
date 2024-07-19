package com.jazzkuh.commandlib.common.resolvers.context;

import com.jazzkuh.commandlib.common.resolvers.ContextResolver;

public final class FloatResolver implements ContextResolver<Float> {
    @Override
    public Float resolve(String args) {
        try {
            return Float.parseFloat(args);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}