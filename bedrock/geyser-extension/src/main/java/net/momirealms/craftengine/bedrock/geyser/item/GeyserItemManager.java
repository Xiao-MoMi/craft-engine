package net.momirealms.craftengine.bedrock.geyser.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.bedrock.BedrockTintSource;
import net.momirealms.craftengine.bedrock.geyser.CraftEngineGeyserExtension;
import net.momirealms.craftengine.bedrock.geyser.item.GeyserItemRegistrationAdapter.ConvertedItem;
import net.momirealms.craftengine.bedrock.geyser.item.GeyserItemRegistrationAdapter.ConvertedVariant;
import net.momirealms.craftengine.bedrock.geyser.item.component.GeyserItemComponentPatch;
import net.momirealms.craftengine.bedrock.geyser.item.component.GeyserItemComponentPatchCache;
import net.momirealms.craftengine.bedrock.geyser.item.component.GeyserItemComponentPatcher;
import net.momirealms.craftengine.bedrock.geyser.item.component.GeyserItemComponentPatches;
import net.momirealms.craftengine.bedrock.geyser.item.component.GeyserItemText;
import net.momirealms.craftengine.bedrock.geyser.locale.GeyserTranslationManager;
import net.momirealms.craftengine.bedrock.geyser.player.GeyserPlayer;
import org.cloudburstmc.protocol.bedrock.data.inventory.CreativeItemCategory;
import org.geysermc.geyser.api.entity.property.type.GeyserIntEntityProperty;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomItemsEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineEntityPropertiesEvent;
import org.geysermc.geyser.api.item.custom.v2.CustomItemDefinition;
import org.geysermc.geyser.api.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static net.momirealms.craftengine.bedrock.BedrockPackLayout.*;

public final class GeyserItemManager {
    private final CraftEngineGeyserExtension extension;
    private final Set<String> registeredItems = new HashSet<>();
    private final GeyserWorldItemViews worldItemViews = new GeyserWorldItemViews();
    private final Map<String, CustomItemDefinition> contextualItems = new HashMap<>();
    private final Map<String, String> headIdentifiers = new HashMap<>();
    private Map<String, CreativeItemCategory> creativeVariants = Map.of();
    private GeyserItemComponentPatchCache componentPatches;
    private GeyserDyeProperties mainHandDye;
    private GeyserDyeProperties offHandDye;
    private GeyserDyeProperties mainHandThirdDye;
    private GeyserDyeProperties offHandThirdDye;
    private GeyserDyeProperties headDye;
    private boolean hasDyedItems;
    private Map<String, GeyserItemTints> tintSources = Map.of();
    private Map<String, Integer> elytraVariants = Map.of();
    private GeyserIntEntityProperty elytraProperty;

    public GeyserItemManager(CraftEngineGeyserExtension extension) {
        this.extension = extension;
    }

    public void registerItems(@Nullable JsonObject plan, @Nullable JsonObject elytraPlan, GeyserDefineCustomItemsEvent event) {
        this.registeredItems.clear();
        this.contextualItems.clear();
        this.headIdentifiers.clear();
        this.creativeVariants = Map.of();
        this.extension.blockManager().clearBlockItems();
        this.hasDyedItems = false;
        this.tintSources = Map.of();
        this.elytraVariants = Map.of();
        this.elytraProperty = null;
        this.componentPatches = null;
        if (plan == null) {
            return;
        }
        GeyserItemRegistrationAdapter adapter;
        JsonArray entries;
        Registration registration;
        try {
            adapter = new GeyserItemRegistrationAdapter(plan.get(SOURCE_VERSION).getAsString());
            entries = plan.get(ENTRIES).getAsJsonArray();
            registration = new Registration(event, elytraPlan);
        } catch (RuntimeException exception) {
            GeyserTranslationManager.error("item.prepare_failed", exception);
            return;
        }
        for (JsonElement value : entries) {
            if (!value.isJsonObject()) {
                registration.failed++;
                GeyserTranslationManager.error("item.register_failed", value, "?", "?");
                continue;
            }
            JsonObject entry = value.getAsJsonObject();
            try {
                ConvertedItem item = adapter.convert(entry, this.extension.blockManager().displayBlocks());
                registration.skipped += item.skipped();
                registration.failed += item.failed();
                for (ConvertedVariant variant : item.variants()) {
                    this.registerVariant(item, variant, registration);
                }
            } catch (UnsupportedOperationException exception) {
                registration.skipped++;
                GeyserTranslationManager.warn("item.skipped", entry.get(BEDROCK_IDENTIFIER), entry.get(MODEL), exception.getMessage());
            } catch (RuntimeException exception) {
                registration.failed++;
                GeyserTranslationManager.error("item.register_failed", exception, entry.get(BEDROCK_IDENTIFIER), entry.get(ITEM_TYPE), entry.get(MODEL));
            }
        }
        if (!registration.patches.isEmpty() || !registration.itemText.isEmpty()) {
            this.componentPatches = new GeyserItemComponentPatchCache(registration.patches, registration.itemText);
        }
        this.tintSources = Map.copyOf(registration.tintSources);
        this.elytraVariants = Map.copyOf(registration.elytraVariants);
        this.creativeVariants = Map.copyOf(registration.creativeVariants);
        GeyserTranslationManager.info("item.registered", registration.registered, registration.skipped, registration.failed);
    }

