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

    protected final String commandName;
    protected final List<AnnotationSubCommand> mainCommands = new ArrayList<>();
    protected final List<AnnotationSubCommand> subCommands = new ArrayList<>();

    public AnnotationCommand(String commandName) {
        super(commandName);

        this.commandName = commandName;
        this.setName(commandName);
        this.init();
    }

    public AnnotationCommand() {
        super("__annotation_command__");
        if (!this.getClass().isAnnotationPresent(com.jazzkuh.commandlib.common.annotations.Command.class)) {
            throw new IllegalArgumentException("AnnotationCommand needs to have a @Command annotation!");
        }

        this.commandName = this.getClass().getAnnotation(com.jazzkuh.commandlib.common.annotations.Command.class).value();
        this.setName(this.commandName);
        this.init();
    }

    private void init() {
        List<Method> mainCommandMethods = Arrays.stream(this.getClass().getMethods()).filter(method -> method.isAnnotationPresent(Main.class)).toList();
        mainCommandMethods.forEach(method -> this.mainCommands.add(AnnotationCommandParser.parse(this, method)));

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
            if (this.mainCommands.isEmpty()) {
                this.formatUsage(sender);
                return true;
            }

            if (this.mainCommands.size() == 1) {
                this.executeCommand(this.mainCommands.get(0), sender, args);
                return true;
            }

            this.formatUsage(sender);
            return true;
        }

        for (AnnotationSubCommand subCommand : subCommands) {
            if (!args[0].equalsIgnoreCase(subCommand.getName()) && !subCommand.getAliases().contains(args[0].toLowerCase())) continue;
            this.executeCommand(subCommand, sender, args);
            return true;
        }

        if (!this.mainCommands.isEmpty()) {
            if (this.mainCommands.size() == 1) {
                this.executeCommand(this.mainCommands.get(0), sender, args);
                return true;
            }

            this.formatUsage(sender);
            return true;
        }

        this.formatUsage(sender);
        return true;
    }

    private void executeCommand(AnnotationSubCommand subCommand, CommandSender sender, String[] args) {
        if (subCommand.getPermission() != null && !sender.hasPermission(subCommand.getPermission())) {
            PermissionException permissionException = new PermissionException("You do not have permission to use this command.");
            sender.sendMessage(SpigotCommandLoader.getFormattingProvider().formatError(permissionException, permissionException.getMessage()));
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
                sender.sendMessage(SpigotCommandLoader.getFormattingProvider().formatError(commandException, permissionException.getMessage()));
            } else if (commandException instanceof ContextResolverException contextResolverException) {
                sender.sendMessage(SpigotCommandLoader.getFormattingProvider().formatError(commandException, "A context resolver was not found for: " + contextResolverException.getMessage()));
            } else if (commandException instanceof ParameterException parameterException) {
                sender.sendMessage(SpigotCommandLoader.getFormattingProvider().formatError(commandException, parameterException.getMessage()));
            } else if (commandException instanceof ErrorException errorException) {
                sender.sendMessage(SpigotCommandLoader.getFormattingProvider().formatError(commandException, "An error occurred while executing this subcommand: " + errorException.getMessage()));
            }
        }
    }

    @Override
    @NotNull
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) {
        List<String> options = new ArrayList<>();
        AnnotationCommandSender<CommandSender> commandSender = new AnnotationCommandSender<>(sender);

        for (AnnotationSubCommand mainCommand : this.mainCommands) {
            if (mainCommand.getPermission() == null || sender.hasPermission(mainCommand.getPermission())) {
                AnnotationCommandExecutor<CommandSender> mainCommandExecutor = new AnnotationCommandExecutor<>(mainCommand, this);
                options.addAll(mainCommandExecutor.complete(commandSender, args));
            }
        }

        if (args.length == 1 && !this.subCommands.isEmpty()) {
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
            List<String> allAliases = new ArrayList<>();
            for (AnnotationSubCommand mainCommand : this.mainCommands) {
                allAliases.addAll(mainCommand.getAliases());
            }
            this.setAliases(allAliases);

            boolean allMainCommandsHavePermissions = !this.mainCommands.isEmpty() &&
                    this.mainCommands.stream().allMatch(cmd -> cmd.getPermission() != null);

            if (allMainCommandsHavePermissions) {
                this.setPermission(this.mainCommands.get(0).getPermission());
                this.permissionMessage(Component.text("You do not have permission to use this command.", TextColor.fromHexString("#FB465C")));
            }

            Field bukkitCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            bukkitCommandMap.setAccessible(true);

            CommandMap commandMap = (CommandMap) bukkitCommandMap.get(Bukkit.getServer());
            commandMap.register(plugin.getName(), this);

            plugin.getLogger().info("Registered command: " + this.getCommandName());
            if (!allAliases.isEmpty()) {
                plugin.getLogger().info("- Registered aliases: " + String.join(", ", allAliases));
            }
        } catch (Exception exception) {
            plugin.getLogger().severe("Unable to register command: " + this.getCommandName());
        }
    }

    protected void formatUsage(CommandSender sender) {
        List<String> usageMessages = new ArrayList<>();

        for (AnnotationSubCommand mainCommand : this.mainCommands) {
            if (mainCommand.getPermission() == null || sender.hasPermission(mainCommand.getPermission())) {
                String usage = "/" + this.getCommandName() + mainCommand.getUsage() + " - " + mainCommand.getDescription();
                usageMessages.add(usage);
            }
        }

        for (AnnotationSubCommand subCommand : this.subCommands) {
            if (subCommand.getPermission() == null || sender.hasPermission(subCommand.getPermission())) {
                String usage = "/" + this.getCommandName() + " " + subCommand.getName() + subCommand.getUsage() + " - " + subCommand.getDescription();
                usageMessages.add(usage);
            }
        }

        if (usageMessages.isEmpty()) {
            sender.sendMessage(Component.text("No available command syntaxes.", TextColor.fromHexString("#FF6B6B")));
            return;
        }

        if (usageMessages.size() == 1) {
            sender.sendMessage(Component.text("Invalid command syntax. Correct command syntax is: ", TextColor.fromHexString("#FBFB00")));
            sender.sendMessage(Component.text(usageMessages.get(0), TextColor.fromHexString("#FBFB00")));
        } else {
            sender.sendMessage(Component.text("Invalid command syntax. Correct command syntax's are:", TextColor.fromHexString("#FBFB00")));
            for (String usage : usageMessages) {
                sender.sendMessage(Component.text(usage, TextColor.fromHexString("#FBFB00")));
            }
        }
    }
}