package com.jazzkuh.commandlib.jda;

import com.jazzkuh.commandlib.common.AnnotationCommandImpl;
import com.jazzkuh.commandlib.common.AnnotationCommandSender;
import com.jazzkuh.commandlib.common.annotations.*;
import com.jazzkuh.commandlib.jda.framework.AnnotationCommandExecutor;
import com.jazzkuh.commandlib.jda.framework.AnnotationCommandParser;
import com.jazzkuh.commandlib.jda.framework.AnnotationSubCommand;
import com.jazzkuh.commandlib.jda.framework.CommandParameter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AnnotationCommand extends ListenerAdapter implements AnnotationCommandImpl {
    private final String commandName;
    private AnnotationSubCommand mainCommand = null;
    private final List<AnnotationSubCommand> subCommands = new ArrayList<>();

    public AnnotationCommand(String commandName) {
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
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        SlashCommandInteraction interaction = event.getInteraction();
        if (!interaction.getName().equalsIgnoreCase(this.commandName)) return;

        String[] args = interaction.getOptions()
                .stream()
                .map(OptionMapping::getAsString)
                .toArray(String[]::new);

        if (args.length < 1) {
            if (this.mainCommand == null) {
                event.reply("Invalid usage.").queue();
                return;
            }

            this.executeCommand(this.mainCommand, event, args);
            return;
        }

        for (AnnotationSubCommand subCommand : subCommands) {
            if (!args[0].equalsIgnoreCase(subCommand.getName()) && !subCommand.getAliases().contains(args[0].toLowerCase())) continue;
            this.executeCommand(subCommand, event, args);
            return;
        }

        this.executeCommand(this.mainCommand, event, args);
    }

    private void executeCommand(AnnotationSubCommand subCommand, SlashCommandInteractionEvent event, String[] args) {
        // TODO permission check

        AnnotationCommandExecutor<SlashCommandInteractionEvent> commandExecutor = new AnnotationCommandExecutor<>(subCommand, this);
        AnnotationCommandSender<SlashCommandInteractionEvent> commandSender = new AnnotationCommandSender<>(event);

        AnnotationCommandExecutor.CommandResult commandResult = commandExecutor.execute(commandSender, args);
        switch (commandResult) {
            case NOT_ENOUGH_ARGUMENTS -> event.reply("Not enough arguments.").queue();
            case ERROR -> event.reply("An error occurred while executing the command.").queue();
            case NOT_ALLOWED -> event.reply("You are not allowed to execute this command.").queue();
            case CONTEXT_RESOLVER_NOT_FOUND -> event.reply("A context resolver for one of the parameters was not found.").queue();
            case PARAMETER_INVALID -> event.reply("One of the parameters is invalid.").queue();
        }
    }

    public void register(JDA jda) {
        SlashCommandData commandData = Commands.slash(commandName, mainCommand.getDescription() == null ? "No description." : mainCommand.getDescription());
        if (mainCommand != null) {
            for (CommandParameter parameter : mainCommand.getCommandParameters()) {
                OptionType type = JDACommandLoader.DEFINITIONS.get(parameter.getType());
                if (type == null) type = OptionType.STRING;

                commandData.addOption(
                        type,
                        parameter.getName(),
                        parameter.getDescription().isEmpty() ? "No description." : parameter.getDescription(),
                        !parameter.isOptional()
                );
            }
        }

        for (AnnotationSubCommand subCommand : subCommands) {
            SubcommandData subcommandData = new SubcommandData(subCommand.getName(), subCommand.getDescription());
            for (CommandParameter parameter : subCommand.getCommandParameters()) {
                OptionType type = JDACommandLoader.DEFINITIONS.get(parameter.getType());
                if (type == null) type = OptionType.STRING;

                subcommandData.addOption(
                        type,
                        parameter.getName(),
                        parameter.getDescription().isEmpty() ? "No description." : parameter.getDescription(),
                        !parameter.isOptional()
                );
            }
            commandData.addSubcommands(subcommandData);
        }

        System.out.println("Registered command " + commandName);
        JDACommandLoader.getToPropagate().add(commandData);
        jda.addEventListener(this);
    }
}
