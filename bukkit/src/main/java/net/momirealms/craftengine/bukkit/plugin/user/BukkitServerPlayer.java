package net.momirealms.craftengine.bukkit.plugin.user;

import com.google.common.collect.Lists;
import io.netty.channel.Channel;
import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.util.*;
import net.momirealms.craftengine.bukkit.world.BukkitWorld;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.recipe.Recipe;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.network.ConnectionState;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.World;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.RayTraceResult;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.UUID;

public class BukkitServerPlayer extends Player {
    private final Channel channel;
    private final BukkitCraftEngine plugin;

    private ConnectionState decoderState;
    private ConnectionState encoderState;

    private Reference<org.bukkit.entity.Player> playerRef;
    private Reference<Object> serverPlayerRef;

    private int sectionCount;
    private int lastSuccessfulInteraction;
    private long lastAttributeSyncTime;
    private Key clientSideDimension;

    private int lastSentState = -1;
    private int lastHitBlockTime;
    private BlockPos destroyPos;
    private Object destroyedState;
    private boolean isDestroyingBlock;
    private boolean isDestroyingCustomBlock;
    private boolean swingHandAck;
    private float miningProgress;

    private int resentSoundTick;
    private int resentSwingTick;

    private Recipe<ItemStack> lastUsedRecipe = null;

    public BukkitServerPlayer(BukkitCraftEngine plugin, Channel channel) {
        this.channel = channel;
        this.plugin = plugin;
    }

