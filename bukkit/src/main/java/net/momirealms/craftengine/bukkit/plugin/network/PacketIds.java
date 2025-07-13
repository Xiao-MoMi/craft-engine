package net.momirealms.craftengine.bukkit.plugin.network;

public interface PacketIds {

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

    int serverboundContainerClickPacket();

    int serverboundSetCreativeModeSlotPacket();

    int serverboundInteractPacket();

    int clientboundUpdateRecipesPacket();
}
