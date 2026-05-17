package net.momirealms.craftengine.core.plugin.context.function;

import net.momirealms.craftengine.core.block.BlockStateWrapper;
import net.momirealms.craftengine.core.block.UpdateFlags;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.plugin.context.number.NumberProviders;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.LazyReference;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.WorldPosition;

import java.util.List;
import java.util.Optional;

public final class PlaceBlockFunction<CTX extends Context> extends AbstractConditionalFunction<CTX> {
    private final LazyReference<BlockStateWrapper> lazyBlockState;
    private final NumberProvider x;
    private final NumberProvider y;
    private final NumberProvider z;
    private final NumberProvider updateFlags;

    private PlaceBlockFunction(List<Condition<CTX>> predicates,
                               NumberProvider x,
                               NumberProvider y,
                               NumberProvider z,
                               NumberProvider updateFlags,
                               LazyReference<BlockStateWrapper> lazyBlockState) {
        super(predicates);
        this.lazyBlockState = lazyBlockState;
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
            world.setBlockState(MiscUtils.floor(this.x.getDouble(ctx)), MiscUtils.floor(this.y.getDouble(ctx)), MiscUtils.floor(this.z.getDouble(ctx)), this.lazyBlockState.get(), this.updateFlags.getInt(ctx));
        }
    }

    public static <CTX extends Context> FunctionFactory<CTX, PlaceBlockFunction<CTX>> factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
        return new Factory<>(factory);
    }

    private static class Factory<CTX extends Context> extends AbstractFactory<CTX, PlaceBlockFunction<CTX>> {
        private static final String[] BLOCK_STATE = new String[] {"block_state", "block-state"};
        private static final String[] UPDATE_FLAGS = new String[] {"update_flags", "update-flags"};

        public Factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
            super(factory);
        }

        @Override
        public PlaceBlockFunction<CTX> create(ConfigSection section) {
            String state = section.getNonEmptyString(BLOCK_STATE);
            return new PlaceBlockFunction<>(
                    getPredicates(section),
                    section.getNumber("x", ConfigConstants.POSITION_X),
                    section.getNumber("y", ConfigConstants.POSITION_Y),
                    section.getNumber("z", ConfigConstants.POSITION_Z),
                    section.getNumber(UPDATE_FLAGS, NumberProviders.direct(UpdateFlags.UPDATE_ALL)),
                    LazyReference.lazyReference(() -> CraftEngine.instance().blockManager().createBlockState(state))
            );
        }
    }
}