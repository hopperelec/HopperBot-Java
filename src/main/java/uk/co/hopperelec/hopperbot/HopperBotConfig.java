package uk.co.hopperelec.hopperbot;

import java.util.Map;

public final class HopperBotConfig {
    private HopperBotFeatures[] enabledFeatures;
    private String logFormat;
    private Map<Long,HopperBotServerConfig> servers;
    private long botOwnerId;
    private String botOwnerFallbackName;
    private String botOwnerFallbackIcon;

    public HopperBotConfig(HopperBotFeatures[] enabledFeatures, String logFormat, Map<Long,HopperBotServerConfig> servers, long botOwnerId, String botOwnerFallbackName, String botOwnerFallbackIcon) {
        this.enabledFeatures = enabledFeatures;
        this.logFormat = logFormat;
        this.servers = servers;
        this.botOwnerId = botOwnerId;
        this.botOwnerFallbackName = botOwnerFallbackName;
        this.botOwnerFallbackIcon = botOwnerFallbackIcon;
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

    public long getBotOwnerId() {
        return botOwnerId;
    }

    public String getBotOwnerFallbackName() {
        return botOwnerFallbackName;
    }

    public String getBotOwnerFallbackIcon() {
        return botOwnerFallbackIcon;
    }
}
