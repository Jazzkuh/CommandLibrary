package com.jazzkuh.commandlib.jda.framework;

import lombok.Getter;
import net.dv8tion.jda.api.Permission;

import java.lang.reflect.Method;
import java.util.List;

public record JDASubCommand(@Getter String name, @Getter String usage, @Getter List<String> aliases,
                            @Getter String description, @Getter Permission permission, @Getter Method method, @Getter List<CommandParameter> commandParameters) {
}
