package com.jazzkuh.commandlib.spigot.resolvers;

import com.jazzkuh.commandlib.common.AnnotationCommandSender;
import com.jazzkuh.commandlib.common.resolvers.CompletionResolver;
import com.jazzkuh.commandlib.common.resolvers.ContextResolver;
import org.apache.commons.lang3.EnumUtils;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.List;

public final class GameModeResolver implements ContextResolver<GameMode>, CompletionResolver<CommandSender> {
    @Override
    public List<String> resolve(AnnotationCommandSender<CommandSender> sender, String s) {
        return Arrays.stream(GameMode.values()).toList().stream().map(gameMode -> gameMode.name().toLowerCase()).toList();
    }

    @Override
    public GameMode resolve(String arg) {
        if (!EnumUtils.isValidEnum(GameMode.class, arg.toUpperCase())) {
            return null;
        }

        return GameMode.valueOf(arg.toUpperCase());
    }
}