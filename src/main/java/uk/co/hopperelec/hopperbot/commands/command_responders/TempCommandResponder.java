package uk.co.hopperelec.hopperbot.commands.command_responders;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class TempCommandResponder extends CommandResponder {
    @NotNull private final Message eventMessage;
    public static final int DELETION_TIME = 10;

    public TempCommandResponder(@NotNull Message eventMessage) {
        this.eventMessage = eventMessage;
    }

    @Override
    public void respond(@NotNull String message, @NotNull Consumer<Message> onSent) {
        tempReply(eventMessage.reply(message).allowedMentions(allowedMentions),eventMessage);
    }
    @Override
    public void respond(@NotNull MessageEmbed embed, @NotNull Consumer<Message> onSent) {
        tempReply(eventMessage.replyEmbeds(embed).allowedMentions(allowedMentions),eventMessage);
    }
    @Override
    public void respond(@NotNull String message, @NotNull List<Button> buttons, @NotNull Consumer<Message> onSent) {
        tempReply(eventMessage.reply(message).setActionRow(buttons).allowedMentions(allowedMentions),eventMessage);
    }
    @Override
    public void respond(@NotNull MessageEmbed embed, @NotNull List<Button> buttons, @NotNull Consumer<Message> onSent) {
        tempReply(eventMessage.replyEmbeds(embed).setActionRow(buttons).allowedMentions(allowedMentions),eventMessage);
    }

    public static void tempReply(@NotNull MessageAction replyAction, @NotNull Message message, @NotNull Consumer<Message> onSent) {
        replyAction.queue(reply -> {
            reply.delete().queueAfter(DELETION_TIME, TimeUnit.SECONDS);
            message.delete().queueAfter(DELETION_TIME, TimeUnit.SECONDS);
            onSent.accept(reply);
        });
    }
    public static void tempReply(@NotNull MessageAction replyAction, @NotNull Message message) {
        tempReply(replyAction, message, m -> {});
    }
    public static void tempReply(@NotNull Message message, @NotNull String replyText) {
        tempReply(message.reply(replyText),message);
    }
}
