package com.jazzkuh.commandlib.common.resolvers.context;

import com.jazzkuh.commandlib.common.resolvers.ContextResolver;

public final class IntegerResolver implements ContextResolver<Integer> {
    @Override
    public Integer resolve(String args) {
        try {
            return Integer.parseInt(args);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}