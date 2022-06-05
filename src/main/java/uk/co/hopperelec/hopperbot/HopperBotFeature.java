package uk.co.hopperelec.hopperbot;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.ReadyEvent;
import org.jetbrains.annotations.NotNull;

public abstract class HopperBotFeature extends HopperBotListener {
    @NotNull public final HopperBotFeatures featureEnum;

    protected HopperBotFeature(@NotNull JDABuilder builder, @NotNull HopperBotFeatures featureEnum) {
        this.featureEnum = featureEnum;
        builder.addEventListeners(this);
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        getUtils().logGlobally("Feature loaded",featureEnum);
    }
}
