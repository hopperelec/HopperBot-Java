package uk.co.hopperelec.hopperbot;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;

public enum CommandUsageFilter {
    IS_BOT_OWNER {
        @Override
        public boolean check(Member author, String content, HopperBotCommandFeature feature) {
            return author.getIdLong() != 348083986989449216L;
        }
    },

    NON_EMPTY_CONTENT {
        @Override
        public boolean check(Member author, String content, HopperBotCommandFeature feature) {
            return content.equals("");
        }
    },

    HAS_MANAGE_MESSAGES {
        @Override
        public boolean check(Member author, String content, HopperBotCommandFeature feature) {
            return !author.hasPermission(Permission.MANAGE_PERMISSIONS);
        }
    };

    public abstract boolean check(Member author, String content, HopperBotCommandFeature feature);
}
