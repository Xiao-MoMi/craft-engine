package net.momirealms.craftengine.core.attribute;

import com.google.common.collect.ImmutableMap;
import net.momirealms.craftengine.core.entity.LivingEntityHolder;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.sparrow.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

import java.util.BitSet;
import java.util.List;

public final class EntityAttributes implements AttributeGetter {
    private static final AttributeInstance[] EMPTY_INSTANCES = new AttributeInstance[0];

    private final LivingEntityHolder holder;
    private final AttributeInstance[] instances;
    private final AttributeInstance[] syncInstances;
    private final ImmutableMap<Key, AttributeInstance> instancesById;
    private final BitSet dirtySyncInstances;
    private final BitSet scheduledInstances;
    private boolean running;

    public EntityAttributes(LivingEntityHolder holder, List<Attribute> applicable) {
        this.holder = holder;
        ImmutableMap.Builder<Key, AttributeInstance> mapBuilder = ImmutableMap.builder();
        int count = 0;
        for (Attribute attribute : applicable) {
            if (attribute.derived() == null) count++;
        }
        this.instances = new AttributeInstance[count];
        this.scheduledInstances = new BitSet(count);
        int index = 0;
        int syncCount = 0;
        for (Attribute attribute : applicable) {
            if (attribute.derived() != null) continue;
            AttributeInstance instance = new AttributeInstance(attribute, holder.context, this, index);
            this.instances[index] = instance;
            mapBuilder.put(attribute.id(), instance);
            if (instance.needVanillaSync()) syncCount++;
            if (instance.nextRequiredTick() != Long.MAX_VALUE) {
                this.scheduledInstances.set(index);
            }
            index++;
        }
        this.instancesById = mapBuilder.build();
        this.syncInstances = syncCount == 0 ? EMPTY_INSTANCES : new AttributeInstance[syncCount];
        this.dirtySyncInstances = new BitSet(syncCount);
        int syncIndex = 0;
        for (AttributeInstance instance : this.instances) {
            if (!instance.needVanillaSync()) continue;
            instance.syncIndex(syncIndex);
            this.syncInstances[syncIndex] = instance;
            if (instance.needInitialVanillaSync()) {
                this.dirtySyncInstances.set(syncIndex);
            }
            syncIndex++;
        }
        if (!this.dirtySyncInstances.isEmpty() || nextRequiredTick() != Long.MAX_VALUE) {
            this.holder.wakeAttributes();
        }
    }

    @Nullable
    public AttributeInstance getInstance(Key attribute) {
        return this.instancesById.get(attribute);
    }

    public void clearSyncModifiers() {
        for (AttributeInstance instance : this.syncInstances) {
            instance.clearSyncModifiers();
        }
    }

    @Override
    public double getAttributeValue(Attribute attribute) {
        if (attribute.derived() != null) {
            return attribute.derive(this::getAttributeValue);
        }
        AttributeInstance instance = getInstance(attribute.id());
        return instance == null ? attribute.currentValue(this.holder.entity) : instance.getValue();
    }

    void onInstanceDirty(AttributeInstance instance) {
        int syncIndex = instance.syncIndex();
        if (syncIndex == -1) return;
        this.dirtySyncInstances.set(syncIndex);
        if (!this.running) {
            this.holder.wakeAttributes();
        }
    }

    void onScheduleChanged(AttributeInstance instance) {
        this.scheduledInstances.set(instance.index(), instance.nextRequiredTick() != Long.MAX_VALUE);
        if (!this.running) {
            this.holder.wakeAttributes();
        }
    }

    public long runDue(long gameTick) {
        this.running = true;
        try {
            for (int index = this.scheduledInstances.nextSetBit(0); index >= 0; index = this.scheduledInstances.nextSetBit(index + 1)) {
                AttributeInstance instance = this.instances[index];
                if (instance.nextRequiredTick() <= gameTick) {
                    instance.runDue(gameTick);
                    if (instance.nextRequiredTick() == Long.MAX_VALUE) {
                        this.scheduledInstances.clear(index);
                    }
                }
            }
            flushDirtySyncInstances();
            return nextRequiredTick();
        } finally {
            this.running = false;
        }
    }

    public long nextRequiredTick() {
        long next = Long.MAX_VALUE;
        for (int index = this.scheduledInstances.nextSetBit(0);
             index >= 0;
             index = this.scheduledInstances.nextSetBit(index + 1)) {
            next = Math.min(next, this.instances[index].nextRequiredTick());
        }
        return next;
    }

    private void flushDirtySyncInstances() {
        for (int index = this.dirtySyncInstances.nextSetBit(0); index >= 0; index = this.dirtySyncInstances.nextSetBit(index + 1)) {
            AttributeInstance instance = this.syncInstances[index];
            instance.getValue();
            instance.syncToVanilla();
            this.dirtySyncInstances.clear(index);
        }
    }

    public EntityAttributesSnapshot createSnapshot() {
        CompoundTag data = new CompoundTag();
        for (AttributeInstance instance : this.instances) {
            data.putDouble(instance.attribute().id().asString(), instance.getValue());
        }
        return new EntityAttributesSnapshot(data);
    }
}
