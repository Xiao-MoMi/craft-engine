package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.sound.SoundData;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.sounds.SoundSourceProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelAccessorProxy;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

public class ChimeBlockBehavior extends BukkitBlockBehavior {
    public static final BlockBehaviorFactory<ChimeBlockBehavior> FACTORY = new Factory();
    private final SoundData hitSound;

    public ChimeBlockBehavior(CustomBlock customBlock, SoundData hitSound) {
        super(customBlock);
        this.hitSound = hitSound;
    }

    @Override
    public void onProjectileHit(Object thisBlock, Object[] args, Callable<Object> superMethod) {
        Object blockPos = FastNMS.INSTANCE.field$BlockHitResult$blockPos(args[2]);
        Object sound = FastNMS.INSTANCE.constructor$SoundEvent(KeyUtils.toIdentifier(hitSound.id()), Optional.empty());
        if (VersionHelper.isOrAbove1_21_5()) {
            LevelAccessorProxy.INSTANCE.playSound$0(args[0], null, blockPos, sound, SoundSourceProxy.BLOCKS, hitSound.volume().get(), hitSound.pitch().get());
        } else {
            LevelAccessorProxy.INSTANCE.playSound$1(args[0], null, blockPos, sound, SoundSourceProxy.BLOCKS, hitSound.volume().get(), hitSound.pitch().get());
        }
    }

    private static class Factory implements BlockBehaviorFactory<ChimeBlockBehavior> {

        @Override
        public ChimeBlockBehavior create(CustomBlock block, Map<String, Object> arguments) {
            SoundData hitSound = SoundData.create(ResourceConfigUtils.requireNonNullOrThrow(
                    Optional.ofNullable(ResourceConfigUtils.getAsMap(arguments.get("sounds"), "sounds"))
                            .map(sounds -> ResourceConfigUtils.get(sounds, "projectile-hit", "chime"))
                            .orElse(null),
                    "warning.config.block.behavior.chime.missing_sounds_projectile_hit"
            ), SoundData.SoundValue.FIXED_1, SoundData.SoundValue.ranged(0.9f, 1f));
            return new ChimeBlockBehavior(block, hitSound);
        }
    }
}
