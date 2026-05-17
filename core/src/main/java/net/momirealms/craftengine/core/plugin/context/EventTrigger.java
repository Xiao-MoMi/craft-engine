package net.momirealms.craftengine.core.plugin.context;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public enum EventTrigger {
    LEFT_CLICK("left_click"),
    RIGHT_CLICK("right_click", "use_on", "use", "use_item_on"),
    ATTACK("attack", "hit"),
    CONSUME("eat", "consume", "drink"),
    BREAK("break", "dig"),
    PLACE("place", "build"),
    PICK_UP("pick_up", "pick"),
    STEP("step"),
    FALL("fall"),
    SHOOT("shoot");

    private static final Map<String, EventTrigger> BY_ID = new HashMap<>();
    private final String[] ids;

    EventTrigger(String... ids) {
        this.ids = ids;
    }

    public String[] ids() {
        return this.ids;
    }

    static {
        for (EventTrigger trigger : EventTrigger.values()) {
            for (String name : trigger.ids()) {
                BY_ID.put(name, trigger);
            }
        }
    }

    @Nullable
    public static EventTrigger byId(String id) {
        return BY_ID.get(id.toLowerCase(Locale.ROOT));
    }
}
