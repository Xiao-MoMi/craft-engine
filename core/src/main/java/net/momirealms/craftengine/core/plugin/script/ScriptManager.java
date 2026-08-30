package net.momirealms.craftengine.core.plugin.script;

import net.momirealms.craftengine.core.plugin.Manageable;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.script.binding.ScriptBinding;
import net.momirealms.craftengine.core.plugin.script.placeholder.ScriptPlaceholderManager;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public interface ScriptManager extends Manageable {

    Optional<ScriptFile> script(Key id);

    boolean isAvailable();

    void registerBinding(ScriptBinding binding);

    Optional<ScriptFile> script(String id);

    @Nullable
    Object invoke(String id, String function, Map<String, Object> injected);

    boolean test(String id, String function, Map<String, Object> injected, boolean def);

    ScriptPlaceholderManager placeholderManager();

    static Map<String, Object> flattenContext(Context context) {
        Map<String, Object> map = new HashMap<>();
        context.contexts().params().forEach((key, value) -> {
            if (value != null) map.put(key.node(), value);
        });
        map.put("ctx", context);
        return map;
    }
}
