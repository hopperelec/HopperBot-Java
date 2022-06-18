package uk.co.hopperelec.hopperbot.commands;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import org.jetbrains.annotations.NotNull;
import uk.co.hopperelec.hopperbot.HopperBotFeatures;

public abstract class HopperBotButtonFeature extends HopperBotCommandFeature {
    public final String featureButtonPrefix;

    public HopperBotButtonFeature(String featureButtonPrefix, @NotNull JDABuilder builder, @NotNull HopperBotFeatures featureEnum, String commandPrefix) {
        super(builder,featureEnum,commandPrefix);
        this.featureButtonPrefix = featureButtonPrefix;
    }

    public abstract void runButtonCommand(@NotNull ButtonInteractionEvent event, @NotNull String[] parts);
}
