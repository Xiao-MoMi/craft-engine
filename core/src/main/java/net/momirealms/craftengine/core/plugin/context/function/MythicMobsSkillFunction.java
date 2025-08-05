package net.momirealms.craftengine.core.plugin.context.function;

import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;

import java.util.List;
import java.util.Map;

public class MythicMobsSkillFunction<CTX extends Context> extends AbstractConditionalFunction<CTX> {
    private final String skill;
    private final float power;

    public MythicMobsSkillFunction(String skill, float power, List<Condition<CTX>> predicates) {
        super(predicates);
        this.skill = skill;
        this.power = power;
    }

    @Override
    protected void runInternal(CTX ctx) {
        ctx.getOptionalParameter(DirectContextParameters.PLAYER).ifPresent(it -> {
            CraftEngine.instance().compatibilityManager().executeMMSkill(this.skill, this.power, it);
        });
    }

    @Override
    public Key type() {
        return CommonFunctions.MYTHIC_MOBS_SKILL;
    }

    public static class FactoryImpl<CTX extends Context> extends AbstractFactory<CTX> {

        public FactoryImpl(java.util.function.Function<Map<String, Object>, Condition<CTX>> factory) {
            super(factory);
        }

        @Override
        public Function<CTX> create(Map<String, Object> args) {
            String skill = ResourceConfigUtils.requireNonEmptyStringOrThrow(args.get("skill"), "warning.config.function.mythic_mobs_skill.missing_skill");
            float power = ResourceConfigUtils.getAsFloat(args.getOrDefault("power", 1.0), "power");
            return new MythicMobsSkillFunction<>(skill, power, getPredicates(args));
        }
    }
}
