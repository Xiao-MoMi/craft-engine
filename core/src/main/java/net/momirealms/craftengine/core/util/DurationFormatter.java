package net.momirealms.craftengine.core.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DurationFormatter {
    private static final Pattern SEGMENT_PATTERN = Pattern.compile("([DdHhms]+)([^DdHhms]*)");

    private final List<Token> tokens = new ArrayList<>();

    private DurationFormatter(String pattern) {
        Matcher matcher = SEGMENT_PATTERN.matcher(pattern);
        while (matcher.find()) {
            tokens.add(new Token(matcher.group(1), matcher.group(2)));
        }
    }

    public static DurationFormatter of(String pattern) {
        return new DurationFormatter(pattern);
    }

    public String format(long millis) {
        if (tokens.isEmpty()) return "";

        long totalSeconds = millis / 1000;

        // 1. 预计算所有可能的单位值
        long d = totalSeconds / (24 * 3600);
        long h = (totalSeconds % (24 * 3600)) / 3600;
        long m = (totalSeconds % 3600) / 60;
        long s = totalSeconds % 60;

        // 2. 动态合并逻辑：
        // 如果模式中没有 'd'，那么小时数 'h' 应该包含天的部分 (h + d*24)
        // 我们需要根据 Pattern 中存在的最高单位来重新分配数值
        boolean hasD = hasUnit('d');
        boolean hasH = hasUnit('h');
        boolean hasM = hasUnit('m');

        long displayD = d;
        long displayH = hasD ? h : (h + d * 24);
        long displayM = hasH ? m : (m + (hasD ? h : (h + d * 24)) * 60);
        long displayS = hasM ? s : totalSeconds;

        StringBuilder sb = new StringBuilder();
        boolean foundFirstNonZero = false;

        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            long value = switch (Character.toLowerCase(token.unitChar)) {
                case 'd' -> displayD;
                case 'h' -> displayH;
                case 'm' -> displayM;
                case 's' -> displayS;
                default -> 0;
            };

            // 抹除最高位为 0 的逻辑：
            // 如果还没找到第一个非零值，且当前值是 0，且不是最后一个单位，则跳过
            if (!foundFirstNonZero && value == 0 && i < tokens.size() - 1) {
                continue;
            }

            foundFirstNonZero = true;

            // 格式化数值：补零
            String formattedValue = String.format("%0" + token.length + "d", value);
            sb.append(formattedValue).append(token.suffix);
        }

        return sb.toString().trim();
    }

    private boolean hasUnit(char u) {
        for (Token t : tokens) {
            if (Character.toLowerCase(t.unitChar) == u) return true;
        }
        return false;
    }

    private static class Token {
        final char unitChar;
        final int length;
        final String suffix;

        Token(String field, String suffix) {
            this.unitChar = field.charAt(0);
            this.length = field.length();
            this.suffix = suffix;
        }
    }
}