package net.momirealms.craftengine.bukkit.util;

import net.momirealms.craftengine.core.util.Mirror;

public final class MirrorUtils {
    private MirrorUtils() {}

    public static Mirror fromNMSMirror(Object mirror) {
        int index = ((Enum<?>) mirror).ordinal();
        return Mirror.values()[index];
    }
}
