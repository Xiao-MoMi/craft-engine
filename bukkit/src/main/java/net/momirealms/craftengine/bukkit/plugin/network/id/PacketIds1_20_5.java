package net.momirealms.craftengine.bukkit.plugin.network.id;

import net.momirealms.craftengine.bukkit.plugin.network.PacketIds;

public class PacketIds1_20_5 implements PacketIds {

    @Override
    public int clientboundBlockUpdatePacket() {
        return PacketIdFinder.clientboundByName("minecraft:block_update");
    }

    @Override
    public int clientboundSectionBlocksUpdatePacket() {
        return PacketIdFinder.clientboundByName("minecraft:section_blocks_update");
    }

    @Override
    public int clientboundLevelParticlesPacket() {
        return PacketIdFinder.clientboundByName("minecraft:level_particles");
    }

    @Override
    public int clientboundLevelEventPacket() {
        return PacketIdFinder.clientboundByName("minecraft:level_event");
    }

    @Override
    public int clientboundAddEntityPacket() {
        return PacketIdFinder.clientboundByName("minecraft:add_entity");
    }

    @Override
    public int clientboundOpenScreenPacket() {
        return PacketIdFinder.clientboundByName("minecraft:open_screen");
    }

    @Override
    public int clientboundSoundPacket() {
        return PacketIdFinder.clientboundByName("minecraft:sound");
    }

    @Override
    public int clientboundRemoveEntitiesPacket() {
        return PacketIdFinder.clientboundByName("minecraft:remove_entities");
    }

    @Override
    public int clientboundSetEntityDataPacket() {
        return PacketIdFinder.clientboundByName("minecraft:set_entity_data");
    }

    @Override
    public int clientboundSetTitleTextPacket() {
        return PacketIdFinder.clientboundByName("minecraft:set_title_text");
    }

    @Override
    public int clientboundSetSubtitleTextPacket() {
        return PacketIdFinder.clientboundByName("minecraft:set_subtitle_text");
    }

    @Override
    public int clientboundSetActionBarTextPacket() {
        return PacketIdFinder.clientboundByName("minecraft:set_action_bar_text");
    }

    @Override
    public int clientboundBossEventPacket() {
        return PacketIdFinder.clientboundByName("minecraft:boss_event");
    }

    @Override
    public int clientboundSystemChatPacket() {
        return PacketIdFinder.clientboundByName("minecraft:system_chat");
    }

    @Override
    public int clientboundTabListPacket() {
        return PacketIdFinder.clientboundByName("minecraft:tab_list");
    }

    @Override
    public int clientboundSetPlayerTeamPacket() {
        return PacketIdFinder.clientboundByName("minecraft:set_player_team");
    }

    @Override
    public int clientboundSetObjectivePacket() {
        return PacketIdFinder.clientboundByName("minecraft:set_objective");
    }

    @Override
    public int clientboundLevelChunkWithLightPacket() {
        return PacketIdFinder.clientboundByName("minecraft:level_chunk_with_light");
    }

    @Override
    public int clientboundPlayerInfoUpdatePacket() {
        return PacketIdFinder.clientboundByName("minecraft:player_info_update");
    }

    @Override
    public int clientboundSetScorePacket() {
        return PacketIdFinder.clientboundByName("minecraft:set_score");
    }

    @Override
    public int clientboundContainerSetContentPacket() {
        return PacketIdFinder.clientboundByName("minecraft:container_set_content");
    }

    @Override
    public int clientboundContainerSetSlotPacket() {
        return PacketIdFinder.clientboundByName("minecraft:container_set_slot");
    }

    @Override
    public int clientboundSetCursorItemPacket() {
        return PacketIdFinder.clientboundByName("minecraft:set_cursor_item");
    }

    @Override
    public int clientboundSetEquipmentPacket() {
        return PacketIdFinder.clientboundByName("minecraft:set_equipment");
    }

    @Override
    public int clientboundSetPlayerInventoryPacket() {
        return PacketIdFinder.clientboundByName("minecraft:set_player_inventory");
    }

    @Override
    public int clientboundBlockEventPacket() {
        return PacketIdFinder.clientboundByName("minecraft:block_event");
    }

    @Override
    public int clientboundRecipeBookAddPacket() {
        return PacketIdFinder.clientboundByName("minecraft:recipe_book_add");
    }

    @Override
    public int clientboundPlaceGhostRecipePacket() {
        return PacketIdFinder.clientboundByName("minecraft:place_ghost_recipe");
    }

    @Override
    public int clientboundUpdateRecipesPacket() {
        return PacketIdFinder.clientboundByName("minecraft:update_recipes");
    }

    @Override
    public int clientboundUpdateAdvancementsPacket() {
        return PacketIdFinder.clientboundByName("minecraft:update_advancements");
    }

    @Override
    public int serverboundContainerClickPacket() {
        return PacketIdFinder.serverboundByName("minecraft:container_click");
    }

    @Override
    public int serverboundSetCreativeModeSlotPacket() {
        return PacketIdFinder.serverboundByName("minecraft:set_creative_mode_slot");
    }

    @Override
    public int serverboundInteractPacket() {
        return PacketIdFinder.serverboundByName("minecraft:interact");
    }
}
