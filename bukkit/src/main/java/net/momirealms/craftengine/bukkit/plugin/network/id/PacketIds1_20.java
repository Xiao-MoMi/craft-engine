package net.momirealms.craftengine.bukkit.plugin.network.id;

import net.momirealms.craftengine.bukkit.plugin.network.PacketIds;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.NetworkReflections;
import net.momirealms.craftengine.bukkit.reflection.minecraft.network.protocol.game.ClientboundAddEntityPacketProxy;
import net.momirealms.craftengine.bukkit.reflection.minecraft.network.protocol.game.ClientboundBossEventPacketProxy;
import net.momirealms.craftengine.bukkit.reflection.minecraft.network.protocol.game.ClientboundSetActionBarTextPacketProxy;
import net.momirealms.craftengine.bukkit.reflection.minecraft.network.protocol.game.ClientboundSystemChatPacketProxy;
import net.momirealms.craftengine.core.plugin.network.ConnectionState;
import net.momirealms.craftengine.core.plugin.network.PacketFlow;

public class PacketIds1_20 implements PacketIds {

    @Override
    public int clientboundBlockUpdatePacket() {
        return PacketIdHelper.byClazz(NetworkReflections.clazz$ClientboundBlockUpdatePacket, PacketFlow.CLIENTBOUND, ConnectionState.PLAY);
    }

    @Override
    public int clientboundSectionBlocksUpdatePacket() {
        return PacketIdHelper.byClazz(NetworkReflections.clazz$ClientboundSectionBlocksUpdatePacket, PacketFlow.CLIENTBOUND, ConnectionState.PLAY);
    }

    @Override
    public int clientboundLevelParticlesPacket() {
        return PacketIdHelper.byClazz(NetworkReflections.clazz$ClientboundLevelParticlesPacket, PacketFlow.CLIENTBOUND, ConnectionState.PLAY);
    }

    @Override
    public int clientboundLevelEventPacket() {
        return PacketIdHelper.byClazz(NetworkReflections.clazz$ClientboundLevelEventPacket, PacketFlow.CLIENTBOUND, ConnectionState.PLAY);
    }

    @Override
    public int clientboundAddEntityPacket() {
        return PacketIdHelper.byClazz(ClientboundAddEntityPacketProxy.CLAZZ, PacketFlow.CLIENTBOUND, ConnectionState.PLAY);
    }

    @Override
    public int clientboundOpenScreenPacket() {
        return PacketIdHelper.byClazz(NetworkReflections.clazz$ClientboundOpenScreenPacket, PacketFlow.CLIENTBOUND, ConnectionState.PLAY);
    }

    @Override
    public int clientboundSoundPacket() {
        return PacketIdHelper.byClazz(NetworkReflections.clazz$ClientboundSoundPacket, PacketFlow.CLIENTBOUND, ConnectionState.PLAY);
    }

    @Override
    public int clientboundRemoveEntitiesPacket() {
        return PacketIdHelper.byClazz(NetworkReflections.clazz$ClientboundRemoveEntitiesPacket, PacketFlow.CLIENTBOUND, ConnectionState.PLAY);
    }

    @Override
    public int clientboundSetEntityDataPacket() {
        return PacketIdHelper.byClazz(NetworkReflections.clazz$ClientboundSetEntityDataPacket, PacketFlow.CLIENTBOUND, ConnectionState.PLAY);
    }

    @Override
    public int clientboundSetTitleTextPacket() {
        return PacketIdHelper.byClazz(NetworkReflections.clazz$ClientboundSetTitleTextPacket, PacketFlow.CLIENTBOUND, ConnectionState.PLAY);
    }

    @Override
    public int clientboundSetSubtitleTextPacket() {
        return PacketIdHelper.byClazz(NetworkReflections.clazz$ClientboundSetSubtitleTextPacket, PacketFlow.CLIENTBOUND, ConnectionState.PLAY);
    }

    @Override
    public int clientboundSetActionBarTextPacket() {
        return PacketIdHelper.byClazz(ClientboundSetActionBarTextPacketProxy.CLAZZ, PacketFlow.CLIENTBOUND, ConnectionState.PLAY);
    }

    @Override
    public int clientboundBossEventPacket() {
        return PacketIdHelper.byClazz(ClientboundBossEventPacketProxy.CLAZZ, PacketFlow.CLIENTBOUND, ConnectionState.PLAY);
    }

