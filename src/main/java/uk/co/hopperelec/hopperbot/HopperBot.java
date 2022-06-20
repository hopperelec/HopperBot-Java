package uk.co.hopperelec.hopperbot;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.AllowedMentions;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.hopperelec.hopperbot.commands.HopperBotCommandFeature;
import uk.co.hopperelec.hopperbot.commands.HopperBotCommandHandler;

import javax.annotation.CheckReturnValue;
import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static com.fasterxml.jackson.databind.PropertyNamingStrategies.SNAKE_CASE;

public final class HopperBot {
    public final static Logger logger = LoggerFactory.getLogger(HopperBot.class);
    private final static String TOKEN_FILE_NAME = "token";
    private final static String CONFIG_RESOURCE_PATH = "/config.yml";
    private final static String CONFIG_FILE_NAME = "config.yml";

    private static class HopperBotLoadingException extends Exception {
        public HopperBotLoadingException(String message) {
            super(message);
        }
        public HopperBotLoadingException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    @CheckReturnValue
    private static String getToken() throws HopperBotLoadingException {
        try {
            return Files.readString(Paths.get(TOKEN_FILE_NAME));
        } catch (IOException e) {
            throw new HopperBotLoadingException("'token' file must be created in the working directory",e);
        }
    }

    private static void createConfig(@NotNull Path configPath) throws HopperBotLoadingException {
        final InputStream configStream = getResourceStream(CONFIG_RESOURCE_PATH);
        try {
            Files.copy(configStream,configPath);
        } catch (IOException e) {
            throw new HopperBotLoadingException("Failed to save default config!",e);
        }
    }

    @CheckReturnValue
    private static HopperBotConfig readConfig(@NotNull File configFile) throws HopperBotLoadingException {
        try {
            final YAMLMapper objectMapper = YAMLMapper.builder().enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS).build();
            objectMapper.setPropertyNamingStrategy(SNAKE_CASE);
            return objectMapper.readValue(configFile, HopperBotConfig.class);
        } catch (IOException e) {
            throw new HopperBotLoadingException("Failed to read config to HopperBotConfig object (maybe incorrectly formatted)",e);
        }
    }

    private static void initializeFeatures(@NotNull HopperBotConfig config, @NotNull JDABuilder builder) {
        final Set<HopperBotCommandFeature> commandFeatures = new HashSet<>();
        for (HopperBotFeatures feature : config.getEnabledFeatures()) {
            if (feature.handler != null) {
                try {
                    final HopperBotFeature featureInstance = feature.handler.newInstance(builder);
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

    @NotNull
    @CheckReturnValue
    public static InputStream getResourceStream(@NotNull String path) throws HopperBotLoadingException {
        final InputStream stream = HopperBot.class.getResourceAsStream(path);
        if (stream == null) {
            throw new HopperBotLoadingException("Failed to read resource "+path);
        }
        return stream;
    }

    public static void main(String[] args) throws HopperBotLoadingException {
        final Path configPath = Paths.get(System.getProperty("user.dir"),CONFIG_FILE_NAME);
        final File configFile = configPath.toFile();
        if (!configFile.exists() || configFile.isDirectory()) {
            createConfig(configPath);
            logger.info("Created new config.yml");
        } else {
            logger.info("Found config.yml");
        }
        final HopperBotConfig config = readConfig(configFile);
        HopperBotListener.setConfig(config);
        logger.info("Loaded config");

        AllowedMentions.setDefaultMentions(Collections.emptySet());
        AllowedMentions.setDefaultMentionRepliedUser(true);

        final JDABuilder builder = JDABuilder.create(GatewayIntent.GUILD_MEMBERS,GatewayIntent.GUILD_MESSAGES,GatewayIntent.GUILD_VOICE_STATES);
        builder.setToken(getToken());
        logger.info("Retrieved token");
        builder.disableCache(CacheFlag.ACTIVITY,CacheFlag.EMOTE,CacheFlag.CLIENT_STATUS,CacheFlag.ONLINE_STATUS,CacheFlag.ROLE_TAGS,CacheFlag.MEMBER_OVERRIDES);
        logger.info("Configured JDA builder");

        initializeFeatures(config,builder);
        logger.info("Done initializing features");

        final JDA jda;
        try {
            jda = builder.build();
        } catch (LoginException e) {
            throw new HopperBotLoadingException("Failed to login to the bot!",e);
        }
        HopperBotListener.setJDA(jda);
        logger.info("Logged into bot");
    }
}
