package net.momirealms.craftengine.core.plugin.context.function;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.momirealms.craftengine.core.block.BlockStateWrapper;
import net.momirealms.craftengine.core.block.UpdateFlags;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.plugin.context.number.NumberProviders;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.WorldPosition;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class CycleBlockPropertyFunction<CTX extends Context> extends AbstractConditionalFunction<CTX> {
    private final String property;
    @Nullable
    private final Map<String, String> rules;
    @Nullable
    private final NumberProvider inverse;
    private final NumberProvider x;
    private final NumberProvider y;
    private final NumberProvider z;
    private final NumberProvider updateFlags;

    public CycleBlockPropertyFunction(
            List<Condition<CTX>> predicates,
            String property,
            @Nullable Map<String, String> rules,
            @Nullable NumberProvider inverse,
            NumberProvider x,
            NumberProvider y,
            NumberProvider z,
            NumberProvider updateFlags
    ) {
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

    public static <CTX extends Context> FunctionFactory<CTX, CycleBlockPropertyFunction<CTX>> factory(java.util.function.Function<Map<String, Object>, Condition<CTX>> factory) {
        return new Factory<>(factory);
    }

    private static class Factory<CTX extends Context> extends AbstractFactory<CTX, CycleBlockPropertyFunction<CTX>> {

        public Factory(java.util.function.Function<Map<String, Object>, Condition<CTX>> factory) {
            super(factory);
        }

        @Override
        public CycleBlockPropertyFunction<CTX> create(Map<String, Object> arguments) {
            String property = ResourceConfigUtils.requireNonEmptyStringOrThrow(arguments.get("property"), "warning.config.function.cycle_block_property.missing_property");
            Map<String, String> rules;
            if (arguments.containsKey("rules")) {
                rules = new Object2ObjectOpenHashMap<>();
                MiscUtils.castToMap(arguments.get("rules"), false).forEach((k, v) -> rules.put(k, v.toString()));
            } else rules = null;
            return new CycleBlockPropertyFunction<>(getPredicates(arguments),
                    property,
                    rules,
                    NumberProviders.fromObject(arguments.getOrDefault("inverse", "<arg:player.is_sneaking>")),
                    NumberProviders.fromObject(arguments.getOrDefault("x", "<arg:position.x>")),
                    NumberProviders.fromObject(arguments.getOrDefault("y", "<arg:position.y>")),
                    NumberProviders.fromObject(arguments.getOrDefault("z", "<arg:position.z>")),
                    Optional.ofNullable(arguments.get("update-flags")).map(NumberProviders::fromObject).orElse(NumberProviders.direct(UpdateFlags.UPDATE_ALL)));
        }
    }
}