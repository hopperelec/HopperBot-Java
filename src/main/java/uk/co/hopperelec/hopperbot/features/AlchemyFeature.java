package uk.co.hopperelec.hopperbot.features;

import net.dv8tion.jda.api.JDABuilder;
import org.jetbrains.annotations.NotNull;
import uk.co.hopperelec.hopperbot.HopperBotCommandFeature;
import uk.co.hopperelec.hopperbot.HopperBotFeatures;

public final class AlchemyFeature extends HopperBotCommandFeature {
    public AlchemyFeature(@NotNull JDABuilder builder) {
        super(builder, HopperBotFeatures.ALCHEMY, "a!");
    }
}
