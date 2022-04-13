package uk.co.hopperelec.hopperbot.Features.Economy;

import uk.co.hopperelec.hopperbot.Features.Economy.Addons.MMHM;
import uk.co.hopperelec.hopperbot.HopperBot;

import java.lang.reflect.Constructor;

public enum EconomyAddons {
    mmhm(true, MMHM.class);

    public final boolean configurable;
    public final Constructor<? extends EconomyAddon> handler;
    EconomyAddons(boolean configurable, Class<? extends EconomyAddon> featureHandler) {
        this.configurable = configurable;

        if (featureHandler == null) {
            handler = null;
        } else {
            Constructor<? extends EconomyAddon> setHandler;
            try {
                setHandler = featureHandler.getConstructor();
            } catch (NoSuchMethodException e) {
                HopperBot.logger.error("Could not find EconomyAddon constructor for feature {}",name());
                setHandler = null;
            }
            handler = setHandler;
        }
    }
}
