package uk.co.hopperelec.hopperbot.features.economy.addons;

import org.jetbrains.annotations.NotNull;
import uk.co.hopperelec.hopperbot.features.economy.EconomyFeature;
import uk.co.hopperelec.hopperbot.features.economy.EconomyAddon;
import uk.co.hopperelec.hopperbot.features.economy.EconomyAddons;

public final class MmhmAddon extends EconomyAddon {
    public MmhmAddon(@NotNull EconomyFeature economyFeature) {
        super(economyFeature, EconomyAddons.mmhm);
    }
}
