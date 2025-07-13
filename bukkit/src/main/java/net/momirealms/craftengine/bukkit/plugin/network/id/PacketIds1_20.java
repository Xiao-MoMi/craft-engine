package net.momirealms.craftengine.bukkit.plugin.network.id;

import net.momirealms.craftengine.bukkit.plugin.network.PacketIds;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.NetworkReflections;

public class PacketIds1_20 implements PacketIds {

    @Override
    public int clientboundBlockUpdatePacket() {
        return PacketIdFinder.clientboundByClazz(NetworkReflections.clazz$ClientboundBlockUpdatePacket);
    }

    @Override
    public int clientboundSectionBlocksUpdatePacket() {
        return PacketIdFinder.clientboundByClazz(NetworkReflections.clazz$ClientboundSectionBlocksUpdatePacket);
    }

    @Override
    public int clientboundLevelParticlesPacket() {
        return PacketIdFinder.clientboundByClazz(NetworkReflections.clazz$ClientboundLevelParticlesPacket);
    }

    @Override
    public int clientboundLevelEventPacket() {
        return PacketIdFinder.clientboundByClazz(NetworkReflections.clazz$ClientboundLevelEventPacket);
    }

    @Override
    public int clientboundAddEntityPacket() {
        return PacketIdFinder.clientboundByClazz(NetworkReflections.clazz$ClientboundAddEntityPacket);
    }

    @Override
    public int clientboundOpenScreenPacket() {
        return PacketIdFinder.clientboundByClazz(NetworkReflections.clazz$ClientboundOpenScreenPacket);
    }

    @Override
    public int clientboundSoundPacket() {
        return PacketIdFinder.clientboundByClazz(NetworkReflections.clazz$ClientboundSoundPacket);
    }

    @Override
    public int clientboundRemoveEntitiesPacket() {
        return PacketIdFinder.clientboundByClazz(NetworkReflections.clazz$ClientboundRemoveEntitiesPacket);
    }

    @Override
    public int clientboundSetEntityDataPacket() {
        return PacketIdFinder.clientboundByClazz(NetworkReflections.clazz$ClientboundSetEntityDataPacket);
    }

    @Override
    public int clientboundSetTitleTextPacket() {
        return PacketIdFinder.clientboundByClazz(NetworkReflections.clazz$ClientboundSetTitleTextPacket);
    }

    @Override
    public int clientboundSetSubtitleTextPacket() {
        return PacketIdFinder.clientboundByClazz(NetworkReflections.clazz$ClientboundSetSubtitleTextPacket);
    }

    @Override
    public int clientboundSetActionBarTextPacket() {
        return PacketIdFinder.clientboundByClazz(NetworkReflections.clazz$ClientboundSetActionBarTextPacket);
    }

    @Override
    public int clientboundBossEventPacket() {
        return PacketIdFinder.clientboundByClazz(NetworkReflections.clazz$ClientboundBossEventPacket);
    }

    @Override
    public int clientboundSystemChatPacket() {
        return PacketIdFinder.clientboundByClazz(NetworkReflections.clazz$ClientboundSystemChatPacket);
    }

    @Override
    public int clientboundTabListPacket() {
        return PacketIdFinder.clientboundByClazz(NetworkReflections.clazz$ClientboundTabListPacket);
    }

    @Override
    public int clientboundSetPlayerTeamPacket() {
        return PacketIdFinder.clientboundByClazz(NetworkReflections.clazz$ClientboundSetPlayerTeamPacket);
    }

    @Override
    public int clientboundSetObjectivePacket() {
        return PacketIdFinder.clientboundByClazz(NetworkReflections.clazz$ClientboundSetObjectivePacket);
    }

    @Override
    public int clientboundLevelChunkWithLightPacket() {
        return PacketIdFinder.clientboundByClazz(NetworkReflections.clazz$ClientboundLevelChunkWithLightPacket);
    }

    @Override
    public int clientboundPlayerInfoUpdatePacket() {
        return PacketIdFinder.clientboundByClazz(NetworkReflections.clazz$ClientboundPlayerInfoUpdatePacket);
    }

    @Override
    public int clientboundSetScorePacket() {
        return PacketIdFinder.clientboundByClazz(NetworkReflections.clazz$ClientboundSetScorePacket);
    }

    @Override
    public int clientboundContainerSetContentPacket() {
        return PacketIdFinder.clientboundByClazz(NetworkReflections.clazz$ClientboundContainerSetContentPacket);
    }

    @Override
    public int clientboundContainerSetSlotPacket() {
        return PacketIdFinder.clientboundByClazz(NetworkReflections.clazz$ClientboundContainerSetSlotPacket);
    }

    @Override
    public int clientboundSetCursorItemPacket() {
        return PacketIdFinder.clientboundByClazz(NetworkReflections.clazz$ClientboundSetCursorItemPacket);
    }

    @Override
    public int clientboundSetEquipmentPacket() {
        return PacketIdFinder.clientboundByClazz(NetworkReflections.clazz$ClientboundSetEquipmentPacket);
    }

    @Override
    public int clientboundSetPlayerInventoryPacket() {
        return PacketIdFinder.clientboundByClazz(NetworkReflections.clazz$ClientboundSetPlayerInventoryPacket);
    }

    @Override
    public int serverboundContainerClickPacket() {
        return PacketIdFinder.serverboundByClazz(NetworkReflections.clazz$ServerboundContainerClickPacket);
    }

    @Override
    public int serverboundSetCreativeModeSlotPacket() {
        return PacketIdFinder.serverboundByClazz(NetworkReflections.clazz$ServerboundSetCreativeModeSlotPacket);
    }

    @Override
    public int clientboundBlockEventPacket() {
        return PacketIdFinder.clientboundByClazz(NetworkReflections.clazz$ClientboundBlockEventPacket);
    }

    @Override
    public int serverboundInteractPacket() {
        return PacketIdFinder.serverboundByClazz(NetworkReflections.clazz$ServerboundInteractPacket);
    }

    @Override
    public int clientboundRecipeBookAddPacket() {
        return PacketIdFinder.clientboundByClazz(NetworkReflections.clazz$ClientboundRecipeBookAddPacket);
    }

    @Override
    public int clientboundPlaceGhostRecipePacket() {
        return PacketIdFinder.clientboundByClazz(NetworkReflections.clazz$ClientboundPlaceGhostRecipePacket);
    }

    @Override
    public int clientboundUpdateRecipesPacket() {
        return PacketIdFinder.clientboundByClazz(NetworkReflections.clazz$ClientboundUpdateRecipesPacket);
    }

    @Override
    public int clientboundUpdateAdvancementsPacket() {
        return PacketIdFinder.clientboundByClazz(NetworkReflections.clazz$ClientboundUpdateAdvancementsPacket);
    }
}
