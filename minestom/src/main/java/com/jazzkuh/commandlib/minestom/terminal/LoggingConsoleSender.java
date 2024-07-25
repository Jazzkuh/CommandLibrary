package com.jazzkuh.commandlib.minestom.terminal;

import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.command.ConsoleSender;
import org.jetbrains.annotations.NotNull;

public class LoggingConsoleSender extends ConsoleSender {
    private static final ComponentLogger LOGGER = ComponentLogger.logger("CommandLibrary");

    @Override
    public void sendMessage(@NotNull String message) {
        LOGGER.info(TerminalColorConverter.format(message));
    }

    @Override
    @SuppressWarnings("all")
    public void sendMessage(@NotNull Identity source, @NotNull Component message, @NotNull MessageType type) {
        LOGGER.info(TerminalColorConverter.format(PlainTextComponentSerializer.plainText().serialize(message)));
    }
}
