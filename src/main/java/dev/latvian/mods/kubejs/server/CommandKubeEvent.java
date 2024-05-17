package dev.latvian.mods.kubejs.server;

import com.mojang.brigadier.ParseResults;
import dev.architectury.event.events.common.CommandPerformEvent;
import net.minecraft.commands.CommandSourceStack;

public class CommandKubeEvent extends ServerKubeEvent {
	private final CommandPerformEvent event;
	private final String commandName;

	public CommandKubeEvent(CommandPerformEvent e) {
		super(e.getResults().getContext().getSource().getServer());
		event = e;
		commandName = event.getResults().getContext().getNodes().isEmpty() ? "" : event.getResults().getContext().getNodes().get(0).getNode().getName();
	}

	public String getCommandName() {
		return commandName;
	}

	public String getInput() {
		return event.getResults().getReader().getString();
	}

	public ParseResults<CommandSourceStack> getParseResults() {
		return event.getResults();
	}

	public void setParseResults(ParseResults<CommandSourceStack> parse) {
		event.setResults(parse);
	}

	public Throwable getException() {
		return event.getThrowable();
	}

	public void setException(Throwable exception) {
		event.setThrowable(exception);
	}
}