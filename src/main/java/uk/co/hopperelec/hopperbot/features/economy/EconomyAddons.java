package uk.co.hopperelec.hopperbot.features.economy;

import uk.co.hopperelec.hopperbot.HopperBot;
import uk.co.hopperelec.hopperbot.features.economy.addons.MmhmAddon;

import java.lang.reflect.Constructor;

public enum EconomyAddons {
    mmhm(true, MmhmAddon.class);

    public final boolean configurable;
    public final Constructor<? extends EconomyAddon> handler;
    EconomyAddons(boolean configurable, Class<? extends EconomyAddon> addonHandler) {
        this.configurable = configurable;

        if (addonHandler == null) {
            handler = null;
        } else {
            Constructor<? extends EconomyAddon> setHandler;
            try {
                setHandler = addonHandler.getConstructor();
            } catch (NoSuchMethodException e) {
                HopperBot.logger.error("Could not find EconomyAddon constructor for feature {}",name());
                setHandler = null;
            }
            handler = setHandler;
        }
    }
}
