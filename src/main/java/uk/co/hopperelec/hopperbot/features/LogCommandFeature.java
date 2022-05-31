package uk.co.hopperelec.hopperbot.features;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import uk.co.hopperelec.hopperbot.*;

public final class LogCommandFeature extends HopperBotCommandFeature {
    public LogCommandFeature(JDABuilder builder) {
        super(builder, HopperBotFeatures.LOG_COMMAND, "!",
                new HopperBotCommand("log","Debugging command for logging a message",null, new OptionData[]{new OptionData(OptionType.STRING, "content", "The text to log")}) {
                    @Override
                    public void runTextCommand(MessageReceivedEvent event, String content, HopperBotCommandFeature feature, HopperBotUtils utils) {
                        utils.logGlobally(content,feature.featureEnum);
                        event.getMessage().addReaction("\uD83D\uDC4D").queue();
                    }

                    @Override
                    public void runSlashCommand(SlashCommandInteractionEvent event, HopperBotCommandFeature feature, HopperBotUtils utils) {
                        final OptionMapping optionMapping = event.getOption("content");
                        if (optionMapping != null) {
                            utils.logGlobally(optionMapping.getAsString(),feature.featureEnum);
                            event.reply("\uD83D\uDC4D").setEphemeral(true).queue();
                        }
                    }
                }
        );
    }
}
