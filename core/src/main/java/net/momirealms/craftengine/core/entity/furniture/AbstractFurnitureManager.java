package net.momirealms.craftengine.core.entity.furniture;

import net.momirealms.craftengine.core.entity.Billboard;
import net.momirealms.craftengine.core.entity.ItemDisplayContext;
import net.momirealms.craftengine.core.loot.LootTable;
import net.momirealms.craftengine.core.pack.LoadingSequence;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.ConfigParser;
import net.momirealms.craftengine.core.plugin.context.event.EventFunctions;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import org.incendo.cloud.suggestion.Suggestion;
import org.joml.Vector3f;

import java.nio.file.Path;
import java.util.*;

public abstract class AbstractFurnitureManager implements FurnitureManager {
    protected final Map<Key, CustomFurniture> byId = new HashMap<>();
    private final CraftEngine plugin;
    private final FurnitureParser furnitureParser;
    // Cached command suggestions
    private final List<Suggestion> cachedSuggestions = new ArrayList<>();

    public AbstractFurnitureManager(CraftEngine plugin) {
        this.plugin = plugin;
        this.furnitureParser = new FurnitureParser();
    }

    @Override
    public ConfigParser parser() {
        return this.furnitureParser;
    }

    @Override
    public void delayedLoad() {
        this.initSuggestions();
    }

    @Override
    public void initSuggestions() {
        this.cachedSuggestions.clear();
        for (Key key : this.byId.keySet()) {
            this.cachedSuggestions.add(Suggestion.suggestion(key.toString()));
        }
    }

    @Override
    public Collection<Suggestion> cachedSuggestions() {
        return Collections.unmodifiableCollection(this.cachedSuggestions);
    }

    @Override
    public Optional<CustomFurniture> furnitureById(Key id) {
        return Optional.ofNullable(this.byId.get(id));
    }

    @Override
    public void unload() {
        this.byId.clear();
    }

    protected abstract HitBox defaultHitBox();

    protected abstract FurnitureElement.Builder furnitureElementBuilder();

    protected abstract CustomFurniture.Builder furnitureBuilder();

    public class FurnitureParser implements ConfigParser {
        public static final String[] CONFIG_SECTION_NAME = new String[] { "furniture" };

        @Override
        public String[] sectionId() {
            return CONFIG_SECTION_NAME;
        }

