package uk.co.hopperelec.hopperbot.features.economy;

import org.jetbrains.annotations.NotNull;
import uk.co.hopperelec.hopperbot.HopperBotAddon;
import uk.co.hopperelec.hopperbot.HopperBotFeatures;

public class EconomyAddon extends HopperBotAddon<EconomyAddons> {
    public EconomyAddon(@NotNull EconomyFeature parentFeature, @NotNull EconomyAddons addonEnum) {
        super(HopperBotFeatures.ECONOMY, parentFeature, addonEnum);
    }
}
