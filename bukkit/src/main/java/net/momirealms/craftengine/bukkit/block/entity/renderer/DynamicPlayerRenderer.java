package net.momirealms.craftengine.bukkit.block.entity.renderer;

import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.ints.IntList;
import net.momirealms.craftengine.bukkit.block.behavior.BedBlockBehavior;
import net.momirealms.craftengine.bukkit.block.entity.BedBlockEntity;
import net.momirealms.craftengine.bukkit.entity.data.PlayerData;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MEntityTypes;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.NetworkReflections;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.entity.render.DynamicBlockEntityRenderer;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.util.*;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

public class DynamicPlayerRenderer implements DynamicBlockEntityRenderer {
    private static final EnumSet<?> ADD_PLAYER_ACTION = createAction();
    private static final List<Object> EMPTY_EQUIPMENT = List.of(
            Pair.of(CoreReflections.instance$EquipmentSlot$HEAD, CoreReflections.instance$ItemStack$EMPTY),
            Pair.of(CoreReflections.instance$EquipmentSlot$CHEST, CoreReflections.instance$ItemStack$EMPTY),
            Pair.of(CoreReflections.instance$EquipmentSlot$LEGS, CoreReflections.instance$ItemStack$EMPTY),
            Pair.of(CoreReflections.instance$EquipmentSlot$FEET, CoreReflections.instance$ItemStack$EMPTY),
            Pair.of(CoreReflections.instance$EquipmentSlot$OFFHAND, CoreReflections.instance$ItemStack$EMPTY),
            Pair.of(CoreReflections.instance$EquipmentSlot$MAINHAND, CoreReflections.instance$ItemStack$EMPTY)
    );
    public final UUID uuid = UUID.randomUUID();
    public final BedBlockEntity blockEntity;
    public final int entityId;
    public final LazyReference<Vec3d> pos;
    public final Vector3f offset;
    public final Object cachedDespawnPacket;
    public final Object cachedPlayerInfoRemovePacket;
    public final float yRot;
    private boolean isShow;
    private boolean hasCachedPacket;
    private @Nullable Object cachedSpawnPacket;
    private @Nullable Object cachedPlayerInfoUpdatePacket;
    private @Nullable Object cachedSetOccupierDataPacket;
    private @Nullable Object cachedSetOccupierEquipmentPacket;
    private @Nullable Object cachedHideOccupierPacket;
    private @Nullable Object cachedSetEntityDataPacket;
    private @Nullable Object cachedSetEquipmentPacket;

    public DynamicPlayerRenderer(BedBlockEntity blockEntity, BlockPos pos, Vector3f sleepOffset) {
        this.blockEntity = blockEntity;
        this.entityId = CoreReflections.instance$Entity$ENTITY_COUNTER.incrementAndGet();
        ImmutableBlockState blockState = this.blockEntity.blockState();
        BedBlockBehavior behavior = blockState.behavior().getAs(BedBlockBehavior.class).orElse(null);
        if (behavior != null) {
            this.yRot = switch (blockState.get(behavior.facingProperty)) {
                case NORTH -> 270;
                case SOUTH -> 90;
                case WEST -> 0;
                case EAST -> 180;
            };
            this.offset = QuaternionUtils.toQuaternionf(0, Math.toRadians(180 - this.yRot), 0).conjugate().transform(new Vector3f(sleepOffset));
        } else {
            this.yRot = 0;
            this.offset = sleepOffset;
        }
        this.pos = LazyReference.lazyReference(() -> {
            Object state = blockState.visualBlockState().literalObject();
            Object shape = FastNMS.INSTANCE.method$BlockState$getShape(state, blockEntity.world.world.serverWorld(), LocationUtils.toBlockPos(pos), CoreReflections.instance$CollisionContext$empty);
            Object bounds = FastNMS.INSTANCE.method$VoxelShape$bounds(shape);
            double maxY = FastNMS.INSTANCE.field$AABB$maxY(bounds);
            return new Vec3d(pos.x + 0.5, pos.y + maxY, pos.z + 0.5);
        });
        this.cachedDespawnPacket = FastNMS.INSTANCE.constructor$ClientboundRemoveEntitiesPacket(IntList.of(entityId));
        this.cachedPlayerInfoRemovePacket = FastNMS.INSTANCE.constructor$ClientboundPlayerInfoRemovePacket(List.of(this.uuid));
    }

