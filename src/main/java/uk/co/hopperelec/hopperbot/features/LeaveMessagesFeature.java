package uk.co.hopperelec.hopperbot.features;

import com.fasterxml.jackson.databind.JsonNode;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import org.jetbrains.annotations.NotNull;
import uk.co.hopperelec.hopperbot.HopperBotFeature;
import uk.co.hopperelec.hopperbot.HopperBotFeatures;

import java.util.Map;

public final class LeaveMessagesFeature extends HopperBotFeature {
    public LeaveMessagesFeature(@NotNull JDABuilder builder) {
        super(builder,HopperBotFeatures.LEAVE_MESSAGES);
    }

    @Override
    public void onGuildMemberRemove(@NotNull GuildMemberRemoveEvent event) {
        final Map<String, JsonNode> config = getUtils().getFeatureConfig(event.getGuild(),featureEnum);
        if (config != null) {
            final TextChannel channel = event.getGuild().getTextChannelById(config.get("channel").asLong());
            if (channel != null) {
                channel.sendMessage(config.get("format").textValue()
                        .replaceAll("\\{name}",event.getUser().getName())
                        .replaceAll("\\{discriminator}",event.getUser().getDiscriminator())
                        .replaceAll("\\{id}",event.getUser().getId())
                ).queue();
            } else {
                getUtils().logToGuild("leave_messages has not been configured for this server",featureEnum,event.getGuild());
            }
        }
    }
}
