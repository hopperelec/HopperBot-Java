package uk.co.hopperelec.hopperbot.features;

import com.fasterxml.jackson.databind.JsonNode;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;
import uk.co.hopperelec.hopperbot.*;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class InfoCommandsFeature extends HopperBotCommandFeature {
    public InfoCommandsFeature(@NotNull JDABuilder builder) {
        super(builder,HopperBotFeatures.INFO_COMMANDS,"?");
    }

    @Override
    @NotNull
    public Set<HopperBotCommand> getExtraCommands(@NotNull Guild guild, @NotNull HopperBotServerConfig serverConfig) {
        Set<HopperBotCommand> extraCommands = new HashSet<>();
        for (Map.Entry<String, JsonNode> commandConfig : serverConfig.getFeatureConfig(featureEnum).entrySet()) {
            final String desc = commandConfig.getValue().asText();
            final HopperBotCommand command = new HopperBotCommand(commandConfig.getKey(),desc,null,null) {
                @Override
                public void runTextCommand(@NotNull MessageReceivedEvent event, @NotNull String content, @NotNull HopperBotCommandFeature feature, @NotNull HopperBotUtils utils) {
                    if (event.getGuild() == guild) {
                        event.getMessage().reply(desc).queue();
                    }
                }

                @Override
                public void runSlashCommand(@NotNull SlashCommandInteractionEvent event, @NotNull HopperBotCommandFeature feature, @NotNull HopperBotUtils utils) {
                    event.reply(desc).queue();
                }
            };
            commands.add(command);
            extraCommands.add(command);
        }
        return extraCommands;
    }
}
