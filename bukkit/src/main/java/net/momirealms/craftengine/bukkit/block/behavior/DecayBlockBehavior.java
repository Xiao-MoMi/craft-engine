package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.block.BlockStateWrapper;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.UpdateFlags;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.behavior.RandomTickBlock;
import net.momirealms.craftengine.core.block.property.IntegerProperty;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.plugin.context.number.ConstantNumberProvider;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.util.LazyReference;
import net.momirealms.craftengine.core.util.random.ThreadLocalRandomSource;
import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.core.DirectionProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelAccessorProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelReaderProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelWriterProxy;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public final class DecayBlockBehavior extends BukkitBlockBehavior implements RandomTickBlock {
    public static final BlockBehaviorFactory<DecayBlockBehavior> FACTORY = new Factory();
    public final IntegerProperty ageProperty;
    public final @Nullable NumberProvider delay;
    public final int requiredLight;
    public final boolean hasRequiredLight;
    public final LazyReference<BlockStateWrapper> decayInto;
    public final boolean useRandomTick;

    private DecayBlockBehavior(
            BlockDefinition blockDefinition,
            IntegerProperty ageProperty,
            @Nullable NumberProvider delay,
            int requiredLight,
            LazyReference<BlockStateWrapper> decayInto
    ) {
        super(blockDefinition);
        this.ageProperty = ageProperty;
        this.delay = delay;
        this.requiredLight = requiredLight;
        this.hasRequiredLight = requiredLight > 0;
        this.decayInto = decayInto;
        this.useRandomTick = delay == null;
    }

    @SuppressWarnings("DataFlowIssue")
    @Override
    public void onPlace(Object thisBlock, Object[] args) {
        if (this.useRandomTick) return;
        LevelAccessorProxy.INSTANCE.scheduleTick$0(args[1], args[2], thisBlock, this.delay.getInt(ThreadLocalRandomSource.INSTANCE));
    }

    @SuppressWarnings("DataFlowIssue")
    @Override
    public void tick(Object thisBlock, Object[] args) {
        if (this.useRandomTick) return;
        Object level = args[1];
        Object pos = args[2];
        ImmutableBlockState blockState = BlockStateUtils.getNullableCustomBlockState(args[0]);
        if (blockState == null || blockState.isEmpty()) {
            LevelAccessorProxy.INSTANCE.scheduleTick$0(level, pos, thisBlock, this.delay.getInt(ThreadLocalRandomSource.INSTANCE));
            return;
        }
        if (this.hasRequiredLight && getLight(level, pos) < this.requiredLight) {
            LevelAccessorProxy.INSTANCE.scheduleTick$0(level, pos, thisBlock, this.delay.getInt(ThreadLocalRandomSource.INSTANCE));
            return;
        }
        int age = blockState.get(this.ageProperty);
        if (age < this.ageProperty.max) {
            LevelWriterProxy.INSTANCE.setBlock(level, pos, blockState.with(this.ageProperty, age + 1).customBlockState().minecraftState(), UpdateFlags.UPDATE_CLIENTS);
            LevelAccessorProxy.INSTANCE.scheduleTick$0(level, pos, thisBlock, this.delay.getInt(ThreadLocalRandomSource.INSTANCE));
        } else {
            LevelWriterProxy.INSTANCE.setBlock(level, pos, this.decayInto.get().minecraftState(), UpdateFlags.UPDATE_ALL);
        }
    }

    @Override
    public void randomTick(Object thisBlock, Object[] args) {
        if (!this.useRandomTick) return;
        Object level = args[1];
        Object pos = args[2];
        ImmutableBlockState blockState = BlockStateUtils.getNullableCustomBlockState(args[0]);
        if (blockState == null || blockState.isEmpty()) return;
        if (this.hasRequiredLight && getLight(level, pos) < this.requiredLight) return;
        int age = blockState.get(this.ageProperty);
        if (age < this.ageProperty.max) {
            LevelWriterProxy.INSTANCE.setBlock(level, pos, blockState.with(this.ageProperty, age + 1).customBlockState().minecraftState(), UpdateFlags.UPDATE_CLIENTS);
        } else {
            LevelWriterProxy.INSTANCE.setBlock(level, pos, this.decayInto.get().minecraftState(), UpdateFlags.UPDATE_ALL);
        }
    }

    @Override
    public boolean canRandomlyTick(ImmutableBlockState state) {
        return this.useRandomTick;
    }

    private static int getLight(Object level, Object pos) {
        int light = LevelReaderProxy.INSTANCE.getMaxLocalRawBrightness(level, pos);
        if (light == 15) return 15;
        Object blockPos = BlockPosProxy.MutableBlockPosProxy.INSTANCE.newInstance();
        for (Enum<?> direction : DirectionProxy.VALUES) {
            BlockPosProxy.MutableBlockPosProxy.INSTANCE.setWithOffset(blockPos, pos, direction);
            int targetLight = LevelReaderProxy.INSTANCE.getMaxLocalRawBrightness(level, blockPos);
            if (targetLight == 15) return 15;
            if (targetLight > light) light = targetLight;
        }
        return light;
    }

    private static class Factory implements BlockBehaviorFactory<DecayBlockBehavior> {
        private static final String[] REQUIRED_LIGHT = new String[]{"required_light", "required-light"};
        private static final String[] DECAY_INTO = new String[]{"decay_into", "decay-into"};

        @Override
        public DecayBlockBehavior create(BlockDefinition block, ConfigSection section) {
            String decayInto = section.getString(DECAY_INTO, "air");
            ConfigValue value = section.getValue("delay");
            NumberProvider delay = null;
            if (value != null) {
                NumberProvider number = value.getAsNumber();
                if (!(number instanceof ConstantNumberProvider(double v))) {
                    delay = number;
                } else if (v > 0) {
                    delay = number;
                }
            }
            return new DecayBlockBehavior(
                    block,
                    (IntegerProperty) BlockBehaviorFactory.getProperty(section.path(), block, "age", Integer.class),
                    delay,
                    section.getInt(REQUIRED_LIGHT, 0),
                    LazyReference.lazyReference(() -> Objects.requireNonNull(CraftEngine.instance().blockManager().createBlockState(decayInto), decayInto))
            );
        }
    }
}
