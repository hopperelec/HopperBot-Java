package uk.co.hopperelec.hopperbot.commands;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;

import javax.annotation.CheckReturnValue;

import static uk.co.hopperelec.hopperbot.HopperBotListener.getConfig;

public enum CommandUsageFilter {
    IS_BOT_OWNER {
        @Override
        public boolean check(Member author, String content, HopperBotCommandFeature feature) {
            return author.getIdLong() == getConfig().getBotOwnerId();
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

    @CheckReturnValue
    public abstract boolean check(Member author, String content, HopperBotCommandFeature feature);
}
