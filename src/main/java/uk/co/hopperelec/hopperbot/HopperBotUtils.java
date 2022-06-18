package uk.co.hopperelec.hopperbot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckReturnValue;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Map;

public record HopperBotUtils(JDA jda, HopperBotConfig config) {
    private static final Logger logger = LoggerFactory.getLogger(HopperBotUtils.class);
    private static HopperBotUtils instance;
    public static final long BOT_OWNER_ID = 348083986989449216L;
    @NotNull public static final String BOT_OWNER_DEFAULT_NAME = "hopperelec#3060";
    @NotNull public static final String BOT_OWNER_DEFAULT_ICON = "https://www.hopperelec.co.uk/resources/hopper.png?scale=0.2&padding=30&padding-side=-8&type3D=2&shadow=32&fill=100&outline-width=6&bg=1";

    public void logToGuild(@NotNull String message, @NotNull Guild guild) {
        final HopperBotServerConfig guildConfig = config.getServerConfig(guild.getIdLong());
        if (guildConfig != null) {
            final long channelId = guildConfig.getLogChannel();
            final TextChannel logChannel = guild.getTextChannelById(channelId);
            if (logChannel == null) {
                logger.error("The log channel for {} ({}) could not be found!", guildConfig.getName(), channelId);
            } else {
                logChannel.sendMessage(message).queue();
            }
        }
    }

    @NotNull
    private String log(@NotNull String message, @Nullable HopperBotFeatures feature) {
        final String featureName = feature == null ? "main" : feature.name();
        message = config.getLogFormat().replaceAll("\\{message}", message).replaceAll("\\{feature}", featureName);
        logger.info(message);
        return message;
    }
    public void logToGuild(@NotNull String message, @Nullable HopperBotFeatures feature, @Nullable Guild guild) {
        message = log(message,feature);
        if (guild != null) {
            logToGuild(message, guild);
        }
    }
    public void logGlobally(@NotNull String message, @Nullable HopperBotFeatures feature) {
        message = log(message,feature);
        for (Guild guild : jda.getGuilds()) {
            if (feature == null || usesFeature(guild,feature)) {
                logToGuild(message, guild);
            }
        }
    }

    @Nullable
    @CheckReturnValue
    public Map<String, JsonNode> getFeatureConfig(@NotNull Guild guild, @NotNull HopperBotFeatures feature) {
        final HopperBotServerConfig serverConfig = config().getServerConfig(guild.getIdLong());
        if (serverConfig == null) {
            return null;
        }
        return serverConfig.getFeatureConfig(feature);
    }

    @CheckReturnValue
    public boolean usesFeature(@NotNull Guild guild, @NotNull HopperBotFeatures feature) {
        final HopperBotServerConfig serverConfig = config().getServerConfig(guild.getIdLong());
        if (serverConfig == null) {
            return false;
        }
        return config.getServerConfig(guild.getIdLong()).usesFeature(feature);
    }

    @NotNull
    @CheckReturnValue
    public EmbedBuilder getEmbedBase() {
        final User botOwnerUser = jda.getUserById(BOT_OWNER_ID);
        final EmbedBuilder embedBuilder = new EmbedBuilder();
        if (botOwnerUser == null) {
            embedBuilder.setFooter("Made by "+BOT_OWNER_DEFAULT_NAME,BOT_OWNER_DEFAULT_ICON);
        } else {
            embedBuilder.setFooter("Made by "+botOwnerUser.getName()+"#"+botOwnerUser.getDiscriminator(),botOwnerUser.getAvatarUrl());
        }
        return embedBuilder.setColor(0xe31313);
    }

    static synchronized void createInstance(@NotNull JDA jda, @NotNull HopperBotConfig config) {
        if (instance == null) {
            instance = new HopperBotUtils(jda, config);
        } else {
            logger.error("Attempt made to make an instance of HopperBotUtils but one already exists");
        }
    }

    @Nullable
    @CheckReturnValue
    public <T> T getYAMLFile(@NotNull HopperBotFeatures feature, @NotNull String fileLocation, @NotNull  Class<T> serializedClass) {
        final File file = Paths.get(System.getProperty("user.dir"),fileLocation).toFile();
        try {
            if (file.createNewFile()) {
                logger.warn(fileLocation+" couldn't be found. An empty file has been created for you. Please enter the host, name, user and password into it");
                return null;
            } else {
                logGlobally("Found "+fileLocation,feature);
            }
        } catch (IOException e) {
            logger.error(fileLocation+" couldn't be found and an empty file could not be created");
            return null;
        }

        final T serializedResult;
        final ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        try {
            serializedResult = objectMapper.readValue(file, serializedClass);
        } catch (IOException e) {
            logger.error("Failed to serialize {} (maybe incorrectly formatted)",fileLocation,e);
            return null;
        }
        logGlobally("Loaded "+fileLocation,feature);

        return serializedResult;
    }

    public static HopperBotUtils getInstance() {
        return instance;
    }
}
