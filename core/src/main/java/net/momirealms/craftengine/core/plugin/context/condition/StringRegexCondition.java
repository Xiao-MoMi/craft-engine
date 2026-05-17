package net.momirealms.craftengine.core.plugin.context.condition;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.text.TextProvider;
import net.momirealms.craftengine.core.plugin.context.text.TextProviders;

public final class StringRegexCondition<CTX extends Context> implements Condition<CTX> {
    private final TextProvider value;
    private final TextProvider regex;

    private StringRegexCondition(TextProvider value, TextProvider regex) {
        this.value = value;
        this.regex = regex;
    }

    @Override
    public boolean test(CTX ctx) {
        return this.value.get(ctx).matches(this.regex.get(ctx));
    }

    public static <CTX extends Context> ConditionFactory<CTX, StringRegexCondition<CTX>> factory() {
        return new Factory<>();
    }

    private static class Factory<CTX extends Context> implements ConditionFactory<CTX, StringRegexCondition<CTX>> {

        @Override
        public StringRegexCondition<CTX> create(ConfigSection arguments) {
            return new StringRegexCondition<>(
                    TextProviders.fromString(arguments.getNonNullString("value")),
                    TextProviders.fromString(arguments.getNonNullString("regex"))
            );
        }
    }
}