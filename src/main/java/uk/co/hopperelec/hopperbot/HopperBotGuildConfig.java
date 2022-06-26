package uk.co.hopperelec.hopperbot;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.CheckForNull;
import javax.annotation.CheckReturnValue;
import java.util.EnumSet;
import java.util.Map;

public record HopperBotGuildConfig(
        @NotNull String name,
        @Nullable String inviteCode,
        long logChannel,
        @NotNull ImmutableSet<HopperBotFeatures> extraFeatures,
        @NotNull ImmutableMap<HopperBotFeatures,Map<String, JsonNode>> featureConfigs
) {
    @JsonCreator
    public HopperBotGuildConfig(
            @NotNull @JsonProperty("name") String name,
            @Nullable @JsonProperty("invite_code") @JsonAlias("invite") String inviteCode,
            @JsonProperty("log_channel") long logChannel,
            @NotNull @JsonProperty("extra_features") EnumSet<HopperBotFeatures> extraFeatures,
            @NotNull @JsonProperty("feature_configs") Map<HopperBotFeatures,Map<String,JsonNode>> featureConfigs
    ) {
        this(name,inviteCode,logChannel,ImmutableSet.copyOf(extraFeatures),filterConfigurableFeatures(name,extraFeatures,featureConfigs));
    }

    @NotNull
    @CheckReturnValue
    private static ImmutableMap<HopperBotFeatures,Map<String,JsonNode>> filterConfigurableFeatures(
            @NotNull String name,
            @NotNull EnumSet<HopperBotFeatures> extraFeatures,
            @NotNull Map<HopperBotFeatures,Map<String,JsonNode>> featureConfigs
    ) {
        for (HopperBotFeatures feature : featureConfigs.keySet()) {
            if (!feature.configurable) {
                HopperBot.logger.warn("Guild config for '{}' has unnecessarily tried to configure the unconfigurable feature {}",name,feature);
                featureConfigs.remove(feature);
            }
            if (!extraFeatures.contains(feature)) {
                HopperBot.logger.warn("Guild config for '{}' has unnecessarily configured {} when it is not included it in extra_features",name,feature);
                featureConfigs.remove(feature);
            }
        }
        return ImmutableMap.copyOf(featureConfigs);
    }

    @Nullable
    @CheckForNull
    @CheckReturnValue
    public String getInvite() {
        return inviteCode == null ? null : "https://discord.gg/"+inviteCode;
    }

    @CheckReturnValue
    public boolean usesFeature(HopperBotFeatures feature) {
        return extraFeatures.contains(feature);
    }

    @Nullable
    @CheckReturnValue
    public Map<String,JsonNode> getFeatureConfig(HopperBotFeatures feature) {
        return featureConfigs.get(feature);
    }
}
