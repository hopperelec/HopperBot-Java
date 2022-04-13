package uk.co.hopperelec.hopperbot;

import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public abstract class HopperBotFeature extends ListenerAdapter {
    public final HopperBotFeatures featureEnum;

    public HopperBotFeature(HopperBotFeatures featureEnum) {
        this.featureEnum = featureEnum;
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        getUtils().log("Feature loaded",null,featureEnum);
    }

    protected final HopperBotUtils getUtils() {
        return HopperBotUtils.getInstance();
    }
}
