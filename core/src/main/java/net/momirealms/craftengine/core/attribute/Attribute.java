package net.momirealms.craftengine.core.attribute;

import net.momirealms.craftengine.core.attribute.base.BaseValueSource;
import net.momirealms.craftengine.core.attribute.derived.DerivedValue;
import net.momirealms.craftengine.core.attribute.sync.SyncTarget;
import net.momirealms.craftengine.core.entity.Entity;
import net.momirealms.craftengine.core.entity.LivingEntity;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public final class Attribute {
    private static final String SYNC_MODIFIER_PREFIX = "ce_sync/";
    // 派生属性解析栈，检测循环引用（A 派生自 B、B 派生自 A），对所有 DerivedValue 实现生效
    private static final ThreadLocal<Deque<Key>> DERIVATION_STACK = ThreadLocal.withInitial(ArrayDeque::new);
    public final Key id;
    private final Key syncModifierId;
    public final BaseValueSource baseValueSource;
    public final ValueConstraint constraint;
    public final List<AttributeOperation> operations;
    @Nullable
    public final Set<Key> applicableEntityTypes;
    public final List<SyncTarget> syncTargets;
    @Nullable
    public final DerivedValue derived;

    public Attribute(Key id, BaseValueSource baseValueSource, ValueConstraint constraint, List<AttributeOperation> operations, @Nullable Set<Key> applicableEntityTypes, List<SyncTarget> syncTargets, @Nullable DerivedValue derived) {
        this.id = id;
        this.syncModifierId = Key.of(id.namespace(), SYNC_MODIFIER_PREFIX + id.value());
        this.baseValueSource = baseValueSource;
        this.constraint = constraint;
        this.operations = operations;
        this.applicableEntityTypes = applicableEntityTypes;
        this.syncTargets = syncTargets;
        this.derived = derived;
    }

    public Key id() {
        return this.id;
    }

    Key syncModifierId() {
        return this.syncModifierId;
    }

    public double limit(double value) {
        return this.constraint.limit(value);
    }

    public double defaultValue(Entity entity) {
        return this.baseValueSource.resolve(entity);
    }

    public double currentValue(Entity entity) {
        return this.limit(this.baseValueSource.resolveCurrent(entity));
    }

    public BaseValueSource baseValueSource() {
        return this.baseValueSource;
    }

    public ValueConstraint constraint() {
        return this.constraint;
    }

    public List<AttributeOperation> operations() {
        return this.operations;
    }

    public boolean appliesTo(LivingEntity entity) {
        return this.applicableEntityTypes == null || this.applicableEntityTypes.contains(entity.id());
    }

    public List<SyncTarget> syncTargets() {
        return this.syncTargets;
    }

    @Nullable
    public DerivedValue derived() {
        return this.derived;
    }

    // 派生属性求值：变量经 resolver 取同侧其他属性的有效值
    public double derive(Function<Attribute, Double> resolver) {
        Deque<Key> stack = DERIVATION_STACK.get();
        if (stack.contains(this.id)) {
            throw new IllegalStateException("Circular derived attribute reference: " + String.join(" -> ", stack.stream().map(Key::asString).toList()) + " -> " + this.id.asString());
        }
        stack.push(this.id);
        try {
            return this.limit(this.derived.evaluate(resolver));
        } finally {
            stack.pop();
            if (stack.isEmpty()) {
                DERIVATION_STACK.remove();
            }
        }
    }
}
