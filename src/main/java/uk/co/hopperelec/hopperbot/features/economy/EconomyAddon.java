package uk.co.hopperelec.hopperbot.features.economy;

import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import uk.co.hopperelec.hopperbot.HopperBotFeatures;
import uk.co.hopperelec.hopperbot.HopperBotUtils;

public class EconomyAddon extends ListenerAdapter {
    @NotNull public final EconomyAddons addonEnum;
    @NotNull public final HopperBotFeatures featureEnum = HopperBotFeatures.ECONOMY;
    protected final EconomyFeature economyFeature;

    public EconomyAddon(@NotNull EconomyFeature economyFeature, @NotNull EconomyAddons addonEnum) {
        this.economyFeature = economyFeature;
        this.addonEnum = addonEnum;
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        getUtils().logGlobally("Addon '"+addonEnum+"' loaded",featureEnum);
    }

    protected final HopperBotUtils getUtils() {
        return HopperBotUtils.getInstance();
    }
}
