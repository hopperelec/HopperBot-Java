package uk.co.hopperelec.hopperbot;

import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class HopperBotListener extends ListenerAdapter {
    protected final HopperBotUtils getUtils() {
        return HopperBotUtils.getInstance();
    }
}
