package com.jazzkuh.commandlib.common.resolvers.context;

import com.jazzkuh.commandlib.common.resolvers.ContextResolver;

import java.util.UUID;

public final class UUIDResolver implements ContextResolver<UUID> {
    @Override
    public UUID resolve(String args) {
        try {
            return UUID.fromString(args);
        } catch (Exception e) {
            return null;
        }
    }
}