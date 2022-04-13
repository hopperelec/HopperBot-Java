package uk.co.hopperelec.HopperBot;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;
import java.util.Set;

public final class HopperBotServerConfig {
    private String name;
    private String invite;
    private long logChannel;
    private Set<HopperBotFeatures> extraFeatures;
    private Map<HopperBotFeatures,Map<String, JsonNode>> featureConfigs;

    public static class NonConfigurableFeatureException extends Exception {
        public NonConfigurableFeatureException(HopperBotFeatures feature) {
            super("The feature '"+feature+"' has been referred to in the feature_configs but cannot be configured");
        }
    }

    public HopperBotServerConfig(String name, String invite, long logChannel, Set<HopperBotFeatures> extraFeatures, Map<HopperBotFeatures,Map<String,JsonNode>> featureConfigs) throws NonConfigurableFeatureException {
        this.name = name;
        this.invite = invite;
        this.logChannel = logChannel;
        this.extraFeatures = extraFeatures;
        this.featureConfigs = featureConfigs;

        for (HopperBotFeatures feature : featureConfigs.keySet()) {
            if (!feature.configurable) {
                throw new NonConfigurableFeatureException(feature);
            }
        }
    }
    public HopperBotServerConfig() {}

    public String getName() {
        return name;
    }

    public String getInvite() {
        if (invite == null) return null;
        return "https://discord.gg/"+invite;
    }

    public long getLogChannel() {
        return logChannel;
    }

    public Set<HopperBotFeatures> getExtraFeatures() {
        return extraFeatures;
    }

    public boolean usesFeature(HopperBotFeatures feature) {
        return extraFeatures.contains(feature);
    }

    public Map<HopperBotFeatures,Map<String,JsonNode>> getFeatureConfigs() {
        return featureConfigs;
    }

    public Map<String,JsonNode> getFeatureConfig(HopperBotFeatures feature) {
        return featureConfigs.get(feature);
    }
}
