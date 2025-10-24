package net.momirealms.craftengine.bukkit.util;

import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.core.util.Mirror;

public final class MirrorUtils {

    private MirrorUtils() {}

    public static Mirror fromNMSMirror(Object mirror) {
        try {
            int index = (int) CoreReflections.method$Mirror$ordinal.invoke(mirror);
            return Mirror.values()[index];
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static Object toNMSMirror(Mirror mirror) {
        switch (mirror) {
            case FRONT_BACK -> {
                return CoreReflections.Instance.mirror$FRONT_BACK;
            }
            case LEFT_RIGHT -> {
                return CoreReflections.Instance.mirror$LEFT_RIGHT;
            }
            default -> {
                return CoreReflections.Instance.mirror$NONE;
            }
        }
    }
}