    @Override
    public int clientboundSystemChatPacket() {
        return PacketIdHelper.byClazz(ClientboundSystemChatPacketProxy.CLAZZ, PacketFlow.CLIENTBOUND, ConnectionState.PLAY);
    }

    @Override
    public int clientboundTabListPacket() {
        return PacketIdHelper.byClazz(NetworkReflections.clazz$ClientboundTabListPacket, PacketFlow.CLIENTBOUND, ConnectionState.PLAY);
    }

    @Override
    public int clientboundSetPlayerTeamPacket() {
        return PacketIdHelper.byClazz(NetworkReflections.clazz$ClientboundSetPlayerTeamPacket, PacketFlow.CLIENTBOUND, ConnectionState.PLAY);
    }

    @Override
    public int clientboundSetObjectivePacket() {
        return PacketIdHelper.byClazz(NetworkReflections.clazz$ClientboundSetObjectivePacket, PacketFlow.CLIENTBOUND, ConnectionState.PLAY);
    }

    @Override
    public int clientboundLevelChunkWithLightPacket() {
        return PacketIdHelper.byClazz(NetworkReflections.clazz$ClientboundLevelChunkWithLightPacket, PacketFlow.CLIENTBOUND, ConnectionState.PLAY);
    }

    @Override
    public int clientboundPlayerInfoUpdatePacket() {
        return PacketIdHelper.byClazz(NetworkReflections.clazz$ClientboundPlayerInfoUpdatePacket, PacketFlow.CLIENTBOUND, ConnectionState.PLAY);
    }

    @Override
    public int clientboundSetScorePacket() {
        return PacketIdHelper.byClazz(NetworkReflections.clazz$ClientboundSetScorePacket, PacketFlow.CLIENTBOUND, ConnectionState.PLAY);
    }

    @Override
    public int clientboundContainerSetContentPacket() {
        return PacketIdHelper.byClazz(NetworkReflections.clazz$ClientboundContainerSetContentPacket, PacketFlow.CLIENTBOUND, ConnectionState.PLAY);
    }

    @Override
    public int clientboundContainerSetSlotPacket() {
        return PacketIdHelper.byClazz(NetworkReflections.clazz$ClientboundContainerSetSlotPacket, PacketFlow.CLIENTBOUND, ConnectionState.PLAY);
    }

    @Override
    public int clientboundSetCursorItemPacket() {
        return PacketIdHelper.byClazz(NetworkReflections.clazz$ClientboundSetCursorItemPacket, PacketFlow.CLIENTBOUND, ConnectionState.PLAY);
    }

    @Override
    public int clientboundSetEquipmentPacket() {
        return PacketIdHelper.byClazz(NetworkReflections.clazz$ClientboundSetEquipmentPacket, PacketFlow.CLIENTBOUND, ConnectionState.PLAY);
    }

    @Override
    public int clientboundSetPlayerInventoryPacket() {
        return PacketIdHelper.byClazz(NetworkReflections.clazz$ClientboundSetPlayerInventoryPacket, PacketFlow.CLIENTBOUND, ConnectionState.PLAY);
    }

    @Override
    public int clientboundRecipeBookAddPacket() {
        return PacketIdHelper.byClazz(NetworkReflections.clazz$ClientboundRecipeBookAddPacket, PacketFlow.CLIENTBOUND, ConnectionState.PLAY);
    }

    @Override
    public int clientboundPlaceGhostRecipePacket() {
        return PacketIdHelper.byClazz(NetworkReflections.clazz$ClientboundPlaceGhostRecipePacket, PacketFlow.CLIENTBOUND, ConnectionState.PLAY);
    }

    @Override
    public int clientboundUpdateRecipesPacket() {
        return PacketIdHelper.byClazz(NetworkReflections.clazz$ClientboundUpdateRecipesPacket, PacketFlow.CLIENTBOUND, ConnectionState.PLAY);
    }

    @Override
    public int clientboundUpdateAdvancementsPacket() {
        return PacketIdHelper.byClazz(NetworkReflections.clazz$ClientboundUpdateAdvancementsPacket, PacketFlow.CLIENTBOUND, ConnectionState.PLAY);
    }

