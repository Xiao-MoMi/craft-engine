package net.momirealms.craftengine.bukkit.plugin.injector;

import net.bytebuddy.implementation.bind.annotation.This;
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
import net.momirealms.craftengine.core.world.CEWorld;
import net.momirealms.craftengine.core.world.SectionPos;
import net.momirealms.craftengine.core.world.chunk.CEChunk;
import net.momirealms.craftengine.core.world.chunk.CESection;
import net.momirealms.craftengine.core.world.chunk.InjectedStorage;
import net.momirealms.craftengine.proxy.minecraft.world.level.chunk.LevelChunkSectionProxy;

import java.util.List;
import java.util.function.Consumer;

public final class WorldStorageInjector {
    private WorldStorageInjector() {}

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
        } else {
            if (nmsSection instanceof InjectedStorage.Section holder) {
                storage = holder;
            } else {
                storage = ChunkStorageAdapters.createSection(nmsSection);
                callback.accept(storage);
            }
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
        } else {
            if (section instanceof InjectedStorage.Section holder) {
                holder.setActive(false);
            }
        }
    }

    @SuppressWarnings("DuplicatedCode")
    public static void compareAndUpdateBlockState(int x, int y, int z, Object newState, Object previousState, InjectedStorage holder) {
        CESection section = holder.section();
        if (newState instanceof DelegatingBlockState delegatingBlockState) {
            ImmutableBlockState newImmutableBlockState = delegatingBlockState.blockState();
            if (newImmutableBlockState == null) return;
            ImmutableBlockState previousImmutableBlockState = section.setBlockState(x, y, z, newImmutableBlockState);
            if (previousImmutableBlockState == newImmutableBlockState) return;
            // 处理  自定义块到自定义块或原版块到自定义块
            CEChunk chunk = holder.chunk();
            chunk.setUnsaved(true);
            // 尽量还是减少判断逻辑，性能为上，允许存在小幅破坏进度不同步
            BlockSettings settings = previousImmutableBlockState.settings();
            if (settings != null) {
                DestroyStageDisplayEntitySetting destroyStages = settings.destroyStageDisplay();
                if (destroyStages != null) {
                    BlockPos pos = new BlockPos(chunk.chunkPos.x * 16 + x, section.sectionY * 16 + y, chunk.chunkPos.z * 16 + z);
                    DestroyStageDisplayRecorder.PosKey key = new DestroyStageDisplayRecorder.PosKey(chunk.world.uuid(), pos.asLong());
                    DestroyStageDisplayEntitySetting newDestroyStages = newImmutableBlockState.settings().destroyStageDisplay();
                    if (newDestroyStages == null) {
                        BukkitDestroyStageDisplayRecorder.INSTANCE.remove(key);
                    } else {
                        BukkitDestroyStageDisplayRecorder.INSTANCE.swap(key, newDestroyStages);
                    }
                }
            }
            ConstantBlockEntityRenderer previousRenderer = null;
            // 如果两个方块没有相同的主人 且 旧方块有方块实体
            if (!previousImmutableBlockState.isEmpty()) {
                if (previousImmutableBlockState.owner() != newImmutableBlockState.owner() && previousImmutableBlockState.hasBlockEntity()) {
                    BlockPos pos = new BlockPos(chunk.chunkPos.x * 16 + x, section.sectionY * 16 + y, chunk.chunkPos.z * 16 + z);
                    BlockEntity blockEntity = chunk.getBlockEntity(pos, false);
                    if (blockEntity != null) {
                        try {
                            if (chunk.isLoaded()) {
                                blockEntity.preRemove();
                            }
                        } catch (Throwable t) {
                            CraftEngine.instance().logger().warn("Error removing block entity " + blockEntity.getClass().getName(), t);
                        }
                        chunk.removeBlockEntity(pos);
                    }
                }
                if (previousImmutableBlockState.hasConstantBlockEntityRenderer()) {
                    BlockPos pos = new BlockPos(chunk.chunkPos.x * 16 + x, section.sectionY * 16 + y, chunk.chunkPos.z * 16 + z);
                    // 如果新状态没有entity renderer，那么直接移除，否则暂存
                    previousRenderer = chunk.removeConstantBlockEntityRenderer(pos, !newImmutableBlockState.hasConstantBlockEntityRenderer());
                }
            }

            if (newImmutableBlockState.hasBlockEntity()) {
                BlockPos pos = new BlockPos(chunk.chunkPos.x * 16 + x, section.sectionY * 16 + y, chunk.chunkPos.z * 16 + z);
                BlockEntity blockEntity = chunk.getBlockEntity(pos, false);
                if (blockEntity == null) {
                    // 如果新状态有方块实体
                    blockEntity = new BlockEntity(pos, newImmutableBlockState);
                    chunk.addBlockEntity(blockEntity);
                } else {
                    blockEntity.setBlockState(newImmutableBlockState);
                    // 方块类型未变，仅更新状态，选择性更新ticker
                    if (chunk.isActivated()) {
                        chunk.replaceOrCreateTickingBlockEntity(blockEntity);
                        chunk.createDynamicBlockEntityRenderer(blockEntity);
                    }
                }
            }

            // 处理新老entity renderer更替
            if (newImmutableBlockState.hasConstantBlockEntityRenderer()) {
                BlockPos pos = new BlockPos(chunk.chunkPos.x * 16 + x, section.sectionY * 16 + y, chunk.chunkPos.z * 16 + z);
                chunk.addConstantBlockEntityRenderer(pos, newImmutableBlockState, previousRenderer);
            }

            // 如果新方块的光照属性和客户端认为的不同
            if (Config.enableBlockLightSystem() && chunk.isLoaded()) {
                if (previousImmutableBlockState.isEmpty()) {
                    // 原版块到自定义块，只需要判断新块是否和客户端视觉一致
                    updateLight(holder, newImmutableBlockState.visualBlockState().minecraftState(), newState, x, y, z);
                } else {
                    // 自定义块到自定义块
                    updateLight$complex(holder, newImmutableBlockState.visualBlockState().minecraftState(), newState, previousState, x, y, z);
                }
            }
        } else {
            // 如果是原版方块
            // 那么应该清空自定义块
            ImmutableBlockState previous = section.setBlockState(x, y, z, EmptyBlockDefinition.STATE);
            // 处理  自定义块 -> 原版块
            if (previous != null && !previous.isEmpty()) {
                CEChunk chunk = holder.chunk();
                chunk.setUnsaved(true);
                if (previous.hasBlockEntity()) {
                    BlockPos pos = new BlockPos(chunk.chunkPos.x * 16 + x, section.sectionY * 16 + y, chunk.chunkPos.z * 16 + z);
                    BlockEntity blockEntity = chunk.getBlockEntity(pos, false);
                    if (blockEntity != null) {
                        try {
                            blockEntity.preRemove();
                        } catch (Throwable t) {
                            CraftEngine.instance().logger().warn("Error removing block entity " + blockEntity.getClass().getName(), t);
                        }
                        chunk.removeBlockEntity(pos);
                    }
                }
                if (previous.hasConstantBlockEntityRenderer()) {
                    BlockPos pos = new BlockPos(chunk.chunkPos.x * 16 + x, section.sectionY * 16 + y, chunk.chunkPos.z * 16 + z);
                    chunk.removeConstantBlockEntityRenderer(pos);
                }
                BlockSettings settings = previous.settings();
                if (settings != null) {
                    DestroyStageDisplayEntitySetting destroyStages = settings.destroyStageDisplay();
                    if (destroyStages != null) {
                        BlockPos pos = new BlockPos(chunk.chunkPos.x * 16 + x, section.sectionY * 16 + y, chunk.chunkPos.z * 16 + z);
                        BukkitDestroyStageDisplayRecorder.INSTANCE.remove(new DestroyStageDisplayRecorder.PosKey(chunk.world.uuid(), pos.asLong()));
                    }
                }
                if (Config.enableBlockLightSystem() && chunk.isLoaded()) {
                    // 自定义块到原版块，只需要判断旧块是否和客户端一直
                    BlockStateWrapper wrapper = previous.visualBlockState();
                    if (wrapper != null) {
                        updateLight(holder, wrapper.minecraftState(), previousState, x, y, z);
                    }
                }
            }
        }
    }

    @SuppressWarnings("DuplicatedCode")
    private static void updateLight(@This InjectedStorage thisObj, Object clientState, Object serverState, int x, int y, int z) {
        CEChunk chunk = thisObj.chunk();
        CEWorld world = chunk.world;
        if (LightUtils.hasDifferentLightProperties(serverState, clientState)) {
            SectionPos sectionPos = thisObj.pos();
            List<SectionPos> pos = SectionPosUtils.calculateAffectedRegions((sectionPos.x() << 4) + x, (sectionPos.y() << 4) + y, (sectionPos.z() << 4) + z, 15);
            world.sectionLightUpdated(pos);
        }
    }

    @SuppressWarnings("DuplicatedCode")
    private static void updateLight$complex(@This InjectedStorage thisObj, Object newClientState, Object newServerState, Object oldServerState, int x, int y, int z) {
        CEChunk chunk = thisObj.chunk();
        CEWorld world = chunk.world;
        // 如果客户端新状态和服务端新状态光照属性不同
        if (LightUtils.hasDifferentLightProperties(newClientState, newServerState)) {
            SectionPos sectionPos = thisObj.pos();
            List<SectionPos> pos = SectionPosUtils.calculateAffectedRegions((sectionPos.x() << 4) + x, (sectionPos.y() << 4) + y, (sectionPos.z() << 4) + z, 15);
            world.sectionLightUpdated(pos);
            return;
        }
        if (LightUtils.hasDifferentLightProperties(newServerState, oldServerState)) {
            SectionPos sectionPos = thisObj.pos();
            List<SectionPos> pos = SectionPosUtils.calculateAffectedRegions((sectionPos.x() << 4) + x, (sectionPos.y() << 4) + y, (sectionPos.z() << 4) + z, 15);
            world.sectionLightUpdated(pos);
        }
    }
}
