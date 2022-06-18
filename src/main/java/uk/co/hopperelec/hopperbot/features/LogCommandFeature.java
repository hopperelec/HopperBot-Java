package uk.co.hopperelec.hopperbot.features;

import net.dv8tion.jda.api.JDABuilder;
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

public final class LogCommandFeature extends HopperBotCommandFeature {
    public LogCommandFeature(@NotNull JDABuilder builder) {
        super(builder, HopperBotFeatures.LOG_COMMAND, "!");
        addCommands(
            new HopperBotCommand<>(this, "log", "Debugging command for logging a message", null,
                    new OptionData[]{new OptionData(OptionType.STRING, "content", "The text to log",true)},
                    CommandUsageFilter.IS_BOT_OWNER, CommandUsageFilter.NON_EMPTY_CONTENT
            ) {
                @Override
                public void runTextCommand(@NotNull MessageReceivedEvent event, @NotNull String content) {
                    feature.getUtils().logGlobally(content,feature.featureEnum);
                    event.getMessage().addReaction("\uD83D\uDC4D").queue();
                }

                @Override
                public void runSlashCommand(@NotNull SlashCommandInteractionEvent event) {
                    final OptionMapping optionMapping = event.getOption("content");
                    if (optionMapping != null) {
                        feature.getUtils().logGlobally(optionMapping.getAsString(),feature.featureEnum);
                        event.reply("\uD83D\uDC4D").setEphemeral(true).queue();
                    }
                }
            }
        );
    }
}
