package uk.co.hopperelec.hopperbot;

import net.dv8tion.jda.api.events.ReadyEvent;
import org.jetbrains.annotations.NotNull;

public class HopperBotAddon<A extends Enum<A>> extends HopperBotListener {
    @NotNull public final A addonEnum;
    @NotNull public final HopperBotFeatures parentFeatureEnum;
    protected final HopperBotFeature parentFeature;

    protected HopperBotAddon(@NotNull HopperBotFeatures parentFeatureEnum, @NotNull HopperBotFeature parentFeature, @NotNull A addonEnum) {
        this.parentFeatureEnum = parentFeatureEnum;
        this.parentFeature = parentFeature;
        this.addonEnum = addonEnum;
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        logGlobally("Addon '"+addonEnum+"' loaded",parentFeatureEnum);
    }
}
