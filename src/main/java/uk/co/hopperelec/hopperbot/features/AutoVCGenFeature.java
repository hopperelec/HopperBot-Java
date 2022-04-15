package uk.co.hopperelec.hopperbot.features;

import com.fasterxml.jackson.databind.JsonNode;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import org.jetbrains.annotations.NotNull;
import uk.co.hopperelec.hopperbot.HopperBotFeature;
import uk.co.hopperelec.hopperbot.HopperBotFeatures;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AutoVCGenFeature extends HopperBotFeature {
    public AutoVCGenFeature(JDABuilder builder) {
        super(builder,HopperBotFeatures.AUTO_VC_GEN);
    }

    private final Map<Guild, List<String>> channelNames =  new HashMap<>();
    private final Pattern pattern = Pattern.compile("^(.+) ([1-9]\\d*)$");

    @Override
    public void onGuildReady(@NotNull GuildReadyEvent event) {
        final Map<String,JsonNode> config = getUtils().getFeatureConfig(event.getGuild(),featureEnum);
        if (config != null) {
            channelNames.put(event.getGuild(),new ArrayList<>());
            for (JsonNode channelName : config.get("channel_names")) {
                channelNames.get(event.getGuild()).add(channelName.textValue());
            }
        }
    }

    private String[] getAutoVC(VoiceChannel channel, boolean size) {
        if (channel != null && channel.getMembers().size() == (size ? 1 : 0)) {
            final Matcher matcher = pattern.matcher(channel.getName());
            if (matcher.matches()) {
                final String name = matcher.group(1);
                if (channelNames.get(channel.getGuild()).contains(name)) {
                    return new String[]{name,matcher.group(2)};
                }
            }
        }
        return null;
    }

    @Override
    public void onGuildVoiceUpdate(@NotNull GuildVoiceUpdateEvent event) {
        VoiceChannel channel = (VoiceChannel) event.getChannelLeft();
        final String[] autoVCLeft = getAutoVC(channel,false);
        if (autoVCLeft != null) {
            channel.delete().queue();
            int count = Integer.parseInt(autoVCLeft[1]);
            while (true) {
                final List<VoiceChannel> nextChannels = channel.getGuild().getVoiceChannelsByName(autoVCLeft[0]+" "+(++count),false);
                if (nextChannels.size() != 1) {
                    if (nextChannels.size() != 0) {
                        getUtils().log("Multiple channels by name '"+autoVCLeft[0]+" "+count+"' in guild. Please only keep one.",channel.getGuild(),featureEnum);
                    }
                    break;
                }
                nextChannels.get(0).getManager().setName(autoVCLeft[0]+" "+(count-1)).queue();
            }
        }

        channel = (VoiceChannel) event.getChannelJoined();
        final String[] autoVCJoined = getAutoVC(channel,true);
        if (autoVCJoined != null) {
            int count = Integer.parseInt(autoVCJoined[1]);
            if (autoVCLeft != null && autoVCLeft[0].equals(autoVCJoined[0])) {
                count--;
            }
            channel.createCopy().setName(autoVCJoined[0]+" "+(count+1)).setPosition(channel.getPosition()).queue();
        }
    }
}
