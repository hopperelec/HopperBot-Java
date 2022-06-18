package uk.co.hopperelec.hopperbot.commands.command_responders;

import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SlashCommandResponder extends CommandResponder {
    @NotNull private final SlashCommandInteractionEvent event;
    private final boolean ephemeral;

    public SlashCommandResponder(@NotNull SlashCommandInteractionEvent event, boolean ephemeral) {
        this.event = event;
        this.ephemeral = ephemeral;
    }

    private void complete(@NotNull ReplyCallbackAction action) {
        action.allowedMentions(allowedMentions).setEphemeral(ephemeral).queue();
    }

    @Override
    public void respond(@NotNull String message) {
        complete(event.reply(message));
    }
    @Override
    public void respond(@NotNull MessageEmbed embed) {
        complete(event.replyEmbeds(embed));
    }
    @Override
    public void respond(@NotNull String message, @NotNull List<Button> buttons) {
        complete(event.reply(message).addActionRow(buttons));
    }
    @Override
    public void respond(@NotNull MessageEmbed embed, @NotNull List<Button> buttons) {
        complete(event.replyEmbeds(embed).addActionRow(buttons));
    }

}
