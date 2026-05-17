package net.momirealms.craftengine.bukkit.plugin.network.id;

import net.momirealms.craftengine.core.util.VersionHelper;

public interface PacketIds {
    PacketIds INSTANCE = VersionHelper.isOrAbove1_20_5 ? new PacketIds1_20_5() : new PacketIds1_20();

    int clientboundBlockUpdatePacket();

    int clientboundSectionBlocksUpdatePacket();

    int clientboundLevelParticlesPacket();

    int clientboundLevelEventPacket();

    int clientboundAddEntityPacket();

    int clientboundOpenScreenPacket();

    int clientboundSoundPacket();

    int clientboundRemoveEntitiesPacket();

    int clientboundSetEntityDataPacket();

    int clientboundSetTitleTextPacket();

    int clientboundSetSubtitleTextPacket();

    int clientboundSetActionBarTextPacket();

    int clientboundBossEventPacket();

    int clientboundSystemChatPacket();

    int clientboundTabListPacket();

    int clientboundSetPlayerTeamPacket();

    int clientboundSetObjectivePacket();

    int clientboundLevelChunkWithLightPacket();

    int clientboundPlayerInfoUpdatePacket();

    int clientboundSetScorePacket();

    int clientboundContainerSetContentPacket();

    int clientboundContainerSetSlotPacket();

    int clientboundSetCursorItemPacket();

    int clientboundSetEquipmentPacket();

    int clientboundSetPlayerInventoryPacket();

    int clientboundBlockEventPacket();

    int clientboundRecipeBookAddPacket();

    int clientboundPlaceGhostRecipePacket();

    int clientboundUpdateAdvancementsPacket();

    int clientBoundMerchantOffersPacket();

    int clientboundBlockEntityDataPacket();

    int serverboundContainerClickPacket();

    int serverboundSetCreativeModeSlotPacket();

    int serverboundInteractPacket();

    int clientboundUpdateRecipesPacket();

    int clientboundForgetLevelChunkPacket();

    int serverboundCustomPayloadPacket$play();

    int serverboundCustomPayloadPacket$configuration();

    int clientboundPlayerChatPacket();

    int clientIntentionPacket();

    int clientboundStatusResponsePacket();

    int serverboundFinishConfigurationPacket();

    int clientboundLoginPacket();

    int clientboundLoginFinishedPacket();

    int serverboundLoginAcknowledgedPacket();

    int clientboundStartConfigurationPacket();

    int serverboundConfigurationAcknowledgedPacket();

    int clientboundCustomPayloadPacket$play();

    int clientboundCustomPayloadPacket$configuration();

    int serverboundAttackPacket();

    int clientboundCustomChatCompletionsPacket();

    int clientboundFinishConfigurationPacket();

    int serverboundChatSessionUpdatePacket();

    int clientboundServerDataPacket();

    int serverboundHelloPacket();

    int serverboundSwingPacket();

    int serverboundPlayerActionPacket();

    int serverboundUseItemOnPacket();

    int serverboundPickItemFromBlockPacket();

    int serverboundPickItemFromEntityPacket();

    int clientboundUpdateTagsPacket$play();

    int clientboundupdatetagspacket$configuration();

    int clientboundEntityEventPacket();

    int serverboundResourcePackPacket$play();

    int serverboundResourcePackPacket$configuration();

    int serverboundRenameItemPacket();

    int serverboundSignUpdatePacket();

    int serverboundEditBookPacket();

    int clientboundEntityPositionSyncPacket();

    int clientboundMoveEntityPacket$PosRot();

    int clientboundMoveEntityPacket$Pos();

    int clientboundRespawnPacket();

    int serverboundClientInformationPacket$play();

    int serverboundClientInformationPacket$configuration();

    int clientboundRegistryDataPacket();

    int clientboundShowDialogPacket$play();

    int clientboundShowDialogPacket$configuration();
}
