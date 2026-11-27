package net.momirealms.craftengine.bukkit.compatibility.model;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.momirealms.craftengine.bedrock.network.NativeModelPayload;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class NativeModelParts {
    private final Object2IntOpenHashMap<String> animations = new Object2IntOpenHashMap<>();

    void clear() {
        this.animations.clear();
    }

    public void animate(String name, int mode) {
        this.animations.putIfAbsent(name, mode);
    }

    public List<NativeModelPayload.Animation> animations(List<NativeModelPayload.Animation> previous) {
        if (this.animations.size() == previous.size()) {
            boolean unchanged = true;
            for (int i = 0; i < previous.size(); i++) {
                NativeModelPayload.Animation animation = previous.get(i);
                if (this.animations.getInt(animation.name()) != animation.mode()) {
                    unchanged = false;
                    break;
                }
            }
            if (unchanged) {
                return previous;
            }
        }
        List<NativeModelPayload.Animation> result = new ArrayList<>(this.animations.size());
        this.animations.object2IntEntrySet().fastForEach(entry -> result.add(new NativeModelPayload.Animation(entry.getKey(), entry.getIntValue())));
        result.sort(Comparator.comparing(NativeModelPayload.Animation::name));
        return List.copyOf(result);
    }
}
