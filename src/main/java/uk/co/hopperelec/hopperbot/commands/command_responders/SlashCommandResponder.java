package uk.co.hopperelec.hopperbot.commands.command_responders;

import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SlashCommandResponder implements CommandResponder {
    @NotNull private final SlashCommandInteractionEvent event;
    private final boolean ephemeral;

    public SlashCommandResponder(@NotNull SlashCommandInteractionEvent event, boolean ephemeral) {
        this.event = event;
        this.ephemeral = ephemeral;
    }

    @Override
    public void respond(@NotNull String message) {
        event.reply(message).setEphemeral(ephemeral).queue();
    }

    @Override
    public void respond(@NotNull MessageEmbed embed, @NotNull List<Button> buttons) {
        event.replyEmbeds(embed).addActionRow(buttons).setEphemeral(ephemeral).queue();
    }
}
