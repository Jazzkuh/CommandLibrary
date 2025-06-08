package com.jazzkuh.commandlib.minestom;

import com.jazzkuh.commandlib.common.AnnotationCommandImpl;
import com.jazzkuh.commandlib.common.AnnotationCommandParser;
import com.jazzkuh.commandlib.common.AnnotationCommandSender;
import com.jazzkuh.commandlib.common.AnnotationSubCommand;
import com.jazzkuh.commandlib.common.annotations.Completion;
import com.jazzkuh.commandlib.common.annotations.Greedy;
import com.jazzkuh.commandlib.common.annotations.Main;
import com.jazzkuh.commandlib.common.annotations.Optional;
import com.jazzkuh.commandlib.common.annotations.Subcommand;
import com.jazzkuh.commandlib.common.exception.ArgumentException;
import com.jazzkuh.commandlib.common.exception.CommandException;
import com.jazzkuh.commandlib.common.exception.ContextResolverException;
import com.jazzkuh.commandlib.common.exception.ErrorException;
import com.jazzkuh.commandlib.common.exception.ParameterException;
import com.jazzkuh.commandlib.common.exception.PermissionException;
import com.jazzkuh.commandlib.common.resolvers.CompletionResolver;
import com.jazzkuh.commandlib.common.resolvers.ContextResolver;
import com.jazzkuh.commandlib.common.resolvers.Resolvers;
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
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentBoolean;
import net.minestom.server.command.builder.arguments.ArgumentString;
import net.minestom.server.command.builder.arguments.ArgumentStringArray;
import net.minestom.server.command.builder.arguments.ArgumentWord;
import net.minestom.server.command.builder.arguments.number.ArgumentDouble;
import net.minestom.server.command.builder.arguments.number.ArgumentFloat;
import net.minestom.server.command.builder.arguments.number.ArgumentInteger;
import net.minestom.server.command.builder.arguments.number.ArgumentLong;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.entity.Player;
import org.codehaus.plexus.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AnnotationCommand extends Command implements AnnotationCommandImpl {

    private static final ComponentLogger LOGGER = ComponentLogger.logger("CommandLibrary");

    protected String commandName;
    protected final List<AnnotationSubCommand> mainCommands = new ArrayList<>();
    protected final List<AnnotationSubCommand> subCommands = new ArrayList<>();
    protected final Map<AnnotationSubCommand, List<Argument<?>>> commandArguments = new HashMap<>();

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

        // We register subcommands first to make sure they don't get overridden by main commands
        for (AnnotationSubCommand subCommand : this.subCommands) {
            this.createBrigadierSyntax(subCommand);
        }

        for (AnnotationSubCommand mainCommand : this.mainCommands) {
            this.createBrigadierSyntax(mainCommand);
        }

        this.setDefaultExecutor(this::executeDefault);

        boolean allMainCommandsHavePermissions = !this.mainCommands.isEmpty() &&
                this.mainCommands.stream().allMatch(cmd -> cmd.getPermission() != null);

        if (allMainCommandsHavePermissions) {
            this.setCondition((commandSender, s) -> {
                Permissable permissable = new Permissable(null);
                if (commandSender instanceof Player player) {
                    permissable = new Permissable(player.getUuid());
                }

                Permissable finalPermissable = permissable;
                return this.mainCommands.stream().anyMatch(cmd -> finalPermissable.hasPermission(cmd.getPermission()));
            });
        }
    }

    private void createBrigadierSyntax(AnnotationSubCommand command) {
        Method method = command.getMethod();
        Parameter[] parameters = method.getParameters();
        List<Argument<?>> arguments = new ArrayList<>();

        List<String> usageParamNames = this.parseUsageParameterNames(command);

        for (int i = 1; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            String paramName = usageParamNames.size() >= i ? usageParamNames.get(i - 1) : parameter.getName();

            Argument<?> argument = this.createArgumentForParameter(parameter, paramName);
            arguments.add(argument);
        }

        this.commandArguments.put(command, arguments);

        if (method.isAnnotationPresent(Subcommand.class)) {
            ArgumentWord subcommandArg = new ArgumentWord(command.getName() + "_sub");
            subcommandArg.from(command.getName());
            for (String alias : command.getAliases()) {
                subcommandArg.from(alias);
            }

            if (arguments.isEmpty()) {
                this.addSyntax((sender, context) -> executeSubcommand(command, sender, context), subcommandArg);
                return;
            }

            this.createOptionalSyntaxCombinations(command, subcommandArg, arguments);
            return;
        }

        if (arguments.isEmpty()) {
            this.addSyntax((sender, context) -> executeMainCommand(command, sender, context));
            return;
        }

        this.createOptionalSyntaxCombinations(command, null, arguments);
    }

    private void createOptionalSyntaxCombinations(AnnotationSubCommand command, ArgumentWord subcommandArg, List<Argument<?>> arguments) {
        Parameter[] parameters = command.getMethod().getParameters();

        List<Argument<?>> requiredArgs = new ArrayList<>();
        List<Argument<?>> optionalArgs = new ArrayList<>();

        for (int i = 0; i < arguments.size(); i++) {
            Parameter param = parameters[i + 1];
            if (param.isAnnotationPresent(Optional.class)) optionalArgs.add(arguments.get(i));
            else requiredArgs.add(arguments.get(i));
        }

        int totalOptional = optionalArgs.size();
        for (int optionalCount = 0; optionalCount <= totalOptional; optionalCount++) {
            List<Argument<?>> syntaxArgs = new ArrayList<>();
            if (subcommandArg != null)
                syntaxArgs.add(subcommandArg);

            syntaxArgs.addAll(requiredArgs);
            syntaxArgs.addAll(optionalArgs.subList(0, optionalCount));

            Argument<?>[] argsArray = syntaxArgs.toArray(new Argument[0]);
            addSyntax((sender, context) -> {
                if (subcommandArg != null) {
                    this.executeSubcommand(command, sender, context);
                    return;
                }

                this.executeMainCommand(command, sender, context);
            }, argsArray);
        }
    }

    private Argument<?> createArgumentForParameter(Parameter parameter, String name) {
        Class<?> type = parameter.getType();

        if (parameter.isAnnotationPresent(Greedy.class) && type == String.class) {
            ArgumentStringArray greedyArg = new ArgumentStringArray(name);
            this.setupCompletionForArgument(greedyArg, parameter);
            return greedyArg;
        }

        if (type == String.class) {
            ArgumentString stringArg = new ArgumentString(name);
            this.setupCompletionForArgument(stringArg, parameter);
            return stringArg;
        } else if (type == Integer.class || type == int.class) {
            return new ArgumentInteger(name);
        } else if (type == Double.class || type == double.class) {
            return new ArgumentDouble(name);
        } else if (type == Float.class || type == float.class) {
            return new ArgumentFloat(name);
        } else if (type == Long.class || type == long.class) {
            return new ArgumentLong(name);
        } else if (type == Boolean.class || type == boolean.class) {
            return new ArgumentBoolean(name);
        } else if (type == UUID.class) {
            ArgumentString uuidArg = new ArgumentString(name);
            this.setupCompletionForArgument(uuidArg, parameter);
            return uuidArg;
        } else if (type.isEnum()) {
            ArgumentString enumArg = new ArgumentString(name);
            this.setupEnumCompletion(enumArg, (Class<? extends Enum>) type);
            return enumArg;
        } else {
            ArgumentString customArg = new ArgumentString(name);
            this.setupCompletionForArgument(customArg, parameter);
            return customArg;
        }
    }

    private <T extends Argument<?>> void setupCompletionForArgument(T argument, Parameter parameter) {
        argument.setSuggestionCallback((sender, context, suggestionCallback) -> {
            String currentInput = "";

            if (argument instanceof ArgumentString) {
                currentInput = context.getOrDefault((ArgumentString) argument, "");
            } else if (argument instanceof ArgumentStringArray) {
                String[] array = context.getOrDefault((ArgumentStringArray) argument, new String[0]);
                currentInput = array.length > 0 ? array[array.length - 1] : "";
            }

            List<String> suggestions = getParameterCompletions(sender, parameter, currentInput);
            for (String suggestion : suggestions) {
                SuggestionEntry entry = new SuggestionEntry(suggestion);
                if (suggestionCallback.getEntries().contains(entry))
                    continue;

                suggestionCallback.addEntry(entry);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void setupEnumCompletion(ArgumentString argument, Class<? extends Enum> enumClass) {
        argument.setSuggestionCallback((sender, context, suggestionCallback) -> {
            String currentInput = context.getOrDefault(argument, "");
            List<String> enumValues = EnumSet.allOf(enumClass).stream()
                    .map(e -> e.toString().toLowerCase())
                    .toList();

            List<String> matches = currentInput.isEmpty() ? enumValues : StringUtils.copyPartialMatches(currentInput, enumValues, new ArrayList<>());
            for (String match : matches) {
                suggestionCallback.addEntry(new SuggestionEntry(match));
            }
        });
    }

    private List<String> getParameterCompletions(CommandSender sender, Parameter parameter, String currentInput) {
        AnnotationCommandSender<CommandSender> commandSender = new AnnotationCommandSender<>(sender);

        if (parameter.isAnnotationPresent(Completion.class)) {
            Completion completion = parameter.getAnnotation(Completion.class);
            CompletionResolver<CommandSender> resolver = Resolvers.completion(completion.value());
            if (resolver != null) {
                List<String> allCompletions = resolver.resolve(commandSender, currentInput);

                if (currentInput.isEmpty()) {
                    return allCompletions;
                }

                return StringUtils.copyPartialMatches(currentInput, allCompletions, new ArrayList<>());
            }
        }

        CompletionResolver<CommandSender> completionResolver = Resolvers.completion(parameter.getType());
        if (completionResolver != null) {
            List<String> allCompletions = completionResolver.resolve(commandSender, currentInput);

            if (currentInput.isEmpty()) {
                return allCompletions;
            }

            return StringUtils.copyPartialMatches(currentInput, allCompletions, new ArrayList<>());
        }

        return new ArrayList<>();
    }

    private void executeDefault(CommandSender sender, CommandContext context) {
        if (this.mainCommands.isEmpty()) {
            this.formatUsage(sender);
            return;
        }

        if (this.mainCommands.size() == 1) {
            AnnotationSubCommand singleMain = this.mainCommands.get(0);
            List<Argument<?>> args = this.commandArguments.get(singleMain);
            if (args.isEmpty()) {
                this.executeMainCommand(singleMain, sender, context);
                return;
            }
        }

        this.formatUsage(sender);
    }

    private void executeMainCommand(AnnotationSubCommand command, CommandSender sender, CommandContext context) {
        this.executeCommandWithContext(command, sender, context);
    }

    private void executeSubcommand(AnnotationSubCommand command, CommandSender sender, CommandContext context) {
        this.executeCommandWithContext(command, sender, context);
    }

    private void executeCommandWithContext(AnnotationSubCommand subCommand, CommandSender sender, CommandContext context) {
        Permissable permissable = new Permissable(null);
        if (sender instanceof ConsoleSender)
            sender = new LoggingConsoleSender();
        if (sender instanceof Player player) {
            permissable = new Permissable(player.getUuid());
            LOGGER.info("Command executed by {}: {} {}", player.getUsername(), this.getCommandName(), context.getInput());
        }

        if (subCommand.getPermission() != null && !(sender instanceof ConsoleSender) && !permissable.hasPermission(subCommand.getPermission())) {
            PermissionException permissionException = new PermissionException("You do not have permission to use this command.");
            sender.sendMessage(MinestomCommandLoader.getFormattingProvider().formatError(permissionException, permissionException.getMessage()));
            return;
        }

        try {
            this.executeMethodWithBrigadierContext(subCommand, sender, context);
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

    private void executeMethodWithBrigadierContext(AnnotationSubCommand subCommand, CommandSender sender, CommandContext context) throws CommandException {
        Method method = subCommand.getMethod();
        Parameter[] parameters = method.getParameters();
        List<Argument<?>> arguments = this.commandArguments.get(subCommand);

        Object[] resolvedParameters = new Object[parameters.length];
        resolvedParameters[0] = sender;

        for (int i = 1; i < parameters.length; i++) {
            resolvedParameters[i] = this.resolveParameter(parameters[i], i - 1, arguments, context);
        }

        try {
            method.setAccessible(true);
            method.invoke(this, resolvedParameters);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ErrorException(e.getMessage());
        }
    }

    private Object resolveParameter(Parameter parameter, int argIndex, List<Argument<?>> arguments, CommandContext context) throws CommandException {
        if (argIndex >= arguments.size()) {
            return handleMissingArgument(parameter);
        }

        Argument<?> argument = arguments.get(argIndex);
        Object value = context.get(argument);

        if (value != null) {
            return this.resolveParameterValue(parameter, value, parameter.getType());
        }

        return this.handleMissingArgument(parameter);
    }

    private Object handleMissingArgument(Parameter parameter) throws ArgumentException {
        if (parameter.isAnnotationPresent(Optional.class)) {
            return null;
        }

        throw new ArgumentException();
    }

    private Object resolveParameterValue(Parameter parameter, Object value, Class<?> paramClass) throws CommandException {
        if (parameter.isAnnotationPresent(Greedy.class) && paramClass == String.class && value instanceof String[])
            return String.join(" ", (String[]) value);

        if (paramClass.isInstance(value))
            return value;

        if (value instanceof String stringValue) {
            if (paramClass.isEnum()) {
                try {
                    return Enum.valueOf((Class<? extends Enum>) paramClass, stringValue.toUpperCase());
                } catch (Exception e) {
                    throw new ParameterException("Cannot resolve parameter " + stringValue + " for type " + paramClass.getSimpleName());
                }
            }

            ContextResolver<?> contextResolver = Resolvers.context(paramClass);
            if (contextResolver != null) {
                Object resolved = contextResolver.resolve(stringValue);
                if (resolved == null) {
                    throw new ParameterException("Cannot resolve parameter " + stringValue + " for type " + paramClass.getSimpleName());
                }

                return resolved;
            }
        }

        return value;
    }

    private List<String> parseUsageParameterNames(AnnotationSubCommand command) {
        List<String> paramNames = new ArrayList<>();

        if (command.usage() == null || command.usage().isEmpty())
            return paramNames;

        String usage = command.usage().trim();

        Pattern pattern = java.util.regex.Pattern.compile("[<\\[]([^>\\]]+)[>\\]]");
        Matcher matcher = pattern.matcher(usage);

        while (matcher.find()) {
            String paramName = matcher.group(1).trim();
            paramNames.add(paramName);
        }

        return paramNames;
    }

    @Override
    public String getCommandName() {
        return this.commandName;
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