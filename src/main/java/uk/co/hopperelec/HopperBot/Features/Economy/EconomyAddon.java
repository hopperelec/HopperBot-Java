package uk.co.hopperelec.HopperBot.Features.Economy;

import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import uk.co.hopperelec.HopperBot.HopperBotFeatures;
import uk.co.hopperelec.HopperBot.HopperBotUtils;

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

    protected HopperBotUtils getUtils() {
        return HopperBotUtils.getInstance();
    }
}
