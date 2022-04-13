package uk.co.hopperelec.hopperbot.features.economy;

import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import uk.co.hopperelec.hopperbot.HopperBotFeatures;
import uk.co.hopperelec.hopperbot.HopperBotUtils;

public class EconomyAddon extends ListenerAdapter {
    public final EconomyAddons addonEnum;
    public final HopperBotFeatures featureEnum = HopperBotFeatures.economy;
    protected final Economy economy;

    public EconomyAddon(Economy economy, EconomyAddons addonEnum) {
        this.economy = economy;
        this.addonEnum = addonEnum;
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        getUtils().log("Addon '"+addonEnum+"' loaded",null,featureEnum);
    }

    protected final HopperBotUtils getUtils() {
        return HopperBotUtils.getInstance();
    }
}
