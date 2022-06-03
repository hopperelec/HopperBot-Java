package uk.co.hopperelec.hopperbot;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;

public abstract class HopperBotCommand {
    @NotNull public final String name;
    @NotNull public final String description;
    @NotNull public final Set<String> aliases = new HashSet<>();
    public final CommandUsageFilter[] filters;
    @NotNull public final SlashCommandData slashCommand;

    public HopperBotCommand(@NotNull String name, @NotNull String description, @Nullable String[] aliases, @Nullable OptionData[] options, CommandUsageFilter... filters) {
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

    public abstract void runTextCommand(@NotNull MessageReceivedEvent event, @NotNull String content, @NotNull HopperBotCommandFeature feature, @NotNull HopperBotUtils utils);
    public abstract void runSlashCommand(@NotNull SlashCommandInteractionEvent event, @NotNull HopperBotCommandFeature feature, @NotNull HopperBotUtils utils);
}
