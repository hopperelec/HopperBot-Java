package uk.co.hopperelec.HopperBot.Features;

import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import uk.co.hopperelec.HopperBot.CommandUsageFilter;
import uk.co.hopperelec.HopperBot.HopperBotCommand;
import uk.co.hopperelec.HopperBot.HopperBotCommandFeature;
import uk.co.hopperelec.HopperBot.HopperBotFeatures;

public final class LogCommand extends HopperBotCommandFeature {
    public LogCommand() {
        super(HopperBotFeatures.log_command, "!",
                new HopperBotCommand("log","Debugging command for logging a message",null, new OptionData[]{new OptionData(OptionType.STRING, "content", "The text to log")},
                    (event,feature,utils) -> {
                        final OptionMapping optionMapping = event.getOption("content");
                        if (optionMapping != null) {
                            utils.log(optionMapping.getAsString(), null, feature.featureEnum);
                            event.reply("\uD83D\uDC4D").setEphemeral(true).queue();
                        }
                    }, (event,content,feature,utils) -> {
                        utils.log(content, null, feature.featureEnum);
                        event.getMessage().addReaction("\uD83D\uDC4D").queue();
                    }, CommandUsageFilter.is_bot_owner, CommandUsageFilter.non_empty_content
                )
        );
    }
}
