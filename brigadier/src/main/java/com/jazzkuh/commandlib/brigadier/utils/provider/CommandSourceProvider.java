package com.jazzkuh.commandlib.brigadier.utils.provider;

import com.jazzkuh.commandlib.brigadier.utils.source.CommandSource;
import net.kyori.adventure.text.Component;

public interface CommandSourceProvider<S> {

    void sendMessage(CommandSource<S> source, Component message);

    boolean hasPermission(CommandSource<S> source, String permission);
}
