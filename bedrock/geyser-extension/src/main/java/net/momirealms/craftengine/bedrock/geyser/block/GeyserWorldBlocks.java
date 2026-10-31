package net.momirealms.craftengine.bedrock.geyser.block;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.momirealms.craftengine.bedrock.geyser.proxy.GeyserBootstrapProxy;
import org.geysermc.geyser.GeyserBootstrap;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.level.WorldManager;
import org.geysermc.geyser.registry.BlockRegistries;
import org.geysermc.geyser.level.physics.BoundingBox;
import org.geysermc.geyser.translator.collision.BlockCollision;
import org.geysermc.geyser.translator.collision.OtherCollision;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class GeyserWorldBlocks {
    private final List<JsonObject> definitions = new ArrayList<>();
    private final Int2IntMap visualStates = new Int2IntOpenHashMap();
    private final BitSet customStates = new BitSet();
    private final Map<String, Integer> aliases = new HashMap<>();
    private final Int2ObjectMap<BlockCollision> originalCollisions = new Int2ObjectOpenHashMap<>();
    private GeyserBootstrap originalBootstrap;
    private GeyserBootstrap installedBootstrap;
    private volatile WorldManager originalWorld;
    private volatile WorldManager mappedWorld;

    public void add(JsonObject definition) {
        this.definitions.add(definition);
    }

    public boolean contains(int visualState) {
        return this.customStates.get(visualState);
    }

    public boolean containsServerState(int serverState) {
        return this.visualStates.containsKey(serverState);
    }

    public boolean isEmpty() {
        return this.customStates.isEmpty();
    }

    public void install() {
        if (this.definitions.isEmpty()) {
            return;
        }
        Map<String, Integer> identifiers = BlockRegistries.JAVA_BLOCK_STATE_IDENTIFIER_TO_ID.get();
        for (JsonObject definition : this.definitions) {
            String state = definition.get("java_state").getAsString();
            Integer visual = identifiers.get(state);
            if (visual == null) {
                throw new IllegalStateException("Unknown visual block state: " + state);
            }
            this.customStates.set(visual);
            if (definition.has("collision_boxes")) {
                this.originalCollisions.put(visual.intValue(), BlockRegistries.COLLISIONS.get(visual));
                BlockRegistries.COLLISIONS.get().set(visual, collision(definition.getAsJsonArray("collision_boxes")));
            }
            for (JsonElement element : definition.getAsJsonArray("server_states")) {
                JsonObject server = element.getAsJsonObject();
                this.visualStates.put(server.get("id").getAsInt(), visual.intValue());
                String alias = server.get("state").getAsString();
                if (!identifiers.containsKey(alias)) {
                    identifiers.put(alias, visual);
                    this.aliases.put(alias, visual);
                }
            }
        }
        GeyserImpl geyser = GeyserImpl.getInstance();
        this.originalBootstrap = geyser.getBootstrap();
        this.installedBootstrap = (GeyserBootstrap) Proxy.newProxyInstance(GeyserBootstrap.class.getClassLoader(),
                    new Class<?>[]{GeyserBootstrap.class}, (proxy, method, arguments) -> {
                        if (method.getName().equals("getWorldManager")) {
                            return this.worldManager();
                        }
                        try {
                            return method.invoke(this.originalBootstrap, arguments);
                        } catch (InvocationTargetException exception) {
                            throw exception.getCause();
                        }
                    });
        GeyserBootstrapProxy.INSTANCE.bootstrap(geyser, this.installedBootstrap);
        this.definitions.clear();
    }

    private WorldManager worldManager() {
        WorldManager current = this.originalBootstrap.getWorldManager();
        if (current != this.originalWorld) {
            synchronized (this) {
                // Spigot constructs its manager after Geyser's post-initialize event.
                // Only the native manager returns raw server IDs; other managers use Geyser IDs already.
                this.mappedWorld = current != null && current.getClass().getName().endsWith(".GeyserSpigotNativeWorldManager")
                        ? new GeyserWorldManager(current, id -> this.visualStates.getOrDefault(id, id)) : current;
                this.originalWorld = current;
            }
        }
        return this.mappedWorld;
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
            bounds[i] = new BoundingBox((8 - origin.get(0).getAsDouble()) / 16 - x / 2,
                    origin.get(1).getAsDouble() / 16 + y / 2,
                    (origin.get(2).getAsDouble() + 8) / 16 + z / 2, x, y, z);
        }
        return new OtherCollision(bounds);
    }

    public void uninstall() {
        GeyserImpl geyser = GeyserImpl.getInstance();
        if (this.installedBootstrap != null && geyser.getBootstrap() == this.installedBootstrap) {
            GeyserBootstrapProxy.INSTANCE.bootstrap(geyser, this.originalBootstrap);
        }
        Map<String, Integer> identifiers = BlockRegistries.JAVA_BLOCK_STATE_IDENTIFIER_TO_ID.get();
        this.aliases.forEach((key, value) -> identifiers.remove(key, value));
        for (Int2ObjectMap.Entry<BlockCollision> entry : this.originalCollisions.int2ObjectEntrySet()) {
            BlockRegistries.COLLISIONS.get().set(entry.getIntKey(), entry.getValue());
        }
        this.originalCollisions.clear();
    }
}
