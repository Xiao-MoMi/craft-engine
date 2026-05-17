package net.momirealms.craftengine.core.plugin.context.function;

import net.momirealms.craftengine.core.block.BlockStateWrapper;
import net.momirealms.craftengine.core.block.UpdateFlags;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.plugin.context.number.NumberProviders;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.WorldPosition;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class CycleBlockPropertyFunction<CTX extends Context> extends AbstractConditionalFunction<CTX> {
    private final String property;
    @Nullable
    private final Map<String, String> rules;
    @Nullable
    private final NumberProvider inverse;
    private final NumberProvider x;
    private final NumberProvider y;
    private final NumberProvider z;
    private final NumberProvider updateFlags;

    private CycleBlockPropertyFunction(List<Condition<CTX>> predicates,
                                       String property,
                                       @Nullable Map<String, String> rules,
                                       @Nullable NumberProvider inverse,
                                       NumberProvider x,
                                       NumberProvider y,
                                       NumberProvider z,
                                       NumberProvider updateFlags) {
        super(predicates);
        this.property = property;
        this.rules = rules;
        this.inverse = inverse;
        this.x = x;
        this.y = y;
        this.z = z;
        this.updateFlags = updateFlags;
    }

    @Override
    protected void runInternal(CTX ctx) {
        Optional<WorldPosition> optionalWorldPosition = ctx.getOptionalParameter(DirectContextParameters.POSITION);
        if (optionalWorldPosition.isEmpty()) return;
        World world = optionalWorldPosition.get().world();
        int x = MiscUtils.floor(this.x.getDouble(ctx));
        int y = MiscUtils.floor(this.y.getDouble(ctx));
        int z = MiscUtils.floor(this.z.getDouble(ctx));
        BlockStateWrapper wrapper = updateBlockState(world.getBlock(x, y, z).blockState(), ctx);
        world.setBlockState(x, y, z, wrapper, this.updateFlags.getInt(ctx));
    }

    private BlockStateWrapper updateBlockState(BlockStateWrapper wrapper, CTX ctx) {
        boolean inverse = this.inverse != null && this.inverse.getInt(ctx) == 0;
        if (this.rules == null) {
            return wrapper.cycleProperty(this.property, inverse);
        }
        Object value = wrapper.getProperty(this.property);
        if (value == null) {
            return wrapper.cycleProperty(this.property, inverse);
        }
        String mapValue = this.rules.get(value.toString().toLowerCase(Locale.ROOT));
        if (mapValue == null) {
            return wrapper.cycleProperty(this.property, inverse);
        }
        return wrapper.withProperty(this.property, mapValue);
    }

    public static <CTX extends Context> FunctionFactory<CTX, CycleBlockPropertyFunction<CTX>> factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
        return new Factory<>(factory);
    }

    private static class Factory<CTX extends Context> extends AbstractFactory<CTX, CycleBlockPropertyFunction<CTX>> {
        private static final String[] UPDATE_FLAGS = new String[]{"update_flags", "update-flags"};

        public Factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
            super(factory);
        }

        @Override
        public CycleBlockPropertyFunction<CTX> create(ConfigSection section) {
            ConfigSection rulesSection = section.getSection("rules");
            Map<String, String> rules = null;
            if (rulesSection != null) {
                rules = new HashMap<>();
                for (String key : rulesSection.keySet()) {
                    rules.put(key, rulesSection.getNonEmptyString(key));
                }
            }
            return new CycleBlockPropertyFunction<>(
                    getPredicates(section),
                    section.getNonNullString("property"),
                    rules,
                    section.getNumber("inverse", ConfigConstants.IS_SNEAKING),
                    section.getNumber("x", ConfigConstants.POSITION_X),
                    section.getNumber("y", ConfigConstants.POSITION_Y),
                    section.getNumber("z", ConfigConstants.POSITION_Z),
                    section.getNumber(UPDATE_FLAGS, NumberProviders.direct(UpdateFlags.UPDATE_ALL))
            );
        }
    }
}