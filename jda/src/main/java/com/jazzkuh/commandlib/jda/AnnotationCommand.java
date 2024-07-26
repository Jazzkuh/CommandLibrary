package com.jazzkuh.commandlib.jda;

import com.jazzkuh.commandlib.common.AnnotationCommandImpl;
import com.jazzkuh.commandlib.common.AnnotationCommandSender;
import com.jazzkuh.commandlib.common.annotations.*;
import com.jazzkuh.commandlib.common.exception.*;
import com.jazzkuh.commandlib.jda.framework.JDACommandExecutor;
import com.jazzkuh.commandlib.jda.framework.JDACommandParser;
import com.jazzkuh.commandlib.jda.framework.JDASubCommand;
import com.jazzkuh.commandlib.jda.framework.CommandParameter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
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
    private JDASubCommand mainCommand = null;
    private final List<JDASubCommand> subCommands = new ArrayList<>();

    public AnnotationCommand(String commandName) {
        this.commandName = commandName;

        List<Method> mainCommands = Arrays.stream(this.getClass().getMethods()).filter(method -> method.isAnnotationPresent(Main.class)).toList();
        if (mainCommands.size() > 1) {
            throw new IllegalArgumentException("There can only be one main command per class");
        }
        mainCommands.forEach(method -> this.mainCommand = JDACommandParser.parse(this, method));

        List<Method> subcommandMethods = Arrays.stream(this.getClass().getMethods()).filter(method -> method.isAnnotationPresent(Subcommand.class)).toList();
        subcommandMethods.forEach(method -> this.subCommands.add(JDACommandParser.parse(this, method)));
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

        for (JDASubCommand subCommand : subCommands) {
            if (!args[0].equalsIgnoreCase(subCommand.getName()) && !subCommand.getAliases().contains(args[0].toLowerCase())) continue;
            this.executeCommand(subCommand, event, args);
            return;
        }

        this.executeCommand(this.mainCommand, event, args);
    }

    private void executeCommand(JDASubCommand subCommand, SlashCommandInteractionEvent event, String[] args) {
        if (subCommand.getPermission() != null && !event.getMember().hasPermission(subCommand.getPermission())) {
            event.reply("You do not have permission to execute this command.").queue();
            return;
        }

        JDACommandExecutor<SlashCommandInteractionEvent> commandExecutor = new JDACommandExecutor<>(subCommand, this);
        AnnotationCommandSender<SlashCommandInteractionEvent> commandSender = new AnnotationCommandSender<>(event);

        try {
            commandExecutor.execute(commandSender, args);
        } catch (CommandException commandException) {
            if (commandException instanceof ArgumentException) {
                event.getChannel().sendMessage("Not enough arguments.").queue();
            } else if (commandException instanceof PermissionException permissionException) {
                event.getChannel().sendMessage(permissionException.getMessage()).queue();
            } else if (commandException instanceof ContextResolverException contextResolverException) {
                event.getChannel().sendMessage("A context resolver was not found for: " + contextResolverException.getMessage()).queue();
            } else if (commandException instanceof ParameterException parameterException) {
                event.getChannel().sendMessage(parameterException.getMessage()).queue();
            } else if (commandException instanceof ErrorException errorException) {
                event.getChannel().sendMessage("An error occurred while executing this subcommand: " + errorException.getMessage()).queue();
            }
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

        for (JDASubCommand subCommand : subCommands) {
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
