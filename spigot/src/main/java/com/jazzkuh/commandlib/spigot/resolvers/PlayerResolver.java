package com.jazzkuh.commandlib.spigot.resolvers;

import com.jazzkuh.commandlib.common.AnnotationCommandSender;
import com.jazzkuh.commandlib.common.resolvers.CompletionResolver;
import com.jazzkuh.commandlib.common.resolvers.ContextResolver;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public final class PlayerResolver implements ContextResolver<Player>, CompletionResolver<CommandSender> {
    @Override
    public List<String> resolve(AnnotationCommandSender<CommandSender> sender, String s) {
        return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
    }

    @Override
    public Player resolve(String args) {
        return Bukkit.getPlayer(args);
    }
}