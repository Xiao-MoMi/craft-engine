package net.momirealms.craftengine.bukkit.plugin.network.listener.game;

import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.plugin.network.listener.ByteBufferPacketListener;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.CraftWorldProxy;
import net.momirealms.craftengine.proxy.minecraft.sounds.SoundEventProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.BlockGetterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.SoundTypeProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockBehaviourProxy;
import org.bukkit.World;
import org.bukkit.entity.Player;

public final class PlayerActionListener implements ByteBufferPacketListener {
    public static final ByteBufferPacketListener INSTANCE = new PlayerActionListener();

    private PlayerActionListener() {}

    @Override
    public void onPacketReceive(NetWorkUser user, ByteBufPacketEvent event) {
        BukkitServerPlayer player = (BukkitServerPlayer) user;
        Player platformPlayer = player.platformPlayer();
        World world = platformPlayer.getWorld();
        FriendlyByteBuf buf = event.getBuffer();
        int action = buf.readVarInt();
        BlockPos pos = buf.readBlockPos();
        if (!player.canInteractPoint(new Vec3d(pos.x, pos.y, pos.z), 4)) {
            return;
        }
        BukkitCraftEngine.instance().scheduler().platform().run(
                () -> handlePlayerActionPacketOnMainThread(player, world, pos, action),
                world, pos.x >> 4, pos.z >> 4
        );
    }

    private static void handlePlayerActionPacketOnMainThread(BukkitServerPlayer player, World world, BlockPos pos, int action) {
        if (action == 0/*START_DESTROY_BLOCK*/) {
            Object serverLevel = CraftWorldProxy.INSTANCE.getWorld(world);
            Object blockState = BlockGetterProxy.INSTANCE.getBlockState(serverLevel, LocationUtils.toBlockPos(pos));
            int stateId = BlockStateUtils.blockStateToId(blockState);
            // not a custom block
            if (BlockStateUtils.isVanillaBlock(stateId)) {
                if (Config.enableSoundSystem()) {
                    Object soundType = BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.getSoundType(blockState);
                    Object soundEvent = SoundTypeProxy.INSTANCE.getHitSound(soundType);
                    Object soundId = SoundEventProxy.INSTANCE.getLocation(soundEvent);
                    if (BukkitBlockManager.instance().isHitSoundMissing(soundId)) {
                        player.startMiningBlock(pos, blockState, null);
                        return;
                    }
                }
                if (player.isMiningBlock()) {
                    player.finishMiningBlock();
                } else {
                    player.setClientSideCanBreakBlock(true);
                }
                return;
            }
            if (player.isAdventureMode()) {
                if (Config.simplifyAdventureBreakCheck()) {
                    ImmutableBlockState state = BukkitBlockManager.instance().getImmutableBlockStateUnsafe(stateId);
                    if (!player.canBreak(pos, state.visualBlockState().minecraftState())) {
                        player.preventMiningBlock();
                        return;
                    }
                } else {
                    if (!player.canBreak(pos, null)) {
                        player.preventMiningBlock();
                        return;
                    }
                }
            }
            player.startMiningBlock(pos, blockState, BukkitBlockManager.instance().getImmutableBlockStateUnsafe(stateId));
        } else if (action == 1/*ABORT_DESTROY_BLOCK*/) {
            if (player.isMiningBlock()) {
                player.abortMiningBlock();
            }
        } else if (action == 2/*STOP_DESTROY_BLOCK*/) {
            if (player.isMiningBlock()) {
                player.finishMiningBlock();
            }
        }
    }
}
