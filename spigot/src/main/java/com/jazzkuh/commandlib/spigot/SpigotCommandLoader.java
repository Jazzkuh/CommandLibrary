package com.jazzkuh.commandlib.spigot;

import com.jazzkuh.commandlib.common.chat.FormattingProvider;
import com.jazzkuh.commandlib.common.resolvers.Resolvers;
import com.jazzkuh.commandlib.spigot.resolvers.GameModeResolver;
import com.jazzkuh.commandlib.spigot.resolvers.PlayerResolver;
import com.jazzkuh.commandlib.spigot.resolvers.WorldResolver;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class SpigotCommandLoader {
    @Getter
    @Setter
    private static FormattingProvider formattingProvider;

    public static void loadResolvers() {
        Resolvers.register(Player.class, new PlayerResolver());
        Resolvers.register(GameMode.class, new GameModeResolver());
        Resolvers.register(World.class, new WorldResolver());
    }
}
