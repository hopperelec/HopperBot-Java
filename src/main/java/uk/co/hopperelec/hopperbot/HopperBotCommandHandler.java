package uk.co.hopperelec.hopperbot;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.privileges.CommandPrivilege;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static java.util.regex.Pattern.quote;

public class HopperBotCommandHandler extends ListenerAdapter {
    private final HopperBotCommandFeature[] features;

    public HopperBotCommandHandler(Set<HopperBotCommandFeature> features) {
        this.features = features.toArray(new HopperBotCommandFeature[0]);
    }

    private HopperBotUtils getUtils() {
        return HopperBotUtils.getInstance();
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.getMessage().getAuthor().isBot() && event.getMessage().getMember() != null) {
            final Guild guild = event.getMessage().getGuild();
            final String originalContent = event.getMessage().getContentRaw();
            for (HopperBotCommandFeature feature : features) {
                if (feature.guilds.contains(guild)) {
                    if (originalContent.startsWith(feature.commandPrefix)) {
                        for (HopperBotCommand command : feature.commands) {
                            for (String name : command.aliases) {
                                final String content = originalContent.replaceFirst("^"+quote(feature.commandPrefix+name), "");
                                if (!content.equals(originalContent)) {
                                    for (CommandUsageFilter filter : command.filters) {
                                        if (filter.manualCheck(event.getMember(),content,feature)) {
                                            getUtils().log(event.getAuthor().getId()+" tried to use text command "+feature.commandPrefix+name+" at message "+event.getMessageId()+" but failed usage filter "+filter.name(),guild,feature.featureEnum);
                                            event.getMessage().reply("You cannot use this command here!").queue(message -> message.delete().queueAfter(5L, TimeUnit.SECONDS));
                                            return;
                                        }
                                    }
                                    getUtils().log(event.getAuthor().getId()+" successfully used text command "+feature.commandPrefix+name+" at message "+event.getMessageId(),guild,feature.featureEnum);
                                    command.textCommand(event,content,feature,getUtils());
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        for (HopperBotCommandFeature feature : features) {
            for (HopperBotCommand command : feature.commands) {
                if (command.name.equals(event.getName())) {
                    for (CommandUsageFilter filter : command.filters) {
                        if (filter.autoChecks.size() == 0 && filter.manualCheck(event.getMember(),event.getOptions().get(0).toString(),feature)) {
                            getUtils().log(event.getUser().getId()+" tried to use slash command "+command.name+" but failed usage filter "+filter.name(),event.getGuild(),feature.featureEnum);
                            event.reply("You cannot use this command here!").queue();
                            return;
                        }
                    }
                    getUtils().log(event.getUser().getId()+" successfully used slash command /"+command.name,event.getGuild(),feature.featureEnum);
                    command.slashCommand(event,feature,getUtils());
                    return;
                }
            }
        }
    }

    @Override
    public void onGuildReady(@NotNull GuildReadyEvent event) {
        final HopperBotServerConfig serverConfig = getUtils().config().getServerConfig(event.getGuild().getIdLong());
        if (serverConfig != null) {
            CommandListUpdateAction commandListUpdateAction = event.getGuild().updateCommands();
            final Map<String, Collection<CommandPrivilege>> commandPrivilegeNames = new HashMap<>();
            for (HopperBotCommandFeature feature : features) {
                if (serverConfig.usesFeature(feature.featureEnum)) {
                    feature.guilds.add(event.getGuild());
                    for (HopperBotCommand command : feature.commands) {
                        commandListUpdateAction = commandListUpdateAction.addCommands(command.slashCommand);
                        commandPrivilegeNames.put(command.name,command.privileges);
                    }
                    Set<HopperBotCommand> extraCommands = feature.getExtraCommands(event.getGuild(),serverConfig);
                    if (extraCommands != null) {
                        for (HopperBotCommand command : extraCommands) {
                            commandListUpdateAction = commandListUpdateAction.addCommands(command.slashCommand);
                            commandPrivilegeNames.put(command.name,command.privileges);
                        }
                    }
                }
            }
            commandListUpdateAction.queue(commands -> {
                Map<String, Collection<CommandPrivilege>> commandPrivilegeIDs = new HashMap<>();
                for (Command command : commands) {
                    commandPrivilegeIDs.put(command.getId(),commandPrivilegeNames.get(command.getName()));
                }
                event.getGuild().updateCommandPrivileges(commandPrivilegeIDs).queue();
            }, new ErrorHandler().handle(ErrorResponse.MISSING_ACCESS, error -> {
                getUtils().log("Missing Oauth2 scope 'applications.commands' which is needed to be able to add slash commands to the server. Re-invite the bot using this link: "+
                        "https://discord.com/api/oauth2/authorize?client_id=769709648092856331&scope=bot%20applications.commands&permissions=8", event.getGuild(),null);
            }));
        }
    }
}
