package net.momirealms.craftengine.bedrock.geyser.block;

import com.google.gson.JsonArray;
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
    private GeyserBlockBreakEffects breakEffects;

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
            this.registerEntries(plan.getAsJsonArray(ENTRIES), event, false);
            JsonArray overrides = plan.getAsJsonArray(VANILLA_OVERRIDES);
            if (overrides != null) {
                this.registerEntries(overrides, event, true);
            }
        } catch (RuntimeException exception) {
            GeyserTranslationManager.error("block.registration_failed", exception);
        }
    }

    private void registerEntries(JsonArray entries, GeyserDefineCustomBlocksEvent event, boolean vanillaOverride) {
        for (JsonElement value : entries) {
            JsonObject display = value.getAsJsonObject();
            String name = display.get(NAME).getAsString();
            if (this.displayBlocks.containsKey(name)) {
                continue;
            }
            try {
                this.displayBlocks.put(name, GeyserBlockRegistrationAdapter.registerBlock(display, event, vanillaOverride));
                if (!vanillaOverride && display.has("java_state")) {
                    this.worldBlocks.add(display);
                }
            } catch (RuntimeException exception) {
                GeyserTranslationManager.error("block.register_failed", exception, name);
            }
        }
    }

    public void init() {
        this.worldBlocks.install();
        if (!this.worldBlocks.isEmpty()) {
            this.breakEffects = GeyserBlockBreakEffects.install(this.worldBlocks);
        }
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
        if (this.breakEffects != null) {
            this.breakEffects.uninstall();
        }
        this.worldBlocks.uninstall();
    }

    public void disable() {
        if (this.placementHandler != null) {
            this.placementHandler.uninstall();
        }
    }
}
