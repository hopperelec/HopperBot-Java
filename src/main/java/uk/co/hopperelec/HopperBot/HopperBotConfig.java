package uk.co.hopperelec.HopperBot;

import java.util.Map;

public final class HopperBotConfig {
    private HopperBotFeatures[] enabledFeatures;
    private String logFormat;
    private Map<Long,HopperBotServerConfig> servers;

    public HopperBotConfig(HopperBotFeatures[] enabledFeatures, String logFormat, Map<Long,HopperBotServerConfig> servers) {
        this.enabledFeatures = enabledFeatures;
        this.logFormat = logFormat;
        this.servers = servers;
        if (!logFormat.contains("{message}")) {
            HopperBot.logger.warn("{message} is missing from the log_format meaning messages will be very arbitrary");
        }
    }
    public HopperBotConfig() {}

    public HopperBotFeatures[] getEnabledFeatures() {
        return enabledFeatures;
    }

    public String getLogFormat() {
        return logFormat;
    }

    public Map<Long,HopperBotServerConfig> getServers() {
        return servers;
    }

    public HopperBotServerConfig getServerConfig(Long id) {
        if (servers.get(id) == null) {
            HopperBot.logger.warn("Tried to get config for server ({}) but the server has not been configured!",id);
        }
        return servers.get(id);
    }
}
