package com.jazzkuh.commandlib.spigot;

import com.jazzkuh.commandlib.common.resolvers.CompletionResolverRegistry;
import com.jazzkuh.commandlib.common.resolvers.ContextResolverRegistry;
import com.jazzkuh.commandlib.spigot.resolvers.GameModeResolver;
import com.jazzkuh.commandlib.spigot.resolvers.PlayerResolver;
import com.jazzkuh.commandlib.spigot.resolvers.WorldResolver;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class SpigotCommandLoader {
    public static void loadResolvers() {
        ContextResolverRegistry.registerResolver(Player.class, new PlayerResolver());
        ContextResolverRegistry.registerResolver(GameMode.class, new GameModeResolver());
        ContextResolverRegistry.registerResolver(World.class, new WorldResolver());

        CompletionResolverRegistry.registerResolver(Player.class, new PlayerResolver());
        CompletionResolverRegistry.registerResolver(GameMode.class, new GameModeResolver());
        CompletionResolverRegistry.registerResolver(World.class, new WorldResolver());
    }
}
