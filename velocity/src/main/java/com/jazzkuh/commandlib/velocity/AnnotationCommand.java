package com.jazzkuh.commandlib.velocity;

import com.jazzkuh.commandlib.common.*;
import com.jazzkuh.commandlib.common.annotations.Main;
import com.jazzkuh.commandlib.common.annotations.Subcommand;
import com.jazzkuh.commandlib.common.exception.*;
import com.jazzkuh.commandlib.velocity.utils.StringUtils;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AnnotationCommand implements AnnotationCommandImpl, SimpleCommand {
    private final String commandName;
    private AnnotationSubCommand mainCommand = null;
    private final List<AnnotationSubCommand> subCommands = new ArrayList<>();

    public AnnotationCommand() {
        if (!this.getClass().isAnnotationPresent(com.jazzkuh.commandlib.common.annotations.Command.class)) {
            throw new IllegalArgumentException("AnnotationCommand needs to have a @Command annotation!");
        }

        this.commandName = this.getClass().getAnnotation(com.jazzkuh.commandlib.common.annotations.Command.class).value();

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
    public void execute(Invocation invocation) {
        String[] args = invocation.arguments();
        CommandSource sender = invocation.source();

        if (args.length < 1) {
            if (this.mainCommand == null) {
                this.formatUsage(sender);
                return;
            }

            this.executeCommand(this.mainCommand, sender, args);
            return;
        }

        for (AnnotationSubCommand subCommand : subCommands) {
            if (!args[0].equalsIgnoreCase(subCommand.getName()) && !subCommand.getAliases().contains(args[0].toLowerCase())) continue;
            this.executeCommand(subCommand, sender, args);
            return;
        }

        this.executeCommand(this.mainCommand, sender, args);
    }

    private void executeCommand(AnnotationSubCommand subCommand, CommandSource sender, String[] args) {
        if (subCommand.getPermission() != null && !sender.hasPermission(subCommand.getPermission())) {
            PermissionException permissionException = new PermissionException("You do not have permission to use this command.");
            sender.sendMessage(VelocityCommandLoader.getFormattingProvider().formatError(permissionException, permissionException.getMessage()));
            return;
        }

        AnnotationCommandExecutor<CommandSource> commandExecutor = new AnnotationCommandExecutor<>(subCommand, this);
        AnnotationCommandSender<CommandSource> commandSender = new AnnotationCommandSender<>(sender);

        try {
            commandExecutor.execute(commandSender, args);
        } catch (CommandException commandException) {
            if (commandException instanceof ArgumentException) {
                this.formatUsage(sender);
            } else if (commandException instanceof PermissionException permissionException) {
                sender.sendMessage(VelocityCommandLoader.getFormattingProvider().formatError(commandException, permissionException.getMessage()));
            } else if (commandException instanceof ContextResolverException contextResolverException) {
                sender.sendMessage(VelocityCommandLoader.getFormattingProvider().formatError(commandException, "A context resolver was not found for: " + contextResolverException.getMessage()));
            } else if (commandException instanceof ParameterException parameterException) {
                sender.sendMessage(VelocityCommandLoader.getFormattingProvider().formatError(commandException, parameterException.getMessage()));
            } else if (commandException instanceof ErrorException errorException) {
                sender.sendMessage(VelocityCommandLoader.getFormattingProvider().formatError(commandException, "An error occurred while executing this subcommand: " + errorException.getMessage()));
            }
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        CommandSource sender = invocation.source();
        AnnotationCommandExecutor<CommandSource> mainCommandExecutor = new AnnotationCommandExecutor<>(this.mainCommand, this);
        AnnotationCommandSender<CommandSource> commandSender = new AnnotationCommandSender<>(sender);

        List<String> options = new ArrayList<>(mainCommandExecutor.complete(commandSender, args));

        if (args.length == 1 && this.subCommands.size() >= 1) {
            for (AnnotationSubCommand subCommand : this.subCommands) {
                if (subCommand.getPermission() == null) {
                    options.add(subCommand.getName());
                    continue;
                }

                if (commandSender.getSender().hasPermission(subCommand.getPermission())) options.add(subCommand.getName());
            }

            return StringUtils.copyPartialMatches(args[0], options, new ArrayList<>(options.size()));
        }

        for (AnnotationSubCommand subCommand : this.subCommands) {
            if (args.length < 1) {
                if (subCommand.getPermission() == null) {
                    options.add(subCommand.getName());
                    continue;
                }

                if (commandSender.getSender().hasPermission(subCommand.getPermission())) options.add(subCommand.getName());
                continue;
            }

            if (!args[0].equalsIgnoreCase(subCommand.getName()) && !subCommand.getAliases().contains(args[0].toLowerCase())) continue;
            AnnotationCommandExecutor<CommandSource> subCommandExecutor = new AnnotationCommandExecutor<>(subCommand, this);
            options.addAll(subCommandExecutor.complete(commandSender, args));
        }

        return options;
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return this.mainCommand.getPermission() == null || invocation.source().hasPermission(this.mainCommand.getPermission());
    }

    public void register(CommandManager commandManager) {
        commandManager.register(commandName, this, this.mainCommand.getAliases().toArray(new String[0]));
    }

    protected void formatUsage(CommandSource sender) {
        if (mainCommand.getUsage() != null && this.mainCommand.getUsage().length() > 0 && this.subCommands.isEmpty()) {
            sender.sendMessage(Component.text("Invalid command syntax. Correct command syntax is: ", TextColor.fromHexString("#FBFB00")));
            sender.sendMessage(Component.text("/" + this.getCommandName() + this.mainCommand.getUsage() + " - " + this.mainCommand.getDescription(), TextColor.fromHexString("#FBFB00")));
            return;
        }

        sender.sendMessage(Component.text("Invalid command syntax. Correct command syntax's are:", TextColor.fromHexString("#FBFB00")));
        if (mainCommand.getUsage() != null && mainCommand.getUsage().length() > 0) {
            sender.sendMessage(Component.text("/" + this.getCommandName() + this.mainCommand.getUsage() + " - " + this.mainCommand.getDescription(), TextColor.fromHexString("#FBFB00")));
        }

        for (AnnotationSubCommand subCommand : this.subCommands) {
            if (subCommand.getPermission() == null || sender.hasPermission(subCommand.getPermission())) {
                sender.sendMessage(Component.text("/" + this.getCommandName() + " " + subCommand.getName() + subCommand.getUsage() + " - " + subCommand.getDescription(), TextColor.fromHexString("#FBFB00")));
            }
        }
    }
}
