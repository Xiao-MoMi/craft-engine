package net.momirealms.craftengine.bedrock.geyser.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.bedrock.RegistrationPlanKind;
import net.momirealms.craftengine.bedrock.geyser.block.GeyserBlockManager;
import net.momirealms.craftengine.bedrock.geyser.item.component.GeyserItemComponentPatch;
import net.momirealms.craftengine.bedrock.geyser.item.component.GeyserItemComponentPatcher;
import net.momirealms.craftengine.bedrock.geyser.item.component.GeyserItemComponentPatches;
import net.momirealms.craftengine.bedrock.geyser.pack.GeyserRegistrationPlanReader;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomItemsEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineEntityPropertiesEvent;
import org.geysermc.geyser.api.extension.ExtensionLogger;
import org.geysermc.geyser.api.item.custom.v2.CustomItemDefinition;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.session.GeyserSession;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.momirealms.craftengine.bedrock.BedrockPackLayout.*;

@SuppressWarnings("UnstableApiUsage")
public final class GeyserItemManager {
    private final ExtensionLogger logger;
    private final GeyserBlockManager blockManager;
    private final GeyserWorldItemViews worldItemViews = new GeyserWorldItemViews();
    private final Map<String, CustomItemDefinition> contextualItems = new HashMap<>();
    private final Map<String, String> headIdentifiers = new HashMap<>();
    private Map<String, GeyserItemComponentPatch> componentPatches = Map.of();
    private GeyserDyeProperties mainHandDye;
    private GeyserDyeProperties offHandDye;
    private boolean hasDyedItems;

    public GeyserItemManager(ExtensionLogger logger, GeyserBlockManager blockManager) {
        this.logger = logger;
        this.blockManager = blockManager;
    }

    public void registerItems(List<Path> files, GeyserDefineCustomItemsEvent event) {
        this.contextualItems.clear();
        this.headIdentifiers.clear();
        this.blockManager.clearBlockItems();
        this.hasDyedItems = false;
        Map<String, GeyserItemComponentPatch> patches = new HashMap<>();
        for (Path path : files) {
            this.registerItems(path, event, patches);
        }
        patches.replaceAll((identifier, patch) -> GeyserItemComponentPatches.cached(patch));
        this.componentPatches = Map.copyOf(patches);
    }

    private void registerItems(Path path, GeyserDefineCustomItemsEvent event, Map<String, GeyserItemComponentPatch> patches) {
        GeyserItemRegistrationAdapter adapter;
        JsonArray entries;
        try {
            JsonObject plan = GeyserRegistrationPlanReader.readPack(path, RegistrationPlanKind.ITEM);
            if (plan == null) {
                return;
            }
            adapter = new GeyserItemRegistrationAdapter(plan.get(SOURCE_VERSION).getAsString());
            entries = plan.getAsJsonArray(ENTRIES);
        } catch (IOException | RuntimeException exception) {
            this.logger.error("Failed to read item registration plan from " + path, exception);
            return;
        }
        int registered = 0;
        int failed = 0;
        int skipped = 0;
        for (JsonElement value : entries) {
            JsonObject entry = value.getAsJsonObject();
            Identifier identifier = Identifier.of(entry.get(ITEM_TYPE).getAsString());
            try {
                GeyserItemRegistrationAdapter.ConvertedItem converted = adapter.convert(entry, this.blockManager.displayBlocks());
                JsonObject behavior = entry.getAsJsonObject(CLIENT_BEHAVIOR);
                boolean placesBlock = behavior != null && behavior.has(PLACES_BLOCK) && behavior.get(PLACES_BLOCK).getAsBoolean();
                for (CustomItemDefinition definition : converted.definitions()) {
                    event.register(identifier, definition);
                    this.hasDyedItems |= definition.bedrockOptions().defaultDyeableColor() != null;
                    registered++;
                    this.registerVariant(definition, converted, placesBlock, patches);
                }
                if (converted.contextual()) {
                    for (CustomItemDefinition definition : converted.definitions()) {
                        this.contextualItems.put(definition.bedrockIdentifier().toString(), definition);
                    }
                }
            } catch (UnsupportedOperationException exception) {
                skipped++;
                this.logger.warning("Skipped custom item " + entry.get(BEDROCK_IDENTIFIER) + " [model=" + entry.get(MODEL) + ", pack=" + path + "]: " + exception.getMessage());
            } catch (RuntimeException exception) {
                failed++;
                this.logger.error("Failed to register custom item " + entry.get(BEDROCK_IDENTIFIER) + " [java=" + entry.get(ITEM_TYPE) + ", model=" + entry.get(MODEL) + ", pack=" + path + "]", exception);
            }
        }
        this.logger.info("Registered " + registered + " custom item variants from " + path + "; skipped=" + skipped + ", failed=" + failed + ".");
    }

    private void registerVariant(CustomItemDefinition definition, GeyserItemRegistrationAdapter.ConvertedItem converted,
                                 boolean placesBlock, Map<String, GeyserItemComponentPatch> patches) {
        String identifier = definition.bedrockIdentifier().toString();
        if (placesBlock) {
            this.blockManager.registerBlockItem(identifier);
        }
        GeyserItemComponentPatch patch = converted.componentPatch();
        String headIdentifier = converted.headIdentifiers().get(identifier);
        if (headIdentifier != null) {
            patch = GeyserItemComponentPatches.withoutWearable(patch);
            this.headIdentifiers.put(identifier, headIdentifier);
        }
        if (patch != null) {
            patches.put(identifier, patch);
        }
    }

    public void registerProperties(GeyserDefineEntityPropertiesEvent event) {
        if (this.hasDyedItems) {
            Identifier player = Identifier.of("minecraft:player");
            this.mainHandDye = GeyserDyeProperties.register(event, player, MAIN_HAND_DYE);
            this.offHandDye = GeyserDyeProperties.register(event, player, OFF_HAND_DYE);
        }
    }

    public void init() {
        this.worldItemViews.install();
    }

    public void initSessionDye(GeyserSession session) {
        if (this.hasDyedItems) {
            GeyserHeldDye.install(session, this.mainHandDye, this.offHandDye);
        }
    }

    public void initSession(GeyserSession session) {
        if (!this.componentPatches.isEmpty()) {
            GeyserItemComponentPatcher.install(session, this.componentPatches);
        }
        if (!this.contextualItems.isEmpty() || !this.headIdentifiers.isEmpty()) {
            GeyserItemRuntime.install(session, this.contextualItems, this.headIdentifiers);
        }
    }

    public void disable() {
        this.worldItemViews.uninstall();
    }
}
