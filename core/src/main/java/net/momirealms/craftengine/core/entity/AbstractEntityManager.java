package net.momirealms.craftengine.core.entity;

import net.momirealms.craftengine.core.entity.setting.EntitySettings;
import net.momirealms.craftengine.core.entity.tick.EntityTickScheduler;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.compatibility.EntityProvider;
import net.momirealms.craftengine.core.plugin.config.*;
import net.momirealms.craftengine.core.plugin.config.lifecycle.LoadingStage;
import net.momirealms.craftengine.core.plugin.config.lifecycle.LoadingStages;
import net.momirealms.craftengine.core.util.ConcurrentChainedUUID2ReferenceHashTable;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.StringUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractEntityManager implements EntityManager {
    protected final CraftEngine plugin;
    protected final Map<Key, List<Key>> customEntityTags = new HashMap<>();
    protected final Map<Key, EntityDefinition> entityDefinitions = new ConcurrentHashMap<>();
    protected final ConcurrentChainedUUID2ReferenceHashTable<LivingEntityHolder> livingEntities = ConcurrentChainedUUID2ReferenceHashTable.createWithCapacity(512);
    protected final EntityTickScheduler entityTickScheduler = new EntityTickScheduler();
    private final EntityParser entityParser = new EntityParser();
    private volatile EntityProvider[] entityProviders = new EntityProvider[0];

    protected AbstractEntityManager(CraftEngine plugin) {
        this.plugin = plugin;
    }

    @Override
    public void unload() {
        this.customEntityTags.clear();
        this.entityDefinitions.clear();
    }

    @Override
    public List<Key> customEntityIdsByTag(Key tag) {
        return Collections.unmodifiableList(this.customEntityTags.getOrDefault(tag, List.of()));
    }

    @Override
    @Nullable
    public EntityDefinition entityDefinition(Key entityType) {
        return this.entityDefinitions.get(entityType);
    }

    public void registerCustomEntityTag(Key tag, Key entityId) {
        this.customEntityTags.computeIfAbsent(tag, k -> new ArrayList<>()).add(entityId);
    }

    @Override
    public ConfigParser[] parsers() {
        return new ConfigParser[]{this.entityParser};
    }

    @Override
    public void delayedLoad() {
        resetEntityProviders();
        for (LivingEntityHolder holder : this.livingEntities.values()) {
            holder.equipments.invalidateSetMemberships();
        }
    }

    @Override
    public void resetEntityProviders() {
        List<EntityProvider> providers = new ArrayList<>();
        for (String source : Config.entityIdSources()) {
            Optional.ofNullable(this.plugin.compatibilityManager().getEntityProvider(source)).ifPresent(providers::add);
        }
        this.entityProviders = providers.toArray(new EntityProvider[0]);
    }

    @Override
    public Key getEntityId(Entity entity) {
        for (EntityProvider provider : this.entityProviders) {
            String entityId = provider.getEntityId(entity);
            if (entityId != null) {
                return Key.of(provider.plugin(), StringUtils.normalizeString(entityId));
            }
        }
        return entity.type();
    }

    @Nullable
    public LivingEntityHolder trackLivingEntity(LivingEntity entity) {
        UUID uuid = entity.uuid();
        LivingEntityHolder previous = this.livingEntities.remove(uuid);
        if (previous != null) {
            previous.close(false);
        }
        LivingEntityHolder holder = createLivingEntityHolder(entity);
        LivingEntityHolder displaced = this.livingEntities.put(uuid, holder);
        if (displaced != null && displaced != holder) {
            displaced.close(false);
        }
        try {
            onLivingEntityTracked(holder);
        } catch (Throwable t) {
            if (this.livingEntities.remove(uuid, holder) == holder) {
                holder.close(false);
            }
            throw t;
        }
        return holder;
    }

    protected LivingEntityHolder createLivingEntityHolder(LivingEntity entity) {
        return VersionHelper.hasFoliaPatch ? new LivingEntityHolder(entity) : new LivingEntityHolder(entity, this.entityTickScheduler);
    }

    protected void onLivingEntityTracked(LivingEntityHolder holder) {
    }

    public void untrackLivingEntity(UUID uuid, boolean death) {
        LivingEntityHolder removed = this.livingEntities.remove(uuid);
        if (removed != null) {
            removed.close(death);
        }
    }

    protected void retireLivingEntity(UUID uuid, LivingEntityHolder expected) {
        if (this.livingEntities.remove(uuid, expected) == expected) {
            expected.retire();
        }
    }

    public void tickLivingEntities() {
        this.entityTickScheduler.advance();
    }

    public void clearTrackedLivingEntities() {
        for (LivingEntityHolder holder : this.livingEntities.values()) {
            holder.close(false);
        }
        this.livingEntities.clear();
    }

    @Override
    public void disable() {
        clearTrackedLivingEntities();
        unload();
    }

    @Override
    public LivingEntityHolder getEntityHolder(UUID uuid) {
        return this.livingEntities.get(uuid);
    }

    private final class EntityParser extends IdSectionConfigParser {
        public static final String[] CONFIG_SECTION_NAME = ConfigKeys.of("entit(y|ies)");

        @Override
        public Key type() {
            return Key.ce("entity");
        }

        @Override
        public String[] sectionId() {
            return CONFIG_SECTION_NAME;
        }

        @Override
        public LoadingStage loadingStage() {
            return LoadingStages.ENTITY;
        }

        @Override
        public int count() {
            return AbstractEntityManager.this.entityDefinitions.size();
        }

        @Override
        public boolean async() {
            return Config.multiThreadedConfigLoad();
        }

        @Override
        protected void parseSection(@NotNull Pack pack, @NotNull Path path, @NotNull Key id, @NotNull ConfigSection section) {
            AbstractEntityManager.this.entityDefinitions.put(id, new EntityDefinition(id, EntitySettings.fromConfig(section.getSection("settings"))));
        }

        @Override
        public void postProcess() {
            AbstractEntityManager.this.customEntityTags.clear();
            for (EntityDefinition definition : AbstractEntityManager.this.entityDefinitions.values()) {
                for (Key tag : definition.settings().tags()) {
                    AbstractEntityManager.this.registerCustomEntityTag(tag, definition.id());
                }
            }
        }
    }
}
