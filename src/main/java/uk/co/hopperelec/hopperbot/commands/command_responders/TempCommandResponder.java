package uk.co.hopperelec.hopperbot.commands.command_responders;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class TempCommandResponder implements CommandResponder {
    @NotNull private final Message eventMessage;
    public static final int DELETION_TIME = 10;

    public TempCommandResponder(@NotNull Message eventMessage) {
        this.eventMessage = eventMessage;
    }

    @Override
    public void respond(@NotNull String message) {
        tempReply(eventMessage, message);
    }

    @Override
    public void respond(@NotNull MessageEmbed embed, @NotNull List<Button> buttons) {
        tempReply(eventMessage, embed);
        eventMessage.replyEmbeds(embed).setActionRow(buttons).queue();
    }

    public static void tempReply(@NotNull Message message, @NotNull String reply) {
        message.reply(reply).queue(replyMsg -> {
            replyMsg.delete().queueAfter(DELETION_TIME, TimeUnit.SECONDS);
            message.delete().queueAfter(DELETION_TIME, TimeUnit.SECONDS);
        });
    }

    public static void tempReply(@NotNull Message message, @NotNull MessageEmbed embed) {
        message.replyEmbeds(embed).queue(replyMsg -> {
            replyMsg.delete().queueAfter(DELETION_TIME, TimeUnit.SECONDS);
            message.delete().queueAfter(DELETION_TIME, TimeUnit.SECONDS);
        });
    }
}
