package com.jazzkuh.commandlib.minestom.resolvers;

import com.jazzkuh.commandlib.common.AnnotationCommandSender;
import com.jazzkuh.commandlib.common.resolvers.CompletionResolver;
import com.jazzkuh.commandlib.common.resolvers.ContextResolver;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandSender;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;

import java.util.ArrayList;
import java.util.List;

public final class PlayerResolver implements ContextResolver<Player>, CompletionResolver<CommandSender> {
    @Override
    public List<String> resolve(AnnotationCommandSender<CommandSender> sender, String s) {
        List<Player> players = new ArrayList<>();
        for (Instance instance : MinecraftServer.getInstanceManager().getInstances()) {
            players.addAll(instance.getPlayers());
        }

        return players.stream().map(Player::getUsername).toList();
    }

    @Override
    public Player resolve(String args) {
        List<Player> players = new ArrayList<>();
        for (Instance instance : MinecraftServer.getInstanceManager().getInstances()) {
            players.addAll(instance.getPlayers());
        }

        return players.stream().filter(player -> player.getUsername().equalsIgnoreCase(args)).findFirst().orElse(null);
    }
}