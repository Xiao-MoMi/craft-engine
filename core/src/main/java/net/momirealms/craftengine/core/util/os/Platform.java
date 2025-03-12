/*
 * Copyright (C) <2025> <XiaoMoMi>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.momirealms.craftengine.core.util.os;

public enum Platform {
    FREEBSD("FreeBSD", "freebsd"),
    LINUX("Linux", "linux"),
    MACOS("macOS", "macos"),
    WINDOWS("Windows", "windows");

    private static final Platform current;
    private final String name;
    private final String nativePath;

    Platform(String name, String nativePath) {
        this.name = name;
        this.nativePath = nativePath;
    }

    public String getName() {
        return this.name;
    }

    public String getNativePath() {
        return nativePath;
    }

    public static Platform get() {
        return current;
    }

    static {
        String osName = System.getProperty("os.name");
        if (osName.startsWith("Windows")) {
            current = WINDOWS;
        } else if (osName.startsWith("FreeBSD")) {
            current = FREEBSD;
        } else if (!osName.startsWith("Linux") && !osName.startsWith("SunOS") && !osName.startsWith("Unix")) {
            if (!osName.startsWith("Mac OS X") && !osName.startsWith("Darwin")) {
                throw new LinkageError("Unknown platform: " + osName);
            }
            current = MACOS;
        } else {
            current = LINUX;
        }
    }
}
