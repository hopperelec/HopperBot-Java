package uk.co.hopperelec.hopperbot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public record HopperBotUtils(JDA jda, HopperBotConfig config) {
    private static final Logger logger = LoggerFactory.getLogger(HopperBotUtils.class);
    private static HopperBotUtils instance;

    public void logToGuild(String message, Guild guild) {
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

    private String log(String message, HopperBotFeatures feature) {
        final String featureName = feature == null ? "main" : feature.name();
        message = config.getLogFormat().replaceAll("\\{message}", message).replaceAll("\\{feature}", featureName);
        logger.info(message);
        return message;
    }
    public void logToGuild(String message, HopperBotFeatures feature, Guild guild) {
        logToGuild(log(message,feature), guild);
    }
    public void logGlobally(String message, HopperBotFeatures feature) {
        message = log(message,feature);
        for (Guild guild : jda.getGuilds()) {
            if (feature == null || usesFeature(guild,feature)) {
                logToGuild(message, guild);
            }
        }
    }

    public Map<String, JsonNode> getFeatureConfig(Guild guild, HopperBotFeatures feature) {
        final HopperBotServerConfig serverConfig = config().getServerConfig(guild.getIdLong());
        if (serverConfig == null) {
            return null;
        }
        return serverConfig.getFeatureConfig(feature);
    }

    public boolean usesFeature(Guild guild, HopperBotFeatures feature) {
        final HopperBotServerConfig serverConfig = config().getServerConfig(guild.getIdLong());
        if (serverConfig == null) {
            return false;
        }
        return config.getServerConfig(guild.getIdLong()).usesFeature(feature);
    }

    public EmbedBuilder getEmbedBase() {
        return new EmbedBuilder().setFooter("Made by hopperelec#3060").setColor(0xe31313);
    }

    public void tempReply(Message message, String reply) {
        message.reply(reply).queue(replyMsg -> {
            replyMsg.delete().queueAfter(10, TimeUnit.SECONDS);
            message.delete().queueAfter(10, TimeUnit.SECONDS);
        });
    }

    static synchronized void createInstance(JDA jda, HopperBotConfig config) {
        if (instance == null) {
            instance = new HopperBotUtils(jda, config);
        } else {
            logger.error("Attempt made to make an instance of HopperBotUtils but one already exists");
        }
    }

    public <T> T getYAMLFile(HopperBotFeatures feature, String fileLocation, Class<T> serializedClass) {
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
