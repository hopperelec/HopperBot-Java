package uk.co.hopperelec.hopperbot;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.CheckForNull;
import javax.annotation.CheckReturnValue;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;

public class HopperBotCommandFeature extends HopperBotFeature {
    @NotNull public final String commandPrefix;
    @NotNull public final Set<HopperBotCommand> commands;
    @NotNull public final Set<Guild> guilds = new HashSet<>();

    public HopperBotCommandFeature(@NotNull JDABuilder builder, @NotNull HopperBotFeatures featureEnum, @NotNull String commandPrefix, @NotNull HopperBotCommand @NotNull ... commands) {
        super(builder,featureEnum);
        this.commandPrefix = commandPrefix;
        if (commands.length == 0) {
            this.commands = new HashSet<>();
        } else {
            this.commands = stream(commands).collect(Collectors.toSet());
        }
    }

    @Nullable
    @CheckForNull
    @CheckReturnValue
    public Set<HopperBotCommand> getExtraCommands(@NotNull Guild guild, @NotNull HopperBotServerConfig serverConfig) {
        return null;
    }
}
