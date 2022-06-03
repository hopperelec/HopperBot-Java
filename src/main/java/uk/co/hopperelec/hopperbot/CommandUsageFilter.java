package uk.co.hopperelec.hopperbot;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;

import static uk.co.hopperelec.hopperbot.HopperBotUtils.BOT_OWNER_ID;

public enum CommandUsageFilter {
    IS_BOT_OWNER {
        @Override
        public boolean check(Member author, String content, HopperBotCommandFeature feature) {
            return author.getIdLong() == BOT_OWNER_ID;
        }
    },

    NON_EMPTY_CONTENT {
        @Override
        public boolean check(Member author, String content, HopperBotCommandFeature feature) {
            return !content.equals("");
        }
    },

    HAS_MANAGE_MESSAGES {
        @Override
        public boolean check(Member author, String content, HopperBotCommandFeature feature) {
            return author.hasPermission(Permission.MANAGE_PERMISSIONS);
        }
    };

    public abstract boolean check(Member author, String content, HopperBotCommandFeature feature);
}
