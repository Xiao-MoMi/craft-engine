package net.momirealms.craftengine.core.attribute;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.momirealms.craftengine.core.attribute.base.BaseValueSource;
import net.momirealms.craftengine.core.attribute.modifier.AttributeModifier;
import net.momirealms.craftengine.core.attribute.sync.SyncTarget;
import net.momirealms.craftengine.core.attribute.vanilla.VanillaAttributeInstance;
import net.momirealms.craftengine.core.attribute.vanilla.VanillaAttributes;
import net.momirealms.craftengine.core.attribute.vanilla.LegacyVanillaAttributes;
import net.momirealms.craftengine.core.entity.LivingEntity;
import net.momirealms.craftengine.core.entity.LivingEntityContext;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.SwapList;
import net.momirealms.craftengine.core.util.VersionHelper;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AttributeInstance {
    private static final BoundSyncTarget[] EMPTY_SYNC_TARGETS = new BoundSyncTarget[0];

    private final Attribute attribute;
    private final EntityAttributes owner;
    private final int index;
    // Dense index in EntityAttributes.syncInstances; -1 means no supported vanilla target.
    private int syncIndex = -1;
    private final BaseValueSource baseValueSource;
    private final int baseUpdateInterval;
    private final BoundSyncTarget[] syncTargets;
    private final Map<Key, Map<Key, AttributeModifier>> byOperation = new HashMap<>();
    private final Map<Key, AttributeModifier> byId = new Object2ObjectArrayMap<>();
    private final LivingEntityContext context;
    @Nullable
    private Map<Key, TrackedModifier> trackedById;
    @Nullable
    private SwapList<TrackedModifier> trackedList;
    private long nextTrackedTick = Long.MAX_VALUE;
    private long nextBaseTick;
    private double cachedValue;
    private boolean dirty = true;
    private double lastBase;

    public AttributeInstance(Attribute attribute, LivingEntityContext context, EntityAttributes owner, int index) {
        this.attribute = attribute;
        this.context = context;
        this.owner = owner;
        this.index = index;
        this.baseValueSource = attribute.baseValueSource().bind(context.entity);
        this.baseUpdateInterval = this.baseValueSource.updateInterval();
        this.lastBase = this.baseValueSource.resolve(context.entity);
        this.nextBaseTick = this.baseUpdateInterval > 0 ? Long.MIN_VALUE : Long.MAX_VALUE;
        this.syncTargets = bindSyncTargets(attribute.syncTargets(), context.entity);
    }

    public Attribute attribute() {
        return this.attribute;
    }

    public int index() {
        return this.index;
    }

    int syncIndex() {
        return this.syncIndex;
    }

    void syncIndex(int syncIndex) {
        this.syncIndex = syncIndex;
    }

    public double getValue() {
        if (this.dirty) {
            this.cachedValue = this.recalculate();
            this.dirty = false;
        }
        return this.cachedValue;
    }

    public boolean hasModifier(Key id) {
        return this.byId.containsKey(id);
    }

    public AttributeModifier getModifier(Key id) {
        return this.byId.get(id);
    }

    public void removeModifier(Key id) {
        AttributeModifier removed = this.byId.remove(id);
        boolean changed = removed != null;
        if (removed != null) {
            Map<Key, AttributeModifier> operations = this.getModifiersByOperation(removed.operation());
            if (operations != null) {
                operations.remove(id);
            }
        }
        if (this.trackedById != null) {
            TrackedModifier removedTracked = this.trackedById.remove(id);
            if (removedTracked != null) {
                changed = true;
                this.trackedList.swapRemove(removedTracked);
                if (this.trackedById.isEmpty()) {
                    this.nextTrackedTick = Long.MAX_VALUE;
                }
            }
        }
        if (changed) {
            this.setDirty();
            this.owner.onScheduleChanged(this);
        }
    }

    public void removeModifier(AttributeModifier modifier) {
        this.removeModifier(modifier.id());
    }

    public void addModifier(AttributeModifier modifier) {
        AttributeModifier previous = this.byId.putIfAbsent(modifier.id(), modifier);
        if (previous != null) {
            throw new IllegalArgumentException("Modifier is already applied on this attribute!");
        } else {
            this.getModifiersByOperation(modifier.operation()).put(modifier.id(), modifier);
            this.trackIfNeeded(modifier);
            this.setDirty();
        }
    }

    public void addOrUpdateModifier(AttributeModifier modifier) {
        AttributeModifier oldModifier = this.byId.put(modifier.id(), modifier);
        if (modifier != oldModifier) {
            if (oldModifier != null && !oldModifier.operation().equals(modifier.operation())) {
                Map<Key, AttributeModifier> oldOperations = this.byOperation.get(oldModifier.operation());
                if (oldOperations != null) {
                    oldOperations.remove(modifier.id());
                }
            }
            this.getModifiersByOperation(modifier.operation()).put(modifier.id(), modifier);
            this.trackIfNeeded(modifier);
            this.setDirty();
        }
    }

    private void trackIfNeeded(AttributeModifier modifier) {
        if (modifier.isDynamic() && modifier.updateInterval() > 0) {
            if (this.trackedById == null) {
                this.trackedById = new Object2ObjectOpenHashMap<>();
                this.trackedList = new SwapList<>();
            }
            TrackedModifier tracked = new TrackedModifier(modifier, modifier.amount(this.context), modifier.test(this.context));
            TrackedModifier previous = this.trackedById.put(modifier.id(), tracked);
            if (previous != null) {
                // 原位替换列表中的旧快照
                this.trackedList.set(previous.index(), tracked);
            } else {
                this.trackedList.add(tracked);
            }
            // 新条目待初始化，下一 tick 强制扫描一次
            this.nextTrackedTick = Long.MIN_VALUE;
            this.owner.onScheduleChanged(this);
        } else if (this.trackedById != null) {
            TrackedModifier removed = this.trackedById.remove(modifier.id());
            if (removed != null) {
                this.trackedList.swapRemove(removed);
                // 不再有活跃的条目，无需要更新
                if (this.trackedById.isEmpty()) {
                    this.nextTrackedTick = Long.MAX_VALUE;
                }
                this.owner.onScheduleChanged(this);
            }
        }
    }

    public void updateTrackedModifiers(long tick) {
        if (tick < this.nextTrackedTick) return;
        SwapList<TrackedModifier> tracked = this.trackedList;
        if (tracked == null) {
            this.nextTrackedTick = Long.MAX_VALUE;
            return;
        }
        Object[] array = tracked.elements();
        int size = tracked.size();
        if (size == 0) {
            this.nextTrackedTick = Long.MAX_VALUE;
            return;
        }
        long nextDue = Long.MAX_VALUE;
        for (int i = 0; i < size; i++) {
            TrackedModifier state = (TrackedModifier) array[i];
            if (state == null || state.index() == -1) continue;
            AttributeModifier modifier = state.modifier;
            int interval = modifier.updateInterval();
            if (state.nextTick == -1) {
                state.nextTick = tick + interval;
            }
            if (tick < state.nextTick) {
                nextDue = Math.min(nextDue, state.nextTick);
                continue;
            }
            state.nextTick = tick + interval;
            nextDue = Math.min(nextDue, state.nextTick);
            boolean condition = modifier.test(this.context);
            // 条件不满足时快照值不参与 recalculate，跳过 amount 求值
            double amount = condition ? modifier.amount(this.context) : state.amount;
            if (condition != state.condition || amount != state.amount) {
                state.amount = amount;
                state.condition = condition;
                this.setDirty();
            }
        }
        this.nextTrackedTick = nextDue;
    }

    public void setDirty() {
        this.dirty = true;
        this.owner.onInstanceDirty(this);
    }

    public boolean isDirty() {
        return this.dirty;
    }

    public Map<Key, AttributeModifier> getModifiersByOperation(Key operation) {
        return this.byOperation.computeIfAbsent(operation, k -> new Object2ObjectOpenHashMap<>());
    }

    private void updateBaseValue() {
        double base = this.baseValueSource.resolve(this.context.entity);
        if (base != this.lastBase) {
            this.lastBase = base;
            this.setDirty();
        }
    }

    public void runDue(long tick) {
        if (tick >= this.nextBaseTick) {
            this.updateBaseValue();
            this.nextBaseTick = tick + this.baseUpdateInterval;
        }
        this.updateTrackedModifiers(tick);
    }

    public long nextRequiredTick() {
        return Math.min(this.nextBaseTick, this.nextTrackedTick);
    }

    public double recalculate() {
        double value = this.lastBase;
        Map<Key, TrackedModifier> tracked = this.trackedById;
        for (AttributeOperation operation : this.attribute.operations()) {
            Map<Key, AttributeModifier> attributeModifiers = this.byOperation.get(operation.id());
            if (attributeModifiers != null) {
                double phaseBase = value;
                for (AttributeModifier modifier : attributeModifiers.values()) {
                    // 被轮询跟踪的修饰符吃快照值——轮询点是唯一求值点，避免双重求值与异步变量的前后不一致
                    TrackedModifier snapshot = tracked == null ? null : tracked.get(modifier.id());
                    if (snapshot != null) {
                        if (snapshot.condition) {
                            value = operation.apply(phaseBase, value, snapshot.amount);
                        }
                    } else if (modifier.test(this.context)) {
                        value = operation.apply(phaseBase, value, modifier.amount(this.context));
                    }
                }
            }
        }
        return this.attribute.limit(value);
    }

    public boolean needVanillaSync() {
        return this.syncTargets.length != 0;
    }

    public boolean needInitialVanillaSync() {
        if (this.syncTargets.length == 0) return false;
        double value = getValue();
        for (BoundSyncTarget target : this.syncTargets) {
            if (target.syncTarget.evaluate(value, this.lastBase) != 0.0d) {
                return true;
            }
        }
        return false;
    }

    public void syncToVanilla() {
        if (this.syncTargets.length == 0) return;
        double value = this.cachedValue;
        double base = this.lastBase;
        for (BoundSyncTarget target : this.syncTargets) {
            double amount = target.syncTarget.evaluate(value, base);
            if (target.applied) {
                if (Double.compare(amount, target.lastAmount) == 0) continue;
            } else if (amount == 0.0d) {
                continue;
            }
            updateVanillaModifier(target, amount);
            target.lastAmount = amount;
            target.applied = amount != 0.0d;
        }
    }

    public void clearSyncModifiers() {
        for (BoundSyncTarget target : this.syncTargets) {
            if (!target.applied) continue;
            target.attribute.removeModifier(this.attribute.syncModifierId());
            target.lastAmount = 0.0d;
            target.applied = false;
        }
    }

    private void updateVanillaModifier(BoundSyncTarget target, double amount) {
        LivingEntity living = this.context.entity;
        if (isMaxHealth(target.syncTarget.target())) {
            double oldMaxHealth = target.attribute.getValue();
            double health = living.health();
            setVanillaModifier(target, amount);
            double newMaxHealth = target.attribute.getValue();
            if (oldMaxHealth > 0 && newMaxHealth > 0 && newMaxHealth != oldMaxHealth) {
                living.setHealth(health * newMaxHealth / oldMaxHealth);
            }
        } else {
            setVanillaModifier(target, amount);
        }
    }

    private void setVanillaModifier(BoundSyncTarget target, double amount) {
        if (amount == 0.0d) {
            target.attribute.removeModifier(this.attribute.syncModifierId());
        } else {
            target.attribute.addOrUpdateTransientModifier(this.attribute.syncModifierId(), target.syncTarget.operation(), amount);
        }
    }

    private static BoundSyncTarget[] bindSyncTargets(List<SyncTarget> targets, LivingEntity entity) {
        if (targets.isEmpty()) return EMPTY_SYNC_TARGETS;
        BoundSyncTarget[] bound = new BoundSyncTarget[targets.size()];
        int size = 0;
        for (SyncTarget target : targets) {
            VanillaAttributeInstance attribute = entity.getVanillaAttribute(target.target());
            if (attribute != null) {
                bound[size++] = new BoundSyncTarget(target, attribute);
            }
        }
        if (size == 0) return EMPTY_SYNC_TARGETS;
        return size == bound.length ? bound : Arrays.copyOf(bound, size);
    }

    private boolean isMaxHealth(Key id) {
        if (VersionHelper.isOrAbove1_21_2) {
            return VanillaAttributes.MAX_HEALTH.equals(id);
        } else {
            return LegacyVanillaAttributes.MAX_HEALTH.equals(id);
        }
    }

    private static final class BoundSyncTarget {
        private final SyncTarget syncTarget;
        private final VanillaAttributeInstance attribute;
        private double lastAmount;
        private boolean applied;

        private BoundSyncTarget(SyncTarget target, VanillaAttributeInstance attribute) {
            this.syncTarget = target;
            this.attribute = attribute;
        }
    }

    private static final class TrackedModifier implements SwapList.Indexed {
        private final AttributeModifier modifier;
        private long nextTick = -1;
        private double amount;
        private boolean condition;
        private int index = -1;

        private TrackedModifier(AttributeModifier modifier, double amount, boolean condition) {
            this.modifier = modifier;
            this.amount = amount;
            this.condition = condition;
        }

        @Override
        public int index() {
            return this.index;
        }

        @Override
        public void index(int index) {
            this.index = index;
        }
    }
}
