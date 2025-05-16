package net.momirealms.craftengine.core.plugin.context.condition;

import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.properties.Property;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.Pair;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MatchBlockPropertyCondition<CTX extends Context> implements Condition<CTX> {
    private final List<Pair<String, String>> properties;

    public MatchBlockPropertyCondition(List<Pair<String, String>> properties) {
        this.properties = properties;
    }

    @Override
    public Key type() {
        return CommonConditions.MATCH_BLOCK_PROPERTY;
    }

    @Override
    public boolean test(CTX ctx) {
        return ctx.getOptionalParameter(DirectContextParameters.BLOCK_STATE).map(state -> {
            CustomBlock block = state.owner().value();
            for (Pair<String, String> property : this.properties) {
                Property<?> propertyIns = block.getProperty(property.left());
                if (propertyIns == null) {
                    return false;
                }
                if (!state.get(propertyIns).toString().toLowerCase(Locale.ENGLISH).equals(property.right())) {
                    return false;
                }
            }
            return true;
        }).orElse(false);
    }

    public static class FactoryImpl<CTX extends Context> implements ConditionFactory<CTX> {

        @Override
        public Condition<CTX> create(Map<String, Object> arguments) {
            Object propertyObj = ResourceConfigUtils.requireNonNullOrThrow(arguments.get("properties"), "warning.config.condition.match_block_property.missing_properties");
            List<Pair<String, String>> propertyList = new ArrayList<>();
            for (Map.Entry<String, Object> entry : MiscUtils.castToMap(propertyObj, false).entrySet()) {
                propertyList.add(new Pair<>(entry.getKey(), entry.getValue().toString()));
            }
            return new MatchBlockPropertyCondition<>(propertyList);
        }
    }
}
