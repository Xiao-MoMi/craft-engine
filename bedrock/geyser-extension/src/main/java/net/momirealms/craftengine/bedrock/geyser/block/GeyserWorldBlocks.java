package net.momirealms.craftengine.bedrock.geyser.block;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntMaps;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.geysermc.geyser.GeyserBootstrap;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.util.PlatformType;
import org.geysermc.geyser.level.physics.BoundingBox;
import org.geysermc.geyser.registry.BlockRegistries;
import org.geysermc.geyser.translator.collision.BlockCollision;
import org.geysermc.geyser.translator.collision.OtherCollision;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.IntUnaryOperator;

public final class GeyserWorldBlocks {
    private static final boolean[] NO_CUSTOM_VISUAL_STATES = new boolean[0];
    private final List<JsonObject> pendingDefinitions = new ArrayList<>();
    private final List<JsonObject> pendingStateMappings = new ArrayList<>();
    private final Object2IntMap<String> registeredAliases = new Object2IntOpenHashMap<>();
    private final Int2ObjectMap<BlockCollision> originalCollisions = new Int2ObjectOpenHashMap<>();
    private volatile boolean[] customVisualStates = NO_CUSTOM_VISUAL_STATES;
    private GeyserSpigotWorldManagerHook worldManagerHook;

    public void add(JsonObject definition) {
        this.pendingDefinitions.add(definition);
    }

    public void addStateMappings(JsonObject mappings) {
        this.pendingStateMappings.add(mappings);
    }

    public boolean contains(int visualState) {
        boolean[] states = this.customVisualStates;
        return visualState >= 0 && visualState < states.length && states[visualState];
    }

    public boolean isEmpty() {
        return this.customVisualStates.length == 0;
    }

    public void install() {
        if (this.pendingDefinitions.isEmpty() && this.pendingStateMappings.isEmpty()) {
            return;
        }
        Map<String, Integer> identifiers = BlockRegistries.JAVA_BLOCK_STATE_IDENTIFIER_TO_ID.get();
        Int2IntMap serverToVisualStates = new Int2IntOpenHashMap();
        this.loadStateMappings(identifiers, serverToVisualStates);
        this.registerDefinitions(identifiers, serverToVisualStates);
        this.installWorldManager(serverToVisualStates);
    }

    private void loadStateMappings(Map<String, Integer> identifiers, Int2IntMap serverToVisualStates) {
        // Native world queries return server IDs even for blocks that keep their vanilla visuals.
        for (JsonObject mappings : this.pendingStateMappings) {
            for (Map.Entry<String, JsonElement> entry : mappings.entrySet()) {
                Integer visualStateId = identifiers.get(entry.getKey());
                if (visualStateId != null) {
                    int visualState = visualStateId;
                    for (JsonElement id : entry.getValue().getAsJsonArray()) {
                        serverToVisualStates.put(id.getAsInt(), visualState);
                    }
                }
            }
        }
        this.pendingStateMappings.clear();
    }

    private void registerDefinitions(Map<String, Integer> identifiers, Int2IntMap serverToVisualStates) {
        if (this.pendingDefinitions.isEmpty()) {
            return;
        }
        boolean[] customStates = this.customVisualStates;
        if (customStates.length == 0) {
            customStates = new boolean[BlockRegistries.BLOCK_STATES.get().size()];
        }
        for (JsonObject definition : this.pendingDefinitions) {
            String state = definition.get("java_state").getAsString();
            Integer visualStateId = identifiers.get(state);
            if (visualStateId == null) {
                throw new IllegalStateException("Unknown visual block state: " + state);
            }
            int visualState = visualStateId;
            customStates[visualState] = true;
            if (definition.has("collision_boxes")) {
                List<BlockCollision> collisions = BlockRegistries.COLLISIONS.get();
                this.originalCollisions.put(visualState, collisions.set(visualState, collision(definition.getAsJsonArray("collision_boxes"))));
            }
            for (JsonElement element : definition.getAsJsonArray("server_states")) {
                JsonObject server = element.getAsJsonObject();
                serverToVisualStates.put(server.get("id").getAsInt(), visualState);
                String alias = server.get("state").getAsString();
                if (!identifiers.containsKey(alias)) {
                    identifiers.put(alias, visualStateId);
                    this.registeredAliases.put(alias, visualState);
                }
            }
        }
        this.customVisualStates = customStates;
        this.pendingDefinitions.clear();
    }

    private void installWorldManager(Int2IntMap serverToVisualStates) {
        if (serverToVisualStates.isEmpty()) {
            return;
        }
        GeyserImpl geyser = GeyserImpl.getInstance();
        GeyserBootstrap bootstrap = geyser.getBootstrap();
        if (bootstrap.platformType() == PlatformType.SPIGOT) {
            this.worldManagerHook = GeyserSpigotWorldManagerHook.install(bootstrap, stateLookup(serverToVisualStates));
        }
    }

    private static IntUnaryOperator stateLookup(Int2IntMap serverToVisualStates) {
        int maxId = 0;
        for (IntIterator ids = serverToVisualStates.keySet().iterator(); ids.hasNext();) {
            maxId = Math.max(maxId, ids.nextInt());
        }
        int[] visualStates = new int[maxId + 1];
        for (int id = 0; id < visualStates.length; id++) {
            visualStates[id] = id;
        }
        for (Int2IntMap.Entry entry : Int2IntMaps.fastIterable(serverToVisualStates)) {
            visualStates[entry.getIntKey()] = entry.getIntValue();
        }
        return id -> id >= 0 && id < visualStates.length ? visualStates[id] : id;
    }

    public void uninstall() {
        if (this.worldManagerHook != null) {
            this.worldManagerHook.uninstall();
            this.worldManagerHook = null;
        }

        Map<String, Integer> identifiers = BlockRegistries.JAVA_BLOCK_STATE_IDENTIFIER_TO_ID.get();
        for (Object2IntMap.Entry<String> entry : Object2IntMaps.fastIterable(this.registeredAliases)) {
            identifiers.remove(entry.getKey(), entry.getIntValue());
        }
        if (!this.originalCollisions.isEmpty()) {
            List<BlockCollision> collisions = BlockRegistries.COLLISIONS.get();
            for (Int2ObjectMap.Entry<BlockCollision> entry : Int2ObjectMaps.fastIterable(this.originalCollisions)) {
                collisions.set(entry.getIntKey(), entry.getValue());
            }
            this.originalCollisions.clear();
        }
        this.customVisualStates = NO_CUSTOM_VISUAL_STATES;
        this.registeredAliases.clear();
    }

    private static BlockCollision collision(JsonArray boxes) {
        BoundingBox[] bounds = new BoundingBox[boxes.size()];
        for (int i = 0; i < boxes.size(); i++) {
            JsonObject box = boxes.get(i).getAsJsonObject();
            JsonArray origin = box.getAsJsonArray("origin");
            JsonArray size = box.getAsJsonArray("size");
            double x = size.get(0).getAsDouble() / 16;
            double y = size.get(1).getAsDouble() / 16;
            double z = size.get(2).getAsDouble() / 16;
            // Component X is mirrored; Geyser's movement checks use Java world coordinates.
            bounds[i] = new BoundingBox(
                    (8 - origin.get(0).getAsDouble()) / 16 - x / 2,
                    origin.get(1).getAsDouble() / 16 + y / 2,
                    (origin.get(2).getAsDouble() + 8) / 16 + z / 2,
                    x, y, z
            );
        }
        return new OtherCollision(bounds);
    }
}
