package net.momirealms.craftengine.bedrock.geyser.block;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.bedrock.RegistrationPlanKind;
import net.momirealms.craftengine.bedrock.geyser.pack.GeyserRegistrationPlanReader;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomBlocksEvent;
import org.geysermc.geyser.api.extension.ExtensionLogger;
import org.geysermc.geyser.session.GeyserSession;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static net.momirealms.craftengine.bedrock.BedrockPackLayout.ENTRIES;
import static net.momirealms.craftengine.bedrock.BedrockPackLayout.NAME;
import static net.momirealms.craftengine.bedrock.BedrockPackLayout.WORLD_STATE_MAPPINGS;

public final class GeyserBlockManager {
    private final ExtensionLogger logger;
    private final GeyserWorldBlocks worldBlocks = new GeyserWorldBlocks();
    private final Map<String, String> displayBlocks = new HashMap<>();
    private final Set<String> blockItems = new HashSet<>();
    private GeyserBlockPlacementHandler placementHandler;

    public GeyserBlockManager(ExtensionLogger logger) {
        this.logger = logger;
    }

    public Map<String, String> displayBlocks() {
        return this.displayBlocks;
    }

    public void clearBlockItems() {
        this.blockItems.clear();
    }

    public void registerBlockItem(String identifier) {
        this.blockItems.add(identifier);
    }

    public void registerBlocks(List<Path> files, GeyserDefineCustomBlocksEvent event) {
        this.displayBlocks.clear();
        for (Path path : files) {
            this.registerBlocks(path, event);
        }
    }

    private void registerBlocks(Path path, GeyserDefineCustomBlocksEvent event) {
        try {
            JsonObject plan = GeyserRegistrationPlanReader.readPack(path, RegistrationPlanKind.BLOCK);
            if (plan == null) {
                return;
            }
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
                    this.logger.error("Failed to register display block " + name + " from " + path, exception);
                }
            }
        } catch (IOException | RuntimeException exception) {
            this.logger.error("Failed to read display blocks from " + path, exception);
        }
    }

    public void init() {
        this.worldBlocks.install();
    }

    public void initPlacementHandler() {
        this.placementHandler = GeyserBlockPlacementHandler.install(this.blockItems);
        this.logger.info("Enabled Java placement repeat interval for " + this.blockItems.size() + " CE block item variants.");
    }

    public void initSession(GeyserSession session) {
        if (!this.worldBlocks.isEmpty()) {
            session.setBlockBreakHandler(new GeyserBlockBreakHandler(session, this.worldBlocks));
        }
    }

    public void disconnect(GeyserConnection connection) {
        if (this.placementHandler != null && connection instanceof GeyserSession session) {
            this.placementHandler.disconnect(session);
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
