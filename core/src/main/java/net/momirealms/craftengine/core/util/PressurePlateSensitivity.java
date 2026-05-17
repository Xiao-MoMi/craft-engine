package net.momirealms.craftengine.core.util;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public enum PressurePlateSensitivity {
    EVERYTHING("everything", "all"),
    MOBS("mobs", "mob");

    private static final Map<String, PressurePlateSensitivity> BY_ID = new HashMap<>();
    private final String[] ids;

    PressurePlateSensitivity(String... ids) {
        this.ids = ids;
    }

    public String[] ids() {
        return ids;
    }

    static {
        for (PressurePlateSensitivity trigger : PressurePlateSensitivity.values()) {
            for (String name : trigger.ids()) {
                BY_ID.put(name, trigger);
            }
        }
    }

    @Nullable
    public static PressurePlateSensitivity byId(String id) {
        return BY_ID.get(id.toLowerCase(Locale.ROOT));
    }
}
