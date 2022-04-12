package uk.co.hopperelec.HopperBot;

import net.dv8tion.jda.api.entities.Guild;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;

public class HopperBotCommandFeature extends HopperBotFeature {
    public final String commandPrefix;
    public final Set<HopperBotCommand> commands;
    public final Set<Guild> guilds = new HashSet<>();

    public HopperBotCommandFeature(HopperBotFeatures featureEnum, String commandPrefix, HopperBotCommand... commands) {
        super(featureEnum);
        this.commandPrefix = commandPrefix;
        if (commands.length == 0) {
            this.commands = new HashSet<>();
        } else {
            this.commands = stream(commands).collect(Collectors.toSet());
        }
    }

    public Set<HopperBotCommand> getExtraCommands(Guild guild, HopperBotServerConfig serverConfig) {
        return null;
    }
}
