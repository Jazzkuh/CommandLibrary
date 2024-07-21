package com.jazzkuh.commandlib.spigot;

import com.jazzkuh.commandlib.common.*;
import com.jazzkuh.commandlib.common.annotations.Main;
import com.jazzkuh.commandlib.common.annotations.Subcommand;
import com.jazzkuh.commandlib.common.exception.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class AnnotationCommand extends Command implements AnnotationCommandImpl {
    private final String commandName;
    private AnnotationSubCommand mainCommand = null;
    private final List<AnnotationSubCommand> subCommands = new ArrayList<>();

    public AnnotationCommand(String commandName) {
        super(commandName);
        this.commandName = commandName;

        List<Method> mainCommands = Arrays.stream(this.getClass().getMethods()).filter(method -> method.isAnnotationPresent(Main.class)).toList();
        if (mainCommands.size() > 1) {
            throw new IllegalArgumentException("There can only be one main command per class");
        }
        mainCommands.forEach(method -> this.mainCommand = AnnotationCommandParser.parse(this, method));

        List<Method> subcommandMethods = Arrays.stream(this.getClass().getMethods()).filter(method -> method.isAnnotationPresent(Subcommand.class)).toList();
        subcommandMethods.forEach(method -> this.subCommands.add(AnnotationCommandParser.parse(this, method)));
    }

    @Override
    public String getCommandName() {
        return this.commandName;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
        if (args.length < 1) {
            if (this.mainCommand == null) {
                this.formatUsage(sender);
                return true;
            }

            this.executeCommand(this.mainCommand, sender, args);
            return true;
        }

        for (AnnotationSubCommand subCommand : subCommands) {
            if (!args[0].equalsIgnoreCase(subCommand.getName()) && !subCommand.getAliases().contains(args[0].toLowerCase())) continue;
            this.executeCommand(subCommand, sender, args);
            return true;
        }

        this.executeCommand(this.mainCommand, sender, args);
        return true;
    }

    private void executeCommand(AnnotationSubCommand subCommand, CommandSender sender, String[] args) {
        if (subCommand.getPermission() != null && !sender.hasPermission(subCommand.getPermission())) {
            sender.sendMessage(Component.text("You do not have permission to use this command.", TextColor.fromHexString("#FB465C")));
            return;
        }

        AnnotationCommandExecutor<CommandSender> commandExecutor = new AnnotationCommandExecutor<>(subCommand, this);
        AnnotationCommandSender<CommandSender> commandSender = new AnnotationCommandSender<>(sender);

        try {
            commandExecutor.execute(commandSender, args);
        } catch (CommandException commandException) {
            if (commandException instanceof ArgumentException) {
                this.formatUsage(sender);
            } else if (commandException instanceof PermissionException permissionException) {
                sender.sendMessage(Component.text(permissionException.getMessage(), TextColor.fromHexString("#FB465C")));
            } else if (commandException instanceof ContextResolverException contextResolverException) {
                sender.sendMessage(Component.text("A context resolver was not found for: " + contextResolverException.getMessage(), TextColor.fromHexString("#FB465C")));
            } else if (commandException instanceof ParameterException parameterException) {
                sender.sendMessage(Component.text(parameterException.getMessage(), TextColor.fromHexString("#FB465C")));
            } else if (commandException instanceof ErrorException errorException) {
                sender.sendMessage(Component.text("An error occurred while executing this subcommand: " + errorException.getMessage(), TextColor.fromHexString("#FB465C")));
            }
        }
    }

    @Override
    @NotNull
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) {
        AnnotationCommandExecutor<CommandSender> mainCommandExecutor = new AnnotationCommandExecutor<>(this.mainCommand, this);
        AnnotationCommandSender<CommandSender> commandSender = new AnnotationCommandSender<>(sender);

        List<String> options = new ArrayList<>(mainCommandExecutor.complete(commandSender, args));

        if (args.length == 1 && this.subCommands.size() >= 1) {
            for (AnnotationSubCommand subCommand : this.subCommands) {
                if (subCommand.getPermission() == null) {
                    options.add(subCommand.getName());
                    continue;
                }

                if (commandSender.getSender().hasPermission(subCommand.getPermission())) options.add(subCommand.getName());
            }

            return StringUtil.copyPartialMatches(args[0], options, new ArrayList<>(options.size()));
        }

        for (AnnotationSubCommand subCommand : this.subCommands) {
            if (!args[0].equalsIgnoreCase(subCommand.getName()) && !subCommand.getAliases().contains(args[0].toLowerCase())) continue;
            AnnotationCommandExecutor<CommandSender> subCommandExecutor = new AnnotationCommandExecutor<>(subCommand, this);
            if (subCommand.getPermission() != null && !commandSender.getSender().hasPermission(subCommand.getPermission())) continue;
            options.addAll(subCommandExecutor.complete(commandSender, args));
        }
        return options;
    }


    public void register(JavaPlugin plugin) {
        try {
            if (this.mainCommand.getPermission() != null) {
                this.setPermission(this.mainCommand.getPermission());
                this.permissionMessage(Component.text("You do not have permission to use this command.", TextColor.fromHexString("#FB465C")));
            }
            this.setAliases(this.mainCommand.getAliases());

            Field bukkitCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            bukkitCommandMap.setAccessible(true);

            CommandMap commandMap = (CommandMap) bukkitCommandMap.get(Bukkit.getServer());
            commandMap.register(plugin.getName(), this);

            plugin.getLogger().info("Registered command: " + this.getCommandName());
            if (this.mainCommand.getAliases().size() > 0) {
                plugin.getLogger().info("- Registered aliases: " + String.join(", ", this.getAliases()));
            }
        } catch (Exception exception) {
            plugin.getLogger().severe("Unable to register command: " + this.getCommandName());
        }
    }

    protected void formatUsage(CommandSender sender) {
        if (mainCommand.getUsage() != null && this.mainCommand.getUsage().length() > 0 && this.subCommands.isEmpty()) {
            sender.sendMessage(Component.text("Invalid command syntax. Correct command syntax is: " + this.getCommandName() + this.mainCommand.getUsage(), TextColor.fromHexString("#FBFB00")));
            return;
        }

        sender.sendMessage(Component.text("Invalid command syntax. Correct command syntax's are:", TextColor.fromHexString("#FBFB00")));
        if (mainCommand.getUsage() != null && mainCommand.getUsage().length() > 0) {
            sender.sendMessage(Component.text("/" + this.getCommandName() + this.mainCommand.getUsage() + " - " + this.mainCommand.getDescription(), TextColor.fromHexString("#FBFB00")));
        }

        List<AnnotationSubCommand> sortedSubCommands = new ArrayList<>(this.subCommands);
        sortedSubCommands.sort(Comparator.comparingInt(cmd -> -(cmd.getName() + cmd.getUsage()).length()));

        for (AnnotationSubCommand subCommand : sortedSubCommands) {
            if (subCommand.getPermission() == null || sender.hasPermission(subCommand.getPermission())) {
                sender.sendMessage(Component.text("/" + this.getCommandName() + " " + subCommand.getName() + subCommand.getUsage() + " - " + subCommand.getDescription(), TextColor.fromHexString("#FBFB00")));
            }
        }
    }
}
