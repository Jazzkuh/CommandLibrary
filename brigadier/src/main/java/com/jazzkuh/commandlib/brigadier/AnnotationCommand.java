package com.jazzkuh.commandlib.brigadier;

import com.jazzkuh.commandlib.brigadier.utils.provider.CommandSourceProvider;
import com.jazzkuh.commandlib.brigadier.utils.source.CommandSource;
import com.jazzkuh.commandlib.common.*;
import com.jazzkuh.commandlib.common.exception.*;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class AnnotationCommand<S> implements AnnotationCommandImpl {
    private static final ComponentLogger LOGGER = ComponentLogger.logger("CommandLibrary");

    protected BrigadierCommandLoader<S> commandLoader;
    protected String commandName;
    protected AnnotationSubCommand mainCommand = null;
    protected final List<AnnotationSubCommand> subCommands = new ArrayList<>();

    public AnnotationCommand(BrigadierCommandLoader<S> commandLoader, String commandName) {
        this.commandLoader = commandLoader;
        this.commandName = commandName;
    }

    public AnnotationCommand(BrigadierCommandLoader<S> commandLoader) {
        if (!this.getClass().isAnnotationPresent(com.jazzkuh.commandlib.common.annotations.Command.class)) {
            throw new IllegalArgumentException("AnnotationCommand needs to have a @Command annotation!");
        }

        this.commandLoader = commandLoader;
        this.commandName = this.getClass().getAnnotation(com.jazzkuh.commandlib.common.annotations.Command.class).value();
    }

    @Override
    public String getCommandName() {
        return this.commandName;
    }

    private void executeCommand(AnnotationSubCommand subCommand, CommandSource<S> source, String[] args) {
        CommandSourceProvider<S> sourceProvider = commandLoader.getCommandSourceProvider();
        if (subCommand.getPermission() != null && !sourceProvider.hasPermission(source, subCommand.getPermission())) {
            PermissionException permissionException = new PermissionException("You do not have permission to use this command.");
            sourceProvider.sendMessage(source, BrigadierCommandLoader.getFormattingProvider().formatError(permissionException, permissionException.getMessage()));
            return;
        }

        AnnotationCommandExecutor<CommandSource<S>> commandExecutor = new AnnotationCommandExecutor<>(subCommand, this);
        AnnotationCommandSender<CommandSource<S>> commandSender = new AnnotationCommandSender<>(source);

        try {
            commandExecutor.execute(commandSender, args);
        } catch (CommandException commandException) {
            if (commandException instanceof ArgumentException) {
                this.formatUsage(source);
            } else if (commandException instanceof PermissionException permissionException) {
                sourceProvider.sendMessage(source, BrigadierCommandLoader.getFormattingProvider().formatError(commandException, permissionException.getMessage()));
            } else if (commandException instanceof ContextResolverException contextResolverException) {
                sourceProvider.sendMessage(source, BrigadierCommandLoader.getFormattingProvider().formatError(commandException, "A context resolver was not found for: " + contextResolverException.getMessage()));
            } else if (commandException instanceof ParameterException parameterException) {
                sourceProvider.sendMessage(source, BrigadierCommandLoader.getFormattingProvider().formatError(commandException, parameterException.getMessage()));
            } else if (commandException instanceof ErrorException errorException) {
                sourceProvider.sendMessage(source, BrigadierCommandLoader.getFormattingProvider().formatError(commandException, "An error occurred while executing this subcommand: " + errorException.getMessage()));
            }
        }
    }

    public LiteralArgumentBuilder<S> register(CommandDispatcher<S> dispatcher) {
        LiteralArgumentBuilder<S> builder = LiteralArgumentBuilder.literal(this.commandName);
        builder.then(RequiredArgumentBuilder.argument("args", StringArgumentType.greedyString())).executes(context -> {
            String[] args = Arrays.stream(StringArgumentType.getString(context, "args").split(" ")).skip(1).toArray(String[]::new);
            CommandSource<S> commandSource = new CommandSource<>(context.getSource());

            if (args.length < 1) {
                if (this.mainCommand == null) {
                    this.formatUsage(commandSource);
                    return 1;
                }

                this.executeCommand(this.mainCommand, commandSource, args);
                return 1;
            }

            for (AnnotationSubCommand subCommand : subCommands) {
                if (!args[0].equalsIgnoreCase(subCommand.getName()) && !subCommand.getAliases().contains(args[0].toLowerCase()))
                    continue;
                this.executeCommand(subCommand, commandSource, args);
                return 1;
            }

            this.executeCommand(this.mainCommand, commandSource, args);
            return 1;
        });

        dispatcher.register(builder);
        return builder;
    }

    public void formatUsage(CommandSource<S> source) {
        CommandSourceProvider<S> sourceProvider = commandLoader.getCommandSourceProvider();
        if (mainCommand.getUsage() != null && !this.mainCommand.getUsage().isEmpty() && this.subCommands.isEmpty()) {
            sourceProvider.sendMessage(source, Component.text("Invalid command syntax. Correct command syntax is: ", TextColor.fromHexString("#FBFB00")));
            sourceProvider.sendMessage(source, Component.text("/" + this.getCommandName() + this.mainCommand.getUsage() + " - " + this.mainCommand.getDescription(), TextColor.fromHexString("#FBFB00")));
            return;
        }

        sourceProvider.sendMessage(source, Component.text("Invalid command syntax. Correct command syntax's are:", TextColor.fromHexString("#FBFB00")));
        if (mainCommand.getUsage() != null && !mainCommand.getUsage().isEmpty()) {
            sourceProvider.sendMessage(source, Component.text("/" + this.getCommandName() + this.mainCommand.getUsage() + " - " + this.mainCommand.getDescription(), TextColor.fromHexString("#FBFB00")));
        }

        for (AnnotationSubCommand subCommand : this.subCommands) {
            if (subCommand.getPermission() == null || sourceProvider.hasPermission(source, subCommand.getPermission())) {
                sourceProvider.sendMessage(source, Component.text("/" + this.getCommandName() + " " + subCommand.getName() + subCommand.getUsage() + " - " + subCommand.getDescription(), TextColor.fromHexString("#FBFB00")));
            }
        }
    }}