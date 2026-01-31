package net.momirealms.craftengine.bukkit.block.entity;

import com.mojang.authlib.GameProfile;
import net.momirealms.craftengine.bukkit.block.behavior.BedBlockBehavior;
import net.momirealms.craftengine.bukkit.block.entity.renderer.DynamicPlayerRenderer;
import net.momirealms.craftengine.bukkit.entity.seat.BukkitSeat;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.LegacyAuthLibUtils;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.entity.BlockEntity;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.entity.seat.Seat;
import net.momirealms.craftengine.core.entity.seat.SeatOwner;
import net.momirealms.craftengine.core.util.HorizontalDirection;
import net.momirealms.craftengine.core.util.LazyReference;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.*;
import net.momirealms.sparrow.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public sealed abstract class BedBlockEntity extends BlockEntity permits BedBlockEntity.Controller, BedBlockEntity.Requestor {

    protected BedBlockEntity(BlockPos pos, ImmutableBlockState blockState) {
        super(BukkitBlockEntityTypes.BED, pos, blockState);
    }

    public abstract @Nullable DynamicPlayerRenderer renderer();

    public abstract @Nullable Seat<Controller> seat();

    public abstract @Nullable BukkitServerPlayer occupier();

    public abstract @Nullable GameProfile gameProfile();

    public abstract void setOccupier(@Nullable BukkitServerPlayer occupier);

    public abstract void playAnimation(int action);

    public static non-sealed class Controller extends BedBlockEntity implements SeatOwner {
        public final DynamicPlayerRenderer renderer;
        public final Seat<Controller> seat;
        public final ChunkPos chunkPos;
        public final float yRot;
        private @Nullable BukkitServerPlayer occupier;
        private @Nullable GameProfile gameProfile;
        private int tickCount;

        public Controller(BlockPos pos, ImmutableBlockState blockState) {
            super(pos, blockState);
            BedBlockBehavior behavior = blockState.behavior().getAs(BedBlockBehavior.class).orElseThrow();
            super.blockEntityRenderer = this.renderer = new DynamicPlayerRenderer(this, super.pos, behavior.sleepOffset);
            this.seat = new BukkitSeat<>(this, behavior.seatConfig);
            this.yRot = switch (blockState.get(behavior.facingProperty)) {
                case NORTH -> 0;
                case SOUTH -> 180;
                case WEST -> 270;
                case EAST -> 90;
            };
            this.chunkPos = ChunkPos.of(pos.x >> 4, pos.z >> 4);
        }

        @Override
        public DynamicPlayerRenderer renderer() {
            return this.renderer;
        }

        @Override
        public Seat<Controller> seat() {
            return this.seat;
        }

        @Override
        public BukkitServerPlayer occupier() {
            return this.occupier;
        }

        @Override
        public GameProfile gameProfile() {
            return this.gameProfile;
        }

        @Override
        public void setOccupier(@Nullable BukkitServerPlayer occupier) {
            BukkitServerPlayer before = this.occupier;
            this.occupier = occupier;
            this.gameProfile = before == occupier ? this.gameProfile : occupier == null ? null : VersionHelper.isOrAbove1_21_9()
                    ? new GameProfile(this.renderer.uuid, occupier.name(), occupier.propertyMap())
                    : LegacyAuthLibUtils.constructor$GameProfile(this.renderer.uuid, occupier.name(), occupier.propertyMap());
            List<Player> trackedBy = this.world.world.getTrackedBy(this.chunkPos);
            this.renderer.updateCachedPacket(before);
            if (this.occupier != null) {
                trackedBy.forEach(this.renderer::update);
            } else if (before != null) {
                trackedBy.forEach(this.renderer::hide);
            }
            if (occupier == null) {
                this.seat.destroy();
            } else if (before != occupier) {
                Vec3d pos = this.renderer.pos.get();
                double y = pos.y + 0.1125 * FastNMS.INSTANCE.method$LivingEntity$getScale(occupier.serverPlayer());
                this.seat.spawnSeat(occupier, new WorldPosition(super.world.world, pos.x, y, pos.z, 0, this.yRot));
            }
        }

        @Override
        public void playAnimation(int action) {
            List<Player> trackedBy = this.world.world.getTrackedBy(this.chunkPos);
            trackedBy.forEach(p -> this.renderer.playAnimation(p, action));
        }

        @Override
        public void saveEntityData(CompoundTag data) {
            data.putString("type", "bed_block_entity");
        }

        @Override
        public void preRemove() {
            BukkitServerPlayer player = this.occupier;
            if (player != null) {
                player.setBedBlockEntity(null);
            }
            this.setOccupier(null);
            this.seat.destroy();
        }

        @SuppressWarnings("unused")
        public static void tick(CEWorld world, BlockPos pos, ImmutableBlockState state, Controller bed) {
            if (bed.tickCount++ % 5 != 0) {
                return;
            }
            for (Player player : world.world.getTrackedBy(bed.chunkPos)) {
                bed.renderer.updateNoAdd(player);
            }
        }
    }

    public static non-sealed class Requestor extends BedBlockEntity {
        public final LazyReference<Controller> controller;

        @SuppressWarnings("PatternVariableHidesField")
        public Requestor(BlockPos pos, ImmutableBlockState blockState) {
            super(pos, blockState);
            this.controller = LazyReference.lazyReference(() -> {
                BedBlockBehavior behavior = blockState.behavior().getAs(BedBlockBehavior.class).orElseThrow();
                HorizontalDirection direction = super.blockState.get(behavior.facingProperty);
                BlockPos offset = super.pos.offset(direction.stepX(), 0, direction.stepZ());
                BlockEntity blockEntity = world.getBlockEntityAtIfLoaded(offset);
                return blockEntity instanceof Controller controller ? controller : null;
            });
        }

        @Override
        public DynamicPlayerRenderer renderer() {
            Controller controller = this.controller.get();
            if (controller == null) {
                return null;
            }
            return controller.renderer();
        }

        @Override
        public Seat<Controller> seat() {
            Controller controller = this.controller.get();
            if (controller == null) {
                return null;
            }
            return controller.seat();
        }

        @Override
        public @Nullable BukkitServerPlayer occupier() {
            Controller controller = this.controller.get();
            if (controller == null) {
                return null;
            }
            return controller.occupier();
        }

        @Override
        public @Nullable GameProfile gameProfile() {
            Controller controller = this.controller.get();
            if (controller == null) {
                return null;
            }
            return controller.gameProfile();
        }

        @Override
        public void setOccupier(@Nullable BukkitServerPlayer occupier) {
            Controller controller = this.controller.get();
            if (controller == null) {
                return;
            }
            controller.setOccupier(occupier);
        }

        @Override
        public void playAnimation(int action) {
            Controller controller = this.controller.get();
            if (controller == null) {
                return;
            }
            controller.playAnimation(action);
        }
    }
}
