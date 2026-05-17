package net.momirealms.craftengine.core.plugin.context.function;

import net.momirealms.craftengine.core.block.BlockStateWrapper;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.world.ExistingBlock;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.sparrow.nbt.CompoundTag;

import java.util.List;
import java.util.Optional;

public final class UpdateBlockPropertyFunction<CTX extends Context> extends AbstractConditionalFunction<CTX> {
    private final CompoundTag properties;
    private final NumberProvider x;
    private final NumberProvider y;
    private final NumberProvider z;
    private final NumberProvider updateFlags;

    private UpdateBlockPropertyFunction(List<Condition<CTX>> predicates, NumberProvider x, NumberProvider y, NumberProvider z, NumberProvider updateFlags, CompoundTag properties) {
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

    public static <CTX extends Context> FunctionFactory<CTX, UpdateBlockPropertyFunction<CTX>> factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
        return new Factory<>(factory);
    }

    private static class Factory<CTX extends Context> extends AbstractFactory<CTX, UpdateBlockPropertyFunction<CTX>> {
        private static final String[] UPDATE_FLAGS = new String[] {"update_flags", "update-flags"};

        public Factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
            super(factory);
        }

        @Override
        public UpdateBlockPropertyFunction<CTX> create(ConfigSection section) {
            ConfigSection propertiesSection = section.getNonNullSection("properties");
            CompoundTag properties = new CompoundTag();
            for (String key : propertiesSection.keySet()) {
                properties.putString(key, propertiesSection.getNonEmptyString(key));
            }
            return new UpdateBlockPropertyFunction<>(
                    getPredicates(section),
                    section.getNumber("x", ConfigConstants.POSITION_X),
                    section.getNumber("y", ConfigConstants.POSITION_Y),
                    section.getNumber("z", ConfigConstants.POSITION_Z),
                    section.getNumber(UPDATE_FLAGS, ConfigConstants.UPDATE_ALL),
                    properties
            );
        }
    }
}