    private void registerVariant(ConvertedItem item, ConvertedVariant variant, Registration registration) {
        CustomItemDefinition definition = variant.definition();
        String primaryId = definition.bedrockIdentifier().toString();
        try {
            this.registerDefinition(item, definition, registration);
            if (variant.creativeCategory() != null) {
                registration.creativeVariants.put(primaryId, variant.creativeCategory());
            }
            if (!variant.tintSources().isEmpty()) {
                registration.tintSources.put(primaryId, GeyserItemTints.from(variant.tintSources()));
                this.hasDyedItems |= variant.tintSources().values().stream().anyMatch(BedrockTintSource::isDynamic);
            }
            if (variant.head() != null) {
                definition = variant.head();
                this.registerDefinition(item, definition, registration);
                this.headIdentifiers.put(primaryId, definition.bedrockIdentifier().toString());
                registration.patches.put(primaryId, GeyserItemComponentPatches.withoutWearable(item.componentPatch()));
            }
        } catch (RuntimeException exception) {
            registration.failed++;
            GeyserTranslationManager.error("item.register_failed", exception, definition.bedrockIdentifier(), item.itemType(), definition.model());
        }
    }

    private void registerDefinition(ConvertedItem item, CustomItemDefinition definition, Registration registration) {
        String identifier = definition.bedrockIdentifier().toString();
        JsonElement elytra = registration.elytraPlan == null ? null : registration.elytraPlan.get(identifier);
        Integer elytraVariant = elytra == null ? null : elytra.getAsInt();
        registration.event.register(item.itemType(), definition);
        registration.registered++;
        this.registeredItems.add(identifier);
        if (elytraVariant != null) {
            registration.elytraVariants.put(identifier, elytraVariant);
        }
        this.hasDyedItems |= definition.bedrockOptions().defaultDyeableColor() != null;
        if (item.placesBlock()) {
            this.extension.blockManager().registerBlockItem(identifier);
        }
        if (item.contextual()) {
            this.contextualItems.put(identifier, definition);
        }
        if (item.componentPatch() != null) {
            registration.patches.put(identifier, item.componentPatch());
        }
        if (item.text() != null) {
            registration.itemText.put(identifier, item.text());
        }
    }

    public void registerProperties(GeyserDefineEntityPropertiesEvent event) {
        if (!this.elytraVariants.isEmpty()) {
            int max = this.elytraVariants.values().stream().mapToInt(Integer::intValue).max().orElse(0);
            this.elytraProperty = event.registerIntegerProperty(Identifier.of("minecraft:player"), Identifier.of(ELYTRA_VARIANT_PROPERTY), 0, max, 0);
        }
        if (this.hasDyedItems) {
            Identifier player = Identifier.of("minecraft:player");
            this.mainHandDye = GeyserDyeProperties.register(event, player, MAIN_HAND_DYE);
            this.offHandDye = GeyserDyeProperties.register(event, player, OFF_HAND_DYE);
            this.mainHandThirdDye = GeyserDyeProperties.register(event, player, MAIN_HAND_DYE + "_third");
            this.offHandThirdDye = GeyserDyeProperties.register(event, player, OFF_HAND_DYE + "_third");
            this.headDye = GeyserDyeProperties.register(event, player, HEAD_DYE);
        }
    }

    public void init() {
        this.worldItemViews.installShelf();
    }

    public boolean hasDyedItems() {
        return this.hasDyedItems;
    }

    public GeyserHeldDye initPlayerDye(GeyserPlayer player) {
        if (this.hasDyedItems) {
            return GeyserHeldDye.install(player, this.mainHandDye, this.offHandDye, this.mainHandThirdDye, this.offHandThirdDye, this.headDye);
        }
        return null;
    }

    public GeyserElytraEquipment initPlayerElytra(GeyserPlayer player) {
        return this.elytraProperty == null ? null : new GeyserElytraEquipment(player, this.elytraProperty, this.elytraVariants);
    }

    public GeyserItemComponentPatcher createComponentPatcher(GeyserPlayer player) {
        return this.componentPatches == null ? null : new GeyserItemComponentPatcher(player, this.componentPatches);
    }

    public GeyserItemRuntime initPlayer(GeyserPlayer player) {
        if (!this.contextualItems.isEmpty() || !this.headIdentifiers.isEmpty() || !this.tintSources.isEmpty()) {
            return GeyserItemRuntime.install(player, this.contextualItems, this.headIdentifiers, this.tintSources);
        }
        return null;
    }

    @Nullable
    public GeyserCreativeInventory initCreativeInventory(GeyserPlayer player) {
        return this.creativeVariants.isEmpty() ? null : new GeyserCreativeInventory(player.session(), this.creativeVariants, this.extension.logger());
    }

    public boolean hasRegisteredItems() {
        return !this.registeredItems.isEmpty();
    }

    public boolean isRegisteredItem(String identifier) {
        return this.registeredItems.contains(identifier);
    }

    public void disable() {
        this.worldItemViews.uninstallShelf();
    }

    private static final class Registration {
        private final GeyserDefineCustomItemsEvent event;
        private final JsonObject elytraPlan;
        private final Map<String, Integer> elytraVariants = new HashMap<>();
        private final Map<String, GeyserItemComponentPatch> patches = new HashMap<>();
        private final Map<String, GeyserItemText> itemText = new HashMap<>();
        private final Map<String, GeyserItemTints> tintSources = new HashMap<>();
        private final Map<String, CreativeItemCategory> creativeVariants = new HashMap<>();
        private int registered;
        private int skipped;
        private int failed;

        private Registration(GeyserDefineCustomItemsEvent event, @Nullable JsonObject elytraPlan) {
            this.event = event;
            this.elytraPlan = elytraPlan;
        }
    }
}
