package net.momirealms.craftengine.core.plugin.context.function;

import net.momirealms.craftengine.core.block.BlockStateWrapper;
import net.momirealms.craftengine.core.block.UpdateOption;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.plugin.context.number.NumberProviders;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.LazyReference;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.Tag;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class TransformBlockFunction<CTX extends Context> extends AbstractConditionalFunction<CTX> {
    private final LazyReference<BlockStateWrapper> lazyBlockState;
    private final CompoundTag properties;
    private final NumberProvider x;
    private final NumberProvider y;
    private final NumberProvider z;
    private final NumberProvider updateFlags;

    public TransformBlockFunction(List<Condition<CTX>> predicates, CompoundTag properties, NumberProvider x, NumberProvider y, NumberProvider z, NumberProvider updateFlags, LazyReference<BlockStateWrapper> lazyBlockState) {
        super(predicates);
        this.properties = properties;
        this.x = x;
        this.y = y;
        this.z = z;
        this.updateFlags = updateFlags;
        this.lazyBlockState = lazyBlockState;
    }

    @Override
    public void runInternal(CTX ctx) {
        Optional<WorldPosition> optionalWorldPosition = ctx.getOptionalParameter(DirectContextParameters.POSITION);
        if (optionalWorldPosition.isPresent()) {
            World world = optionalWorldPosition.get().world();
            int x = MiscUtils.floor(this.x.getDouble(ctx));
            int y = MiscUtils.floor(this.y.getDouble(ctx));
            int z = MiscUtils.floor(this.z.getDouble(ctx));
            BlockStateWrapper existingBlockState = world.getBlock(x, y, z).blockState().withProperties(this.properties);
            CompoundTag newProperties = new CompoundTag();
            for (String propertyName : existingBlockState.getPropertyNames()) {
                Object property = existingBlockState.getProperty(propertyName);
                newProperties.putString(propertyName, String.valueOf(property).toLowerCase(Locale.ROOT));
            }
            if (!this.properties.isEmpty()) {
                for (Map.Entry<String, Tag> tagEntry : this.properties.entrySet()) {
                    newProperties.put(tagEntry.getKey(), tagEntry.getValue());
                }
            }
            world.setBlockState(x, y, z, this.lazyBlockState.get().withProperties(newProperties), this.updateFlags.getInt(ctx));
        }
    }

    public static <CTX extends Context> FunctionFactory<CTX, TransformBlockFunction<CTX>> factory(java.util.function.Function<Map<String, Object>, Condition<CTX>> factory) {
        return new Factory<>(factory);
    }

    private static class Factory<CTX extends Context> extends AbstractFactory<CTX, TransformBlockFunction<CTX>> {

        public Factory(java.util.function.Function<Map<String, Object>, Condition<CTX>> factory) {
            super(factory);
        }

        @Override
        public TransformBlockFunction<CTX> create(Map<String, Object> arguments) {
            String block = ResourceConfigUtils.requireNonEmptyStringOrThrow(arguments.get("block"), "warning.config.function.transform_block.missing_block");
            CompoundTag properties = new CompoundTag();
            Map<String, Object> propertiesMap = MiscUtils.castToMap(arguments.get("properties"), true);
            if (propertiesMap != null) {
                for (Map.Entry<String, Object> entry : propertiesMap.entrySet()) {
                    properties.putString(entry.getKey(), String.valueOf(entry.getValue()));
                }
            }
            return new TransformBlockFunction<>(
                    getPredicates(arguments), properties, NumberProviders.fromObject(arguments.getOrDefault("x", "<arg:position.x>")), NumberProviders.fromObject(arguments.getOrDefault("y", "<arg:position.y>")), NumberProviders.fromObject(arguments.getOrDefault("z", "<arg:position.z>")), Optional.ofNullable(arguments.get("update-flags")).map(NumberProviders::fromObject).orElse(NumberProviders.direct(UpdateOption.UPDATE_ALL.flags())), LazyReference.lazyReference(() -> CraftEngine.instance().blockManager().createBlockState(block))
            );
        }
    }
}