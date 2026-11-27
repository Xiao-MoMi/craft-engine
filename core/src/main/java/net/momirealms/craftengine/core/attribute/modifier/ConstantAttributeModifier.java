package net.momirealms.craftengine.core.attribute.modifier;

import net.momirealms.craftengine.core.attribute.modifier.removal.RemovalCondition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public class ConstantAttributeModifier implements AttributeModifier {
    private final Key id;
    private final double amount;
    private final Key operation;
    @Nullable
    private final Predicate<Context> condition;
    @Nullable
    private final RemovalCondition removalCondition;

    public ConstantAttributeModifier(Key id, double amount, Key operation, @Nullable Predicate<Context> condition) {
        this(id, amount, operation, condition, null);
    }

    public ConstantAttributeModifier(Key id, double amount, Key operation, @Nullable Predicate<Context> condition, @Nullable RemovalCondition removalCondition) {
        this.id = id;
        this.amount = amount;
        this.operation = operation;
        this.condition = condition;
        this.removalCondition = removalCondition;
    }

    public static ConstantAttributeModifier simple(Key id, double amount, Key operation) {
        return new ConstantAttributeModifier(id, amount, operation, null);
    }

    public static ConstantAttributeModifier simple(Key id, double amount, Key operation, RemovalCondition removalCondition) {
        return new ConstantAttributeModifier(id, amount, operation, null, removalCondition);
    }

    public static ConstantAttributeModifier conditional(Key id, double amount, Key operation, @Nullable Predicate<Context> condition) {
        return new ConstantAttributeModifier(id, amount, operation, condition);
    }

    public static ConstantAttributeModifier conditional(Key id, double amount, Key operation, @Nullable Predicate<Context> condition, RemovalCondition removalCondition) {
        return new ConstantAttributeModifier(id, amount, operation, condition, removalCondition);
    }

    @Nullable
    @Override
    public RemovalCondition removalCondition() {
        return this.removalCondition;
    }

    @Override
    public boolean test(Context context) {
        return this.condition == null || this.condition.test(context);
    }

    @Override
    public Key id() {
        return this.id;
    }

    @Override
    public double amount(Context context) {
        return this.amount;
    }

    @Override
    public Key operation() {
        return this.operation;
    }
}
