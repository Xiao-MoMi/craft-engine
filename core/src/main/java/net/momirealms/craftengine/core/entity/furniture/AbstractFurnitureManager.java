package net.momirealms.craftengine.core.entity.furniture;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.momirealms.craftengine.core.entity.furniture.behavior.FurnitureBehaviors;
import net.momirealms.craftengine.core.entity.furniture.element.FurnitureElementConfig;
import net.momirealms.craftengine.core.entity.furniture.element.FurnitureElementConfigs;
import net.momirealms.craftengine.core.entity.furniture.hitbox.FurnitureHitBoxConfig;
import net.momirealms.craftengine.core.entity.furniture.hitbox.FurnitureHitBoxes;
import net.momirealms.craftengine.core.entity.furniture.tick.TickingFurniture;
import net.momirealms.craftengine.core.loot.LootTable;
import net.momirealms.craftengine.core.pack.LoadingSequence;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.pack.PendingConfigSection;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.config.IdSectionConfigParser;
import net.momirealms.craftengine.core.plugin.context.CommonFunctions;
import net.momirealms.craftengine.core.entity.culling.CullingData;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.plugin.scheduler.SchedulerTask;
import net.momirealms.craftengine.core.util.*;
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

    protected final Int2ObjectOpenHashMap<TickingFurniture> syncTickers = new Int2ObjectOpenHashMap<>(256, 0.5f);
    protected final Int2ObjectOpenHashMap<TickingFurniture> asyncTickers = new Int2ObjectOpenHashMap<>(256, 0.5f);
    protected final TickersList<TickingFurniture> syncTickingFurniture = new TickersList<>();
    protected final List<TickingFurniture> pendingSyncTickingFurniture = new ArrayList<>();
    protected final TickersList<TickingFurniture> asyncTickingFurniture = new TickersList<>();
    protected final List<TickingFurniture> pendingAsyncTickingFurniture = new ArrayList<>();
    private boolean isTickingSyncFurniture = false;
    private boolean isTickingAsyncFurniture = false;

    protected SchedulerTask syncTickTask;
    protected SchedulerTask asyncTickTask;

    public AbstractFurnitureManager(CraftEngine plugin) {
        this.plugin = plugin;
        this.furnitureParser = new FurnitureParser();
    }

    @Override
    public FurnitureParser parser() {
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
    public Map<Key, CustomFurniture> loadedFurniture() {
        return Collections.unmodifiableMap(this.byId);
    }

    private void syncTick() {
        this.isTickingSyncFurniture = true;
        if (!this.pendingSyncTickingFurniture.isEmpty()) {
            this.syncTickingFurniture.addAll(this.pendingSyncTickingFurniture);
            this.pendingSyncTickingFurniture.clear();
        }
        if (!this.syncTickingFurniture.isEmpty()) {
            Object[] entities = this.syncTickingFurniture.elements();
            for (int i = 0, size = this.syncTickingFurniture.size(); i < size; i++) {
                TickingFurniture entity = (TickingFurniture) entities[i];
                if (entity.isValid()) {
                    entity.tick();
                } else {
                    this.syncTickingFurniture.markAsRemoved(i);
                    this.syncTickers.remove(entity.entityId());
                }
            }
            this.syncTickingFurniture.removeMarkedEntries();
        }
        this.isTickingSyncFurniture = false;
    }

    private void asyncTick() {
        this.isTickingAsyncFurniture = true;
        if (!this.pendingAsyncTickingFurniture.isEmpty()) {
            this.asyncTickingFurniture.addAll(this.pendingAsyncTickingFurniture);
            this.pendingAsyncTickingFurniture.clear();
        }
        if (!this.asyncTickingFurniture.isEmpty()) {
            Object[] entities = this.asyncTickingFurniture.elements();
            for (int i = 0, size = this.asyncTickingFurniture.size(); i < size; i++) {
                TickingFurniture entity = (TickingFurniture) entities[i];
                if (entity.isValid()) {
                    entity.tick();
                } else {
                    this.asyncTickingFurniture.markAsRemoved(i);
                    this.asyncTickers.remove(entity.entityId());
                }
            }
            this.asyncTickingFurniture.removeMarkedEntries();
        }
        this.isTickingAsyncFurniture = false;
    }

    public synchronized void addSyncFurnitureTicker(TickingFurniture ticker) {
        if (this.isTickingSyncFurniture) {
            this.pendingSyncTickingFurniture.add(ticker);
        } else {
            this.syncTickingFurniture.add(ticker);
        }
    }

    public synchronized void addAsyncFurnitureTicker(TickingFurniture ticker) {
        if (this.isTickingAsyncFurniture) {
            this.pendingAsyncTickingFurniture.add(ticker);
        } else {
            this.asyncTickingFurniture.add(ticker);
        }
    }

    @Override
    public void delayedInit() {
        if (this.syncTickTask == null || this.syncTickTask.cancelled())
            this.syncTickTask = CraftEngine.instance().scheduler().sync().runRepeating(this::syncTick, 1, 1);
        if (this.asyncTickTask == null || this.asyncTickTask.cancelled())
            this.asyncTickTask = CraftEngine.instance().scheduler().sync().runAsyncRepeating(this::asyncTick, 1, 1);
    }

    @Override
    public void disable() {
        if (this.syncTickTask != null && !this.syncTickTask.cancelled())
            this.syncTickTask.cancel();
        if (this.asyncTickTask != null && !this.asyncTickTask.cancelled())
            this.asyncTickTask.cancel();
    }

    @Override
    public void unload() {
        this.byId.clear();
    }

    protected abstract FurnitureHitBoxConfig<?> defaultHitBox();

    public class FurnitureParser extends IdSectionConfigParser {
        public static final String[] CONFIG_SECTION_NAME = new String[] { "furniture" };
        private final List<PendingConfigSection> pendingConfigSections = new ArrayList<>();

        public void addPendingConfigSection(PendingConfigSection section) {
            this.pendingConfigSections.add(section);
        }

        @Override
        public void preProcess() {
            for (PendingConfigSection section : this.pendingConfigSections) {
                ResourceConfigUtils.runCatching(
                        section.path(),
                        section.node(),
                        () -> parseSection(section.pack(), section.path(), section.node(), section.id(), section.config()),
                        () -> GsonHelper.get().toJson(section.config())
                );
            }
            this.pendingConfigSections.clear();
        }

        @Override
        public String[] sectionId() {
            return CONFIG_SECTION_NAME;
        }

        @Override
        public int loadingSequence() {
            return LoadingSequence.FURNITURE;
        }

        @Override
        public int count() {
            return AbstractFurnitureManager.this.byId.size();
        }

        @Override
        public void parseSection(Pack pack, Path path, String node, Key id, Map<String, Object> section) {
            if (AbstractFurnitureManager.this.byId.containsKey(id)) {
                throw new LocalizedResourceConfigException("warning.config.furniture.duplicate");
            }

            Map<String, Object> variantsMap = ResourceConfigUtils.getAsMap(ResourceConfigUtils.requireNonNullOrThrow(ResourceConfigUtils.get(section, "variants", "placement", "variant"), "warning.config.furniture.missing_variants"), "variants");
            if (variantsMap.isEmpty()) {
                throw new LocalizedResourceConfigException("warning.config.furniture.missing_variants");
            }

            Map<String, FurnitureVariant> variants = new LinkedHashMap<>();
            for (Map.Entry<String, Object> e0 : variantsMap.entrySet()) {
                String variantName = e0.getKey();
                Map<String, Object> variantArguments = ResourceConfigUtils.getAsMap(e0.getValue(), variantName);
                Optional<Vector3f> optionalLootSpawnOffset = Optional.ofNullable(variantArguments.get("loot-spawn-offset")).map(it -> ResourceConfigUtils.getAsVector3f(it, "loot-spawn-offset"));
                List<FurnitureElementConfig<?>> elements = ResourceConfigUtils.parseConfigAsList(variantArguments.get("elements"), FurnitureElementConfigs::fromMap);

                // 外部模型
                Optional<ExternalModel> externalModel;
                if (variantArguments.containsKey("model-engine")) {
                    externalModel = Optional.of(plugin.compatibilityManager().createModel("ModelEngine", variantArguments.get("model-engine").toString()));
                } else if (variantArguments.containsKey("better-model")) {
                    externalModel = Optional.of(plugin.compatibilityManager().createModel("BetterModel", variantArguments.get("better-model").toString()));
                } else {
                    externalModel = Optional.empty();
                }

                // 碰撞箱配置
                List<FurnitureHitBoxConfig<?>> hitboxes = ResourceConfigUtils.parseConfigAsList(variantArguments.get("hitboxes"), FurnitureHitBoxes::fromMap);
                if (hitboxes.isEmpty() && externalModel.isEmpty()) {
                    hitboxes = List.of(defaultHitBox());
                }

                variants.put(variantName, new FurnitureVariant(
                    variantName,
                    parseCullingData(section.get("entity-culling")),
                    elements.toArray(new FurnitureElementConfig[0]),
                    hitboxes.toArray(new FurnitureHitBoxConfig[0]),
                    externalModel,
                    optionalLootSpawnOffset.orElse(new Vector3f(0f, 0f,0f))
                ));
            }

            CustomFurniture furniture = CustomFurniture.builder()
                    .id(id)
                    .settings(FurnitureSettings.fromMap(MiscUtils.castToMap(section.get("settings"), true)))
                    .variants(variants)
                    .events(CommonFunctions.parseEvents(ResourceConfigUtils.get(section, "events", "event")))
                    .lootTable(LootTable.fromMap(MiscUtils.castToMap(section.get("loot"), true)))
                    .build();
            ((CustomFurnitureImpl) furniture).setBehavior(FurnitureBehaviors.fromMap(furniture, ResourceConfigUtils.getAsMapOrNull(ResourceConfigUtils.get(section, "behaviors", "behavior"), "behavior")));

            AbstractFurnitureManager.this.byId.put(id, furniture);
        }

        private CullingData parseCullingData(Object arguments) {
            if (arguments instanceof Boolean b && !b)
                return null;
            if (!(arguments instanceof Map))
                return new CullingData(null, Config.entityCullingViewDistance(), 0.25, true);
            Map<String, Object> argumentsMap = ResourceConfigUtils.getAsMap(arguments, "entity-culling");
            return new CullingData(
                    ResourceConfigUtils.getOrDefault(argumentsMap.get("aabb"), it -> ResourceConfigUtils.getAsAABB(it, "aabb"), null),
                    ResourceConfigUtils.getAsInt(argumentsMap.getOrDefault("view-distance", Config.entityCullingViewDistance()), "view-distance"),
                    ResourceConfigUtils.getAsDouble(argumentsMap.getOrDefault("aabb-expansion", 0.25), "aabb-expansion"),
                    ResourceConfigUtils.getAsBoolean(argumentsMap.getOrDefault("ray-tracing", true), "ray-tracing")
            );
        }
    }
}
