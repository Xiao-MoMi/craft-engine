package net.momirealms.craftengine.bukkit.plugin.network.id;

import net.momirealms.craftengine.bukkit.plugin.network.PacketIds;
import net.momirealms.craftengine.core.plugin.network.ConnectionState;
import net.momirealms.craftengine.core.plugin.network.PacketFlow;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.common.ServerboundCustomPayloadPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.configuration.ServerboundFinishConfigurationPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.*;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.handshake.ClientIntentionPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.login.ClientboundLoginFinishedPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.login.ServerboundLoginAcknowledgedPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.status.ClientboundStatusResponsePacketProxy;

public class PacketIds1_20 implements PacketIds {

    @Override
    public int clientboundBlockUpdatePacket() {
        return PacketIdHelper.byClazz(ClientboundBlockUpdatePacketProxy.CLASS, PacketFlow.CLIENTBOUND, ConnectionState.PLAY);
    }

    @Override
    public int clientboundSectionBlocksUpdatePacket() {
        return PacketIdHelper.byClazz(ClientboundSectionBlocksUpdatePacketProxy.CLASS, PacketFlow.CLIENTBOUND, ConnectionState.PLAY);
    }

    @Override
    public int clientboundLevelParticlesPacket() {
        return PacketIdHelper.byClazz(ClientboundLevelParticlesPacketProxy.CLASS, PacketFlow.CLIENTBOUND, ConnectionState.PLAY);
    }

    @Override
    public int clientboundLevelEventPacket() {
        return PacketIdHelper.byClazz(ClientboundLevelEventPacketProxy.CLASS, PacketFlow.CLIENTBOUND, ConnectionState.PLAY);
    }

    @Override
    public int clientboundAddEntityPacket() {
        return PacketIdHelper.byClazz(ClientboundAddEntityPacketProxy.CLASS, PacketFlow.CLIENTBOUND, ConnectionState.PLAY);
    }

    @Override
    public int clientboundOpenScreenPacket() {
        return PacketIdHelper.byClazz(ClientboundOpenScreenPacketProxy.CLASS, PacketFlow.CLIENTBOUND, ConnectionState.PLAY);
    }

    @Override
    public int clientboundSoundPacket() {
        return PacketIdHelper.byClazz(ClientboundSoundPacketProxy.CLASS, PacketFlow.CLIENTBOUND, ConnectionState.PLAY);
    }

    @Override
    public int clientboundRemoveEntitiesPacket() {
        return PacketIdHelper.byClazz(ClientboundRemoveEntitiesPacketProxy.CLASS, PacketFlow.CLIENTBOUND, ConnectionState.PLAY);
    }

    @Override
    public int clientboundSetEntityDataPacket() {
        return PacketIdHelper.byClazz(ClientboundSetEntityDataPacketProxy.CLASS, PacketFlow.CLIENTBOUND, ConnectionState.PLAY);
    }

    @Override
    public int clientboundSetTitleTextPacket() {
        return PacketIdHelper.byClazz(ClientboundSetTitleTextPacketProxy.CLASS, PacketFlow.CLIENTBOUND, ConnectionState.PLAY);
    }

    @Override
    public int clientboundSetSubtitleTextPacket() {
        return PacketIdHelper.byClazz(ClientboundSetSubtitleTextPacketProxy.CLASS, PacketFlow.CLIENTBOUND, ConnectionState.PLAY);
    }

    @Override
    public int clientboundSetActionBarTextPacket() {
        return PacketIdHelper.byClazz(ClientboundSetActionBarTextPacketProxy.CLASS, PacketFlow.CLIENTBOUND, ConnectionState.PLAY);
    }

    @Override
    public int clientboundBossEventPacket() {
        return PacketIdHelper.byClazz(ClientboundBossEventPacketProxy.CLASS, PacketFlow.CLIENTBOUND, ConnectionState.PLAY);
    }

    @Override
    public int clientboundSystemChatPacket() {
        return PacketIdHelper.byClazz(ClientboundSystemChatPacketProxy.CLASS, PacketFlow.CLIENTBOUND, ConnectionState.PLAY);
    }

