package net.momirealms.craftengine.core.plugin.context.condition;

import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.StatePropertyAccessor;
import net.momirealms.craftengine.core.block.properties.Property;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.Pair;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.world.ExistingBlock;

import java.util.*;

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
        ImmutableBlockState customBlockState = null;
        StatePropertyAccessor vanillaStatePropertyAccessor = null;
        // 优先使用自定义状态，其主要应用于自定义方块掉落物
        Optional<ImmutableBlockState> optionalCustomState = ctx.getOptionalParameter(DirectContextParameters.CUSTOM_BLOCK_STATE);
        if (optionalCustomState.isPresent()) {
            customBlockState = optionalCustomState.get();
        } else {
            // 其次再判断block，这个过程会更慢，因为每次获取都是全新的方块状态，适用于物品等事件
            Optional<ExistingBlock> optionalExistingBlock = ctx.getOptionalParameter(DirectContextParameters.BLOCK);
            if (optionalExistingBlock.isPresent()) {
                ExistingBlock existingBlock = optionalExistingBlock.get();
                customBlockState = existingBlock.customBlockState();
                if (customBlockState == null) {
                    vanillaStatePropertyAccessor = existingBlock.createStatePropertyAccessor();
                }
            } else {
                // 都没有则条件不过
                return false;
            }
        }
        if (customBlockState != null) {
            CustomBlock block = customBlockState.owner().value();
            for (Pair<String, String> property : this.properties) {
                Property<?> propertyIns = block.getProperty(property.left());
                if (propertyIns == null) {
                    return false;
                }
                if (!customBlockState.get(propertyIns).toString().toLowerCase(Locale.ENGLISH).equals(property.right())) {
                    return false;
                }
            }
        } else {
            for (Pair<String, String> property : this.properties) {
                String value = vanillaStatePropertyAccessor.getPropertyValueAsString(property.left());
                if (value == null) {
                    return false;
                }
                if (!value.equals(property.right())) {
                    return false;
                }
            }
        }
        return true;
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
