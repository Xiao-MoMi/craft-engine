package net.momirealms.craftengine.bukkit.compatibility.mythicmobs;

import io.lumine.mythic.core.skills.variables.Variable;
import io.lumine.mythic.core.skills.variables.VariableType;
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
    private final Map<String, VariableProvider> variables;

    private MythicMobsSkillFunction(List<Condition<CTX>> predicates,
                                    TextProvider skill,
                                    @Nullable
                                    NumberProvider power,
                                    Map<String, TextProvider> parameters,
                                    Map<String, VariableProvider> variables
    ) {
        super(predicates);
        this.skill = skill;
        this.power = power;
        this.parameters = parameters;
        this.variables = variables;
    }

    public static <CTX extends Context> FunctionFactory<CTX, MythicMobsSkillFunction<CTX>> factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
        return new Factory<>(factory);
    }

    @Override
    protected void runInternal(CTX ctx) {
        ctx.getOptionalParameter(DirectContextParameters.PLAYER).ifPresent(it -> {
            float power = this.power == null ? 1.0f : this.power.getFloat(ctx);
            String skill = this.skill.get(ctx);
            if (this.parameters.isEmpty() && this.variables.isEmpty()) {
                MythicMobsHelper.executeSkill(skill, power, it);
                return;
            }

            Map<String, String> parameters = Map.of();
            if (!this.parameters.isEmpty()) {
                parameters = new LinkedHashMap<>(this.parameters.size());
                for (Map.Entry<String, TextProvider> entry : this.parameters.entrySet()) {
                    parameters.put(entry.getKey(), entry.getValue().get(ctx));
                }
            }
            Map<String, Variable> variables = Map.of();
            if (!this.variables.isEmpty()) {
                variables = new LinkedHashMap<>(this.variables.size());
                for (Map.Entry<String, VariableProvider> entry : this.variables.entrySet()) {
                    variables.put(entry.getKey(), entry.getValue().get(ctx));
                }
            }
            MythicMobsHelper.executeSkill(skill, power, parameters, variables, it);
        });
    }

    private record VariableProvider(VariableType type, TextProvider value) {

        private Variable get(Context context) {
            Variable variable = Variable.ofType(this.type, this.value.get(context));
            if (variable == null) {
                throw new IllegalArgumentException("Unsupported MythicMobs variable type: " + this.type);
            }
            return variable;
        }
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
                    section.getNumber("power"),
                    getParameters(section),
                    getVariables(section)
            );
        }

        private Map<String, TextProvider> getParameters(ConfigSection section) {
            ConfigSection parameterSection = section.getSection("parameters");
            if (parameterSection == null || parameterSection.size() == 0) {
                return Map.of();
            }
            Map<String, TextProvider> parameters = new LinkedHashMap<>();
            for (String key : parameterSection.keySet()) {
                parameters.put(
                        key.toLowerCase(Locale.ROOT),
                        parameterSection.getValue(key, ConfigValue::getAsText)
                );
            }
            return parameters;
        }

        private Map<String, VariableProvider> getVariables(ConfigSection section) {
            ConfigSection variableSection = section.getSection("variables");
            if (variableSection == null || variableSection.size() == 0) {
                return Map.of();
            }
            Map<String, VariableProvider> variables = new LinkedHashMap<>();
            for (String key : variableSection.keySet()) {
                ConfigValue value = variableSection.getValue(key);
                if (value.is(Map.class)) {
                    ConfigSection explicitVariable = value.getAsSection();
                    ConfigValue explicitValue = explicitVariable.getNonNullValue("value", ConfigConstants.ARGUMENT_STRING);
                    variables.put(key, new VariableProvider(
                            explicitVariable.getEnum("type", VariableType.class, inferType(explicitValue.value())),
                            explicitValue.getAsText()
                    ));
                } else {
                    variables.put(key, new VariableProvider(inferType(value.value()), value.getAsText()));
                }
            }
            return variables;
        }

        private VariableType inferType(Object value) {
            if (value instanceof Byte || value instanceof Short || value instanceof Integer) {
                return VariableType.INTEGER;
            }
            if (value instanceof Float) {
                return VariableType.FLOAT;
            }
            if (value instanceof Number) {
                return VariableType.DOUBLE;
            }
            return VariableType.STRING;
        }
    }
}
