package uk.co.hopperelec.hopperbot;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;

public abstract class HopperBotCommand {
    public final String name;
    public final String description;
    public final Set<String> aliases = new HashSet<>();
    public final CommandUsageFilter[] filters;
    public final SlashCommandData slashCommand;

    public HopperBotCommand(String name, String description, String[] aliases, OptionData[] options, CommandUsageFilter... filters) {
        this.name = name;
        this.description = description;
        this.aliases.add(name);
        if (aliases != null) {
            this.aliases.addAll(asList(aliases));
        }
        this.filters = filters;

        slashCommand = Commands.slash(name,description);
        if (options != null) {
            slashCommand.addOptions(options);
        }
    }

    public abstract void runTextCommand(MessageReceivedEvent event, String content, HopperBotCommandFeature feature, HopperBotUtils utils);
    public abstract void runSlashCommand(SlashCommandInteractionEvent event, HopperBotCommandFeature feature, HopperBotUtils utils);
}