    @Override
    public void show(Player player) {
        this.update(player);
    }

    @Override
    public void hide(Player player) {
        if (player == null) {
            return;
        }
        player.sendPacket(this.cachedPlayerInfoRemovePacket, false);
        player.sendPacket(this.cachedDespawnPacket, false);
        if (this.isShow || !this.hasCachedPacket) {
            return;
        }
        player.sendPacket(this.cachedSetOccupierDataPacket, false);
        player.sendPacket(this.cachedSetOccupierEquipmentPacket, false);
    }

    @Override
    public void update(Player player) {
        if (player == null || !this.isShow || !this.hasCachedPacket) {
            return;
        }
        player.sendPacket(this.cachedPlayerInfoUpdatePacket, false);
        player.sendPacket(this.cachedSpawnPacket, false);
        this.updateNoAdd(player);
    }

    public void updateNoAdd(Player player) {
        if (!this.isShow || !this.hasCachedPacket) {
            return;
        }
        player.sendPacket(this.cachedHideOccupierPacket, false);
        player.sendPacket(this.cachedSetEntityDataPacket, false);
        player.sendPacket(this.cachedSetEquipmentPacket, false);
        player.sendPacket(this.cachedSetOccupierEquipmentPacket, false);
    }

    public void updateCachedPacket(@Nullable BukkitServerPlayer before) {
        GameProfile gameProfile = this.blockEntity.gameProfile();
        BukkitServerPlayer player = this.blockEntity.occupier();
        if (gameProfile == null || player == null) {
            if (before == null) {
                this.hasCachedPacket = false;
                return;
            }
            this.cachedSpawnPacket = null;
            this.cachedPlayerInfoUpdatePacket = null;
            @SuppressWarnings({"rawtypes", "unchecked"})
            List<Object> metadata = new ArrayList<>(FastNMS.INSTANCE.method$SynchedEntityData$getNonDefaultValues(before.entityData()));
            boolean noSharedFlags = true;
            for (Object entry : metadata) {
                int id = FastNMS.INSTANCE.field$SynchedEntityData$DataValue$id(entry);
                if (id != PlayerData.SharedFlags.id) continue;
                noSharedFlags = false;
                break;
            }
            if (noSharedFlags) {
                PlayerData.SharedFlags.addEntityData(PlayerData.SharedFlags.defaultValue, metadata);
            }
            this.cachedSetOccupierDataPacket = FastNMS.INSTANCE.constructor$ClientboundSetEntityDataPacket(before.entityId(), metadata);
            this.cachedSetOccupierEquipmentPacket = FastNMS.INSTANCE.constructor$ClientboundSetEquipmentPacket(before.entityId(), List.of(
                    Pair.of(CoreReflections.instance$EquipmentSlot$HEAD, before.getItemBySlot(39).getLiteralObject()),
                    Pair.of(CoreReflections.instance$EquipmentSlot$CHEST, before.getItemBySlot(38).getLiteralObject()),
                    Pair.of(CoreReflections.instance$EquipmentSlot$LEGS, before.getItemBySlot(37).getLiteralObject()),
                    Pair.of(CoreReflections.instance$EquipmentSlot$FEET, before.getItemBySlot(36).getLiteralObject()),
                    Pair.of(CoreReflections.instance$EquipmentSlot$OFFHAND, before.getItemBySlot(40).getLiteralObject()),
                    Pair.of(CoreReflections.instance$EquipmentSlot$MAINHAND, before.getItemInHand(InteractionHand.MAIN_HAND).getLiteralObject())
            ));
            this.isShow = false;
            this.hasCachedPacket = true;
            return;
        }
        Vec3d pos = this.pos.get();
        double y = pos.y + 0.1125 * FastNMS.INSTANCE.method$LivingEntity$getScale(player.serverPlayer());
        Object entry = FastNMS.INSTANCE.constructor$ClientboundPlayerInfoUpdatePacket$Entry1(
                this.uuid, gameProfile, false, 0,
                CoreReflections.instance$GameType$SURVIVAL, null, false, 0, null
        );
        this.cachedPlayerInfoUpdatePacket = FastNMS.INSTANCE.constructor$ClientboundPlayerInfoUpdatePacket(ADD_PLAYER_ACTION, List.of(entry));
        this.cachedSpawnPacket = FastNMS.INSTANCE.constructor$ClientboundAddEntityPacket(
                this.entityId, this.uuid, pos.x + this.offset.x, y + this.offset.y, pos.z + this.offset.z,
                0, this.yRot, MEntityTypes.PLAYER, 0, CoreReflections.instance$Vec3$Zero, this.yRot
        );
        @SuppressWarnings({"rawtypes", "unchecked"})
        List<Object> metadata = new ArrayList<>(FastNMS.INSTANCE.method$SynchedEntityData$getNonDefaultValues(player.entityData()));
        this.cachedSetOccupierDataPacket = null;
        ArrayList<Object> occupierMetadata = new ArrayList<>(metadata);
        PlayerData.SharedFlags.addEntityData((byte) (1 << 5), occupierMetadata);
        this.cachedHideOccupierPacket = FastNMS.INSTANCE.constructor$ClientboundSetEntityDataPacket(player.entityId(), occupierMetadata);
        PlayerData.Pose.addEntityData(CoreReflections.instance$Pose$SLEEPING, metadata);
        PlayerData.SharedFlags.addEntityData(PlayerData.SharedFlags.defaultValue, metadata);
        this.cachedSetEntityDataPacket = FastNMS.INSTANCE.constructor$ClientboundSetEntityDataPacket(this.entityId, metadata);
        this.updateEquipment(player);
        this.cachedSetOccupierEquipmentPacket = FastNMS.INSTANCE.constructor$ClientboundSetEquipmentPacket(player.entityId(), EMPTY_EQUIPMENT);
        this.isShow = true;
        this.hasCachedPacket = true;
    }

