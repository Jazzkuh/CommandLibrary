package com.jazzkuh.commandlib.spigot.resolvers;

import com.jazzkuh.commandlib.common.AnnotationCommandSender;
import com.jazzkuh.commandlib.common.resolvers.CompletionResolver;
import com.jazzkuh.commandlib.common.resolvers.ContextResolver;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

import java.util.List;

public final class WorldResolver implements ContextResolver<World>, CompletionResolver<CommandSender> {
    @Override
    public List<String> resolve(AnnotationCommandSender<CommandSender> sender, String s) {
        return Bukkit.getWorlds().stream().map(World::getName).toList();
    }

    @Override
    public World resolve(String args) {
        return Bukkit.getWorld(args);
    }
}