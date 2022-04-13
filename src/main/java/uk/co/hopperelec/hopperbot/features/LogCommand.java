package uk.co.hopperelec.hopperbot.features;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import uk.co.hopperelec.hopperbot.*;

public final class LogCommand extends HopperBotCommandFeature {
    public LogCommand() {
        super(HopperBotFeatures.log_command, "!",
                new HopperBotCommand("log","Debugging command for logging a message",null, new OptionData[]{new OptionData(OptionType.STRING, "content", "The text to log")}) {
                    public void textCommand(MessageReceivedEvent event, String content, HopperBotCommandFeature feature, HopperBotUtils utils) {
                        utils.log(content, null, feature.featureEnum);
                        event.getMessage().addReaction("\uD83D\uDC4D").queue();
                    }
                    public void slashCommand(SlashCommandInteractionEvent event, HopperBotCommandFeature feature, HopperBotUtils utils) {
                        final OptionMapping optionMapping = event.getOption("content");
                        if (optionMapping != null) {
                            utils.log(optionMapping.getAsString(), null, feature.featureEnum);
                            event.reply("\uD83D\uDC4D").setEphemeral(true).queue();
                        }
                    }
                }
        );
    }
}