    @Override
    public int clientboundForgetLevelChunkPacket() {
        return PacketIdHelper.byClazz(NetworkReflections.clazz$ClientboundForgetLevelChunkPacket, PacketFlow.CLIENTBOUND, ConnectionState.PLAY);
    }

    @Override
    public int clientboundBlockEventPacket() {
        return PacketIdHelper.byClazz(NetworkReflections.clazz$ClientboundBlockEventPacket, PacketFlow.CLIENTBOUND, ConnectionState.PLAY);
    }

    @Override
    public int clientBoundMerchantOffersPacket() {
        return PacketIdHelper.byClazz(NetworkReflections.clazz$ClientboundMerchantOffersPacket, PacketFlow.CLIENTBOUND, ConnectionState.PLAY);
    }

    @Override
    public int clientboundBlockEntityDataPacket() {
        return PacketIdHelper.byClazz(NetworkReflections.clazz$ClientboundBlockEntityDataPacket, PacketFlow.CLIENTBOUND, ConnectionState.PLAY);
    }

    @Override
    public int serverboundContainerClickPacket() {
        return PacketIdHelper.byClazz(NetworkReflections.clazz$ServerboundContainerClickPacket, PacketFlow.SERVERBOUND, ConnectionState.PLAY);
    }

    @Override
    public int serverboundSetCreativeModeSlotPacket() {
        return PacketIdHelper.byClazz(NetworkReflections.clazz$ServerboundSetCreativeModeSlotPacket, PacketFlow.SERVERBOUND, ConnectionState.PLAY);
    }

    @Override
    public int serverboundInteractPacket() {
        return PacketIdHelper.byClazz(NetworkReflections.clazz$ServerboundInteractPacket, PacketFlow.SERVERBOUND, ConnectionState.PLAY);
    }

    @Override
    public int serverboundCustomPayloadPacket() {
        return PacketIdHelper.byClazz(NetworkReflections.clazz$ServerboundCustomPayloadPacket, PacketFlow.SERVERBOUND, ConnectionState.PLAY);
    }

    @Override
    public int clientboundPlayerChatPacket() {
        return PacketIdHelper.byClazz(NetworkReflections.clazz$ClientboundPlayerChatPacket, PacketFlow.CLIENTBOUND, ConnectionState.PLAY);
    }

    @Override
    public int clientIntentionPacket() {
        return PacketIdHelper.byClazz(NetworkReflections.clazz$ClientIntentionPacket, PacketFlow.SERVERBOUND, ConnectionState.HANDSHAKING);
    }

    @Override
    public int clientboundStatusResponsePacket() {
        return PacketIdHelper.byClazz(NetworkReflections.clazz$ClientboundStatusResponsePacket, PacketFlow.CLIENTBOUND, ConnectionState.STATUS);
    }

    @Override
    public int serverboundFinishConfigurationPacket() {
        return PacketIdHelper.byClazz(NetworkReflections.clazz$ServerboundFinishConfigurationPacket, PacketFlow.SERVERBOUND, ConnectionState.CONFIGURATION);
    }

    @Override
    public int clientboundLoginPacket() {
        return PacketIdHelper.byClazz(NetworkReflections.clazz$ClientboundLoginPacket, PacketFlow.CLIENTBOUND, ConnectionState.PLAY);
    }

    @Override
    public int clientboundLoginFinishedPacket() {
        return PacketIdHelper.byClazz(NetworkReflections.clazz$ClientboundLoginFinishedPacket, PacketFlow.CLIENTBOUND, ConnectionState.LOGIN);
    }

    @Override
    public int serverboundLoginAcknowledgedPacket() {
        return PacketIdHelper.byClazz(NetworkReflections.clazz$ServerboundLoginAcknowledgedPacket, PacketFlow.SERVERBOUND, ConnectionState.LOGIN);
    }

    @Override
    public int clientboundStartConfigurationPacket() {
        return PacketIdHelper.byClazz(NetworkReflections.clazz$ClientboundStartConfigurationPacket, PacketFlow.CLIENTBOUND, ConnectionState.PLAY);
    }

    @Override
    public int serverboundConfigurationAcknowledgedPacket() {
        return PacketIdHelper.byClazz(NetworkReflections.clazz$ServerboundConfigurationAcknowledgedPacket, PacketFlow.SERVERBOUND, ConnectionState.PLAY);
    }
}
