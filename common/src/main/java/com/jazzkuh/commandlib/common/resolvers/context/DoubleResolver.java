package com.jazzkuh.commandlib.common.resolvers.context;

import com.jazzkuh.commandlib.common.resolvers.ContextResolver;

public final class DoubleResolver implements ContextResolver<Double> {
    @Override
    public Double resolve(String args) {
        try {
            return Double.parseDouble(args);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}