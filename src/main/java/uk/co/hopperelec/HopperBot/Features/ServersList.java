package uk.co.hopperelec.HopperBot.Features;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;
import uk.co.hopperelec.HopperBot.HopperBotCommandFeature;
import uk.co.hopperelec.HopperBot.HopperBotFeatures;
import uk.co.hopperelec.HopperBot.HopperBotServerConfig;

public final class ServersList extends HopperBotCommandFeature {
    private MessageEmbed embed;

    public ServersList() {
        super(HopperBotFeatures.server_list,"?");
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        super.onReady(event);
        final EmbedBuilder embedBuilder = getUtils().getEmbedBase();
        for (HopperBotServerConfig server : getUtils().config().getServers().values()) {
            if (server.getInvite() != null) {
                embedBuilder.addField(server.getName(),server.getInvite(),true);
            }
        }
        embed = embedBuilder.build();
        event.getJDA().upsertCommand("servers","Lists invites to all the public servers the bot is in").queue();
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getMessage().getContentRaw().matches("^(\\?)servers")) {
            event.getMessage().replyEmbeds(embed).queue();
        }
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (event.getName().equals("servers")) {
            event.replyEmbeds(embed).queue();
            getUtils().log(event.getUser().getId()+" successfully used global slash command /servers",event.getGuild(),featureEnum);
        }
    }
}
