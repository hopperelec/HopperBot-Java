package uk.co.hopperelec.hopperbot.commands.command_responders;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.List;
import java.util.function.Consumer;

public abstract class CommandResponder {
    public final @NotNull EnumSet<Message.MentionType> allowedMentions = EnumSet.allOf(Message.MentionType.class);

    public void respond(@NotNull String message) {
        respond(message, m -> {});
    }
    public void respond(@NotNull MessageEmbed embed) {
        respond(embed, m -> {});
    }
    public void respond(@NotNull String message, @NotNull List<Button> buttons) {
        respond(message, buttons, m -> {});
    }
    public void respond(@NotNull MessageEmbed embed, @NotNull List<Button> buttons) {
        respond(embed, buttons, m -> {});
    }
    public abstract void respond(@NotNull String message, @NotNull Consumer<Message> onSent);
    public abstract void respond(@NotNull MessageEmbed embed, @NotNull Consumer<Message> onSent);
    public abstract void respond(@NotNull String message, @NotNull List<Button> buttons, @NotNull Consumer<Message> onSent);
    public abstract void respond(@NotNull MessageEmbed embed, @NotNull List<Button> buttons, @NotNull Consumer<Message> onSent);
}
