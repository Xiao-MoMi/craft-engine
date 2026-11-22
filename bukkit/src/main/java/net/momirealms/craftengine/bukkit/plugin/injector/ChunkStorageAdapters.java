package net.momirealms.craftengine.bukkit.plugin.injector;

import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.chunk.InjectedStorage;
import net.momirealms.craftengine.proxy.minecraft.world.level.chunk.LevelChunkSectionProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.chunk.PalettedContainerProxy;
import net.momirealms.sparrow.reflection.SReflection;

final class ChunkStorageAdapters {
    private ChunkStorageAdapters() {
    }

    static InjectedStorage.Palette createPalette(Object container) {
        PalettedContainerProxy access = PalettedContainerProxy.INSTANCE;
        if (VersionHelper.isOrAbove1_21_9 && container.getClass() == PalettedContainerProxy.CLASS) {
            InjectedStorage.Palette palette = ChunkStorageGenerator.createNativePalette(access.getByIndex(container, 0), access.getStrategy(container), access.getPresetValues(container));
            access.setData(palette, access.getData(container));
            return palette;
        }

        ChunkStorageGenerator.PaletteBridge palette = SReflection.allocateInstance(ChunkStorageGenerator.paletteClass());
        palette.setDelegate(container);
        access.setData(palette, access.getData(container));
        access.setStrategy(palette, access.getStrategy(container));
        if (!VersionHelper.isOrAbove1_21_9) {
            access.setRegistry(palette, access.getRegistry(container));
        }
        return palette;
    }

    static InjectedStorage.Section createSection(Object section) {
        LevelChunkSectionProxy access = LevelChunkSectionProxy.INSTANCE;
        InjectedStorage.Section generated = ChunkStorageGenerator.createSection(access.getStates(section), access.getBiomes(section));
        access.setNonEmptyBlockCount(generated, access.getNonEmptyBlockCount(section));
        access.setTickingBlockCount(generated, access.getTickingBlockCount(section));
        access.setTickingFluidCount(generated, access.getTickingFluidCount(section));
        access.setTickingBlocks(generated, access.getTickingBlocks(section));
        if (VersionHelper.isOrAbove1_21_2) {
            access.setSpecialCollidingBlocks(generated, access.getSpecialCollidingBlocks(section));
        } else {
            access.setSpecialCollidingBlocks$legacy(generated, access.getSpecialCollidingBlocks$legacy(section));
        }
        if (!VersionHelper.isOrAbove1_20_2) {
            access.setKnownBlockCollisionData(generated, access.getKnownBlockCollisionData(section));
        }

        access.setFluidCount(generated, access.getFluidCount(section));
        access.setLavaFluids(generated, access.getLavaFluids(section));
        access.setWaterFluids(generated, access.getWaterFluids(section));
        access.setHasOnlyAir(generated, access.getHasOnlyAir(section));
        access.setIsRandomlyTickingBlocksStatus(generated, access.isRandomlyTickingBlocksStatus(section));
        return generated;
    }

    static Object getByIndex(Object container, int index) {
        return PalettedContainerProxy.INSTANCE.getByIndex(container, index);
    }

    static void syncPaletteData(InjectedStorage.Palette palette) {
        PalettedContainerProxy access = PalettedContainerProxy.INSTANCE;
        Object delegate = palette.delegated();
        if (access.getData(delegate) != access.getData(palette)) {
            synchronized (palette) {
                access.setData(palette, access.getData(delegate));
            }
        }
    }
}
