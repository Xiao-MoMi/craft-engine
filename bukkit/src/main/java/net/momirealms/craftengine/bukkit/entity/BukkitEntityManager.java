package net.momirealms.craftengine.bukkit.entity;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.momirealms.craftengine.bukkit.entity.listener.BukkitEntityListener;
import net.momirealms.craftengine.bukkit.entity.listener.PaperEntityListener;
import net.momirealms.craftengine.bukkit.item.BukkitItem;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.listener.AbstractListener;
import net.momirealms.craftengine.bukkit.util.EquipmentSlotUtils;
import net.momirealms.craftengine.bukkit.util.ItemStackUtils;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.core.attribute.equipment.EquipmentSetSlot;
import net.momirealms.craftengine.core.entity.AbstractEntityManager;
import net.momirealms.craftengine.core.entity.LivingEntityHolder;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.scheduler.SchedulerTask;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityProxy;
import org.bukkit.Bukkit;
import org.bukkit.Tag;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class BukkitEntityManager extends AbstractEntityManager {
    private static BukkitEntityManager instance;
    private final BukkitCraftEngine plugin;
    private final BukkitEntityListener entityListener;
    private final PaperEntityListener paperEntityListener;
    private final AbstractListener paperEquipmentListener;

    public BukkitEntityManager(BukkitCraftEngine plugin) {
        super(plugin);
        this.plugin = plugin;
        this.entityListener = new BukkitEntityListener(this);
        this.paperEntityListener = VersionHelper.hasPaperPatch ? new PaperEntityListener(this) : null;
        this.paperEquipmentListener = createPaperEquipmentListener();
        instance = this;
    }

    private AbstractListener createPaperEquipmentListener() {
        if (!VersionHelper.hasPaperPatch || !VersionHelper.isOrAbove1_21_4) return null;
        try {
            Class<?> listenerClass = Class.forName("net.momirealms.craftengine.bukkit.entity.listener.PaperEquipmentListener");
            return (AbstractListener) listenerClass.getConstructor(BukkitEntityManager.class).newInstance(this);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to initialize the Paper equipment listener", e);
        }
    }

    public static BukkitEntityManager instance() {
        return instance;
    }

    public void handleEquipmentChanges(Object minecraftEntity, Map<?, ?> changes) {
        LivingEntityHolder holder = getEntityHolder(EntityProxy.INSTANCE.getUUID(minecraftEntity));
        if (holder == null) return;
        Map<EquipmentSetSlot, BukkitItem> replacements = new Object2ObjectArrayMap<>();
        for (Map.Entry<?, ?> entry : changes.entrySet()) {
            replacements.put(
                    EquipmentSetSlot.fromEquipmentSlot(EquipmentSlotUtils.fromNMSEquipmentSlot(entry.getKey())),
                    ItemStackUtils.wrap(entry.getValue())
            );
        }
        holder.applyEquipmentChanges(replacements);
    }

    @Override
    protected void onLivingEntityTracked(LivingEntityHolder holder) {
        if (!VersionHelper.hasFoliaPatch) return;
        UUID uuid = holder.entity.uuid();
        SchedulerTask task = this.plugin.scheduler().platform().runRepeating(
                holder::tick$folia,
                () -> retireLivingEntity(uuid, holder),
                1,
                1,
                holder.entity
        );
        holder.attachFoliaEntityTickTask(task);
        if (task.cancelled()) {
            retireLivingEntity(uuid, holder);
        }
    }

    @Override
    public void runDelayedSyncTasks() {
        boolean active = Config.enableEntityTracking();
        this.entityListener.setActive(active);
        if (this.paperEntityListener != null) {
            this.paperEntityListener.setActive(active);
        }
        if (this.paperEquipmentListener != null) {
            this.paperEquipmentListener.setActive(active);
        }
    }

    @Override
    public void disable() {
        this.entityListener.setActive(false);
        if (this.paperEntityListener != null) {
            this.paperEntityListener.setActive(false);
        }
        if (this.paperEquipmentListener != null) {
            this.paperEquipmentListener.setActive(false);
        }
        super.disable();
    }

    @Override
    public List<Key> vanillaEntityIdsByTag(Key tag) {
        Tag<EntityType> bukkitTag = Bukkit.getTag(Tag.REGISTRY_ENTITY_TYPES, KeyUtils.toNamespacedKey(tag), EntityType.class);
        if (bukkitTag == null) return List.of();
        List<Key> result = new ArrayList<>();
        for (EntityType type : bukkitTag.getValues()) {
            result.add(KeyUtils.namespacedKeyToKey(type.getKey()));
        }
        return result;
    }
}
