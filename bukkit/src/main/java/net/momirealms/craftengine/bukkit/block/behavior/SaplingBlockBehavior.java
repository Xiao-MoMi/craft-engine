package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.util.BlockTags;
import net.momirealms.craftengine.bukkit.util.Reflections;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.properties.Property;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.shared.block.BlockBehavior;
import org.bukkit.Bukkit;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

public class SaplingBlockBehavior extends BushBlockBehavior {
    public static final Factory FACTORY = new Factory();
    private final Property<Integer> stageProperty;
    private final Object treeGrower;

    public SaplingBlockBehavior(Object treeGrower, List<Object> tagsCanSurviveOn, Property<Integer> stageProperty) {
        super(tagsCanSurviveOn);
        this.stageProperty = stageProperty;
        this.treeGrower = treeGrower;
    }

    public int getStage(ImmutableBlockState state) {
        return state.get(this.stageProperty);
    }

    @Override
    public void randomTick(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        Object blockState = args[0];
        Object level = args[1];
        Object blockPos = args[2];
        Object random = args[3];
        int maxLocalRawBrightness = (int) Reflections.method$LevelReader$getMaxLocalRawBrightness.invoke(level, Reflections.method$BlockPos$above.invoke(blockPos));
        float nextFloat = (float) Reflections.method$RandomSource$nextFloat.invoke(random);
        Object spigotConfig = Reflections.field$Level$spigotConfig.get(level);
        float saplingModifier = (float) Reflections.field$SpigotWorldConfig$saplingModifier.get(spigotConfig) / 700.0F;
        if (maxLocalRawBrightness >= 9 && nextFloat < saplingModifier) {
            advanceTree(level, blockPos, blockState, random);
        }
    }

    public void advanceTree(Object level, Object pos, Object state, Object random) throws Exception {
        if ((Integer) Reflections.method$StateHolder$getValue.invoke(state, stageProperty) == 0) {
            Reflections.method$Level$setBlock.invoke(level, pos, Reflections.method$StateHolder$cycle.invoke(state, stageProperty), 4);
        } else if ((boolean) Reflections.field$Level$captureTreeGeneration.get(level)) {
            Object chunkSource = Reflections.method$ServerLevel$getChunkSource.invoke(level);
            Object generator = Reflections.method$ServerChunkCache$getGenerator.invoke(chunkSource);
            Reflections.method$TreeGrower$growTree.invoke(this.treeGrower, level, generator, pos, state, random);
        } else {
            Reflections.field$Level$captureTreeGeneration.set(level, true);
            Object chunkSource = Reflections.method$ServerLevel$getChunkSource.invoke(level);
            Object generator = Reflections.method$ServerChunkCache$getGenerator.invoke(chunkSource);
            Reflections.method$TreeGrower$growTree.invoke(this.treeGrower, level, generator, pos, state, random);
            Reflections.field$Level$captureTreeGeneration.set(level, false);
        }
    }

    @SuppressWarnings("unchecked")
    public static class Factory implements BlockBehaviorFactory {
        @Override
        public BlockBehavior create(CustomBlock block, Map<String, Object> arguments) {
            if (arguments.containsKey("tags")) {
                Property<Integer> stageProperty = (Property<Integer>) block.getProperty("stage");
                if (stageProperty == null) {
                    throw new NullPointerException("stage property not set for block " + block.id());
                }
                List<Object> tagsCanSurviveOn = MiscUtils.getAsStringList(arguments.get("tags")).stream().map(it -> BlockTags.getOrCreate(Key.of(it))).toList();
                try {
                    Object ACACIA = Reflections.field$TreeGrower$ACACIA.get(null);
                    return new SaplingBlockBehavior(ACACIA, tagsCanSurviveOn, stageProperty);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                return INSTANCE;
            }
        }
    }
}
