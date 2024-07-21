package com.jazzkuh.commandlib.minestom;

import com.jazzkuh.commandlib.common.resolvers.CompletionResolverRegistry;
import com.jazzkuh.commandlib.common.resolvers.ContextResolverRegistry;
import com.jazzkuh.commandlib.minestom.resolvers.GameModeResolver;
import com.jazzkuh.commandlib.minestom.resolvers.PlayerResolver;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;

public class MinestomCommandLoader {
    public static void loadResolvers() {
        ContextResolverRegistry.registerResolver(Player.class, new PlayerResolver());
        ContextResolverRegistry.registerResolver(GameMode.class, new GameModeResolver());

        CompletionResolverRegistry.registerResolver(Player.class, new PlayerResolver());
        CompletionResolverRegistry.registerCompletion("players", new PlayerResolver());
        CompletionResolverRegistry.registerResolver(GameMode.class, new GameModeResolver());
        CompletionResolverRegistry.registerCompletion("gamemodes", new GameModeResolver());
    }
}
