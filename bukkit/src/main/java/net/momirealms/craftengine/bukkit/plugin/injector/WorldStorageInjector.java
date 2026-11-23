package net.momirealms.craftengine.bukkit.plugin.injector;

import net.momirealms.craftengine.bukkit.block.entity.renderer.display.BukkitDestroyStageDisplayRecorder;
import net.momirealms.craftengine.bukkit.util.LightUtils;
import net.momirealms.craftengine.core.block.BlockStateWrapper;
import net.momirealms.craftengine.core.block.DelegatingBlockState;
import net.momirealms.craftengine.core.block.EmptyBlockDefinition;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.entity.BlockEntity;
import net.momirealms.craftengine.core.block.entity.render.ConstantBlockEntityRenderer;
import net.momirealms.craftengine.core.block.entity.render.display.DestroyStageDisplayEntitySetting;
import net.momirealms.craftengine.core.block.entity.render.display.DestroyStageDisplayRecorder;
import net.momirealms.craftengine.core.block.setting.BlockSettings;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.util.SectionPosUtils;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.SectionPos;
import net.momirealms.craftengine.core.world.chunk.CEChunk;
import net.momirealms.craftengine.core.world.chunk.CESection;
import net.momirealms.craftengine.core.world.chunk.InjectedStorage;
import net.momirealms.craftengine.proxy.minecraft.world.level.chunk.LevelChunkSectionProxy;

import java.util.function.Consumer;

public final class WorldStorageInjector {
    private WorldStorageInjector() {
    }

    // 注入某个section，根据配置文件里的注册目标对象而定
    // callback负责是否需要将注入对象设置回去
    public static void inject(Object nmsSection,
                              CESection ceSection,
                              CEChunk ceChunk,
                              SectionPos pos,
                              Consumer<Object> callback) {
        InjectedStorage storage;
        if (Config.injectPaletteOrSection()) {
            Object container = LevelChunkSectionProxy.INSTANCE.getStates(nmsSection);
            if (container instanceof InjectedStorage.Palette holder) {
                storage = holder;
            } else {
                storage = ChunkStorageAdapters.createPalette(container);
                LevelChunkSectionProxy.INSTANCE.setStates(nmsSection, storage);
            }
        } else if (nmsSection instanceof InjectedStorage.Section holder) {
            storage = holder;
        } else {
            storage = ChunkStorageAdapters.createSection(nmsSection);
            callback.accept(storage);
        }
        storage.setChunk(ceChunk);
        storage.setSection(ceSection);
        storage.setPos(pos);
        storage.setActive(true);
    }

    public static boolean isSectionInjected(Object section) {
        if (Config.injectPaletteOrSection()) {
            Object container = LevelChunkSectionProxy.INSTANCE.getStates(section);
            return container instanceof InjectedStorage.Palette;
        } else {
            return section instanceof InjectedStorage.Section;
        }
    }

    public static void uninject(Object section) {
        if (Config.injectPaletteOrSection()) {
            Object states = LevelChunkSectionProxy.INSTANCE.getStates(section);
            if (states instanceof InjectedStorage.Palette holder) {
                holder.setActive(false);
            }
        } else if (section instanceof InjectedStorage.Section holder) {
            holder.setActive(false);
        }
    }

    public static void compareAndUpdateBlockState(int x, int y, int z, Object newState, Object previousState, InjectedStorage holder) {
        CESection section = holder.section();
        if (newState instanceof DelegatingBlockState custom) {
            ImmutableBlockState next = custom.blockState();
            if (next == null) {
                return;
            }
            ImmutableBlockState previous = section.setBlockState(x, y, z, next);
            if (previous != next) {
                updateCustomBlock(holder, section, x, y, z, previous, next, previousState, newState);
            }
        } else {
            ImmutableBlockState previous = section.setBlockState(x, y, z, EmptyBlockDefinition.STATE);
            if (previous != null && !previous.isEmpty()) {
                removeCustomBlock(holder, section, x, y, z, previous, previousState);
            }
        }
    }