    public void setPlayer(org.bukkit.entity.Player player) {
        playerRef = new WeakReference<>(player);
        try {
            serverPlayerRef = new WeakReference<>(Reflections.method$CraftPlayer$getHandle.invoke(player));
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Channel nettyChannel() {
        return channel;
    }

    @Override
    public CraftEngine plugin() {
        return plugin;
    }

    @Override
    public boolean isMiningBlock() {
        return destroyPos != null;
    }

    public void setDestroyedState(Object destroyedState) {
        this.destroyedState = destroyedState;
    }

    public void setDestroyPos(BlockPos destroyPos) {
        this.destroyPos = destroyPos;
    }

    @Override
    public boolean shouldSyncAttribute() {
        long current = System.currentTimeMillis();
        if (current - this.lastAttributeSyncTime > 10000) {
            this.lastAttributeSyncTime = current;
            return true;
        }
        return false;
    }

    @Override
    public boolean isSneaking() {
        return platformPlayer().isSneaking();
    }

    @Override
    public boolean isCreativeMode() {
        return platformPlayer().getGameMode() == GameMode.CREATIVE;
    }

    @Override
    public boolean isSpectatorMode() {
        return platformPlayer().getGameMode() == GameMode.SPECTATOR;
    }

    @Override
    public void sendActionBar(Component text) {
        try {
            Object packet = Reflections.constructor$ClientboundSetActionBarTextPacket.newInstance(ComponentUtils.adventureToMinecraft(text));
            sendPacket(packet, false);
        } catch (ReflectiveOperationException e) {
            plugin.logger().warn("Failed to send action bar", e);
        }
    }

    @Override
    public boolean updateLastSuccessfulInteractionTick(int tick) {
        if (lastSuccessfulInteraction != tick) {
            lastSuccessfulInteraction = tick;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int gameTicks() {
        try {
            Object serverPlayer = serverPlayer();
            Object gameMode = Reflections.field$ServerPlayer$gameMode.get(serverPlayer);
            return (int) Reflections.field$ServerPlayerGameMode$gameTicks.get(gameMode);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to get current tick", e);
        }
    }

    @Override
    public void swingHand(InteractionHand hand) {
        platformPlayer().swingHand(hand == InteractionHand.MAIN_HAND ? EquipmentSlot.HAND : EquipmentSlot.OFF_HAND);
    }

    @Override
    public boolean hasPermission(String permission) {
        return platformPlayer().hasPermission(permission);
    }

    @Override
    public boolean canInstabuild() {
        try {
            Object abilities = Reflections.field$Player$abilities.get(serverPlayer());
            return (boolean) Reflections.field$Abilities$instabuild.get(abilities);
        } catch (ReflectiveOperationException e) {
            CraftEngine.instance().logger().warn("Failed to get canInstabuild for " + name(), e);
            return false;
        }
    }

    @Override
    public String name() {
        return platformPlayer().getName();
    }

    @Override
    public void sendPacket(Object packet, boolean immediately) {
        this.plugin.networkManager().sendPacket(this, packet, immediately);
    }

    @Override
    public ConnectionState decoderState() {
        return decoderState;
    }

    @Override
    public ConnectionState encoderState() {
        return encoderState;
    }

    @Override
    public int clientSideSectionCount() {
        return sectionCount;
    }

    public void setClientSideSectionCount(int sectionCount) {
        this.sectionCount = sectionCount;
    }

    @Override
    public Key clientSideDimension() {
        return clientSideDimension;
    }

    public void setClientSideDimension(Key clientSideDimension) {
        this.clientSideDimension = clientSideDimension;
    }

    public void setConnectionState(ConnectionState connectionState) {
        this.encoderState = connectionState;
        this.decoderState = connectionState;
    }

    public void setDecoderState(ConnectionState decoderState) {
        this.decoderState = decoderState;
    }

    public void setEncoderState(ConnectionState encoderState) {
        this.encoderState = encoderState;
    }

    @Override
    public void tick() {
        // not fully online
        if (serverPlayer() == null) return;
        if (this.isDestroyingBlock)  {
            this.tickBlockDestroy();
        }
    }

    @Override
    public float getDestroyProgress(Object blockState, BlockPos pos) {
        try {
            Object serverPlayer = serverPlayer();
            Object blockPos = Reflections.constructor$BlockPos.newInstance(pos.x(), pos.y(), pos.z());
            return (float) Reflections.method$BlockStateBase$getDestroyProgress.invoke(blockState, serverPlayer, Reflections.method$Entity$level.invoke(serverPlayer), blockPos);
        } catch (ReflectiveOperationException e) {
            plugin.logger().warn("Failed to get destroy progress for player " + platformPlayer().getName());
            return 0f;
        }
    }

    public void startMiningBlock(org.bukkit.World world, BlockPos pos, Object state, boolean custom) {
        // instant break
        if (getDestroyProgress(state, pos) >= 1f) {
            ParticleUtils.addBlockBreakParticles(world, LocationUtils.toBlockPos(pos), state);
            return;
        }
        setCanBreakBlock(!custom);
        setDestroyPos(pos);
        setDestroyedState(state);
        setIsDestroyingBlock(true, custom);
    }

    private void setCanBreakBlock(boolean canBreak) {
        try {
            if (canBreak) {
                if (VersionHelper.isVersionNewerThan1_20_5()) {
                    Object serverPlayer = serverPlayer();
                    Object attributeInstance = Reflections.method$ServerPlayer$getAttribute.invoke(serverPlayer, Reflections.instance$Holder$Attribute$block_break_speed);
                    Object newPacket = Reflections.constructor$ClientboundUpdateAttributesPacket0.newInstance(entityID(), Lists.newArrayList(attributeInstance));
                    sendPacket(newPacket, true);
                } else {
                    resetEffect(Reflections.instance$MobEffecr$mining_fatigue);
                    resetEffect(Reflections.instance$MobEffecr$haste);
                }
            } else {
                if (VersionHelper.isVersionNewerThan1_20_5()) {
                    Object attributeModifier = VersionHelper.isVersionNewerThan1_21() ?
                            Reflections.constructor$AttributeModifier.newInstance(Reflections.method$ResourceLocation$fromNamespaceAndPath.invoke(null, "craftengine", "custom_hardness"), -9999d, Reflections.instance$AttributeModifier$Operation$ADD_VALUE) :
                            Reflections.constructor$AttributeModifier.newInstance(UUID.randomUUID(), "craftengine:custom_hardness", -9999d, Reflections.instance$AttributeModifier$Operation$ADD_VALUE);
                    Object attributeSnapshot = Reflections.constructor$ClientboundUpdateAttributesPacket$AttributeSnapshot.newInstance(Reflections.instance$Holder$Attribute$block_break_speed, 1d, Lists.newArrayList(attributeModifier));
                    Object newPacket = Reflections.constructor$ClientboundUpdateAttributesPacket1.newInstance(entityID(), Lists.newArrayList(attributeSnapshot));
                    sendPacket(newPacket, true);
                } else {
                    Object fatiguePacket = MobEffectUtils.createPacket(Reflections.instance$MobEffecr$mining_fatigue, entityID(), (byte) 9, -1, false, false, false);
                    Object hastePacket = MobEffectUtils.createPacket(Reflections.instance$MobEffecr$haste, entityID(), (byte) 0, -1, false, false, false);
                    sendPacket(fatiguePacket, true);
                    sendPacket(hastePacket, true);
                }
            }
        } catch (ReflectiveOperationException e) {
            plugin.logger().warn("Failed to set attribute for player " + platformPlayer().getName(), e);
        }
    }

    @Override
    public void stopMiningBlock() {
        setCanBreakBlock(true);
        setIsDestroyingBlock(false, false);
    }

    private void resetEffect(Object mobEffect) throws ReflectiveOperationException {
        Object effectInstance = Reflections.method$ServerPlayer$getEffect.invoke(serverPlayer(), mobEffect);
        Object packet;
        if (effectInstance != null) {
            packet = Reflections.constructor$ClientboundUpdateMobEffectPacket.newInstance(entityID(), effectInstance);
        } else {
            packet = Reflections.constructor$ClientboundRemoveMobEffectPacket.newInstance(entityID(), mobEffect);
        }
        sendPacket(packet, true);
    }

    @Override
    public void abortMiningBlock() {
        abortDestroyProgress();
    }

    private void tickBlockDestroy() {
        // prevent server from taking over breaking blocks
        if (this.isDestroyingCustomBlock) {
            try {
                Object serverPlayer = serverPlayer();
                Object gameMode = Reflections.field$ServerPlayer$gameMode.get(serverPlayer);
                Reflections.field$ServerPlayerGameMode$isDestroyingBlock.set(gameMode, false);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }
        if (!this.swingHandAck) return;
        this.swingHandAck = false;
        try {
            org.bukkit.entity.Player player = platformPlayer();
            double range = getInteractionRange();
            RayTraceResult result = player.rayTraceBlocks(range, FluidCollisionMode.NEVER);
            if (result == null) return;
            Block hitBlock = result.getHitBlock();
            if (hitBlock == null) return;
            Location location = hitBlock.getLocation();
            BlockPos hitPos = new BlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ());
            if (!hitPos.equals(this.destroyPos)) {
                return;
            }
            Object blockPos = LocationUtils.toBlockPos(hitPos);
            Object serverPlayer = serverPlayer();
            Object gameMode = Reflections.field$ServerPlayer$gameMode.get(serverPlayer);
            int currentTick = (int) Reflections.field$ServerPlayerGameMode$gameTicks.get(gameMode);
            if (currentTick - this.lastHitBlockTime > 3) {
                Object blockOwner = Reflections.field$StateHolder$owner.get(this.destroyedState);
                Object soundType = Reflections.field$BlockBehaviour$soundType.get(blockOwner);
                Object soundEvent = Reflections.field$SoundType$hitSound.get(soundType);
                Object soundId = Reflections.field$SoundEvent$location.get(soundEvent);
                level().playBlockSound(new Vec3d(this.destroyPos.x(), this.destroyPos.y(), this.destroyPos.z()), Key.of(soundId.toString()), 0.5F, 0.5F);
                this.lastHitBlockTime = currentTick;
            }

            // accumulate progress (custom blocks only)
            if (this.isDestroyingCustomBlock) {
                Item<ItemStack> item = this.getItemInHand(InteractionHand.MAIN_HAND);
                if (item != null) {
                    Material itemMaterial = item.getItem().getType();
                    if (canInstabuild() && (itemMaterial == Material.DEBUG_STICK
                            || itemMaterial == Material.TRIDENT
                            || (VersionHelper.isVersionNewerThan1_20_5() && itemMaterial == MaterialUtils.MACE)
                            || item.is(ItemTags.SWORDS))) {
                        return;
                    }
                }
                this.miningProgress = (float) Reflections.method$BlockStateBase$getDestroyProgress.invoke(this.destroyedState, serverPlayer, Reflections.method$Entity$level.invoke(serverPlayer), blockPos) + miningProgress;
                int packetStage = (int) (this.miningProgress * 10.0F);
                if (packetStage != this.lastSentState) {
                    this.lastSentState = packetStage;
                    broadcastDestroyProgress(player, hitPos, blockPos, packetStage);
                }
                if (this.miningProgress >= 1f) {
                    Reflections.method$ServerPlayerGameMode$destroyBlock.invoke(gameMode, blockPos);
                    ParticleUtils.addBlockBreakParticles(location.getWorld(), blockPos, this.destroyedState);
                    this.stopMiningBlock();
                }
            }
        } catch (ReflectiveOperationException e) {
            plugin.logger().warn("Failed to tick destroy for player " + platformPlayer().getName(), e);
        }
    }

    private void broadcastDestroyProgress(org.bukkit.entity.Player player, BlockPos hitPos, Object blockPos, int stage) throws ReflectiveOperationException {
        Object packet = Reflections.constructor$ClientboundBlockDestructionPacket.newInstance(Integer.MAX_VALUE - entityID(), blockPos, stage);
        for (org.bukkit.entity.Player other : player.getWorld().getPlayers()) {
            Location otherLocation = other.getLocation();
            double d0 = (double) hitPos.x() - otherLocation.getX();
            double d1 = (double) hitPos.y() - otherLocation.getY();
            double d2 = (double) hitPos.z() - otherLocation.getZ();
            if (d0 * d0 + d1 * d1 + d2 * d2 < 1024.0D) {
                plugin.networkManager().sendPacket(other, packet);
            }
        }
    }

    @Override
    public double getInteractionRange() {
        try {
            if (VersionHelper.isVersionNewerThan1_20_5()) {
                Object attributeInstance = Reflections.method$ServerPlayer$getAttribute.invoke(serverPlayer(), Reflections.instance$Holder$Attribute$block_interaction_range);
                if (attributeInstance == null) return 4.5d;
                return (double) Reflections.method$AttributeInstance$getValue.invoke(attributeInstance);
            } else {
                return 4.5d;
            }
        } catch (ReflectiveOperationException e) {
            plugin.logger().warn("Failed to get interaction range for player " + platformPlayer().getName(), e);
            return 4.5d;
        }
    }

    public void setIsDestroyingBlock(boolean value, boolean custom) {
        if (value) {
            this.isDestroyingBlock = true;
            this.isDestroyingCustomBlock = custom;
            this.swingHandAck = true;
            this.miningProgress = 0;
        } else {
            this.isDestroyingBlock = false;
            this.swingHandAck = false;
            if (this.destroyPos != null) {
                try {
                    this.broadcastDestroyProgress(platformPlayer(), this.destroyPos, LocationUtils.toBlockPos(this.destroyPos), -1);
                } catch (ReflectiveOperationException e) {
                    plugin.logger().warn("Failed to set isDestroyingCustomBlock", e);
                }
            }
            this.destroyPos = null;
            this.miningProgress = 0;
            this.destroyedState = null;
            this.isDestroyingCustomBlock = false;
        }
    }

    @Override
    public void abortDestroyProgress() {
        this.swingHandAck = false;
        this.miningProgress = 0;
        if (this.destroyPos == null) return;
        try {
            this.broadcastDestroyProgress(platformPlayer(), this.destroyPos, LocationUtils.toBlockPos(this.destroyPos), -1);
        } catch (ReflectiveOperationException e) {
            plugin.logger().warn("Failed to abort destroyProgress", e);
        }
    }

    @Override
    public void onSwingHand() {
        this.swingHandAck = true;
    }

    @Override
    public int entityID() {
        return platformPlayer().getEntityId();
    }

    @Override
    public boolean isOnline() {
        org.bukkit.entity.Player player = platformPlayer();
        if (player == null) return false;
        return player.isOnline();
    }

    @Override
    public float getYRot() {
        return platformPlayer().getLocation().getPitch();
    }

    @Override
    public float getXRot() {
        return platformPlayer().getLocation().getYaw();
    }

    @Override
    public boolean isSecondaryUseActive() {
        return isSneaking();
    }

    @Override
    public Direction getDirection() {
        return DirectionUtils.toDirection(platformPlayer().getFacing());
    }

    @Nullable
    @Override
    public Item<ItemStack> getItemInHand(InteractionHand hand) {
        PlayerInventory inventory = platformPlayer().getInventory();
        return BukkitItemManager.instance().wrap(hand == InteractionHand.MAIN_HAND ? inventory.getItemInMainHand() : inventory.getItemInOffHand());
    }

    @Override
    public World level() {
        return new BukkitWorld(platformPlayer().getWorld());
    }

    @Override
    public double x() {
        return platformPlayer().getLocation().getX();
    }

    @Override
    public double y() {
        return platformPlayer().getLocation().getY();
    }

    @Override
    public double z() {
        return platformPlayer().getLocation().getZ();
    }

    @Override
    public Object serverPlayer() {
        return serverPlayerRef.get();
    }

    @Override
    public org.bukkit.entity.Player platformPlayer() {
        return playerRef.get();
    }

    public void setResendSound() {
        resentSoundTick = gameTicks();
    }

    public void setResendSwing() {
        resentSwingTick = gameTicks();
    }

    public boolean shouldResendSound() {
        return resentSoundTick == gameTicks();
    }

    public boolean shouldResendSwing() {
        return resentSwingTick == gameTicks();
    }

    public Recipe<ItemStack> lastUsedRecipe() {
        return lastUsedRecipe;
    }

    public void setLastUsedRecipe(Recipe<ItemStack> lastUsedRecipe) {
        this.lastUsedRecipe = lastUsedRecipe;
    }
}
