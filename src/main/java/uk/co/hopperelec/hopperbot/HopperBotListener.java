package uk.co.hopperelec.hopperbot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.CheckReturnValue;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Map;

public class HopperBotListener extends ListenerAdapter {
    private static JDA jda = null;
    private static HopperBotConfig config = null;

    synchronized public static void setJDA(@NotNull JDA jda) {
        if (HopperBotListener.jda != null) {
            throw new IllegalStateException("HopperBotListener::setJDA used when HopperBotListener.jda has already been set");
        }
        HopperBotListener.jda = jda;
    }
    synchronized public static void setConfig(@NotNull HopperBotConfig config) {
        if (HopperBotListener.config != null) {
            throw new IllegalStateException("HopperBotListener::setConfig used when HopperBotListener.config has already been set");
        }
        HopperBotListener.config = config;
    }

    @CheckReturnValue
    protected static JDA getJDA() {
        if (config == null) {
            throw new IllegalStateException("HopperBotListener::getJDA used before HopperBotListener::setJDA");
        }
        return jda;
    }
    @CheckReturnValue
    public static HopperBotConfig getConfig() {
        if (config == null) {
            throw new IllegalStateException("HopperBotListener::getConfig used before HopperBotListener::setConfig");
        }
        return config;
    }

    synchronized protected static void logToGuild(@NotNull String message, @NotNull Guild guild) {
        if (config != null) {
            final HopperBotServerConfig guildConfig = getServerConfig(guild.getIdLong());
            if (guildConfig != null) {
                final long channelId = guildConfig.getLogChannel();
                final TextChannel logChannel = guild.getTextChannelById(channelId);
                if (logChannel == null) {
                    HopperBot.logger.error("The log channel for {} ({}) could not be found!", guildConfig.getName(), channelId);
                } else {
                    logChannel.sendMessage(message).queue();
                }
            }
        }
    }

    @NotNull
    synchronized protected static String log(@NotNull String message, @Nullable HopperBotFeatures feature) {
        final String featureName = feature == null ? "main" : feature.name();
        message = config.getLogFormat().replaceAll("\\{message}", message).replaceAll("\\{feature}", featureName);
        HopperBot.logger.info(message);
        return message;
    }
    protected static void logToGuild(@NotNull String message, @Nullable HopperBotFeatures feature, @Nullable Guild guild) {
        message = log(message,feature);
        if (guild != null) {
            logToGuild(message, guild);
        }
    }
    protected static void logGlobally(@NotNull String message, @Nullable HopperBotFeatures feature) {
        if (jda != null) {
            message = log(message,feature);
            for (Guild guild : jda.getGuilds()) {
                if (feature == null || usesFeature(guild,feature)) {
                    logToGuild(message, guild);
                }
            }
        }
    }

    @Nullable
    @CheckReturnValue
    protected static HopperBotServerConfig getServerConfig(Long id) {
        return config.getServers().get(id);
    }

    @Nullable
    @CheckReturnValue
    protected static Map<String, JsonNode> getFeatureConfig(@NotNull Guild guild, @NotNull HopperBotFeatures feature) {
        final HopperBotServerConfig serverConfig = getServerConfig(guild.getIdLong());
        if (serverConfig == null) {
            return null;
        }
        return serverConfig.getFeatureConfig(feature);
    }

    @CheckReturnValue
    protected static boolean usesFeature(@NotNull Guild guild, @NotNull HopperBotFeatures feature) {
        final HopperBotServerConfig serverConfig = getServerConfig(guild.getIdLong());
        if (serverConfig == null) {
            return false;
        }
        return serverConfig.usesFeature(feature);
    }

    @NotNull
    @CheckReturnValue
    protected static EmbedBuilder getEmbedBase() {
        final User botOwnerUser = jda.getUserById(config.getBotOwnerId());
        final EmbedBuilder embedBuilder = new EmbedBuilder();
        if (botOwnerUser == null) {
            embedBuilder.setFooter("Made by "+config.getBotOwnerFallbackName(),config.getBotOwnerFallbackIcon());
        } else {
            embedBuilder.setFooter("Made by "+botOwnerUser.getName()+"#"+botOwnerUser.getDiscriminator(),botOwnerUser.getAvatarUrl());
        }
        return embedBuilder.setColor(0xe31313);
    }
    
    @Nullable
    @CheckReturnValue
    protected static <T> T getYAMLFile(@NotNull HopperBotFeatures feature, @NotNull String fileLocation, @NotNull  Class<T> serializedClass) {
        final File file = Paths.get(System.getProperty("user.dir"),fileLocation).toFile();
        try {
            if (file.createNewFile()) {
                HopperBot.logger.warn(fileLocation+" couldn't be found. An empty file has been created for you.");
                return null;
            } else {
                logGlobally("Found "+fileLocation,feature);
            }
        } catch (IOException e) {
            HopperBot.logger.error(fileLocation+" couldn't be found and an empty file could not be created");
            return null;
        }

        final T serializedResult;
        final ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        try {
            serializedResult = objectMapper.readValue(file, serializedClass);
        } catch (IOException e) {
            HopperBot.logger.error("Failed to serialize {} (maybe incorrectly formatted)",fileLocation,e);
            return null;
        }
        logGlobally("Loaded "+fileLocation,feature);

        return serializedResult;
    }
}