        @Override
        public int loadingSequence() {
            return LoadingSequence.FURNITURE;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void parseSection(Pack pack, Path path, Key id, Map<String, Object> section) {
            if (byId.containsKey(id)) {
                throw new LocalizedResourceConfigException("warning.config.furniture.duplicate");
            }
            EnumMap<AnchorType, CustomFurniture.Placement> placements = new EnumMap<>(AnchorType.class);
            Object placementObj = section.get("placement");
            Map<String, Object> placementMap = MiscUtils.castToMap(ResourceConfigUtils.requireNonNullOrThrow(placementObj, "warning.config.furniture.missing_placement"), false);
            if (placementMap.isEmpty()) {
                throw new LocalizedResourceConfigException("warning.config.furniture.missing_placement");
            }
            for (Map.Entry<String, Object> entry : placementMap.entrySet()) {
                // anchor type
                AnchorType anchorType = AnchorType.valueOf(entry.getKey().toUpperCase(Locale.ENGLISH));
                Map<String, Object> placementArguments = MiscUtils.castToMap(entry.getValue(), false);
                Optional<Vector3f> optionalLootSpawnOffset = Optional.ofNullable(placementArguments.get("loot-spawn-offset")).map(it -> MiscUtils.getAsVector3f(it, "loot-spawn-offset"));
                // furniture display elements
                List<FurnitureElement> elements = new ArrayList<>();
                List<Map<String, Object>> elementConfigs = (List<Map<String, Object>>) placementArguments.getOrDefault("elements", List.of());
                for (Map<String, Object> element : elementConfigs) {
                    FurnitureElement furnitureElement = furnitureElementBuilder()
                            .item(Key.of(ResourceConfigUtils.requireNonEmptyStringOrThrow(element.get("item"), "warning.config.furniture.element.missing_item")))
                            .applyDyedColor(ResourceConfigUtils.getAsBoolean(element.getOrDefault("apply-dyed-color", true), "apply-dyed-color"))
                            .billboard(ResourceConfigUtils.getOrDefault(element.get("billboard"), o -> Billboard.valueOf(o.toString().toUpperCase(Locale.ENGLISH)), Billboard.FIXED))
                            .transform(ResourceConfigUtils.getOrDefault(element.get("transform"), o -> ItemDisplayContext.valueOf(o.toString().toUpperCase(Locale.ENGLISH)), ItemDisplayContext.NONE))
                            .scale(MiscUtils.getAsVector3f(element.getOrDefault("scale", "1"), "scale"))
                            .position(MiscUtils.getAsVector3f(element.getOrDefault("position", "0"), "position"))
                            .translation(MiscUtils.getAsVector3f(element.getOrDefault("translation", "0"), "translation"))
                            .rotation(MiscUtils.getAsQuaternionf(element.getOrDefault("rotation", "0"), "rotation"))
                            .build();
                    elements.add(furnitureElement);
                }

                // external model providers
                Optional<ExternalModel> externalModel;
                if (placementArguments.containsKey("model-engine")) {
                    externalModel = Optional.of(plugin.compatibilityManager().createModel("ModelEngine", placementArguments.get("model-engine").toString()));
                } else if (placementArguments.containsKey("better-model")) {
                    externalModel = Optional.of(plugin.compatibilityManager().createModel("BetterModel", placementArguments.get("better-model").toString()));
                } else {
                    externalModel = Optional.empty();
                }

                // add hitboxes
                List<HitBox> hitboxes = ResourceConfigUtils.parseConfigAsList(placementArguments.get("hitboxes"), HitBoxTypes::fromMap);
                if (hitboxes.isEmpty() && externalModel.isEmpty()) {
                    hitboxes = List.of(defaultHitBox());
                }

                // rules
                Map<String, Object> ruleSection = MiscUtils.castToMap(placementArguments.get("rules"), true);
                if (ruleSection != null) {
                    placements.put(anchorType, new CustomFurniture.Placement(
                            anchorType,
                            elements.toArray(new FurnitureElement[0]),
                            hitboxes.toArray(new HitBox[0]),
                            ResourceConfigUtils.getOrDefault(ruleSection.get("rotation"), o -> RotationRule.valueOf(o.toString().toUpperCase(Locale.ENGLISH)), RotationRule.ANY),
                            ResourceConfigUtils.getOrDefault(ruleSection.get("alignment"), o -> AlignmentRule.valueOf(o.toString().toUpperCase(Locale.ENGLISH)), AlignmentRule.CENTER),
                            externalModel,
                            optionalLootSpawnOffset
                    ));
                } else {
                    placements.put(anchorType, new CustomFurniture.Placement(
                            anchorType,
                            elements.toArray(new FurnitureElement[0]),
                            hitboxes.toArray(new HitBox[0]),
                            RotationRule.ANY,
                            AlignmentRule.CENTER,
                            externalModel,
                            optionalLootSpawnOffset
                    ));
                }
            }

            CustomFurniture furniture = furnitureBuilder()
                    .id(id)
                    .settings(FurnitureSettings.fromMap(MiscUtils.castToMap(section.get("settings"), true)))
                    .placement(placements)
                    .events(EventFunctions.parseEvents(ResourceConfigUtils.get(section, "events", "event")))
                    .lootTable(LootTable.fromMap(MiscUtils.castToMap(section.get("loot"), true)))
                    .build();
            AbstractFurnitureManager.this.byId.put(id, furniture);
        }
    }
}
