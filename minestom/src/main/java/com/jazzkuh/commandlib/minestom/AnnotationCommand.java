package com.jazzkuh.commandlib.minestom;

import com.jazzkuh.commandlib.common.*;
import com.jazzkuh.commandlib.common.annotations.Main;
import com.jazzkuh.commandlib.common.annotations.Subcommand;
import com.jazzkuh.commandlib.common.exception.*;
import com.jazzkuh.commandlib.minestom.terminal.LoggingConsoleSender;
import com.jazzkuh.commandlib.minestom.utils.StringUtils;
import com.jazzkuh.commandlib.minestom.utils.permission.Permissable;
import lombok.SneakyThrows;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.minestom.server.command.CommandManager;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.ConsoleSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.ArgumentStringArray;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.entity.Player;
import org.codehaus.plexus.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

public class AnnotationCommand extends Command implements AnnotationCommandImpl {
    private static final ComponentLogger LOGGER = ComponentLogger.logger("CommandLibrary");

    protected String commandName;
    protected final List<AnnotationSubCommand> mainCommands = new ArrayList<>();
    protected final List<AnnotationSubCommand> subCommands = new ArrayList<>();

    public AnnotationCommand(String commandName) {
        super(commandName);
        this.commandName = commandName;
        this.init();
    }

    public AnnotationCommand() {
        super("__annotation_command__");
        if (!this.getClass().isAnnotationPresent(com.jazzkuh.commandlib.common.annotations.Command.class)) {
            throw new IllegalArgumentException("AnnotationCommand needs to have a @Command annotation!");
        }

        this.commandName = this.getClass().getAnnotation(com.jazzkuh.commandlib.common.annotations.Command.class).value();
        this.init();
    }

    @SneakyThrows
    private void init() {
        Field nameField = ReflectionUtils.getFieldByNameIncludingSuperclasses("name", Command.class);
        nameField.setAccessible(true);
        nameField.set(this, this.commandName);

        List<Method> mainCommandMethods = Arrays.stream(this.getClass().getMethods())
                .filter(method -> method.isAnnotationPresent(Main.class))
                .toList();
        
        mainCommandMethods.forEach(method -> this.mainCommands.add(AnnotationCommandParser.parse(this, method)));

        List<String> allAliases = new ArrayList<>();
        for (AnnotationSubCommand mainCommand : this.mainCommands) {
            allAliases.addAll(mainCommand.getAliases());
        }

        Field aliasesField = ReflectionUtils.getFieldByNameIncludingSuperclasses("aliases", Command.class);
        aliasesField.setAccessible(true);
        aliasesField.set(this, allAliases.toArray(new String[0]));

        List<String> names = new ArrayList<>();
        names.add(this.commandName);
        names.addAll(allAliases);

        Field namesField = ReflectionUtils.getFieldByNameIncludingSuperclasses("names", Command.class);
        namesField.setAccessible(true);
        namesField.set(this, names.toArray(new String[0]));

        List<Method> subcommandMethods = Arrays.stream(this.getClass().getMethods())
                .filter(method -> method.isAnnotationPresent(Subcommand.class))
                .toList();
        subcommandMethods.forEach(method -> this.subCommands.add(AnnotationCommandParser.parse(this, method)));

        ArgumentStringArray params = new ArgumentStringArray("params");
        params.setDefaultValue(new String[0]);
        params.setSuggestionCallback((sender, context, suggestionCallback) -> {
            String[] args = this.fixArguments(context.get(params));

            List<String> suggestions = this.suggest(sender, args);
            if (suggestions.isEmpty()) return;
            for (String suggestion : suggestions) {
                SuggestionEntry suggestionEntry = new SuggestionEntry(suggestion);
                if (suggestionCallback.getEntries().contains(suggestionEntry)) continue;
                suggestionCallback.addEntry(suggestionEntry);
            }
        });

        setDefaultExecutor(this::execute);
        addSyntax(this::execute, params);
        
        boolean allMainCommandsHavePermissions = !this.mainCommands.isEmpty() &&
                this.mainCommands.stream().allMatch(cmd -> cmd.getPermission() != null);

        if (allMainCommandsHavePermissions) {
            setCondition((commandSender, s) -> {
                Permissable permissable = new Permissable(null);
                if (commandSender instanceof Player player) {
                    permissable = new Permissable(player.getUuid());
                }

                Permissable finalPermissable = permissable;
                return this.mainCommands.stream().anyMatch(cmd -> finalPermissable.hasPermission(cmd.getPermission()));
            });
        }
    }

