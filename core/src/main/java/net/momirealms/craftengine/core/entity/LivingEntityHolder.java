package net.momirealms.craftengine.core.entity;

import net.momirealms.craftengine.core.attribute.Attribute;
import net.momirealms.craftengine.core.attribute.AttributeGetter;
import net.momirealms.craftengine.core.attribute.EntityAttributes;
import net.momirealms.craftengine.core.attribute.NotTrackedHolder;
import net.momirealms.craftengine.core.attribute.equipment.EntityEquipments;
import net.momirealms.craftengine.core.attribute.equipment.EquipmentSetSlot;
import net.momirealms.craftengine.core.attribute.equipment.PotionEffectController;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.entity.tick.EntityTickScheduler;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.plugin.scheduler.SchedulerTask;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class LivingEntityHolder {
    private static final int WAKE_ATTRIBUTES = 1;
    private static final int WAKE_POTION_EFFECTS = 1 << 1;

    public final LivingEntity entity;
    public final AttributeGetter attributes;
    public final EntityEquipments equipments;
    public final PotionEffectController potionEffects;
    public final LivingEntityContext context;
    @Nullable
    private final EntityTickScheduler tickScheduler;
    @Nullable
    private final EntityTickScheduler.Registration tickRegistration;
    @Nullable
    private SchedulerTask entityTickTask;
    private long localTick;
    private long attributeDeadline;
    private long potionEffectDeadline;
    private int wakeMask;
    private volatile boolean closed;

    public LivingEntityHolder(LivingEntity entity) {
        this(entity, null);
    }

    public LivingEntityHolder(LivingEntity entity, @Nullable EntityTickScheduler tickScheduler) {
        this.entity = entity;
        this.tickScheduler = tickScheduler;
        this.context = new LivingEntityContext(entity, ContextHolder.builder()
                .withParameter(DirectContextParameters.ENTITY, entity)
                .withOptionalParameter(DirectContextParameters.PLAYER, entity instanceof Player player ? player : null)
                .immutable(true)
                .build());
        List<Attribute> attributeList = Config.enableAttributeSystem() ? CraftEngine.instance().attributeManager().attributesByEntityType(entity.type()) : List.of();
        this.attributes = attributeList.isEmpty() ? new NotTrackedHolder(entity) : new EntityAttributes(this, attributeList);
        this.potionEffects = new PotionEffectController(this);
        this.equipments = new EntityEquipments(this);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            Item item = this.entity.getItemByEquipmentSlot(slot);
            if (!item.isEmpty()) {
                this.equipments.add(EquipmentSetSlot.fromEquipmentSlot(slot), item);
            }
        }
        this.equipments.updateSets(false);
        this.tickRegistration = tickScheduler == null ? null : tickScheduler.register(this::runDue);
        this.attributeDeadline = this.attributes instanceof EntityAttributes entityAttributes
                ? entityAttributes.nextRequiredTick()
                : EntityTickScheduler.NEVER;
        this.potionEffectDeadline = this.potionEffects.nextRequiredTick(currentTick());
        if (this.tickRegistration == null) {
            return;
        }
        if (this.wakeMask != 0) {
            this.tickRegistration.wake();
        } else {
            scheduleNextDeadline();
        }
    }

    public LivingEntity entity() {
        return this.entity;
    }

    public AttributeGetter attributes() {
        return this.attributes;
    }

    public void ifAttributesExist(Consumer<EntityAttributes> consumer) {
        if (this.attributes instanceof EntityAttributes attr) {
            consumer.accept(attr);
        }
    }

    public void applyEquipmentChanges(Map<EquipmentSetSlot, ? extends Item> changes) {
        applyEquipmentChangesAndReportSetChange(changes);
    }

    public boolean applyEquipmentChangesAndReportSetChange(Map<EquipmentSetSlot, ? extends Item> changes) {
        for (EquipmentSetSlot slot : changes.keySet()) {
            this.equipments.remove(slot);
        }
        for (Map.Entry<EquipmentSetSlot, ? extends Item> entry : changes.entrySet()) {
            Item item = entry.getValue();
            if (item != null && !item.isEmpty()) {
                this.equipments.add(entry.getKey(), item);
            }
        }
        return this.equipments.updateSetsAndReportChange();
    }

    public long currentTick() {
        return this.tickScheduler == null ? this.localTick : this.tickScheduler.currentTick();
    }

    public void tick$folia() {
        if (this.tickScheduler != null) {
            throw new IllegalStateException("A globally scheduled holder cannot be ticked directly");
        }
        runDue(++this.localTick);
    }

    public synchronized void attachFoliaEntityTickTask(SchedulerTask task) {
        if (this.tickScheduler != null) {
            throw new IllegalStateException("A globally scheduled holder cannot own an entity tick task");
        }
        if (this.closed) {
            task.cancel();
            return;
        }
        if (this.entityTickTask != null) {
            this.entityTickTask.cancel();
        }
        this.entityTickTask = task;
    }

    public void wakeAttributes() {
        if (this.closed) return;
        this.wakeMask |= WAKE_ATTRIBUTES;
        if (this.tickRegistration != null) {
            this.tickRegistration.wake();
        }
    }

    public void wakePotionEffects() {
        if (this.closed) return;
        this.wakeMask |= WAKE_POTION_EFFECTS;
        if (this.tickRegistration != null) {
            this.tickRegistration.wake();
        }
    }

    public void refreshPotionEffectSchedule() {
        if (this.closed) return;
        this.potionEffectDeadline = this.potionEffects.nextRequiredTick(currentTick());
        if (this.tickRegistration != null) {
            if (this.wakeMask != 0) {
                this.tickRegistration.wake();
            } else {
                scheduleNextDeadline();
            }
        }
    }

    private long runDue(long gameTick) {
        if (this.closed) return EntityTickScheduler.NEVER;
        int wake = this.wakeMask;
        this.wakeMask = 0;

        if ((wake & WAKE_ATTRIBUTES) != 0 || this.attributeDeadline <= gameTick) {
            if (this.attributes instanceof EntityAttributes entityAttributes) {
                this.attributeDeadline = entityAttributes.runDue(gameTick);
            } else {
                this.attributeDeadline = EntityTickScheduler.NEVER;
            }
        }
        if ((wake & WAKE_POTION_EFFECTS) != 0 || this.potionEffectDeadline <= gameTick) {
            this.potionEffectDeadline = this.potionEffects.runDue(gameTick);
        }
        return Math.min(this.attributeDeadline, this.potionEffectDeadline);
    }

    private void scheduleNextDeadline() {
        if (this.tickRegistration == null) return;
        long next = Math.min(this.attributeDeadline, this.potionEffectDeadline);
        if (next == EntityTickScheduler.NEVER) {
            this.tickRegistration.sleep();
        } else {
            this.tickRegistration.scheduleAt(next);
        }
    }

    public synchronized void close(boolean death) {
        if (this.closed) return;
        this.closed = true;
        if (this.entityTickTask != null) {
            this.entityTickTask.cancel();
            this.entityTickTask = null;
        }
        if (this.tickRegistration != null) {
            this.tickRegistration.close();
        }
        this.equipments.clearSetEffects();
        this.potionEffects.close(death);
        if (this.attributes instanceof EntityAttributes entityAttributes) {
            entityAttributes.clearSyncModifiers();
        }
    }

    public synchronized void retire() {
        if (this.closed) return;
        this.closed = true;
        this.entityTickTask = null;
        if (this.tickRegistration != null) {
            this.tickRegistration.close();
        }
    }
}
