package net.momirealms.craftengine.bedrock.geyser.entity.display;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.momirealms.craftengine.bedrock.network.NativeModelPayload;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.session.GeyserSession;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class GeyserDisplayModelOwners {
    private final GeyserSession session;
    private final Map<UUID, Bones> instances = new HashMap<>();
    private final Int2ObjectOpenHashMap<Bones> displays = new Int2ObjectOpenHashMap<>();

    GeyserDisplayModelOwners(GeyserSession session) {
        this.session = session;
    }

    public void clear() {
        this.instances.clear();
        this.displays.clear();
    }

    String model(int display) {
        Bones bones = this.displays.get(display);
        return bones == null ? null : bones.model();
    }

    void update(NativeModelPayload.DisplayBones state) {
        Bones previous = this.instances.get(state.instance());
        if (previous != null && previous.displays().equals(state.displays()) && previous.model().equals(state.model())) {
            return;
        }
        this.replace(state.instance(), previous, new Bones(state.model(), state.displays()));
    }

    void remove(UUID instance) {
        Bones previous = this.instances.get(instance);
        if (previous != null) {
            this.replace(instance, previous, null);
        }
    }

    private void replace(UUID instance, Bones previous, Bones bones) {
        if (previous != null) {
            this.instances.remove(instance);
            for (int id : previous.displays()) {
                if (this.displays.get(id) == previous) {
                    this.displays.remove(id);
                }
            }
        }
        if (bones != null) {
            this.instances.put(instance, bones);
            for (int id : bones.displays()) {
                this.displays.put(id, bones);
                this.refresh(id);
            }
        }
        if (previous != null) {
            for (int id : previous.displays()) {
                if (!this.displays.containsKey(id)) {
                    this.refresh(id);
                }
            }
        }
    }

    private void refresh(int id) {
        Entity entity = this.session.getEntityCache().getEntityByJavaId(id);
        if (entity instanceof GeyserModelDisplayEntity display) {
            display.refreshModel();
        }
    }

    private record Bones(String model, List<Integer> displays) {
    }
}
