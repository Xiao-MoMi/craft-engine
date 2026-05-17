package net.momirealms.craftengine.core.plugin.context.function;

import net.momirealms.craftengine.core.block.BlockStateWrapper;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.LazyReference;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.Tag;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class TransformBlockFunction<CTX extends Context> extends AbstractConditionalFunction<CTX> {
    private final LazyReference<BlockStateWrapper> lazyBlockState;
    private final CompoundTag properties;
    private final NumberProvider x;
    private final NumberProvider y;
    private final NumberProvider z;
    private final NumberProvider updateFlags;

    private TransformBlockFunction(List<Condition<CTX>> predicates,
                                   CompoundTag properties,
                                   NumberProvider x,
                                   NumberProvider y,
                                   NumberProvider z,
                                   NumberProvider updateFlags,
                                   LazyReference<BlockStateWrapper> lazyBlockState) {
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

    public static <CTX extends Context> FunctionFactory<CTX, TransformBlockFunction<CTX>> factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
        return new Factory<>(factory);
    }

    private static class Factory<CTX extends Context> extends AbstractFactory<CTX, TransformBlockFunction<CTX>> {
        private static final String[] UPDATE_FLAGS = new String[] {"update_flags", "update-flags"};

        public Factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
            super(factory);
        }

        @Override
        public TransformBlockFunction<CTX> create(ConfigSection section) {
            String block = section.getNonNullString("block");
            CompoundTag properties = new CompoundTag();
            ConfigSection propertiesSection = section.getSection("properties");
            if (propertiesSection != null) {
                for (String key : propertiesSection.keySet()) {
                    properties.putString(key, propertiesSection.getNonEmptyString(key));
                }
            }
            return new TransformBlockFunction<>(
                    getPredicates(section),
                    properties,
                    section.getNumber("x", ConfigConstants.POSITION_X),
                    section.getNumber("y", ConfigConstants.POSITION_Y),
                    section.getNumber("z", ConfigConstants.POSITION_Z),
                    section.getNumber(UPDATE_FLAGS, ConfigConstants.UPDATE_ALL),
                    LazyReference.lazyReference(() -> CraftEngine.instance().blockManager().createBlockState(block))
            );
        }
    }
}