package net.momirealms.craftengine.core.plugin.context.function;

import net.momirealms.craftengine.core.block.BlockStateWrapper;
import net.momirealms.craftengine.core.block.UpdateFlags;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.plugin.context.number.NumberProviders;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.world.ExistingBlock;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.sparrow.nbt.CompoundTag;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class UpdateBlockPropertyFunction<CTX extends Context> extends AbstractConditionalFunction<CTX> {
    private final CompoundTag properties;
    private final NumberProvider x;
    private final NumberProvider y;
    private final NumberProvider z;
    private final NumberProvider updateFlags;

    public UpdateBlockPropertyFunction(List<Condition<CTX>> predicates, NumberProvider x, NumberProvider y, NumberProvider z, NumberProvider updateFlags, CompoundTag properties) {
        super(predicates);
        this.properties = properties;
        this.x = x;
        this.y = y;
        this.z = z;
        this.updateFlags = updateFlags;
    }

    @Override
    public void runInternal(CTX ctx) {
        Optional<WorldPosition> optionalWorldPosition = ctx.getOptionalParameter(DirectContextParameters.POSITION);
        if (optionalWorldPosition.isPresent()) {
            World world = optionalWorldPosition.get().world();
            int x = MiscUtils.floor(this.x.getDouble(ctx));
            int y = MiscUtils.floor(this.y.getDouble(ctx));
            int z = MiscUtils.floor(this.z.getDouble(ctx));
            ExistingBlock blockAt = world.getBlock(x, y, z);
            BlockStateWrapper wrapper = blockAt.blockState().withProperties(this.properties);
            world.setBlockState(x, y, z, wrapper, this.updateFlags.getInt(ctx));
        }
    }

    public static <CTX extends Context> FunctionFactory<CTX, UpdateBlockPropertyFunction<CTX>> factory(java.util.function.Function<Map<String, Object>, Condition<CTX>> factory) {
        return new Factory<>(factory);
    }

    private static class Factory<CTX extends Context> extends AbstractFactory<CTX, UpdateBlockPropertyFunction<CTX>> {

        public Factory(java.util.function.Function<Map<String, Object>, Condition<CTX>> factory) {
            super(factory);
        }

        @Override
        public UpdateBlockPropertyFunction<CTX> create(Map<String, Object> arguments) {
            Map<String, Object> state = ResourceConfigUtils.getAsMap(ResourceConfigUtils.requireNonNullOrThrow(arguments.get("properties"), "warning.config.function.update_block_property.missing_properties"), "properties");
            CompoundTag properties = new CompoundTag();
            for (Map.Entry<String, Object> entry : state.entrySet()) {
                properties.putString(entry.getKey(), String.valueOf(entry.getValue()));
            }
            return new UpdateBlockPropertyFunction<>(getPredicates(arguments), NumberProviders.fromObject(arguments.getOrDefault("x", "<arg:position.x>")), NumberProviders.fromObject(arguments.getOrDefault("y", "<arg:position.y>")), NumberProviders.fromObject(arguments.getOrDefault("z", "<arg:position.z>")), Optional.ofNullable(arguments.get("update-flags")).map(NumberProviders::fromObject).orElse(NumberProviders.direct(UpdateFlags.UPDATE_ALL)), properties);
        }
    }
}