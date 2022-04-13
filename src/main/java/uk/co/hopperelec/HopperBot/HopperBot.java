package uk.co.hopperelec.HopperBot;

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
import java.io.FileOutputStream;
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
    }

    public static void main(String[] args) {
        try {
            final JDABuilder builder = JDABuilder.create(GatewayIntent.GUILD_MEMBERS,GatewayIntent.GUILD_MESSAGES,GatewayIntent.GUILD_VOICE_STATES);
            try {
                builder.setToken(Files.readString(Paths.get("token")));
            } catch (IOException e) {
                throw new HopperBotLoadingException("'token' file must be created in the working directory");
            }
            logger.info("Retrieved token");
            builder.disableCache(CacheFlag.ACTIVITY,CacheFlag.EMOTE,CacheFlag.CLIENT_STATUS,CacheFlag.ONLINE_STATUS,CacheFlag.ROLE_TAGS,CacheFlag.MEMBER_OVERRIDES);
            logger.info("Configured JDA builder");

            final Path configPath = Paths.get(System.getProperty("user.dir"),"config.yml");
            final File configFile = configPath.toFile();
            if (!configFile.exists() || configFile.isDirectory()) {
                final InputStream configStream = HopperBot.class.getResourceAsStream("/config.yml");
                if (configStream == null) {
                    throw new HopperBotLoadingException("Failed to read default config from JAR!");
                }
                try {
                    FileOutputStream outputStream = new FileOutputStream(configPath.toString());
                    outputStream.write(configStream.readAllBytes());
                } catch (IOException e) {
                    throw new HopperBotLoadingException("Failed to save default config!");
                }
                logger.info("Created new config.yml");
            } else {
                logger.info("Found config.yml");
            }

            final HopperBotConfig config;
            try {
                ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
                objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
                config = objectMapper.readValue(configFile, HopperBotConfig.class);
            } catch (IOException e) {
                e.printStackTrace();
                throw new HopperBotLoadingException("Failed to read config to uk.co.hopperelec.HopperBot.HopperBotConfig object!");
            }
            logger.info("Loaded config");

            final Set<HopperBotCommandFeature> commandFeatures = new HashSet<>();
            for (HopperBotFeatures feature : config.getEnabledFeatures()) {
                if (feature.handler != null) {
                    try {
                        HopperBotFeature featureInstance = feature.handler.newInstance();
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
            logger.info("Done initializing features");

            final JDA jda;
            try {
                jda = builder.build();
            } catch (LoginException e) {
                throw new HopperBotLoadingException("Failed to login to the bot!");
            }
            HopperBotUtils.createInstance(jda,config);
            logger.info("Logged into bot");

        } catch (HopperBotLoadingException e) {
            logger.error(e.getMessage());
            System.exit(0);
        }
    }
}
