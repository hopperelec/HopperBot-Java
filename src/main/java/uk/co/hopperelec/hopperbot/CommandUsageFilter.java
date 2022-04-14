package uk.co.hopperelec.hopperbot;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.interactions.commands.privileges.CommandPrivilege;

import java.util.List;

public enum CommandUsageFilter {
    IS_BOT_OWNER(new CommandPrivilege(CommandPrivilege.Type.USER,true,348083986989449216L)) {
        @Override
        public boolean manualCheck(Member author, String content, HopperBotCommandFeature feature) {
            return author.getIdLong() != 348083986989449216L;
        }
    },

    NON_EMPTY_CONTENT {
        @Override
        public boolean manualCheck(Member author, String content, HopperBotCommandFeature feature) {
            return content.equals("");
        }
    },

    HAS_MANAGE_MESSAGES {
        @Override
        public boolean manualCheck(Member author, String content, HopperBotCommandFeature feature) {
            return !author.hasPermission(Permission.MANAGE_PERMISSIONS);
        }
    };

    public final List<CommandPrivilege> autoChecks;

    CommandUsageFilter(CommandPrivilege... autoChecks) {
        this.autoChecks = List.of(autoChecks);
    }

    public abstract boolean manualCheck(Member author, String content, HopperBotCommandFeature feature);
}
