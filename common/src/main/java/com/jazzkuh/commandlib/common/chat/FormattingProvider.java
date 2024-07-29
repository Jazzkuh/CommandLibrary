package com.jazzkuh.commandlib.common.chat;

import com.jazzkuh.commandlib.common.exception.CommandException;
import net.kyori.adventure.text.Component;

public interface FormattingProvider {
    Component formatError(CommandException commandException, String message);
}
