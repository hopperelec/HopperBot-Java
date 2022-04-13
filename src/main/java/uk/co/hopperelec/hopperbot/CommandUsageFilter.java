package uk.co.hopperelec.hopperbot;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.interactions.commands.privileges.CommandPrivilege;

import java.util.List;

public enum CommandUsageFilter {
    is_bot_owner(
        (author,content,feature) -> author.getIdLong() != 348083986989449216L,
        new CommandPrivilege(CommandPrivilege.Type.USER,true,348083986989449216L)
    ),

    non_empty_content(
        (author,content,feature) -> content.equals("")
    ),

    has_manage_messages(
        (author,content,feature) -> !author.hasPermission(Permission.MANAGE_PERMISSIONS)
    );

    public final List<CommandPrivilege> autoChecks;
    public final ManualCheck manualCheck;

    public interface ManualCheck {
        boolean op(Member author, String content, HopperBotCommandFeature feature);
    }

    CommandUsageFilter(ManualCheck manualCheck, CommandPrivilege... autoChecks) {
        this.manualCheck = manualCheck;
        this.autoChecks = List.of(autoChecks);
    }
}
