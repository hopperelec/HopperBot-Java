package uk.co.hopperelec.hopperbot;

import uk.co.hopperelec.hopperbot.features.*;
import uk.co.hopperelec.hopperbot.features.economy.EconomyFeature;

import java.lang.reflect.Constructor;

public enum HopperBotFeatures {
  LOG_COMMAND(false, LogCommandFeature.class),
  PURGING(false, PurgeCommandFeature.class),
  AUTO_VC_GEN(true, AutoVCGenFeature.class),
  POLLS(false, PollCommandFeature.class),
  INFO_COMMANDS(true, InfoCommandsFeature.class),
  ECONOMY(true, EconomyFeature.class),
  PLAYLIST(true, PlaylistFeature.class),
  ALCHEMY(true, AlchemyFeature.class),
  LEAVE_MESSAGES(true, LeaveMessagesFeature.class),
  SERVER_LIST(false, ServersCommandFeature.class);

  public final boolean configurable;
  public final Constructor<? extends HopperBotFeature> handler;
  HopperBotFeatures(boolean configurable, Class<? extends HopperBotFeature> featureHandler) {
    this.configurable = configurable;

    if (featureHandler == null) {
      handler = null;
    } else {
      Constructor<? extends HopperBotFeature> setHandler;
      try {
        setHandler = featureHandler.getConstructor();
      } catch (NoSuchMethodException e) {
        HopperBot.logger.error("Could not find HopperBotFeature constructor for feature {}",name());
        setHandler = null;
      }
      handler = setHandler;
    }
  }
}
