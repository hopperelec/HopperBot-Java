package uk.co.hopperelec.hopperbot.features;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import uk.co.hopperelec.hopperbot.*;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;


public final class PurgeCommand extends HopperBotCommandFeature {
    public PurgeCommand() {
        super(HopperBotFeatures.purge, "!",
            new HopperBotCommand("purge","Moderation command for deleting up to 500 messages in bulk",null,
                new OptionData[]{new OptionData(OptionType.INTEGER,"limit","Number of messages to delete").setRequiredRange(1,500)},
                CommandUsageFilter.has_manage_messages
            ) {
                public void runTextCommand(MessageReceivedEvent event, String content, HopperBotCommandFeature feature, HopperBotUtils utils) {
                    if (event.getMember() != null && event.getMember().hasPermission(Permission.MESSAGE_MANAGE)) {
                        final String limitStr = content.replace(" ", "");
                        if (limitStr.equals("")) {
                            utils.tempReply(event.getMessage(),"Please specify the number of messages to delete (up to 500)!");
                        } else {
                            final int limit;
                            try {
                                limit = Integer.parseInt(limitStr);
                            } catch (NumberFormatException e) {
                                utils.tempReply(event.getMessage(),"Unknown number '"+limitStr+"'");
                                return;
                            }
                            if (limit < 1) {
                                utils.tempReply(event.getMessage(),"Cannot purge less than 1 messages!");
                            } else if (limit > 500) {
                                utils.tempReply(event.getMessage(),"Cannot purge more than 500 messages!");
                            } else {
                                event.getMessage().delete().queueAfter(5, TimeUnit.SECONDS);
                                ((PurgeCommand) feature).purgeMessages(event.getTextChannel(),limit,message -> {
                                    event.getMessage().reply(message).queue();
                                    event.getMessage().delete().queueAfter(5, TimeUnit.SECONDS);
                                });
                            }
                        }
                    }
                }
                public void runSlashCommand(SlashCommandInteractionEvent event, HopperBotCommandFeature feature, HopperBotUtils utils) {
                    OptionMapping option = event.getOption("limit");
                    if (option != null) {
                        ((PurgeCommand) feature).purgeMessages(event.getTextChannel(),option.getAsLong(),message -> event.reply(message).setEphemeral(true).queue());
                    }
                }
            }
        );
    }

    public void purgeMessages(TextChannel channel, long limit, Consumer<String> reply) {
        channel.getIterableHistory().takeAsync((int) limit).thenAccept(messages ->
                channel.purgeMessages(messages).forEach(future -> {
                    try {
                        future.get();
                    } catch (InterruptedException | ExecutionException e) {
                        reply.accept("Failed to purge some messages");
                        getUtils().log("Failed to purge some messages: " + e.getMessage(), channel.getGuild(), featureEnum);
                        return;
                    }
                    reply.accept("Messages purged!");
                })
        );
    }
}