    @Override
    public int clientboundTabListPacket() {
        return PacketIdHelper.byClazz(ClientboundTabListPacketProxy.CLASS, PacketFlow.CLIENTBOUND, ConnectionState.PLAY);
    }

    @Override
    public int clientboundSetPlayerTeamPacket() {
        return PacketIdHelper.byClazz(ClientboundSetPlayerTeamPacketProxy.CLASS, PacketFlow.CLIENTBOUND, ConnectionState.PLAY);
    }

    @Override
    public int clientboundSetObjectivePacket() {
        return PacketIdHelper.byClazz(ClientboundSetObjectivePacketProxy.CLASS, PacketFlow.CLIENTBOUND, ConnectionState.PLAY);
    }

    @Override
    public int clientboundLevelChunkWithLightPacket() {
        return PacketIdHelper.byClazz(ClientboundLevelChunkWithLightPacketProxy.CLASS, PacketFlow.CLIENTBOUND, ConnectionState.PLAY);
    }

    @Override
    public int clientboundPlayerInfoUpdatePacket() {
        return PacketIdHelper.byClazz(ClientboundPlayerInfoUpdatePacketProxy.CLASS, PacketFlow.CLIENTBOUND, ConnectionState.PLAY);
    }

    @Override
    public int clientboundSetScorePacket() {
        return PacketIdHelper.byClazz(ClientboundSetScorePacketProxy.CLASS, PacketFlow.CLIENTBOUND, ConnectionState.PLAY);
    }

    @Override
    public int clientboundContainerSetContentPacket() {
        return PacketIdHelper.byClazz(ClientboundContainerSetContentPacketProxy.CLASS, PacketFlow.CLIENTBOUND, ConnectionState.PLAY);
    }

    @Override
    public int clientboundContainerSetSlotPacket() {
        return PacketIdHelper.byClazz(ClientboundContainerSetSlotPacketProxy.CLASS, PacketFlow.CLIENTBOUND, ConnectionState.PLAY);
    }

    @Override
    public int clientboundSetCursorItemPacket() {
        return PacketIdHelper.byClazz(ClientboundSetCursorItemPacketProxy.CLASS, PacketFlow.CLIENTBOUND, ConnectionState.PLAY);
    }

    @Override
    public int clientboundSetEquipmentPacket() {
        return PacketIdHelper.byClazz(ClientboundSetEquipmentPacketProxy.CLASS, PacketFlow.CLIENTBOUND, ConnectionState.PLAY);
    }

    @Override
    public int clientboundSetPlayerInventoryPacket() {
        return PacketIdHelper.byClazz(ClientboundSetPlayerInventoryPacketProxy.CLASS, PacketFlow.CLIENTBOUND, ConnectionState.PLAY);
    }

    @Override
    public int clientboundRecipeBookAddPacket() {
        return PacketIdHelper.byClazz(ClientboundRecipeBookAddPacketProxy.CLASS, PacketFlow.CLIENTBOUND, ConnectionState.PLAY);
    }

    @Override
    public int clientboundPlaceGhostRecipePacket() {
        return PacketIdHelper.byClazz(ClientboundPlaceGhostRecipePacketProxy.CLASS, PacketFlow.CLIENTBOUND, ConnectionState.PLAY);
    }

    @Override
    public int clientboundUpdateRecipesPacket() {
        return PacketIdHelper.byClazz(ClientboundUpdateRecipesPacketProxy.CLASS, PacketFlow.CLIENTBOUND, ConnectionState.PLAY);
    }

    @Override
    public int clientboundUpdateAdvancementsPacket() {
        return PacketIdHelper.byClazz(ClientboundUpdateAdvancementsPacketProxy.CLASS, PacketFlow.CLIENTBOUND, ConnectionState.PLAY);
    }

    @Override
    public int clientboundForgetLevelChunkPacket() {
        return PacketIdHelper.byClazz(ClientboundForgetLevelChunkPacketProxy.CLASS, PacketFlow.CLIENTBOUND, ConnectionState.PLAY);
    }

