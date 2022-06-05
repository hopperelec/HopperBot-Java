package uk.co.hopperelec.hopperbot.commands;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import org.jetbrains.annotations.NotNull;
import uk.co.hopperelec.hopperbot.HopperBotListener;
import uk.co.hopperelec.hopperbot.HopperBotServerConfig;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static java.util.regex.Pattern.quote;

public class HopperBotCommandHandler extends HopperBotListener {
    @NotNull private final HopperBotCommandFeature[] features;

    public HopperBotCommandHandler(@NotNull Set<HopperBotCommandFeature> features) {
        this.features = features.toArray(new HopperBotCommandFeature[0]);
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (!event.getMessage().getAuthor().isBot() && event.getMessage().getMember() != null) {
            final Guild guild = event.getMessage().getGuild();
            final String originalContent = event.getMessage().getContentRaw();
            for (HopperBotCommandFeature feature : features) {
                if (getUtils().usesFeature(guild,feature.featureEnum) && originalContent.startsWith(feature.commandPrefix)) {
                    for (HopperBotCommand command : feature.commands) {
                        for (String name : command.aliases) {
                            final String content = originalContent.replaceFirst("^"+quote(feature.commandPrefix+name), "");
                            if (!content.equals(originalContent)) {
                                for (CommandUsageFilter filter : command.filters) {
                                    if (!filter.check(event.getMember(),content,feature)) {
                                        getUtils().logToGuild(event.getAuthor().getId()+" tried to use text command "+feature.commandPrefix+name+" at message "+event.getMessageId()+" but failed usage filter "+filter.name(),feature.featureEnum,guild);
                                        event.getMessage().reply("You cannot use this command here!").queue(message -> message.delete().queueAfter(5L, TimeUnit.SECONDS));
                                        return;
                                    }
                                }
                                getUtils().logToGuild(event.getAuthor().getId()+" successfully used text command "+feature.commandPrefix+name+" at message "+event.getMessageId(),feature.featureEnum,guild);
                                command.runTextCommand(event,content,feature,getUtils());
                                return;
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
                        if (!filter.check(event.getMember(),event.getOptions().get(0).toString(),feature)) {
                            getUtils().logToGuild(event.getUser().getId()+" tried to use slash command "+command.name+" but failed usage filter "+filter.name(),feature.featureEnum,event.getGuild());
                            event.reply("You cannot use this command here!").queue();
                            return;
                        }
                    }
                    getUtils().logToGuild(event.getUser().getId()+" successfully used slash command /"+command.name,feature.featureEnum,event.getGuild());
                    command.runSlashCommand(event,feature,getUtils());
                    return;
                }
            }
        }
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        final String[] parts = event.getComponentId().split("-");
        Arrays.stream(features)
                .filter(feature -> feature instanceof HopperBotButtonFeature)
                .filter(feature -> parts[0].equals(((HopperBotButtonFeature) feature).featureButtonPrefix))
                .findFirst().ifPresent(feature -> {
                    getUtils().logToGuild("Button pressed: "+event.getComponentId(),feature.featureEnum,event.getGuild());
                    ((HopperBotButtonFeature) feature).runButtonCommand(event,parts);
                });
    }

    @Override
    public void onGuildReady(@NotNull GuildReadyEvent event) {
        final HopperBotServerConfig serverConfig = getUtils().config().getServerConfig(event.getGuild().getIdLong());
        if (serverConfig != null) {
            CommandListUpdateAction commandListUpdateAction = event.getGuild().updateCommands();
            for (HopperBotCommandFeature feature : features) {
                if (serverConfig.usesFeature(feature.featureEnum)) {
                    feature.guilds.add(event.getGuild());
                    for (HopperBotCommand command : feature.commands) {
                        commandListUpdateAction = commandListUpdateAction.addCommands(command.slashCommand);
                    }
                    Set<HopperBotCommand> extraCommands = feature.getExtraCommands(event.getGuild(),serverConfig);
                    if (extraCommands != null) {
                        for (HopperBotCommand command : extraCommands) {
                            commandListUpdateAction = commandListUpdateAction.addCommands(command.slashCommand);
                        }
                    }
                }
            }
            commandListUpdateAction.queue(null,new ErrorHandler().handle(ErrorResponse.MISSING_ACCESS, error -> {
                getUtils().logToGuild("Missing Oauth2 scope 'applications.commands' which is needed to be able to add slash commands to the server. Re-invite the bot using this link: "+
                        "https://discord.com/api/oauth2/authorize?client_id=769709648092856331&scope=bot%20applications.commands&permissions=8",null,event.getGuild());
            }));
        } else {
            getUtils().logToGuild("Bot is in guild "+event.getGuild().getId()+" which has not been configured",null,event.getGuild());
        }
    }
}