    private String[] fixArguments(String[] args) {
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
            if (this.mainCommands.isEmpty()) {
                this.formatUsage(sender);
                return;
            }

            if (this.mainCommands.size() == 1) {
                this.executeCommand(this.mainCommands.get(0), sender, args);
            } else {
                this.formatUsage(sender);
            }
            return;
        }

        for (AnnotationSubCommand subCommand : subCommands) {
            if (!args[0].equalsIgnoreCase(subCommand.getName()) && !subCommand.getAliases().contains(args[0].toLowerCase()))
                continue;
            this.executeCommand(subCommand, sender, args);
            return;
        }

        for (AnnotationSubCommand mainCommand : mainCommands) {
            this.executeCommand(mainCommand, sender, args);
            return;
        }

        this.formatUsage(sender);
    }

    private void executeCommand(AnnotationSubCommand subCommand, CommandSender sender, String[] args) {
        Permissable permissable = new Permissable(null);
        if (sender instanceof ConsoleSender) sender = new LoggingConsoleSender();
        if (sender instanceof Player player) {
            permissable = new Permissable(player.getUuid());
            LOGGER.info("Command executed by {}: {} {}", player.getUsername(), this.getCommandName(), String.join(" ", args));
        }

        if (subCommand.getPermission() != null && !(sender instanceof ConsoleSender) && !permissable.hasPermission(subCommand.getPermission())) {
            PermissionException permissionException = new PermissionException("You do not have permission to use this command.");
            sender.sendMessage(MinestomCommandLoader.getFormattingProvider().formatError(permissionException, permissionException.getMessage()));
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
                sender.sendMessage(MinestomCommandLoader.getFormattingProvider().formatError(commandException, permissionException.getMessage()));
            } else if (commandException instanceof ContextResolverException contextResolverException) {
                sender.sendMessage(MinestomCommandLoader.getFormattingProvider().formatError(commandException, "A context resolver was not found for: " + contextResolverException.getMessage()));
            } else if (commandException instanceof ParameterException parameterException) {
                sender.sendMessage(MinestomCommandLoader.getFormattingProvider().formatError(commandException, parameterException.getMessage()));
            } else if (commandException instanceof ErrorException errorException) {
                sender.sendMessage(MinestomCommandLoader.getFormattingProvider().formatError(commandException, "An error occurred while executing this subcommand: " + errorException.getMessage()));
            }
        }
    }

    public List<String> suggest(CommandSender sender, String[] args) {
        AnnotationCommandSender<CommandSender> commandSender = new AnnotationCommandSender<>(sender);
        Permissable permissable = new Permissable(null);
        if (sender instanceof Player player) {
            permissable = new Permissable(player.getUuid());
        }

        List<String> options = new ArrayList<>();

        for (AnnotationSubCommand mainCommand : mainCommands) {
            if (mainCommand.getPermission() == null || permissable.hasPermission(mainCommand.getPermission())) {
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

                if (permissable.hasPermission(subCommand.getPermission()))
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

                if (permissable.hasPermission(subCommand.getPermission()))
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
        try {
            commandManager.register(this);
            LOGGER.info("Registered command: {}", this.getCommandName());
            if (!Arrays.stream(this.getAliases()).toList().isEmpty()) {
                LOGGER.info("- Registered aliases: {}", String.join(", ", this.getAliases()));
            }
        } catch (Exception exception) {
            LOGGER.info("Unable to register command: {}", this.getCommandName());
        }
    }

    public void formatUsage(CommandSender sender) {
        Permissable permissable = new Permissable(null);
        if (sender instanceof Player player) {
            permissable = new Permissable(player.getUuid());
        }

        List<String> usageMessages = new ArrayList<>();

        for (AnnotationSubCommand mainCommand : this.mainCommands) {
            if (mainCommand.getPermission() == null || permissable.hasPermission(mainCommand.getPermission())) {
                String usage = "/" + this.getCommandName() + mainCommand.getUsage() + " - " + mainCommand.getDescription();
                usageMessages.add(usage);
            }
        }

        for (AnnotationSubCommand subCommand : this.subCommands) {
            if (subCommand.getPermission() == null || permissable.hasPermission(subCommand.getPermission())) {
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