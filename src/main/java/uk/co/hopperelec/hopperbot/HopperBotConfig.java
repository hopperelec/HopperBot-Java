package uk.co.hopperelec.hopperbot;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.Map;

public record HopperBotConfig(
        @NotNull ImmutableSet<HopperBotFeatures> enabledFeatures,
        @NotNull String logFormat,
        @NotNull ImmutableMap<Long, HopperBotGuildConfig> guilds,
        long botOwnerId,
        @NotNull String botOwnerFallbackName,
        @NotNull String botOwnerFallbackIcon
) {
    @JsonCreator
    public HopperBotConfig(
            @NotNull @JsonProperty("enabled_features") EnumSet<HopperBotFeatures> enabledFeatures,
            @NotNull @JsonProperty("log_format") String logFormat,
            @NotNull @JsonProperty("guilds") @JsonAlias("servers") Map<Long, HopperBotGuildConfig> guilds,
            @NotNull @JsonProperty("bot_owner") Map<String, JsonNode> botOwner
    ) {
        this(
                Sets.immutableEnumSet(enabledFeatures),
                logFormat,
                ImmutableMap.copyOf(guilds),
                botOwner.get("id").asLong(),
                botOwner.get("fallback_name").asText(),
                botOwner.get("fallback_icon").asText()
        );
        if (!logFormat.contains("{message}")) {
            HopperBot.logger.warn("{message} is missing from the log_format meaning messages will be very arbitrary");
        }
    }
}
