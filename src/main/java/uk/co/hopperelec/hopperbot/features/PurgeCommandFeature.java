package uk.co.hopperelec.hopperbot.features;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;
import uk.co.hopperelec.hopperbot.HopperBotFeature;
import uk.co.hopperelec.hopperbot.HopperBotFeatures;
import uk.co.hopperelec.hopperbot.commands.CommandUsageFilter;
import uk.co.hopperelec.hopperbot.commands.HopperBotCommand;
import uk.co.hopperelec.hopperbot.commands.HopperBotCommandFeature;
import uk.co.hopperelec.hopperbot.commands.command_responders.CommandResponder;
import uk.co.hopperelec.hopperbot.commands.command_responders.SlashCommandResponder;
import uk.co.hopperelec.hopperbot.commands.command_responders.TempCommandResponder;

import java.util.concurrent.ExecutionException;

import static uk.co.hopperelec.hopperbot.commands.command_responders.TempCommandResponder.tempReply;

public final class PurgeCommandFeature extends HopperBotCommandFeature {
    public PurgeCommandFeature(@NotNull JDABuilder builder) {
        super(builder, HopperBotFeatures.PURGING, "!");
        addCommands(
            new HopperBotCommand<>(this, "purge", "Moderation command for deleting up to 500 messages in bulk", null,
                    new OptionData[]{new OptionData(OptionType.INTEGER,"limit","Number of messages to delete",true).setRequiredRange(1,500)},
                    CommandUsageFilter.HAS_MANAGE_MESSAGES
            ) {
                @Override
                public void runTextCommand(@NotNull MessageReceivedEvent event, @NotNull String content) {
                    if (event.getMember() != null) {
                        final String limitStr = content.replace(" ", "");
                        if (limitStr.equals("")) {
                            tempReply(event.getMessage(),"Please specify the number of messages to delete (up to 500)!");
                        } else {
                            final int limit;
                            try {
                                limit = Integer.parseInt(limitStr);
                            } catch (NumberFormatException e) {
                                tempReply(event.getMessage(),"Unknown number '"+limitStr+"'");
                                return;
                            }
                            if (limit < 1) {
                                tempReply(event.getMessage(),"Cannot purge less than 1 messages!");
                            } else if (limit > 500) {
                                tempReply(event.getMessage(),"Cannot purge more than 500 messages!");
                            } else {
                                purgeMessages(feature, new TempCommandResponder(event.getMessage()),event.getTextChannel(),limit);
                            }
                        }
                    }
                }

                @Override
                public void runSlashCommand(@NotNull SlashCommandInteractionEvent event) {
                    OptionMapping option = event.getOption("limit");
                    if (option != null) {
                        purgeMessages(feature, new SlashCommandResponder(event,true),event.getTextChannel(),option.getAsLong());
                    }
                }
            }
        );
    }

    public static void purgeMessages(@NotNull HopperBotFeature feature, @NotNull CommandResponder responder, @NotNull TextChannel channel, long limit) {
        channel.getIterableHistory().takeAsync((int) limit).thenAccept(messages -> {
                channel.purgeMessages(messages).forEach(future -> {
                    try {
                        future.get();
                    } catch (InterruptedException | ExecutionException e) {
                        responder.respond("Failed to purge some messages");
                        logToGuild("Failed to purge some messages: "+e.getMessage(), feature.featureEnum, channel.getGuild());
                    }
                });
            responder.respond("Messages purged!");
            }
        );
    }
}
