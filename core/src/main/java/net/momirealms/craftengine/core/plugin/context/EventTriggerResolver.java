package net.momirealms.craftengine.core.plugin.context;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@FunctionalInterface
public interface EventTriggerResolver {

    @Nullable
    EventTrigger resolve(String id);

    static EventTriggerResolver registered() {
        return EventTrigger::byId;
    }

    static EventTriggerResolver withAliases(Map<String, EventTrigger> aliases) {
        Map<String, EventTrigger> normalizedAliases = new HashMap<>(aliases.size());
        for (Map.Entry<String, EventTrigger> entry : aliases.entrySet()) {
            normalizedAliases.put(entry.getKey().toLowerCase(Locale.ROOT), Objects.requireNonNull(entry.getValue()));
        }
        return id -> {
            EventTrigger trigger = normalizedAliases.get(id.toLowerCase(Locale.ROOT));
            return trigger != null ? trigger : EventTrigger.byId(id);
        };
    }

    static EventTriggerResolver withAlias(String alias, EventTrigger trigger) {
        return withAliases(Map.of(alias, trigger));
    }
}
