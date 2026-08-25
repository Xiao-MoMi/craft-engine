package net.momirealms.craftengine.bukkit.compatibility.mythicmobs;

import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.function.AbstractConditionalFunction;
import net.momirealms.craftengine.core.plugin.context.function.FunctionFactory;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.plugin.context.text.TextProvider;
import net.momirealms.craftengine.core.plugin.context.text.TextProviders;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class MythicMobsSkillFunction<CTX extends Context> extends AbstractConditionalFunction<CTX> {
    private final TextProvider skill;
    private final NumberProvider power;
    private final Map<String, TextProvider> parameters;

    private MythicMobsSkillFunction(List<Condition<CTX>> predicates,
                                    TextProvider skill,
                                    @Nullable
                                    NumberProvider power,
                                    Map<String, TextProvider> parameters
    ) {
        super(predicates);
        this.skill = skill;
        this.power = power;
        this.parameters = parameters;
    }

    public static <CTX extends Context> FunctionFactory<CTX, MythicMobsSkillFunction<CTX>> factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
        return new Factory<>(factory);
    }

    @Override
    protected void runInternal(CTX ctx) {
        ctx.getOptionalParameter(DirectContextParameters.PLAYER).ifPresent(it -> {
            float power = this.power == null ? 1.0f : this.power.getFloat(ctx);
            Map<String, String> parameters = new LinkedHashMap<>(this.parameters.size());
            for (Map.Entry<String, TextProvider> entry : this.parameters.entrySet()) {
                parameters.put(entry.getKey(), entry.getValue().get(ctx));
            }
            MythicMobsHelper.executeSkill(this.skill.get(ctx), power, parameters, it);
        });
    }

    private static class Factory<CTX extends Context> extends AbstractFactory<CTX, MythicMobsSkillFunction<CTX>> {

        public Factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
            super(factory);
        }

        @Override
        public MythicMobsSkillFunction<CTX> create(ConfigSection section) {
            Map<String, TextProvider> parameters = new LinkedHashMap<>();
            ConfigSection parameterSection = section.getSection("parameters");
            if (parameterSection != null) {
                for (String key : parameterSection.keySet()) {
                    parameters.put(
                            key.toLowerCase(Locale.ROOT),
                            parameterSection.getValue(key, ConfigValue::getAsText)
                    );
                }
            }
            return new MythicMobsSkillFunction<>(
                    getPredicates(section),
                    section.getNonNullValue("skill", ConfigConstants.ARGUMENT_STRING, v -> TextProviders.fromString(v.getAsString())),
                    section.getNumber("power"),
                    parameters
            );
        }
    }
}