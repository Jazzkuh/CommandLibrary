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
    protected final String commandName;
    protected final List<AnnotationSubCommand> mainCommands = new ArrayList<>();
    protected final List<AnnotationSubCommand> subCommands = new ArrayList<>();

    public AnnotationCommand() {
        if (!this.getClass().isAnnotationPresent(com.jazzkuh.commandlib.common.annotations.Command.class)) {
            throw new IllegalArgumentException("AnnotationCommand needs to have a @Command annotation!");
        }

        this.commandName = this.getClass().getAnnotation(com.jazzkuh.commandlib.common.annotations.Command.class).value();

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
    public void execute(Invocation invocation) {
        String[] args = invocation.arguments();
        CommandSource sender = invocation.source();

        if (args.length < 1) {
            if (this.mainCommands.isEmpty()) {
                this.formatUsage(sender);
                return;
            }

            if (this.mainCommands.size() == 1) {
                this.executeCommand(this.mainCommands.get(0), sender, args);
                return;
            }

            this.formatUsage(sender);
            return;
        }

        for (AnnotationSubCommand subCommand : subCommands) {
            if (!args[0].equalsIgnoreCase(subCommand.getName()) && !subCommand.getAliases().contains(args[0].toLowerCase())) continue;
            this.executeCommand(subCommand, sender, args);
            return;
        }

        if (!this.mainCommands.isEmpty()) {
            if (this.mainCommands.size() == 1) {
                this.executeCommand(this.mainCommands.get(0), sender, args);
                return;
            }

            this.formatUsage(sender);
            return;
        }

        this.formatUsage(sender);
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
        List<String> options = new ArrayList<>();
        AnnotationCommandSender<CommandSource> commandSender = new AnnotationCommandSender<>(sender);

        for (AnnotationSubCommand mainCommand : this.mainCommands) {
            if (mainCommand.getPermission() == null || sender.hasPermission(mainCommand.getPermission())) {
                AnnotationCommandExecutor<CommandSource> mainCommandExecutor = new AnnotationCommandExecutor<>(mainCommand, this);
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
            if (subCommand.getPermission() != null && !commandSender.getSender().hasPermission(subCommand.getPermission())) continue;
            options.addAll(subCommandExecutor.complete(commandSender, args));
        }

        return options;
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        if (this.mainCommands.isEmpty()) {
            return true;
        }

        return this.mainCommands.stream().anyMatch(cmd ->
                cmd.getPermission() == null || invocation.source().hasPermission(cmd.getPermission()));
    }

    public void register(CommandManager commandManager) {
        List<String> allAliases = new ArrayList<>();
        for (AnnotationSubCommand mainCommand : this.mainCommands) {
            allAliases.addAll(mainCommand.getAliases());
        }

        commandManager.register(commandName, this, allAliases.toArray(new String[0]));
    }

    protected void formatUsage(CommandSource sender) {
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