    public void updateEquipment(Player player, int mainSlot) {
        this.cachedSetEquipmentPacket = FastNMS.INSTANCE.constructor$ClientboundSetEquipmentPacket(this.entityId, List.of(
                Pair.of(CoreReflections.instance$EquipmentSlot$HEAD, player.getItemBySlot(39).getLiteralObject()),
                Pair.of(CoreReflections.instance$EquipmentSlot$CHEST, player.getItemBySlot(38).getLiteralObject()),
                Pair.of(CoreReflections.instance$EquipmentSlot$LEGS, player.getItemBySlot(37).getLiteralObject()),
                Pair.of(CoreReflections.instance$EquipmentSlot$FEET, player.getItemBySlot(36).getLiteralObject()),
                Pair.of(CoreReflections.instance$EquipmentSlot$OFFHAND, player.getItemBySlot(40).getLiteralObject()),
                Pair.of(CoreReflections.instance$EquipmentSlot$MAINHAND, player.getItemBySlot(mainSlot).getLiteralObject())
        ));
    }

    public void updateEquipment(Player player) {
        this.cachedSetEquipmentPacket = FastNMS.INSTANCE.constructor$ClientboundSetEquipmentPacket(this.entityId, List.of(
                Pair.of(CoreReflections.instance$EquipmentSlot$HEAD, player.getItemBySlot(39).getLiteralObject()),
                Pair.of(CoreReflections.instance$EquipmentSlot$CHEST, player.getItemBySlot(38).getLiteralObject()),
                Pair.of(CoreReflections.instance$EquipmentSlot$LEGS, player.getItemBySlot(37).getLiteralObject()),
                Pair.of(CoreReflections.instance$EquipmentSlot$FEET, player.getItemBySlot(36).getLiteralObject()),
                Pair.of(CoreReflections.instance$EquipmentSlot$OFFHAND, player.getItemBySlot(40).getLiteralObject()),
                Pair.of(CoreReflections.instance$EquipmentSlot$MAINHAND, player.getItemInHand(InteractionHand.MAIN_HAND).getLiteralObject())
        ));
    }

    public void playAnimation(Player player, int action) {
        player.sendPacket(FastNMS.INSTANCE.constructor$ClientboundAnimatePacket(this.entityId, action), false);
    }

    @SuppressWarnings("unchecked")
    private static <E extends Enum<E>> EnumSet<E> createAction() {
        return EnumSet.of((E) NetworkReflections.instance$ClientboundPlayerInfoUpdatePacket$Action$ADD_PLAYER);
    }
}
