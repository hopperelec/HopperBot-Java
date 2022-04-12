package uk.co.hopperelec.HopperBot.Features;

import com.fasterxml.jackson.databind.JsonNode;
import net.dv8tion.jda.api.entities.Guild;
import uk.co.hopperelec.HopperBot.HopperBotCommand;
import uk.co.hopperelec.HopperBot.HopperBotCommandFeature;
import uk.co.hopperelec.HopperBot.HopperBotFeatures;
import uk.co.hopperelec.HopperBot.HopperBotServerConfig;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class InfoCommands extends HopperBotCommandFeature {
    public InfoCommands() {
        super(HopperBotFeatures.info,"?");
    }

    @Override
    public Set<HopperBotCommand> getExtraCommands(Guild guild, HopperBotServerConfig serverConfig) {
        Set<HopperBotCommand> extraCommands = new HashSet<>();
        for (Map.Entry<String, JsonNode> commandConfig : serverConfig.getFeatureConfig(featureEnum).entrySet()) {
            final String desc = commandConfig.getValue().asText();
            final HopperBotCommand command = new HopperBotCommand(commandConfig.getKey(),desc,null,null,
                (slashEvent,feature,utils) -> {
                    slashEvent.reply(desc).queue();
                }, (textEvent,content,feature,utils) -> {
                    if (textEvent.getGuild() == guild) {
                        textEvent.getMessage().reply(desc).queue();
                    }
                }
            );
            commands.add(command);
            extraCommands.add(command);
        }
        return extraCommands;
    }
}