    private static void updateCustomBlock(InjectedStorage holder, CESection section, int x, int y, int z,
                                          ImmutableBlockState previous, ImmutableBlockState next, Object previousState, Object newState) {
        CEChunk chunk = holder.chunk();
        chunk.setUnsaved(true);
        int blockX = chunk.chunkPos.x * 16 + x;
        int blockY = section.sectionY * 16 + y;
        int blockZ = chunk.chunkPos.z * 16 + z;
        BlockSettings settings = previous.settings();
        if (settings != null && settings.destroyStageDisplay() != null) {
            DestroyStageDisplayRecorder.PosKey key = new DestroyStageDisplayRecorder.PosKey(chunk.world.uuid(), BlockPos.asLong(blockX, blockY, blockZ));
            DestroyStageDisplayEntitySetting stages = next.settings().destroyStageDisplay();
            if (stages == null) {
                BukkitDestroyStageDisplayRecorder.INSTANCE.remove(key);
            } else {
                BukkitDestroyStageDisplayRecorder.INSTANCE.swap(key, stages);
            }
        }

        boolean removeEntity = !previous.isEmpty() && previous.owner() != next.owner() && previous.hasBlockEntity();
        boolean removeRenderer = !previous.isEmpty() && previous.hasConstantBlockEntityRenderer();
        if (removeEntity || removeRenderer || next.hasBlockEntity() || next.hasConstantBlockEntityRenderer()) {
            BlockPos pos = new BlockPos(blockX, blockY, blockZ);
            if (removeEntity) {
                removeBlockEntity(chunk, pos, chunk.isLoaded());
            }
            // Keep the old renderer available when the replacement also has one.
            ConstantBlockEntityRenderer renderer = removeRenderer
                    ? chunk.removeConstantBlockEntityRenderer(pos, !next.hasConstantBlockEntityRenderer())
                    : null;
            if (next.hasBlockEntity()) {
                updateBlockEntity(chunk, pos, next);
            }
            if (next.hasConstantBlockEntityRenderer()) {
                chunk.addConstantBlockEntityRenderer(pos, next, renderer);
            }
        }

        if (Config.enableBlockLightSystem() && chunk.isLoaded()) {
            Object visual = next.visualBlockState().minecraftState();
            boolean update = previous.isEmpty()
                    ? LightUtils.hasDifferentLightProperties(newState, visual)
                    : (LightUtils.hasDifferentLightProperties(visual, newState) || LightUtils.hasDifferentLightProperties(newState, previousState));
            if (update) {
                notifyLightChanged(chunk, holder.pos(), x, y, z);
            }
        }
    }

    private static void removeCustomBlock(InjectedStorage holder,
                                          CESection section,
                                          int x, int y, int z,
                                          ImmutableBlockState previous,
                                          Object previousState) {
        CEChunk chunk = holder.chunk();
        chunk.setUnsaved(true);
        int blockX = chunk.chunkPos.x * 16 + x;
        int blockY = section.sectionY * 16 + y;
        int blockZ = chunk.chunkPos.z * 16 + z;
        if (previous.hasBlockEntity() || previous.hasConstantBlockEntityRenderer()) {
            BlockPos pos = new BlockPos(blockX, blockY, blockZ);
            if (previous.hasBlockEntity()) {
                removeBlockEntity(chunk, pos, true);
            }
            if (previous.hasConstantBlockEntityRenderer()) {
                chunk.removeConstantBlockEntityRenderer(pos);
            }
        }
        BlockSettings settings = previous.settings();
        if (settings != null && settings.destroyStageDisplay() != null) {
            BukkitDestroyStageDisplayRecorder.INSTANCE.remove(new DestroyStageDisplayRecorder.PosKey(chunk.world.uuid(), BlockPos.asLong(blockX, blockY, blockZ)));
        }
        if (Config.enableBlockLightSystem() && chunk.isLoaded()) {
            BlockStateWrapper visual = previous.visualBlockState();
            if (visual != null && LightUtils.hasDifferentLightProperties(previousState, visual.minecraftState())) {
                notifyLightChanged(chunk, holder.pos(), x, y, z);
            }
        }
    }

    private static void removeBlockEntity(CEChunk chunk, BlockPos pos, boolean runPreRemove) {
        BlockEntity entity = chunk.getBlockEntity(pos, false);
        if (entity == null) {
            return;
        }
        if (runPreRemove) {
            try {
                entity.preRemove();
            } catch (Throwable t) {
                CraftEngine.instance().logger().warn("Error removing block entity " + entity.getClass().getName(), t);
            }
        }
        chunk.removeBlockEntity(pos);
    }

    private static void updateBlockEntity(CEChunk chunk, BlockPos pos, ImmutableBlockState state) {
        BlockEntity entity = chunk.getBlockEntity(pos, false);
        if (entity == null) {
            chunk.addBlockEntity(new BlockEntity(pos, state));
        } else {
            entity.setBlockState(state);
            if (chunk.isActivated()) {
                chunk.replaceOrCreateTickingBlockEntity(entity);
                chunk.createDynamicBlockEntityRenderer(entity);
            }
        }
    }

    private static void notifyLightChanged(CEChunk chunk, SectionPos section, int x, int y, int z) {
        chunk.world.sectionLightUpdated(SectionPosUtils.calculateAffectedRegions(section.minBlockX() + x, section.minBlockY() + y, section.minBlockZ() + z, 15));
    }
}
