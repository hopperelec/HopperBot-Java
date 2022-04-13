package uk.co.hopperelec.hopperbot.features;

import uk.co.hopperelec.hopperbot.HopperBotCommandFeature;
import uk.co.hopperelec.hopperbot.HopperBotFeatures;

public final class Playlist extends HopperBotCommandFeature {
    public Playlist() {
        super(HopperBotFeatures.playlist, "~");
    }
}
