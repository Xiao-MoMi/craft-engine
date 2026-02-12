package net.momirealms.craftengine.core.plugin.context;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public enum EventTrigger {
    LEFT_CLICK("attack", "left_click", "hit"),
    RIGHT_CLICK("right_click", "use_on", "use", "use_item_on"),
    CONSUME("eat", "consume", "drink"),
    BREAK("break", "dig"),
    PLACE("place", "build"),
    PICK_UP("pick_up", "pick"),
    STEP("step"),
    FALL("fall"),;

    public static final Map<String, EventTrigger> BY_NAME = new HashMap<>();
    private final String[] names;

    EventTrigger(String... names) {
        this.names = names;
    }

    public String[] names() {
        return names;
    }

    static {
        for (EventTrigger trigger : EventTrigger.values()) {
            for (String name : trigger.names()) {
                BY_NAME.put(name, trigger);
            }
        }
    }

    public static EventTrigger byName(String name) {
        return Optional.ofNullable(BY_NAME.get(name)).orElseThrow(() -> new IllegalArgumentException("Unknown event trigger: " + name));
    }
}
