package uk.co.hopperelec.hopperbot.features;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;
import uk.co.hopperelec.hopperbot.HopperBotFeatures;
import uk.co.hopperelec.hopperbot.commands.CommandUsageFilter;
import uk.co.hopperelec.hopperbot.commands.HopperBotCommand;
import uk.co.hopperelec.hopperbot.commands.HopperBotCommandFeature;

public final class PollCommandFeature extends HopperBotCommandFeature {
    public PollCommandFeature(@NotNull JDABuilder builder) {
        super(builder, HopperBotFeatures.POLLS, "?");
        addCommands(
            new HopperBotCommand<>(this, "poll", "Generates a reaction poll", null,
                    new OptionData[]{new OptionData(OptionType.STRING,"question","Question to be voted on",true)},
                    CommandUsageFilter.NON_EMPTY_CONTENT
            ) {
                @Override
                public void runTextCommand(@NotNull MessageReceivedEvent event, @NotNull String content) {
                    createPoll(event.getTextChannel(),content);
                }

                @Override
                public void runSlashCommand(@NotNull SlashCommandInteractionEvent event) {
                    final OptionMapping option = event.getOption("question");
                    if (option != null) {
                        createPoll(event.getTextChannel(),option.getAsString());
                        event.reply("\uD83D\uDC4D").setEphemeral(true).queue();
                    }
                }
            }
        );
    }

    public static void createPoll(@NotNull TextChannel channel, @NotNull String question) {
        channel.sendMessage(question).queue(message -> {
            message.addReaction("\uD83D\uDC4D").queue();
            message.addReaction("\uD83D\uDC4E").queue();
            message.addReaction("\uD83E\uDD37").queue();
        });
    }
}
