package com.jazzkuh.commandlib.minestom;

import com.jazzkuh.commandlib.common.chat.FormattingProvider;
import com.jazzkuh.commandlib.common.resolvers.CompletionResolverRegistry;
import com.jazzkuh.commandlib.common.resolvers.ContextResolverRegistry;
import com.jazzkuh.commandlib.minestom.resolvers.GameModeResolver;
import com.jazzkuh.commandlib.minestom.resolvers.PlayerResolver;
import com.jazzkuh.commandlib.minestom.terminal.MinestomTerminal;
import lombok.Getter;
import lombok.Setter;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;

public class MinestomCommandLoader {
    @Getter @Setter
    private static FormattingProvider formattingProvider;

    public static void startTerminal() {
        MinestomTerminal.start();
    }

    public static void stopTerminal() {
        MinestomTerminal.stop();
    }

    public static void loadResolvers() {
        ContextResolverRegistry.registerResolver(Player.class, new PlayerResolver());
        ContextResolverRegistry.registerResolver(GameMode.class, new GameModeResolver());

        CompletionResolverRegistry.registerResolver(Player.class, new PlayerResolver());
        CompletionResolverRegistry.registerCompletion("players", new PlayerResolver());
        CompletionResolverRegistry.registerResolver(GameMode.class, new GameModeResolver());
        CompletionResolverRegistry.registerCompletion("gamemodes", new GameModeResolver());
    }
}
