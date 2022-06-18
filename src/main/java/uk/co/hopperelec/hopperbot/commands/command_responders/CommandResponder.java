package uk.co.hopperelec.hopperbot.commands.command_responders;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.List;

public abstract class CommandResponder {
    public final @NotNull EnumSet<Message.MentionType> allowedMentions = EnumSet.allOf(Message.MentionType.class);

    public abstract void respond(@NotNull String message);
    public abstract void respond(@NotNull MessageEmbed embed);
    public abstract void respond(@NotNull String message, @NotNull List<Button> buttons);
    public abstract void respond(@NotNull MessageEmbed embed, @NotNull List<Button> buttons);
}
