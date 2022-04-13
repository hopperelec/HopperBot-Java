package uk.co.hopperelec.hopperbot;

import uk.co.hopperelec.hopperbot.features.*;
import uk.co.hopperelec.hopperbot.features.economy.Economy;

import java.lang.reflect.Constructor;

public enum HopperBotFeatures {
  log_command(false,LogCommand.class),
  purge(false,PurgeCommand.class),
  autoVCGen(true,AutoVCGen.class),
  poll(false,PollCommand.class),
  info(true,InfoCommands.class),
  economy(true, Economy.class),
  playlist(true,Playlist.class),
  alchemy(true,Alchemy.class),
  leave_messages(true,LeaveMessages.class),
  server_list(false,ServersList.class);

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
