package com.jazzkuh.commandlib.brigadier;

import com.jazzkuh.commandlib.brigadier.utils.provider.CommandSourceProvider;
import com.jazzkuh.commandlib.common.chat.FormattingProvider;
import lombok.Getter;
import lombok.Setter;

public class BrigadierCommandLoader<S> {
    @Getter @Setter
    private static FormattingProvider formattingProvider;

    @Getter @Setter
    private CommandSourceProvider<S> commandSourceProvider;
}
