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
