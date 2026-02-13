package net.momirealms.craftengine.bukkit.plugin.injector;

import net.bytebuddy.implementation.bind.annotation.This;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.util.LightUtils;
import net.momirealms.craftengine.core.block.BlockStateWrapper;
import net.momirealms.craftengine.core.block.DelegatingBlockState;
import net.momirealms.craftengine.core.block.EmptyBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.entity.BlockEntity;
import net.momirealms.craftengine.core.block.entity.render.ConstantBlockEntityRenderer;
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
import net.momirealms.craftengine.proxy.minecraft.world.level.chunk.PalettedContainerProxy;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
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
                storage = FastNMS.INSTANCE.createInjectedPalettedContainer(container);
                PalettedContainerProxy.INSTANCE.setData(storage, PalettedContainerProxy.INSTANCE.getData(container));
                LevelChunkSectionProxy.INSTANCE.setStates(nmsSection, storage);
            }
        } else {
            if (nmsSection instanceof InjectedStorage.Section holder) {
                storage = holder;
            } else {
                storage = FastNMS.INSTANCE.createInjectedLevelChunkSection(nmsSection);
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

    // 用于监听注入的LevelChunkSection内的变化
    public static Object setBlockState(Object thisObj, int x, int y, int z, @NotNull Object newState, boolean useLocks) {
        InjectedStorage.Section holder = (InjectedStorage.Section) thisObj;
        Object previousState = LevelChunkSectionProxy.INSTANCE.setBlockState(holder, x, y, z, newState, useLocks);
        if (holder.isActive()) {
            compareAndUpdateBlockState(x, y, z, newState, previousState, holder);
        }
        return previousState;
    }

    // 用于监听注入的PalettedContainer内的变化
    public static Object getAndSet(Object thisObj, int x, int y, int z, @NotNull Object newState) {
        InjectedStorage.Palette holder = (InjectedStorage.Palette) thisObj;
        Object previousState = PalettedContainerProxy.INSTANCE.getAndSet(holder.delegated(), x, y, z, newState);
        if (holder.isActive()) {
            compareAndUpdateBlockState(x, y, z, newState, previousState, holder);
        }
        return previousState;
    }

    // 用于监听注入的PalettedContainer内的变化
    public static Object getAndSetUnchecked(Object thisObj, int x, int y, int z, @NotNull Object newState) {
        InjectedStorage.Palette holder = (InjectedStorage.Palette) thisObj;
        Object previousState = PalettedContainerProxy.INSTANCE.getAndSetUnchecked(holder.delegated(), x, y, z, newState);
        if (holder.isActive()) {
            compareAndUpdateBlockState(x, y, z, newState, previousState, holder);
        }
        return previousState;
    }

    @SuppressWarnings("DuplicatedCode")
    private static void compareAndUpdateBlockState(int x, int y, int z, Object newState, Object previousState, InjectedStorage holder) {
        CESection section = holder.section();
        if (newState instanceof DelegatingBlockState delegatingBlockState) {
            ImmutableBlockState newImmutableBlockState = delegatingBlockState.blockState();
            if (newImmutableBlockState == null) return;
            ImmutableBlockState previousImmutableBlockState = section.setBlockState(x, y, z, newImmutableBlockState);
            if (previousImmutableBlockState == newImmutableBlockState) return;
            // 处理  自定义块到自定义块或原版块到自定义块
            CEChunk chunk = holder.chunk();
            chunk.setDirty(true);

            ConstantBlockEntityRenderer previousRenderer = null;
            // 如果两个方块没有相同的主人 且 旧方块有方块实体
            if (!previousImmutableBlockState.isEmpty()) {
                if (previousImmutableBlockState.owner() != newImmutableBlockState.owner() && previousImmutableBlockState.hasBlockEntity()) {
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
                if (previousImmutableBlockState.hasConstantBlockEntityRenderer()) {
                    BlockPos pos = new BlockPos(chunk.chunkPos.x * 16 + x, section.sectionY * 16 + y, chunk.chunkPos.z * 16 + z);
                    // 如果新状态没有entity renderer，那么直接移除，否则暂存
                    previousRenderer = chunk.removeConstantBlockEntityRenderer(pos, !newImmutableBlockState.hasConstantBlockEntityRenderer());
                }
            }

            if (newImmutableBlockState.hasBlockEntity()) {
                BlockPos pos = new BlockPos(chunk.chunkPos.x * 16 + x, section.sectionY * 16 + y, chunk.chunkPos.z * 16 + z);
                BlockEntity blockEntity = chunk.getBlockEntity(pos, false);
                if (blockEntity != null && !blockEntity.isValidBlockState(newImmutableBlockState)) {
                    chunk.removeBlockEntity(pos);
                    blockEntity = null;
                }
                if (blockEntity == null) {
                    blockEntity = Objects.requireNonNull(newImmutableBlockState.behavior().getEntityBehavior()).createBlockEntity(pos, newImmutableBlockState);
                    if (blockEntity != null) {
                        chunk.addBlockEntity(blockEntity);
                    }
                } else {
                    blockEntity.setBlockState(newImmutableBlockState);
                    // 方块类型未变，仅更新状态，选择性更新ticker
                    chunk.replaceOrCreateTickingBlockEntity(blockEntity);
                    chunk.createDynamicBlockEntityRenderer(blockEntity);
                }
            }

            // 处理新老entity renderer更替
            if (newImmutableBlockState.hasConstantBlockEntityRenderer()) {
                BlockPos pos = new BlockPos(chunk.chunkPos.x * 16 + x, section.sectionY * 16 + y, chunk.chunkPos.z * 16 + z);
                chunk.addConstantBlockEntityRenderer(pos, newImmutableBlockState, previousRenderer);
            }

            // 如果新方块的光照属性和客户端认为的不同
            if (Config.enableLightSystem()) {
                if (previousImmutableBlockState.isEmpty()) {
                    // 原版块到自定义块，只需要判断新块是否和客户端视觉一致
                    updateLight(holder, newImmutableBlockState.visualBlockState().literalObject(), newState, x, y, z);
                } else {
                    // 自定义块到自定义块
                    updateLight$complex(holder, newImmutableBlockState.visualBlockState().literalObject(), newState, previousState, x, y, z);
                }
            }
        } else {
            // 如果是原版方块
            // 那么应该清空自定义块
            ImmutableBlockState previous = section.setBlockState(x, y, z, EmptyBlock.STATE);
            // 处理  自定义块 -> 原版块
            if (previous != null && !previous.isEmpty()) {
                CEChunk chunk = holder.chunk();
                chunk.setDirty(true);
                if (previous.hasBlockEntity()) {
                    BlockPos pos = new BlockPos(chunk.chunkPos.x * 16 + x, section.sectionY * 16 + y, chunk.chunkPos.z * 16 + z);
                    BlockEntity blockEntity = chunk.getBlockEntity(pos, false);
                    if (blockEntity != null) {
                        blockEntity.preRemove();
                        chunk.removeBlockEntity(pos);
                    }
                }
                if (previous.hasConstantBlockEntityRenderer()) {
                    BlockPos pos = new BlockPos(chunk.chunkPos.x * 16 + x, section.sectionY * 16 + y, chunk.chunkPos.z * 16 + z);
                    chunk.removeConstantBlockEntityRenderer(pos);
                }
                if (Config.enableLightSystem()) {
                    // 自定义块到原版块，只需要判断旧块是否和客户端一直
                    BlockStateWrapper wrapper = previous.visualBlockState();
                    if (wrapper != null) {
                        updateLight(holder, wrapper.literalObject(), previousState, x, y, z);
                    }
                }
            }
        }
    }

    @SuppressWarnings("DuplicatedCode")
    private static void updateLight(@This InjectedStorage thisObj, Object clientState, Object serverState, int x, int y, int z) {
        CEWorld world = thisObj.chunk().world;
        if (LightUtils.hasDifferentLightProperties(serverState, clientState)) {
            SectionPos sectionPos = thisObj.pos();
            List<SectionPos> pos = SectionPosUtils.calculateAffectedRegions((sectionPos.x() << 4) + x, (sectionPos.y() << 4) + y, (sectionPos.z() << 4) + z, 15);
            world.sectionLightUpdated(pos);
        }
    }

    @SuppressWarnings("DuplicatedCode")
    private static void updateLight$complex(@This InjectedStorage thisObj, Object newClientState, Object newServerState, Object oldServerState, int x, int y, int z) {
        CEWorld world = thisObj.chunk().world;
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
