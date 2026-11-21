package net.momirealms.craftengine.bedrock.geyser.scoreboard;

import net.momirealms.craftengine.bedrock.text.SidebarCodec;

import java.util.UUID;

final class SidebarRowSnapshot {
    private static final String UNSORTED = SidebarCodec.MARKER + "§r§r";
    private final String marker = "ce/" + UUID.randomUUID() + "/";

    boolean isWrapped(String text) {
        return text != null && text.startsWith(this.marker);
    }

    String wrap(String encoded, int teamPrefixLength) {
        // 快照直接随行入队。转义 § 防止 Geyser 合并格式码，转义 | 以区分正文和队伍后缀。
        StringBuilder result = new StringBuilder(this.marker.length() + encoded.length());
        result.append(this.marker).append(teamPrefixLength).append('/');
        for (int i = UNSORTED.length(); i < encoded.length(); i++) {
            char character = encoded.charAt(i);
            switch (character) {
                case '§' -> result.append("~s");
                case '|' -> result.append("~p");
                case '~' -> result.append("~~");
                default -> result.append(character);
            }
        }
        return result.append('|').toString();
    }

    String wireName(String raw, boolean visible) {
        int start = raw.indexOf(this.marker);
        if (start < 0) {
            return visible ? raw.replace("%", "%%") : raw;
        }
        int lengthStart = start + this.marker.length();
        int separator = raw.indexOf('/', lengthStart);
        int teamPrefixLength = Integer.parseInt(raw, lengthStart, separator, 10);
        int end = raw.indexOf('|', separator + 1);
        StringBuilder result = new StringBuilder(end - separator + UNSORTED.length());
        // 同分时 Geyser 会在整个队伍前缀前再加两个格式码；普通队伍前缀本身也可能以颜色码开头。
        if (start == teamPrefixLength + 4 && raw.startsWith("§r", 2)) {
            result.append(SidebarCodec.MARKER).append(raw, 0, 4);
        } else {
            result.append(UNSORTED);
        }
        for (int i = separator + 1; i < end; i++) {
            char character = raw.charAt(i);
            if (character == '~') {
                character = switch (raw.charAt(++i)) {
                    case 's' -> '§';
                    case 'p' -> '|';
                    default -> '~';
                };
            }
            if (character == '%') {
                result.append('%');
            }
            result.append(character);
        }
        return result.toString();
    }
}
