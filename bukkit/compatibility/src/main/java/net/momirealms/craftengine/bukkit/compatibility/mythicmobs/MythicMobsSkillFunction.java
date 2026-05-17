package net.momirealms.craftengine.bukkit.compatibility.mythicmobs;

import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.function.AbstractConditionalFunction;
import net.momirealms.craftengine.core.plugin.context.function.FunctionFactory;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.plugin.context.text.TextProvider;
import net.momirealms.craftengine.core.plugin.context.text.TextProviders;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class MythicMobsSkillFunction<CTX extends Context> extends AbstractConditionalFunction<CTX> {
    private final TextProvider skill;
    private final NumberProvider power;

    private MythicMobsSkillFunction(List<Condition<CTX>> predicates,
                                    TextProvider skill,
                                    @Nullable
                                    NumberProvider power
    ) {
        super(predicates);
        this.skill = skill;
        this.power = power;
    }

    @Override
    protected void runInternal(CTX ctx) {
        ctx.getOptionalParameter(DirectContextParameters.PLAYER).ifPresent(it -> {
            float power = this.power == null ? 1.0f : this.power.getFloat(ctx);
            MythicMobsHelper.executeSkill(this.skill.get(ctx), power, it);
        });
    }

    public static <CTX extends Context> FunctionFactory<CTX, MythicMobsSkillFunction<CTX>> factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
        return new Factory<>(factory);
    }

    private static class Factory<CTX extends Context> extends AbstractFactory<CTX, MythicMobsSkillFunction<CTX>> {

        public Factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
            super(factory);
        }

        @Override
        public MythicMobsSkillFunction<CTX> create(ConfigSection section) {
            return new MythicMobsSkillFunction<>(
                    getPredicates(section),
                    section.getNonNullValue("skill", ConfigConstants.ARGUMENT_STRING, v -> TextProviders.fromString(v.getAsString())),
                    section.getNumber("power")
            );
        }
    }
}