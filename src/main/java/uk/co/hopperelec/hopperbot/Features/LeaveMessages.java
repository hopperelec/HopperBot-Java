package uk.co.hopperelec.hopperbot.Features;

import com.fasterxml.jackson.databind.JsonNode;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import uk.co.hopperelec.hopperbot.HopperBotFeature;
import uk.co.hopperelec.hopperbot.HopperBotFeatures;

import java.util.Map;

public final class LeaveMessages extends HopperBotFeature {
    public LeaveMessages() {
        super(HopperBotFeatures.leave_messages);
    }

    @Override
    public void onGuildMemberRemove(GuildMemberRemoveEvent event) {
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
                getUtils().log("leave_messages has not been configured for this server",event.getGuild(),featureEnum);
            }
        }
    }
}
