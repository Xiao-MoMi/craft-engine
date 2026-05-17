package net.momirealms.craftengine.core.entity.furniture;

import net.momirealms.craftengine.core.entity.culling.CullingData;
import net.momirealms.craftengine.core.entity.furniture.behavior.FurnitureBehaviorTemplate;
import net.momirealms.craftengine.core.entity.furniture.behavior.FurnitureBehaviors;
import net.momirealms.craftengine.core.entity.furniture.element.FurnitureElement;
import net.momirealms.craftengine.core.entity.furniture.element.FurnitureElementConfig;
import net.momirealms.craftengine.core.entity.furniture.element.FurnitureElementConfigs;
import net.momirealms.craftengine.core.entity.furniture.hitbox.FurnitureHitBox;
import net.momirealms.craftengine.core.entity.furniture.hitbox.FurnitureHitBoxConfig;
import net.momirealms.craftengine.core.entity.furniture.hitbox.FurnitureHitBoxConfigs;
import net.momirealms.craftengine.core.entity.furniture.setting.FurnitureSettings;
import net.momirealms.craftengine.core.entity.furniture.tick.TickingFurniture;
import net.momirealms.craftengine.core.loot.Loot;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.*;
import net.momirealms.craftengine.core.plugin.config.lifecycle.LoadingStage;
import net.momirealms.craftengine.core.plugin.config.lifecycle.LoadingStages;
import net.momirealms.craftengine.core.plugin.context.CommonFunctions;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.EventTrigger;
import net.momirealms.craftengine.core.plugin.context.function.Function;
import net.momirealms.craftengine.core.plugin.scheduler.SchedulerTask;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.TickersList;
import net.momirealms.craftengine.core.util.VersionHelper;
import org.incendo.cloud.suggestion.Suggestion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

public abstract class AbstractFurnitureManager implements FurnitureManager {
    protected final Map<Key, FurnitureDefinition> byId = new ConcurrentHashMap<>();
    protected final CraftEngine plugin;
    protected final IdSectionConfigParser furnitureParser;
    // Cached command suggestions
    protected final List<Suggestion> cachedSuggestions = new ArrayList<>();

    protected final Map<Integer, TickingFurniture> syncTickers = new ConcurrentHashMap<>(256, 0.5f);
    protected final Map<Integer, TickingFurniture> asyncTickers = new ConcurrentHashMap<>(256, 0.5f);
    protected final TickersList<TickingFurniture> syncTickingFurniture = new TickersList<>();
    protected final List<TickingFurniture> pendingSyncTickingFurniture = new ArrayList<>();
    protected final TickersList<TickingFurniture> asyncTickingFurniture = new TickersList<>();
    protected final Queue<TickingFurniture> pendingAsyncTickingFurniture = new ConcurrentLinkedQueue<>();
    private boolean isTickingSyncFurniture = false;

    protected SchedulerTask syncTickTask;
    protected SchedulerTask asyncTickTask;

    public AbstractFurnitureManager(CraftEngine plugin) {
        this.plugin = plugin;
        this.furnitureParser = new FurnitureParser();
    }

