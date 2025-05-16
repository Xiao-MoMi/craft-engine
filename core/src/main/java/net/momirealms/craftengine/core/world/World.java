package net.momirealms.craftengine.core.world;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.sound.SoundData;
import net.momirealms.craftengine.core.util.Key;

import java.nio.file.Path;
import java.util.UUID;

public interface World {

    Object platformWorld();

    Object serverWorld();

    WorldHeight worldHeight();

    BlockInWorld getBlockAt(int x, int y, int z);

    default BlockInWorld getBlockAt(final BlockPos pos) {
        return getBlockAt(pos.x(), pos.y(), pos.z());
    }

    String name();

    Path directory();

    UUID uuid();

    void dropItemNaturally(Position location, Item<?> item);

    void dropExp(Position location, int amount);

    void playBlockSound(Position location, Key sound, float volume, float pitch);

    default void playBlockSound(Position location, SoundData data) {
        playBlockSound(location, data.id(), data.volume(), data.pitch());
    }

    long time();
}