    @Override
    public int clientboundBlockEventPacket() {
        return PacketIdHelper.byClazz(ClientboundBlockEventPacketProxy.CLASS, PacketFlow.CLIENTBOUND, ConnectionState.PLAY);
    }

    @Override
    public int clientBoundMerchantOffersPacket() {
        return PacketIdHelper.byClazz(ClientboundMerchantOffersPacketProxy.CLASS, PacketFlow.CLIENTBOUND, ConnectionState.PLAY);
    }

    @Override
    public int clientboundBlockEntityDataPacket() {
        return PacketIdHelper.byClazz(ClientboundBlockEntityDataPacketProxy.CLASS, PacketFlow.CLIENTBOUND, ConnectionState.PLAY);
    }

    @Override
    public int serverboundContainerClickPacket() {
        return PacketIdHelper.byClazz(ServerboundContainerClickPacketProxy.CLASS, PacketFlow.SERVERBOUND, ConnectionState.PLAY);
    }

    @Override
    public int serverboundSetCreativeModeSlotPacket() {
        return PacketIdHelper.byClazz(ServerboundSetCreativeModeSlotPacketProxy.CLASS, PacketFlow.SERVERBOUND, ConnectionState.PLAY);
    }

    @Override
    public int serverboundInteractPacket() {
        return PacketIdHelper.byClazz(ServerboundInteractPacketProxy.CLASS, PacketFlow.SERVERBOUND, ConnectionState.PLAY);
    }

    @Override
    public int serverboundCustomPayloadPacket() {
        return PacketIdHelper.byClazz(ServerboundCustomPayloadPacketProxy.CLASS, PacketFlow.SERVERBOUND, ConnectionState.PLAY);
    }

    @Override
    public int clientboundPlayerChatPacket() {
        return PacketIdHelper.byClazz(ClientboundPlayerChatPacketProxy.CLASS, PacketFlow.CLIENTBOUND, ConnectionState.PLAY);
    }

    @Override
    public int clientIntentionPacket() {
        return PacketIdHelper.byClazz(ClientIntentionPacketProxy.CLASS, PacketFlow.SERVERBOUND, ConnectionState.HANDSHAKING);
    }

    @Override
    public int clientboundStatusResponsePacket() {
        return PacketIdHelper.byClazz(ClientboundStatusResponsePacketProxy.CLASS, PacketFlow.CLIENTBOUND, ConnectionState.STATUS);
    }

    @Override
    public int serverboundFinishConfigurationPacket() {
        return PacketIdHelper.byClazz(ServerboundFinishConfigurationPacketProxy.CLASS, PacketFlow.SERVERBOUND, ConnectionState.CONFIGURATION);
    }

    @Override
    public int clientboundLoginPacket() {
        return PacketIdHelper.byClazz(ClientboundLoginPacketProxy.CLASS, PacketFlow.CLIENTBOUND, ConnectionState.PLAY);
    }

    @Override
    public int clientboundLoginFinishedPacket() {
        return PacketIdHelper.byClazz(ClientboundLoginFinishedPacketProxy.CLASS, PacketFlow.CLIENTBOUND, ConnectionState.LOGIN);
    }

    @Override
    public int serverboundLoginAcknowledgedPacket() {
        return PacketIdHelper.byClazz(ServerboundLoginAcknowledgedPacketProxy.CLASS, PacketFlow.SERVERBOUND, ConnectionState.LOGIN);
    }

    @Override
    public int clientboundStartConfigurationPacket() {
        return PacketIdHelper.byClazz(ClientboundStartConfigurationPacketProxy.CLASS, PacketFlow.CLIENTBOUND, ConnectionState.PLAY);
    }

    @Override
    public int serverboundConfigurationAcknowledgedPacket() {
        return PacketIdHelper.byClazz(ClientboundStartConfigurationPacketProxy.CLASS, PacketFlow.SERVERBOUND, ConnectionState.PLAY);
    }
}
