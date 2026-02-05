package net.momirealms.craftengine.proxy;

import java.util.function.Predicate;

public final class MinecraftVersionPredicate implements Predicate<String> {
    private final int currentVersion;

    public MinecraftVersionPredicate(String version) {
        this.currentVersion = parseVersionToInteger(version);
    }

    @Override
    public boolean test(String version) {
        if (version.isEmpty()) return true;
        return isVersionMatch(version);
    }

    private boolean isVersionMatch(String versionSpec) {
        int index = versionSpec.indexOf('~');
        // 没有范围值
        if (index == -1) {
            char firstChar = versionSpec.charAt(0);
            if (firstChar == '>') {
                int version = parseVersionToInteger(versionSpec);
                return versionSpec.charAt(1) == '=' ? this.currentVersion >= version : this.currentVersion > version;
            } else if (firstChar == '<') {
                int version = parseVersionToInteger(versionSpec);
                return versionSpec.charAt(1) == '=' ? this.currentVersion <= version : this.currentVersion < version;
            } else {
                return parseVersionToInteger(versionSpec) == this.currentVersion;
            }
        } else {
            int min = parseVersionToInteger(versionSpec.substring(0, index));
            int max = parseVersionToInteger(versionSpec.substring(index + 1));
            return this.currentVersion >= min && this.currentVersion <= max;
        }
    }

    public static int parseVersionToInteger(String versionString) {
        int v1 = 0;
        int v2 = 0;
        int v3 = 0;
        int currentNumber = 0;
        int part = 0;
        for (int i = 0; i < versionString.length(); i++) {
            char c = versionString.charAt(i);
            if (c >= '0' && c <= '9') {
                currentNumber = currentNumber * 10 + (c - '0');
            } else if (c == '.') {
                if (part == 0) {
                    v1 = currentNumber;
                }
                if (part == 1) {
                    v2 = currentNumber;
                }
                part++;
                currentNumber = 0;
                if (part > 2) {
                    break;
                }
            }
        }
        // 处理最后一个数字部分
        if (part == 0) {  // 没有点号：如 "26"
            v1 = currentNumber;
        } else if (part == 1) {  // 一个点号：如 "26.1"
            v2 = currentNumber;
        } else if (part == 2) {  // 两个点号：如 "1.2.3"
            v3 = currentNumber;
        }
        return v1 * 10000 + v2 * 100 + v3;
    }
}