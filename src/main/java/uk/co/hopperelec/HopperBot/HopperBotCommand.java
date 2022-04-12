package uk.co.hopperelec.HopperBot;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.privileges.CommandPrivilege;

import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.stream;

public class HopperBotCommand {
    public final String name;
    public final String description;
    public final Set<String> aliases = new HashSet<>();
    public final SlashCommandAction slashCommandAction;
    public final TextCommandAction textCommandAction;
    public final CommandUsageFilter[] filters;
    public final SlashCommandData slashCommand;
    public final Set<CommandPrivilege> privileges = new HashSet<>();

    public HopperBotCommand(String name, String description, String[] aliases, OptionData[] options, SlashCommandAction slashCommandAction, TextCommandAction textCommandAction, CommandUsageFilter... filters) {
        this.name = name;
        this.description = description;
        this.aliases.add(name);
        if (aliases != null) {
            this.aliases.addAll(stream(aliases).toList());
        }
        this.slashCommandAction = slashCommandAction;
        this.textCommandAction = textCommandAction;
        this.filters = filters;

        this.slashCommand = Commands.slash(name,description);
        if (options != null) {
            this.slashCommand.addOptions(options);
        }
        if (filters.length != 0) {
            for (CommandUsageFilter filter : filters) {
                if (!filter.autoChecks.isEmpty()) {
                    slashCommand.setDefaultEnabled(false);
                    privileges.addAll(filter.autoChecks);
                }
            }
        }
    }

    public interface TextCommandAction {
        void op(MessageReceivedEvent event, String content, HopperBotCommandFeature feature, HopperBotUtils utils);
    }
    public interface SlashCommandAction {
        void op(SlashCommandInteractionEvent event, HopperBotCommandFeature feature, HopperBotUtils utils);
    }
}
