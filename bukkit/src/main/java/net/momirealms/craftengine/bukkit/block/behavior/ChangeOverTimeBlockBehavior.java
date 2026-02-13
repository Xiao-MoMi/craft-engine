package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.core.block.BlockStateWrapper;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.UpdateFlags;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.LazyReference;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.util.random.RandomUtils;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.event.CraftEventFactoryProxy;
import net.momirealms.sparrow.nbt.CompoundTag;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public class ChangeOverTimeBlockBehavior extends BukkitBlockBehavior {
    public static final BlockBehaviorFactory<ChangeOverTimeBlockBehavior> FACTORY = new Factory();
    private final float changeSpeed;
    private final String nextBlock;
    private final LazyReference<BlockStateWrapper> lazyState;
    private final List<String> excludedProperties;

    public ChangeOverTimeBlockBehavior(CustomBlock customBlock, float changeSpeed, String nextBlock, List<String> excludedProperties) {
        super(customBlock);
        this.changeSpeed = changeSpeed;
        this.nextBlock = nextBlock;
        this.excludedProperties = excludedProperties;
        this.lazyState = LazyReference.lazyReference(() -> CraftEngine.instance().blockManager().createBlockState(this.nextBlock));
    }

    public String nextBlock() {
        return this.nextBlock;
    }

    public BlockStateWrapper nextState() {
        return this.lazyState.get();
    }

    public CompoundTag filter(CompoundTag properties) {
        for (String property : this.excludedProperties) {
            properties.remove(property);
        }
        return properties;
    }

    @Override
    public void randomTick(Object thisBlock, Object[] args, Callable<Object> superMethod) {
        if (RandomUtils.generateRandomFloat(0F, 1F) >= this.changeSpeed) return;
        Object blockState = args[0];
        BlockStateUtils.getOptionalCustomBlockState(blockState).ifPresent(state -> {
            BlockStateWrapper nextState = this.nextState();
            if (nextState == null) return;
            nextState = nextState.withProperties(filter(state.propertiesNbt()));
            CraftEventFactoryProxy.INSTANCE.handleBlockFormEvent(args[1], args[2], nextState.literalObject(), UpdateFlags.UPDATE_ALL);
        });
    }

    private static class Factory implements BlockBehaviorFactory<ChangeOverTimeBlockBehavior> {

        @Override
        public ChangeOverTimeBlockBehavior create(CustomBlock block, Map<String, Object> arguments) {
            float changeSpeed = ResourceConfigUtils.getAsFloat(arguments.getOrDefault("change-speed", 0.05688889F), "change-speed");
            String nextBlock = ResourceConfigUtils.requireNonEmptyStringOrThrow(arguments.getOrDefault("next-block", "minecraft:air"), "warning.config.block.behavior.change_over_time.missing_next_block");
            List<String> excludedProperties = MiscUtils.getAsStringList(arguments.get("excluded-properties"));
            return new ChangeOverTimeBlockBehavior(block, changeSpeed, nextBlock, excludedProperties);
        }
    }
}
