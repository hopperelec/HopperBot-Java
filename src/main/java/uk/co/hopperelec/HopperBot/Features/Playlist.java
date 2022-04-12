package uk.co.hopperelec.HopperBot.Features;

import uk.co.hopperelec.HopperBot.HopperBotCommandFeature;
import uk.co.hopperelec.HopperBot.HopperBotFeatures;

public final class Playlist extends HopperBotCommandFeature {
    public Playlist() {
        super(HopperBotFeatures.playlist, "~");
    }
}
