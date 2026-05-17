package net.momirealms.craftengine.bukkit.plugin.network.id;

import net.momirealms.craftengine.core.plugin.network.ConnectionState;
import net.momirealms.craftengine.core.plugin.network.PacketFlow;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.common.*;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.configuration.ClientboundFinishConfigurationPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.configuration.ClientboundRegistryDataPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.configuration.ServerboundFinishConfigurationPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.*;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.handshake.ClientIntentionPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.login.ClientboundLoginFinishedPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.login.ServerboundHelloPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.login.ServerboundLoginAcknowledgedPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.status.ClientboundStatusResponsePacketProxy;

final class PacketIds1_20 implements PacketIds {

    PacketIds1_20() {}

    @Override
    public int clientboundBlockUpdatePacket() {
        return PacketIdHelper.byClazz(ClientboundBlockUpdatePacketProxy.CLASS, ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientboundSectionBlocksUpdatePacket() {
        return PacketIdHelper.byClazz(ClientboundSectionBlocksUpdatePacketProxy.CLASS, ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientboundLevelParticlesPacket() {
        return PacketIdHelper.byClazz(ClientboundLevelParticlesPacketProxy.CLASS, ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientboundLevelEventPacket() {
        return PacketIdHelper.byClazz(ClientboundLevelEventPacketProxy.CLASS, ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientboundAddEntityPacket() {
        return PacketIdHelper.byClazz(ClientboundAddEntityPacketProxy.CLASS, ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientboundOpenScreenPacket() {
        return PacketIdHelper.byClazz(ClientboundOpenScreenPacketProxy.CLASS, ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientboundSoundPacket() {
        return PacketIdHelper.byClazz(ClientboundSoundPacketProxy.CLASS, ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientboundRemoveEntitiesPacket() {
        return PacketIdHelper.byClazz(ClientboundRemoveEntitiesPacketProxy.CLASS, ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientboundSetEntityDataPacket() {
        return PacketIdHelper.byClazz(ClientboundSetEntityDataPacketProxy.CLASS, ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientboundSetTitleTextPacket() {
        return PacketIdHelper.byClazz(ClientboundSetTitleTextPacketProxy.CLASS, ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientboundSetSubtitleTextPacket() {
        return PacketIdHelper.byClazz(ClientboundSetSubtitleTextPacketProxy.CLASS, ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientboundSetActionBarTextPacket() {
        return PacketIdHelper.byClazz(ClientboundSetActionBarTextPacketProxy.CLASS, ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientboundBossEventPacket() {
        return PacketIdHelper.byClazz(ClientboundBossEventPacketProxy.CLASS, ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientboundSystemChatPacket() {
        return PacketIdHelper.byClazz(ClientboundSystemChatPacketProxy.CLASS, ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientboundTabListPacket() {
        return PacketIdHelper.byClazz(ClientboundTabListPacketProxy.CLASS, ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientboundSetPlayerTeamPacket() {
        return PacketIdHelper.byClazz(ClientboundSetPlayerTeamPacketProxy.CLASS, ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientboundSetObjectivePacket() {
        return PacketIdHelper.byClazz(ClientboundSetObjectivePacketProxy.CLASS, ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientboundLevelChunkWithLightPacket() {
        return PacketIdHelper.byClazz(ClientboundLevelChunkWithLightPacketProxy.CLASS, ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientboundPlayerInfoUpdatePacket() {
        return PacketIdHelper.byClazz(ClientboundPlayerInfoUpdatePacketProxy.CLASS, ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientboundSetScorePacket() {
        return PacketIdHelper.byClazz(ClientboundSetScorePacketProxy.CLASS, ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientboundContainerSetContentPacket() {
        return PacketIdHelper.byClazz(ClientboundContainerSetContentPacketProxy.CLASS, ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientboundContainerSetSlotPacket() {
        return PacketIdHelper.byClazz(ClientboundContainerSetSlotPacketProxy.CLASS, ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientboundSetCursorItemPacket() {
        return PacketIdHelper.byClazz(ClientboundSetCursorItemPacketProxy.CLASS, ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientboundSetEquipmentPacket() {
        return PacketIdHelper.byClazz(ClientboundSetEquipmentPacketProxy.CLASS, ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientboundSetPlayerInventoryPacket() {
        return PacketIdHelper.byClazz(ClientboundSetPlayerInventoryPacketProxy.CLASS, ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientboundRecipeBookAddPacket() {
        return PacketIdHelper.byClazz(ClientboundRecipeBookAddPacketProxy.CLASS, ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientboundPlaceGhostRecipePacket() {
        return PacketIdHelper.byClazz(ClientboundPlaceGhostRecipePacketProxy.CLASS, ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientboundUpdateRecipesPacket() {
        return PacketIdHelper.byClazz(ClientboundUpdateRecipesPacketProxy.CLASS, ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientboundUpdateAdvancementsPacket() {
        return PacketIdHelper.byClazz(ClientboundUpdateAdvancementsPacketProxy.CLASS, ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientboundForgetLevelChunkPacket() {
        return PacketIdHelper.byClazz(ClientboundForgetLevelChunkPacketProxy.CLASS, ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientboundBlockEventPacket() {
        return PacketIdHelper.byClazz(ClientboundBlockEventPacketProxy.CLASS, ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientBoundMerchantOffersPacket() {
        return PacketIdHelper.byClazz(ClientboundMerchantOffersPacketProxy.CLASS, ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientboundBlockEntityDataPacket() {
        return PacketIdHelper.byClazz(ClientboundBlockEntityDataPacketProxy.CLASS, ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int serverboundContainerClickPacket() {
        return PacketIdHelper.byClazz(ServerboundContainerClickPacketProxy.CLASS, ConnectionState.PLAY, PacketFlow.SERVERBOUND);
    }

    @Override
    public int serverboundSetCreativeModeSlotPacket() {
        return PacketIdHelper.byClazz(ServerboundSetCreativeModeSlotPacketProxy.CLASS, ConnectionState.PLAY, PacketFlow.SERVERBOUND);
    }

    @Override
    public int serverboundInteractPacket() {
        return PacketIdHelper.byClazz(ServerboundInteractPacketProxy.CLASS, ConnectionState.PLAY, PacketFlow.SERVERBOUND);
    }

    @Override
    public int serverboundCustomPayloadPacket$play() {
        return PacketIdHelper.byClazz(ServerboundCustomPayloadPacketProxy.CLASS, ConnectionState.PLAY, PacketFlow.SERVERBOUND);
    }

    @Override
    public int serverboundCustomPayloadPacket$configuration() {
        return PacketIdHelper.byClazz(ServerboundCustomPayloadPacketProxy.CLASS, ConnectionState.CONFIGURATION, PacketFlow.SERVERBOUND);
    }

    @Override
    public int clientboundPlayerChatPacket() {
        return PacketIdHelper.byClazz(ClientboundPlayerChatPacketProxy.CLASS, ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientIntentionPacket() {
        return PacketIdHelper.byClazz(ClientIntentionPacketProxy.CLASS, ConnectionState.HANDSHAKING, PacketFlow.SERVERBOUND);
    }

    @Override
    public int clientboundStatusResponsePacket() {
        return PacketIdHelper.byClazz(ClientboundStatusResponsePacketProxy.CLASS, ConnectionState.STATUS, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int serverboundFinishConfigurationPacket() {
        return PacketIdHelper.byClazz(ServerboundFinishConfigurationPacketProxy.CLASS, ConnectionState.CONFIGURATION, PacketFlow.SERVERBOUND);
    }

    @Override
    public int clientboundLoginPacket() {
        return PacketIdHelper.byClazz(ClientboundLoginPacketProxy.CLASS, ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientboundLoginFinishedPacket() {
        return PacketIdHelper.byClazz(ClientboundLoginFinishedPacketProxy.CLASS, ConnectionState.LOGIN, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int serverboundLoginAcknowledgedPacket() {
        return PacketIdHelper.byClazz(ServerboundLoginAcknowledgedPacketProxy.CLASS, ConnectionState.LOGIN, PacketFlow.SERVERBOUND);
    }

    @Override
    public int clientboundStartConfigurationPacket() {
        return PacketIdHelper.byClazz(ClientboundStartConfigurationPacketProxy.CLASS, ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int serverboundConfigurationAcknowledgedPacket() {
        return PacketIdHelper.byClazz(ClientboundStartConfigurationPacketProxy.CLASS, ConnectionState.PLAY, PacketFlow.SERVERBOUND);
    }

    @Override
    public int clientboundCustomPayloadPacket$play() {
        return PacketIdHelper.byClazz(ClientboundCustomPayloadPacketProxy.CLASS, ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientboundCustomPayloadPacket$configuration() {
        return PacketIdHelper.byClazz(ClientboundCustomPayloadPacketProxy.CLASS, ConnectionState.CONFIGURATION, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int serverboundAttackPacket() {
        return -1;
    }

    @Override
    public int clientboundCustomChatCompletionsPacket() {
        return PacketIdHelper.byClazz(ClientboundCustomChatCompletionsPacketProxy.CLASS, ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientboundFinishConfigurationPacket() {
        return PacketIdHelper.byClazz(ClientboundFinishConfigurationPacketProxy.CLASS, ConnectionState.CONFIGURATION, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int serverboundChatSessionUpdatePacket() {
        return PacketIdHelper.byClazz(ServerboundChatSessionUpdatePacketProxy.CLASS, ConnectionState.PLAY, PacketFlow.SERVERBOUND);
    }

    @Override
    public int clientboundServerDataPacket() {
        return PacketIdHelper.byClazz(ClientboundServerDataPacketProxy.CLASS, ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int serverboundHelloPacket() {
        return PacketIdHelper.byClazz(ServerboundHelloPacketProxy.CLASS, ConnectionState.LOGIN, PacketFlow.SERVERBOUND);
    }

    @Override
    public int serverboundSwingPacket() {
        return PacketIdHelper.byClazz(ServerboundSwingPacketProxy.CLASS, ConnectionState.PLAY, PacketFlow.SERVERBOUND);
    }

    @Override
    public int serverboundPlayerActionPacket() {
        return PacketIdHelper.byClazz(ServerboundPlayerActionPacketProxy.CLASS, ConnectionState.PLAY, PacketFlow.SERVERBOUND);
    }

    @Override
    public int serverboundUseItemOnPacket() {
        return PacketIdHelper.byClazz(ServerboundUseItemOnPacketProxy.CLASS, ConnectionState.PLAY, PacketFlow.SERVERBOUND);
    }

    @Override
    public int serverboundPickItemFromBlockPacket() {
        return -1;
    }

    @Override
    public int serverboundPickItemFromEntityPacket() {
        return -1;
    }

    @Override
    public int clientboundUpdateTagsPacket$play() {
        return PacketIdHelper.byClazz(ClientboundUpdateTagsPacketProxy.CLASS, ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    public int clientboundupdatetagspacket$configuration() {
        return PacketIdHelper.byClazz(ClientboundUpdateTagsPacketProxy.CLASS, ConnectionState.CONFIGURATION, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientboundEntityEventPacket() {
        return PacketIdHelper.byClazz(ClientboundEntityEventPacketProxy.CLASS, ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int serverboundResourcePackPacket$play() {
        return PacketIdHelper.byClazz(ServerboundResourcePackPacketProxy.CLASS, ConnectionState.PLAY, PacketFlow.SERVERBOUND);
    }

    @Override
    public int serverboundResourcePackPacket$configuration() {
        return PacketIdHelper.byClazz(ServerboundResourcePackPacketProxy.CLASS, ConnectionState.CONFIGURATION, PacketFlow.SERVERBOUND);
    }

    @Override
    public int serverboundRenameItemPacket() {
        return PacketIdHelper.byClazz(ServerboundRenameItemPacketProxy.CLASS, ConnectionState.PLAY, PacketFlow.SERVERBOUND);
    }

    @Override
    public int serverboundSignUpdatePacket() {
        return PacketIdHelper.byClazz(ServerboundSignUpdatePacketProxy.CLASS, ConnectionState.PLAY, PacketFlow.SERVERBOUND);
    }

    @Override
    public int serverboundEditBookPacket() {
        return PacketIdHelper.byClazz(ServerboundEditBookPacketProxy.CLASS, ConnectionState.PLAY, PacketFlow.SERVERBOUND);
    }

    @Override
    public int clientboundEntityPositionSyncPacket() {
        return -1;
    }

    @Override
    public int clientboundMoveEntityPacket$PosRot() {
        return PacketIdHelper.byClazz(ClientboundMoveEntityPacketProxy.PosRotProxy.CLASS, ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientboundMoveEntityPacket$Pos() {
        return PacketIdHelper.byClazz(ClientboundMoveEntityPacketProxy.PosProxy.CLASS, ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientboundRespawnPacket() {
        return PacketIdHelper.byClazz(ClientboundRespawnPacketProxy.CLASS, ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int serverboundClientInformationPacket$play() {
        return PacketIdHelper.byClazz(ServerboundClientInformationPacketProxy.CLASS, ConnectionState.PLAY, PacketFlow.SERVERBOUND);
    }

    @Override
    public int serverboundClientInformationPacket$configuration() {
        return PacketIdHelper.byClazz(ServerboundClientInformationPacketProxy.CLASS, ConnectionState.CONFIGURATION, PacketFlow.SERVERBOUND);
    }

    @Override
    public int clientboundRegistryDataPacket() {
        return PacketIdHelper.byClazz(ClientboundRegistryDataPacketProxy.CLASS, ConnectionState.CONFIGURATION, PacketFlow.SERVERBOUND);
    }

    @Override
    public int clientboundShowDialogPacket$play() {
        return -1;
    }

    @Override
    public int clientboundShowDialogPacket$configuration() {
        return -1;
    }
}
