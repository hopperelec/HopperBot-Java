package uk.co.hopperelec.hopperbot.command_responders;

import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface CommandResponder {
    void respond(@NotNull String message);
    void respond(@NotNull MessageEmbed embed, @NotNull List<Button> buttons);
}
