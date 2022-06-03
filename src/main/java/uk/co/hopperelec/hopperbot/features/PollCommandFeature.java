package uk.co.hopperelec.hopperbot.features;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;
import uk.co.hopperelec.hopperbot.*;

public final class PollCommandFeature extends HopperBotCommandFeature {
    public PollCommandFeature(@NotNull JDABuilder builder) {
        super(builder, HopperBotFeatures.POLLS, "?",
            new HopperBotCommand("poll","Generates a reaction poll",null,
                    new OptionData[]{new OptionData(OptionType.STRING,"question","Question to be voted on")},
                    CommandUsageFilter.NON_EMPTY_CONTENT
            ) {
                @Override
                public void runTextCommand(@NotNull MessageReceivedEvent event, @NotNull String content, @NotNull HopperBotCommandFeature feature, @NotNull HopperBotUtils utils) {
                    ((PollCommandFeature) feature).createPoll(event.getTextChannel(),content);
                }

                @Override
                public void runSlashCommand(@NotNull SlashCommandInteractionEvent event, @NotNull HopperBotCommandFeature feature, @NotNull HopperBotUtils utils) {
                    final OptionMapping option = event.getOption("question");
                    if (option != null) {
                        ((PollCommandFeature) feature).createPoll(event.getTextChannel(),option.getAsString());
                        event.reply("\uD83D\uDC4D").setEphemeral(true).queue();
                    }
                }
            }
        );
    }

    public void createPoll(@NotNull TextChannel channel, @NotNull String question) {
        channel.sendMessage(question).queue(message -> {
            message.addReaction("\uD83D\uDC4D").queue();
            message.addReaction("\uD83D\uDC4E").queue();
            message.addReaction("\uD83E\uDD37").queue();
        });
    }
}