    @Override
    public IdSectionConfigParser parser() {
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
    public Optional<FurnitureDefinition> furnitureById(Key id) {
        return Optional.ofNullable(this.byId.get(id));
    }

    @Override
    public Map<Key, FurnitureDefinition> loadedFurniture() {
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
        TickingFurniture pending;
        while ((pending = this.pendingAsyncTickingFurniture.poll()) != null) {
            this.asyncTickingFurniture.add(pending);
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
    }

    public void addSyncFurnitureTicker(TickingFurniture ticker) {
        if (this.isTickingSyncFurniture) {
            this.pendingSyncTickingFurniture.add(ticker);
        } else {
            this.syncTickingFurniture.add(ticker);
        }
    }

    // 此方法可能会被多个区域线程同时调用
    public void addAsyncFurnitureTicker(TickingFurniture ticker) {
        this.pendingAsyncTickingFurniture.add(ticker);
    }

    @Override
    public void delayedInit() {
        if (!VersionHelper.isFolia) {
            if (this.syncTickTask == null || this.syncTickTask.cancelled())
                this.syncTickTask = CraftEngine.instance().scheduler().platform().runRepeating(this::syncTick, 1, 1);
        }
        if (this.asyncTickTask == null || this.asyncTickTask.cancelled())
            this.asyncTickTask = CraftEngine.instance().scheduler().platform().runRepeating(() -> {
                CraftEngine.instance().scheduler().async().execute(this::asyncTick);
            }, 1, 1);
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

    private final class FurnitureParser extends IdSectionConfigParser {
        public static final String[] CONFIG_SECTION_NAME = new String[] { "furniture" };

        @Override
        public String[] sectionId() {
            return CONFIG_SECTION_NAME;
        }

        @Override
        public int count() {
            return AbstractFurnitureManager.this.byId.size();
        }

        @Override
        public LoadingStage loadingStage() {
            return LoadingStages.FURNITURE;
        }

        @Override
        public boolean async() {
            return Config.multiThreadedConfigLoad();
        }

        @Override
        public List<LoadingStage> dependencies() {
            return List.of(LoadingStages.ITEM);
        }

        private static final String[] VARIANT = new String[] {"variant", "variants", "placement"};
        private static final String[] LOOT_SPAWN_OFFSET = new String[] {"loot_spawn_offset", "loot-spawn-offset"};
        private static final String[] BLUEPRINT = new String[] {"blueprint", "better-model", "model-engine"};
        private static final String[] ENTITY_CULLING = new String[] {"entity_culling", "entity-culling"};
        private static final String[] EVENT = new String[] {"events", "event"};
        private static final String[] LOOT = new String[] {"loots", "loot"};
        private static final String[] BEHAVIORS = new String[] {"behaviors", "behavior"};
        private static final String[] VIEW_DISTANCE = new String[] {"view_distance", "view-distance"};
        private static final String[] AABB_EXPANSION = new String[] {"aabb_expansion", "aabb-expansion"};
        private static final String[] RAY_TRACING = new String[] {"ray_tracing", "ray-tracing"};

        @Override
        public void parseSection(@NotNull Pack pack, @NotNull Path path, @NotNull Key id, @NotNull ConfigSection section) {
            // 获取家具设置 （可异常）
            FurnitureSettings settings = FurnitureSettings.of().itemId(id);
            try {
                FurnitureSettings.applyModifiers(settings, section.getSection("settings"));
            } catch (KnownResourceException e) {
                error(e, path);
            }

            // 读取变体配置
            ConfigSection variantsSection = section.getNonNullSection(VARIANT);
            Map<String, FurnitureVariant> variants = new LinkedHashMap<>();

            for (String variant : variantsSection.keySet()) {
                ConfigSection variantSection = variantsSection.getNonNullSection(variant);

                // 掉落物偏移
                Vector3f lootSpawnOffset = variantSection.getVector3f(LOOT_SPAWN_OFFSET, ConfigConstants.ZERO_VECTOR3);

                // 外部模型
                String blueprint = variantSection.getString(BLUEPRINT);
                Supplier<ExternalModel> externalModel = Optional.ofNullable(blueprint)
                        .map(it -> (Supplier<ExternalModel>) () -> AbstractFurnitureManager.this.plugin.compatibilityManager().createModel(it))
                        .orElse(null);

                // 元素与碰撞箱
                List<FurnitureElementConfig<? extends FurnitureElement>> elements = variantSection.getList("elements", v -> FurnitureElementConfigs.fromConfig(v.getAsSection()));
                ConfigValue hitboxValue = variantSection.getValue("hitboxes");
                List<FurnitureHitBoxConfig<? extends FurnitureHitBox>> hitboxes;
                if (hitboxValue != null) {
                    hitboxes = variantSection.getList("hitboxes", v -> FurnitureHitBoxConfigs.fromConfig(v.getAsSection()));
                } else {
                    hitboxes = List.of(defaultHitBox());
                }

                variants.put(variant, new FurnitureVariant(
                        variant,
                        parseCullingData(section.getValue(ENTITY_CULLING)),
                        elements,
                        hitboxes,
                        externalModel,
                        lootSpawnOffset
                ));
            }

            // 解析事件 （可异常）
            Map<EventTrigger, List<Function<Context>>> events = new EnumMap<>(EventTrigger.class);
            try {
                CommonFunctions.parseEvents(section.getValue(EVENT), (t, f) -> events.computeIfAbsent(t, k -> new ArrayList<>()).add(f));
            } catch (KnownResourceException e) {
                error(e, path);
            }

            // 解析战利品表 （可异常）
            Loot loot = null;
            try {
                loot = section.getValue(LOOT, ConfigValue::getAsLoot);
            } catch (KnownResourceException e) {
                error(e, path);
            }

            FurnitureDefinition furniture = FurnitureDefinition.builder()
                    .id(id)
                    .settings(settings)
                    .variants(variants)
                    .events(events)
                    .loot(loot)
                    .build();

            // 家具行为
            ConfigValue value = section.getValue(BEHAVIORS);
            if (value != null) {
                List<FurnitureBehaviorTemplate> furnitureBehaviorTemplates = value.getAsList(v -> FurnitureBehaviors.fromConfig(furniture, v.getAsSection()));
                ((FurnitureDefinitionImpl) furniture).setBehaviors(furnitureBehaviorTemplates);
            }
            AbstractFurnitureManager.this.byId.put(id, furniture);
        }

        private CullingData parseCullingData(@Nullable ConfigValue value) {
            if (value != null) {
                if (value.is(Boolean.class) && !value.getAsBoolean()) {
                    return null;
                } else if (value.is(Map.class)) {
                    ConfigSection section = value.getAsSection();
                    return new CullingData(
                            section.getAABB("aabb"),
                            section.getInt(VIEW_DISTANCE, Config.entityCullingViewDistance()),
                            section.getDouble(AABB_EXPANSION, 0.25),
                            section.getBoolean(RAY_TRACING, true)
                    );
                }
            }
            return new CullingData(null, Config.entityCullingViewDistance(), 0.25, true);
        }
    }
}
