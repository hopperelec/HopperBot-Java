package uk.co.hopperelec.hopperbot.commands;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import uk.co.hopperelec.hopperbot.HopperBotFeature;
import uk.co.hopperelec.hopperbot.HopperBotFeatures;
import uk.co.hopperelec.hopperbot.HopperBotServerConfig;

import javax.annotation.CheckForNull;
import javax.annotation.CheckReturnValue;
import java.util.HashSet;
import java.util.Set;

public class HopperBotCommandFeature extends HopperBotFeature {
    @NotNull public final String commandPrefix;
    @NotNull public final Set<HopperBotCommand<?>> commands = new HashSet<>();
    @NotNull public final Set<Guild> guilds = new HashSet<>();

    public HopperBotCommandFeature(@NotNull JDABuilder builder, @NotNull HopperBotFeatures featureEnum, @NotNull String commandPrefix) {
        super(builder,featureEnum);
        this.commandPrefix = commandPrefix;
    }

    protected void addCommands(@NotNull HopperBotCommand<?> @NotNull ... command) {
        commands.addAll(Set.of(command));
    }

    @Nullable
    @CheckForNull
    @CheckReturnValue
    public Set<HopperBotCommand<?>> getExtraCommands(@NotNull Guild guild, @NotNull HopperBotServerConfig serverConfig) {
        return null;
    }
}
