package net.momirealms.craftengine.bedrock.geyser.block;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.bedrock.geyser.locale.GeyserTranslationManager;
import net.momirealms.craftengine.bedrock.geyser.player.GeyserPlayer;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomBlocksEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static net.momirealms.craftengine.bedrock.BedrockPackLayout.*;

public final class GeyserBlockManager {
    private final GeyserWorldBlocks worldBlocks = new GeyserWorldBlocks();
    private final Map<String, String> displayBlocks = new HashMap<>();
    private final Set<String> blockItems = new HashSet<>();
    private GeyserBlockPlacementHandler placementHandler;

    public Map<String, String> displayBlocks() {
        return this.displayBlocks;
    }

    public void clearBlockItems() {
        this.blockItems.clear();
    }

    public void registerBlockItem(String identifier) {
        this.blockItems.add(identifier);
    }

    public void registerBlocks(JsonObject plan, GeyserDefineCustomBlocksEvent event) {
        this.displayBlocks.clear();
        if (plan == null) {
            return;
        }
        try {
            if (plan.has(WORLD_STATE_MAPPINGS)) {
                this.worldBlocks.addStateMappings(plan.getAsJsonObject(WORLD_STATE_MAPPINGS));
            }
            for (JsonElement value : plan.getAsJsonArray(ENTRIES)) {
                JsonObject display = value.getAsJsonObject();
                String name = display.get(NAME).getAsString();
                if (this.displayBlocks.containsKey(name)) {
                    continue;
                }
                try {
                    this.displayBlocks.put(name, GeyserBlockRegistrationAdapter.registerBlock(display, event));
                    if (display.has("java_state")) {
                        this.worldBlocks.add(display);
                    }
                } catch (RuntimeException exception) {
                    GeyserTranslationManager.error("block.register_failed", exception, name);
                }
            }
        } catch (RuntimeException exception) {
            GeyserTranslationManager.error("block.registration_failed", exception);
        }
    }

    public void init() {
        this.worldBlocks.install();
    }

    public void initPlacementHandler() {
        this.placementHandler = GeyserBlockPlacementHandler.install(this.blockItems);
        GeyserTranslationManager.info("block.placement_enabled", this.blockItems.size());
    }

    public void initPlayer(GeyserPlayer player) {
        if (!this.worldBlocks.isEmpty()) {
            player.blockBreakHandler(new GeyserBlockBreakHandler(player.session(), this.worldBlocks));
        }
    }

    public void unload() {
        this.worldBlocks.uninstall();
    }

    public void disable() {
        if (this.placementHandler != null) {
            this.placementHandler.uninstall();
        }
    }
}
