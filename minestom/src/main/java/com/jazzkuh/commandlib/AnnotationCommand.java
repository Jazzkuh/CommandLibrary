package com.jazzkuh.commandlib;

import com.jazzkuh.commandlib.common.*;
import com.jazzkuh.commandlib.common.annotations.Main;
import com.jazzkuh.commandlib.common.annotations.Subcommand;
import com.jazzkuh.commandlib.utils.StringUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.command.CommandManager;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.ArgumentStringArray;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
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

        ArgumentStringArray arguments = new ArgumentStringArray("[...]");
        arguments.setDefaultValue(new String[0]);
        arguments.setSuggestionCallback((sender, context, suggestionCallback) -> {
            String[] args = this.fixArguments(context.get(arguments));

            List<String> suggestions = this.suggest(sender, args);
            if (suggestions.isEmpty()) {
                suggestionCallback.addEntry(new SuggestionEntry(""));
                return;
            }

            for (String suggestion : suggestions) {
                suggestionCallback.addEntry(new SuggestionEntry(suggestion, Component.empty()));
            }
        });

        setDefaultExecutor(this::execute);
        addSyntax(this::execute, arguments);
    }

    private String[] fixArguments(String[] args) {
        // Minestom set null character to end of array
        if (args.length > 0 && args[args.length - 1].equals("\u0000")) {
            args = Arrays.copyOf(args, args.length);

            args[args.length - 1] = "";
        }

        return args;
    }


    @Override
    public String getCommandName() {
        return this.commandName;
    }

    public void execute(CommandSender sender, CommandContext context) {
        String[] args = Arrays.stream(context.getInput().split(" ")).skip(1).toArray(String[]::new);

        if (args.length < 1) {
            if (this.mainCommand == null) {
                this.formatUsage(sender);
                return;
            }

            this.executeCommand(this.mainCommand, sender, args);
            return;
        }

        for (AnnotationSubCommand subCommand : subCommands) {
            if (!args[0].equalsIgnoreCase(subCommand.getName()) && !subCommand.getAliases().contains(args[0].toLowerCase()))
                continue;
            this.executeCommand(subCommand, sender, args);
            return;
        }

        this.executeCommand(this.mainCommand, sender, args);
    }

    private void executeCommand(AnnotationSubCommand subCommand, CommandSender sender, String[] args) {
        if (subCommand.getPermission() != null && !sender.hasPermission(subCommand.getPermission())) {
            sender.sendMessage(Component.text("You do not have permission to use this command.", TextColor.fromHexString("#FB465C")));
            return;
        }

        AnnotationCommandExecutor<CommandSender> commandExecutor = new AnnotationCommandExecutor<>(subCommand, this);
        AnnotationCommandSender<CommandSender> commandSender = new AnnotationCommandSender<>(sender);

        AnnotationCommandExecutor.CommandResult commandResult = commandExecutor.execute(commandSender, args);
        switch (commandResult) {
            case NOT_ENOUGH_ARGUMENTS -> this.formatUsage(sender);
            case ERROR ->
                    sender.sendMessage(Component.text("An error occurred while executing this subcommand.", TextColor.fromHexString("#FB465C")));
            case NOT_ALLOWED ->
                    sender.sendMessage(Component.text("You are not allowed to execute this command.", TextColor.fromHexString("#FB465C")));
            case CONTEXT_RESOLVER_NOT_FOUND ->
                    sender.sendMessage(Component.text("A context resolver was not found for one of the parameters.", TextColor.fromHexString("#FB465C")));
            case PARAMETER_INVALID ->
                    sender.sendMessage(Component.text("One of the parameters was invalid.", TextColor.fromHexString("#FB465C")));
        }
    }

    public List<String> suggest(CommandSender sender, String[] args) {
        AnnotationCommandExecutor<CommandSender> mainCommandExecutor = new AnnotationCommandExecutor<>(this.mainCommand, this);
        AnnotationCommandSender<CommandSender> commandSender = new AnnotationCommandSender<>(sender);

        List<String> options = new ArrayList<>(mainCommandExecutor.complete(commandSender, args));

        if (args.length == 1 && !this.subCommands.isEmpty()) {
            for (AnnotationSubCommand subCommand : this.subCommands) {
                if (subCommand.getPermission() == null) {
                    options.add(subCommand.getName());
                    continue;
                }

                if (commandSender.getSender().hasPermission(subCommand.getPermission()))
                    options.add(subCommand.getName());
            }

            return StringUtils.copyPartialMatches(args[0], options, new ArrayList<>(options.size()));
        }

        for (AnnotationSubCommand subCommand : this.subCommands) {
            if (args.length < 1) {
                if (subCommand.getPermission() == null) {
                    options.add(subCommand.getName());
                    continue;
                }

                if (commandSender.getSender().hasPermission(subCommand.getPermission()))
                    options.add(subCommand.getName());
                continue;
            }

            if (!args[0].equalsIgnoreCase(subCommand.getName()) && !subCommand.getAliases().contains(args[0].toLowerCase()))
                continue;
            AnnotationCommandExecutor<CommandSender> subCommandExecutor = new AnnotationCommandExecutor<>(subCommand, this);
            options.addAll(subCommandExecutor.complete(commandSender, args));
        }

        return options;
    }

    public void register(CommandManager commandManager) {
        commandManager.register(this);
    }

    protected void formatUsage(CommandSender sender) {
        if (mainCommand.getUsage() != null && !this.mainCommand.getUsage().isEmpty() && this.subCommands.isEmpty()) {
            sender.sendMessage(Component.text("Invalid command syntax. Correct command syntax is: ", TextColor.fromHexString("#FBFB00")));
            sender.sendMessage(Component.text("/" + this.getCommandName() + this.mainCommand.getUsage() + " - " + this.mainCommand.getDescription(), TextColor.fromHexString("#FBFB00")));
            return;
        }

        sender.sendMessage(Component.text("Invalid command syntax. Correct command syntax's are:", TextColor.fromHexString("#FBFB00")));
        if (mainCommand.getUsage() != null && !mainCommand.getUsage().isEmpty()) {
            sender.sendMessage(Component.text("/" + this.getCommandName() + this.mainCommand.getUsage() + " - " + this.mainCommand.getDescription(), TextColor.fromHexString("#FBFB00")));
        }

        for (AnnotationSubCommand subCommand : this.subCommands) {
            if (subCommand.getPermission() == null || sender.hasPermission(subCommand.getPermission())) {
                sender.sendMessage(Component.text("/" + this.getCommandName() + " " + subCommand.getName() + subCommand.getUsage() + " - " + subCommand.getDescription(), TextColor.fromHexString("#FBFB00")));
            }
        }
    }
}
