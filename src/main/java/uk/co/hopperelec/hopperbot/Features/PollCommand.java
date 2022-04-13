package uk.co.hopperelec.hopperbot.Features;

import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import uk.co.hopperelec.hopperbot.CommandUsageFilter;
import uk.co.hopperelec.hopperbot.HopperBotCommand;
import uk.co.hopperelec.hopperbot.HopperBotCommandFeature;
import uk.co.hopperelec.hopperbot.HopperBotFeatures;

public final class PollCommand extends HopperBotCommandFeature {
    public PollCommand() {
        super(HopperBotFeatures.poll, "?",
            new HopperBotCommand("poll","Generates a reaction poll",null, new OptionData[]{new OptionData(OptionType.STRING,"question","Question to be voted on")},
                (event,feature,utils) -> {
                    final OptionMapping option = event.getOption("question");
                    if (option != null) {
                        ((PollCommand) feature).createPoll(event.getTextChannel(),option.getAsString());
                        event.reply("\uD83D\uDC4D").setEphemeral(true).queue();
                    }
                }, (event,content,feature,utils) -> {
                    ((PollCommand) feature).createPoll(event.getTextChannel(),content);
                }, CommandUsageFilter.non_empty_content
            )
        );
    }

    public void createPoll(TextChannel channel, String question) {
        channel.sendMessage(question).queue(message -> {
            message.addReaction("\uD83D\uDC4D").queue();
            message.addReaction("\uD83D\uDC4E").queue();
            message.addReaction("\uD83E\uDD37").queue();
        });
    }
}
