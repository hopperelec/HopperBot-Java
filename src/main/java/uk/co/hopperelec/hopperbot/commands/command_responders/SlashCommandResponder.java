package uk.co.hopperelec.hopperbot.commands.command_responders;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;

public class SlashCommandResponder extends CommandResponder {
    @NotNull private final SlashCommandInteractionEvent event;
    private final boolean ephemeral;

    public SlashCommandResponder(@NotNull SlashCommandInteractionEvent event, boolean ephemeral) {
        this.event = event;
        this.ephemeral = ephemeral;
    }

    private void complete(@NotNull ReplyCallbackAction action, @NotNull Consumer<Message> onSent) {
        action.allowedMentions(allowedMentions).setEphemeral(ephemeral).queue(interactionHook -> {
            interactionHook.retrieveOriginal().queue(onSent);
        });
    }

    @Override
    public void respond(@NotNull String message, @NotNull Consumer<Message> onSent) {
        complete(event.reply(message), onSent);
    }
    @Override
    public void respond(@NotNull MessageEmbed embed, @NotNull Consumer<Message> onSent) {
        complete(event.replyEmbeds(embed), onSent);
    }
    @Override
    public void respond(@NotNull String message, @NotNull List<Button> buttons, @NotNull Consumer<Message> onSent) {
        complete(event.reply(message).addActionRow(buttons), onSent);
    }
    @Override
    public void respond(@NotNull MessageEmbed embed, @NotNull List<Button> buttons, @NotNull Consumer<Message> onSent) {
        complete(event.replyEmbeds(embed).addActionRow(buttons), onSent);
    }

}
