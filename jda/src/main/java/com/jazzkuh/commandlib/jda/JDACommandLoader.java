package com.jazzkuh.commandlib.jda;

import lombok.Getter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class JDACommandLoader {
	private static final @Getter Set<CommandData> toPropagate = new HashSet<>();
	public static final Map<Class<?>, OptionType> DEFINITIONS;

	static {
		DEFINITIONS = new HashMap<>();

		DEFINITIONS.put(String.class, OptionType.STRING);
		DEFINITIONS.put(Integer.class, OptionType.INTEGER);
		DEFINITIONS.put(Boolean.class, OptionType.BOOLEAN);
		DEFINITIONS.put(User.class, OptionType.USER);
		DEFINITIONS.put(Member.class, OptionType.USER);
		DEFINITIONS.put(GuildChannel.class, OptionType.CHANNEL);
		DEFINITIONS.put(Role.class, OptionType.ROLE);
		DEFINITIONS.put(IMentionable.class, OptionType.MENTIONABLE);
		DEFINITIONS.put(Double.class, OptionType.NUMBER);
		DEFINITIONS.put(Long.class, OptionType.NUMBER);
		DEFINITIONS.put(Message.Attachment.class, OptionType.ATTACHMENT);
	}

	public static void propagate(JDA jda) {
		CommandListUpdateAction action = jda.updateCommands();
		action.addCommands(toPropagate).complete();
		System.out.println("Propagated " + toPropagate.size() + " commands.");
		toPropagate.clear();
	}
}
