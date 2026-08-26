package net.momirealms.craftengine.core.plugin.context;

import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class EventTrigger {
    private static final Map<String, EventTrigger> BY_ALIAS = new ConcurrentHashMap<>();

    public static final EventTrigger LEFT_CLICK = register(Key.ce("left_click"));
    public static final EventTrigger RIGHT_CLICK = register(Key.ce("right_click"), "use_on", "use", "use_item_on");
    public static final EventTrigger ATTACK = register(Key.ce("attack"), "hit");
    public static final EventTrigger CONSUME = register(Key.ce("consume"), "eat", "drink");
    public static final EventTrigger BLOCK_BREAK = register(Key.ce("block_break"), "dig");
    public static final EventTrigger ITEM_BREAK = register(Key.ce("item_break"));
    public static final EventTrigger FURNITURE_BREAK = register(Key.ce("furniture_break"));
    public static final EventTrigger PLACE = register(Key.ce("place"), "build");
    public static final EventTrigger PICK_UP = register(Key.ce("pick_up"), "pick");
    public static final EventTrigger STEP = register(Key.ce("step"));
    public static final EventTrigger FALL = register(Key.ce("fall"));
    public static final EventTrigger SHOOT = register(Key.ce("shoot"));

    private final Key id;
    private final List<String> aliases;

    private EventTrigger(Key id, List<String> aliases) {
        this.id = id;
        this.aliases = aliases;
    }

    public Key id() {
        return this.id;
    }

    public List<String> aliases() {
        return this.aliases;
    }

    public String[] ids() {
        String[] ids = new String[this.aliases.size() + 1];
        ids[0] = asConfigId(this.id);
        for (int i = 0; i < this.aliases.size(); i++) {
            ids[i + 1] = this.aliases.get(i);
        }
        return ids;
    }

    public static EventTrigger register(Key id, String... aliases) {
        Objects.requireNonNull(id, "id");

        EventTrigger trigger = new EventTrigger(id, Arrays.asList(aliases));
        synchronized (EventTrigger.class) {
            WritableRegistry<EventTrigger> registry = (WritableRegistry<EventTrigger>) BuiltInRegistries.EVENT_TRIGGER;
            if (registry.containsKey(id)) {
                throw new IllegalStateException("Duplicate event trigger '" + id + "'");
            }
            String configId = asConfigId(id);
            if (BY_ALIAS.containsKey(configId)) {
                throw new IllegalStateException("Event trigger '" + id + "' conflicts with alias '" + configId + "'");
            }
            for (String alias : aliases) {
                if (BY_ALIAS.containsKey(alias) || BuiltInRegistries.EVENT_TRIGGER.containsKey(Key.ce(alias))) {
                    throw new IllegalStateException("Duplicate event trigger alias '" + alias + "'");
                }
            }
            registry.register(ResourceKey.create(Registries.EVENT_TRIGGER.location(), id), trigger);
            for (String alias : aliases) {
                BY_ALIAS.put(alias, trigger);
            }
        }
        return trigger;
    }

    @Nullable
    public static EventTrigger byId(String id) {
        String normalized = normalize(id);
        EventTrigger trigger = BY_ALIAS.get(normalized);
        if (trigger != null) {
            return trigger;
        }
        return BuiltInRegistries.EVENT_TRIGGER.getValue(Key.ce(normalized));
    }

    public static List<String> registeredIds() {
        List<String> ids = new ArrayList<>(BuiltInRegistries.EVENT_TRIGGER.keySet().size() + BY_ALIAS.size());
        for (Key key : BuiltInRegistries.EVENT_TRIGGER.keySet()) {
            ids.add(asConfigId(key));
        }
        ids.addAll(BY_ALIAS.keySet());
        Collections.sort(ids);
        return List.copyOf(ids);
    }

    private static String normalize(String id) {
        return id.toLowerCase(Locale.ROOT);
    }

    private static String asConfigId(Key id) {
        return id.namespace().equals(Key.CRAFTENGINE_NAMESPACE) ? id.value() : id.asString();
    }

    @Override
    public String toString() {
        return asConfigId(this.id);
    }
}
