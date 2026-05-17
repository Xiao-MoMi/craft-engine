package net.momirealms.craftengine.core.plugin.context.condition;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.text.TextProvider;
import net.momirealms.craftengine.core.plugin.context.text.TextProviders;

public final class StringEqualsCondition<CTX extends Context> implements Condition<CTX> {
    private final TextProvider value1;
    private final TextProvider value2;

    private StringEqualsCondition(TextProvider value1, TextProvider value2) {
        this.value1 = value1;
        this.value2 = value2;
    }

    @Override
    public boolean test(CTX ctx) {
        return this.value1.get(ctx).equals(this.value2.get(ctx));
    }

    public static <CTX extends Context> ConditionFactory<CTX, StringEqualsCondition<CTX>> factory() {
        return new Factory<>();
    }

    private static class Factory<CTX extends Context> implements ConditionFactory<CTX, StringEqualsCondition<CTX>> {

        @Override
        public StringEqualsCondition<CTX> create(ConfigSection arguments) {
            return new StringEqualsCondition<>(
                    TextProviders.fromString(arguments.getNonNullString("value1")),
                    TextProviders.fromString(arguments.getNonNullString("value2"))
            );
        }
    }
}