package uk.co.hopperelec.hopperbot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

public final class HopperBot {
    public final static Logger logger = LoggerFactory.getLogger(HopperBot.class);

    private static class HopperBotLoadingException extends Exception {
        public HopperBotLoadingException(String message) {
            super(message);
        }
        public HopperBotLoadingException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private static String getToken() throws HopperBotLoadingException {
        try {
            return Files.readString(Paths.get("token"));
        } catch (IOException e) {
            throw new HopperBotLoadingException("'token' file must be created in the working directory",e);
        }
    }

    private static void createConfig(Path configPath) throws HopperBotLoadingException {
        final InputStream configStream = HopperBot.class.getResourceAsStream("/config.yml");
        if (configStream == null) {
            throw new HopperBotLoadingException("Failed to read default config from JAR!");
        }
        try {
            Files.copy(configStream,configPath);
        } catch (IOException e) {
            throw new HopperBotLoadingException("Failed to save default config!",e);
        }
    }

    private static HopperBotConfig readConfig(File configFile) throws HopperBotLoadingException {
        try {
            ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
            objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
            return objectMapper.readValue(configFile, HopperBotConfig.class);
        } catch (IOException e) {
            throw new HopperBotLoadingException("Failed to read config to HopperBotConfig object!",e);
        }
    }

    private static void initializeFeatures(HopperBotConfig config, JDABuilder builder) {
        final Set<HopperBotCommandFeature> commandFeatures = new HashSet<>();
        for (HopperBotFeatures feature : config.getEnabledFeatures()) {
            if (feature.handler != null) {
                try {
                    final HopperBotFeature featureInstance = feature.handler.newInstance();
                    builder.addEventListeners(featureInstance);
                    if (featureInstance instanceof HopperBotCommandFeature) {
                        commandFeatures.add((HopperBotCommandFeature) featureInstance);
                    }
                } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
                    logger.error("Cannot create instance of HopperBotFeature {}!",feature,e);
                }
            }
        }
        builder.addEventListeners(new HopperBotCommandHandler(commandFeatures));
    }

    public static void main(String[] args) {
        try {
            final JDABuilder builder = JDABuilder.create(GatewayIntent.GUILD_MEMBERS,GatewayIntent.GUILD_MESSAGES,GatewayIntent.GUILD_VOICE_STATES);
            builder.setToken(getToken());
            logger.info("Retrieved token");
            builder.disableCache(CacheFlag.ACTIVITY,CacheFlag.EMOTE,CacheFlag.CLIENT_STATUS,CacheFlag.ONLINE_STATUS,CacheFlag.ROLE_TAGS,CacheFlag.MEMBER_OVERRIDES);
            logger.info("Configured JDA builder");

            final Path configPath = Paths.get(System.getProperty("user.dir"),"config.yml");
            final File configFile = configPath.toFile();
            if (!configFile.exists() || configFile.isDirectory()) {
                createConfig(configPath);
                logger.info("Created new config.yml");
            } else {
                logger.info("Found config.yml");
            }
            final HopperBotConfig config = readConfig(configFile);
            logger.info("Loaded config");

            initializeFeatures(config,builder);
            logger.info("Done initializing features");

            final JDA jda;
            try {
                jda = builder.build();
            } catch (LoginException e) {
                throw new HopperBotLoadingException("Failed to login to the bot!",e);
            }
            HopperBotUtils.createInstance(jda,config);
            logger.info("Logged into bot");

        } catch (HopperBotLoadingException e) {
            logger.error("Error trying to load HopperBot",e);
            System.exit(1);
        }
    }
}
