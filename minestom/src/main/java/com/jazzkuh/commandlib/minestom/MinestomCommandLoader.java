package com.jazzkuh.commandlib.minestom;

import com.jazzkuh.commandlib.common.chat.FormattingProvider;
import com.jazzkuh.commandlib.common.resolvers.Resolvers;
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
        Resolvers.register(Player.class, new PlayerResolver(), "players");
        Resolvers.register(GameMode.class, new GameModeResolver(), "gamemodes");
    }
}
