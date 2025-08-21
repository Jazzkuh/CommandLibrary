package com.jazzkuh.commandlib.brigadier.utils.provider;

import com.jazzkuh.commandlib.brigadier.utils.source.CommandSource;
import net.kyori.adventure.text.Component;

public interface CommandSourceProvider {

    void sendMessage(CommandSource<?> source, Component message);

    boolean hasPermission(CommandSource<?> source, String permission);
}
