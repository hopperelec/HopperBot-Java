package uk.co.hopperelec.hopperbot;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

public abstract class HopperBotButtonFeature extends HopperBotCommandFeature {
    public final String featureButtonPrefix;

    public HopperBotButtonFeature(String featureButtonPrefix, JDABuilder builder, HopperBotFeatures featureEnum, String commandPrefix, HopperBotCommand... commands) {
        super(builder,featureEnum,commandPrefix,commands);
        this.featureButtonPrefix = featureButtonPrefix;
    }

    public abstract void runButtonCommand(ButtonInteractionEvent event,String[] parts);
}
