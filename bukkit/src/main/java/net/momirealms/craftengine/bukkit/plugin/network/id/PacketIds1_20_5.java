package net.momirealms.craftengine.bukkit.plugin.network.id;

import net.momirealms.craftengine.core.plugin.network.ConnectionState;
import net.momirealms.craftengine.core.plugin.network.PacketFlow;
import net.momirealms.craftengine.core.util.VersionHelper;

final class PacketIds1_20_5 implements PacketIds {

    PacketIds1_20_5() {}

    @Override
    public int clientboundBlockUpdatePacket() {
        return PacketIdHelper.byName("minecraft:block_update", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientboundSectionBlocksUpdatePacket() {
        return PacketIdHelper.byName("minecraft:section_blocks_update", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientboundLevelParticlesPacket() {
        return PacketIdHelper.byName("minecraft:level_particles", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientboundLevelEventPacket() {
        return PacketIdHelper.byName("minecraft:level_event", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientboundAddEntityPacket() {
        return PacketIdHelper.byName("minecraft:add_entity", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientboundOpenScreenPacket() {
        return PacketIdHelper.byName("minecraft:open_screen", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientboundSoundPacket() {
        return PacketIdHelper.byName("minecraft:sound", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientboundRemoveEntitiesPacket() {
        return PacketIdHelper.byName("minecraft:remove_entities", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientboundSetEntityDataPacket() {
        return PacketIdHelper.byName("minecraft:set_entity_data", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientboundSetTitleTextPacket() {
        return PacketIdHelper.byName("minecraft:set_title_text", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientboundSetSubtitleTextPacket() {
        return PacketIdHelper.byName("minecraft:set_subtitle_text", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientboundSetActionBarTextPacket() {
        return PacketIdHelper.byName("minecraft:set_action_bar_text", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientboundBossEventPacket() {
        return PacketIdHelper.byName("minecraft:boss_event", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientboundSystemChatPacket() {
        return PacketIdHelper.byName("minecraft:system_chat", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientboundTabListPacket() {
        return PacketIdHelper.byName("minecraft:tab_list", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientboundSetPlayerTeamPacket() {
        return PacketIdHelper.byName("minecraft:set_player_team", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientboundSetObjectivePacket() {
        return PacketIdHelper.byName("minecraft:set_objective", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientboundLevelChunkWithLightPacket() {
        return PacketIdHelper.byName("minecraft:level_chunk_with_light", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientboundPlayerInfoUpdatePacket() {
        return PacketIdHelper.byName("minecraft:player_info_update", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientboundSetScorePacket() {
        return PacketIdHelper.byName("minecraft:set_score", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientboundContainerSetContentPacket() {
        return PacketIdHelper.byName("minecraft:container_set_content", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientboundContainerSetSlotPacket() {
        return PacketIdHelper.byName("minecraft:container_set_slot", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientboundSetCursorItemPacket() {
        return PacketIdHelper.byName("minecraft:set_cursor_item", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientboundSetEquipmentPacket() {
        return PacketIdHelper.byName("minecraft:set_equipment", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientboundSetPlayerInventoryPacket() {
        return PacketIdHelper.byName("minecraft:set_player_inventory", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientboundBlockEventPacket() {
        return PacketIdHelper.byName("minecraft:block_event", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientboundRecipeBookAddPacket() {
        return PacketIdHelper.byName("minecraft:recipe_book_add", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientboundPlaceGhostRecipePacket() {
        return PacketIdHelper.byName("minecraft:place_ghost_recipe", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientboundUpdateRecipesPacket() {
        return PacketIdHelper.byName("minecraft:update_recipes", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientboundUpdateAdvancementsPacket() {
        return PacketIdHelper.byName("minecraft:update_advancements", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientboundForgetLevelChunkPacket() {
        return PacketIdHelper.byName("minecraft:forget_level_chunk", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientBoundMerchantOffersPacket() {
        return PacketIdHelper.byName("minecraft:merchant_offers", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientboundBlockEntityDataPacket() {
        return PacketIdHelper.byName("minecraft:block_entity_data", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int serverboundContainerClickPacket() {
        return PacketIdHelper.byName("minecraft:container_click", ConnectionState.PLAY, PacketFlow.SERVERBOUND);
    }

    @Override
    public int serverboundSetCreativeModeSlotPacket() {
        return PacketIdHelper.byName("minecraft:set_creative_mode_slot", ConnectionState.PLAY, PacketFlow.SERVERBOUND);
    }

    @Override
    public int serverboundInteractPacket() {
        return PacketIdHelper.byName("minecraft:interact", ConnectionState.PLAY, PacketFlow.SERVERBOUND);
    }

    @Override
    public int serverboundCustomPayloadPacket$play() {
        return PacketIdHelper.byName("minecraft:custom_payload", ConnectionState.PLAY, PacketFlow.SERVERBOUND);
    }

    @Override
    public int serverboundCustomPayloadPacket$configuration() {
        return PacketIdHelper.byName("minecraft:custom_payload", ConnectionState.CONFIGURATION, PacketFlow.SERVERBOUND);
    }

    @Override
    public int clientboundPlayerChatPacket() {
        return PacketIdHelper.byName("minecraft:player_chat", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientIntentionPacket() {
        return PacketIdHelper.byName("minecraft:intention", ConnectionState.HANDSHAKING, PacketFlow.SERVERBOUND);
    }

    @Override
    public int clientboundStatusResponsePacket() {
        return PacketIdHelper.byName("minecraft:status_response", ConnectionState.STATUS, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int serverboundFinishConfigurationPacket() {
        return PacketIdHelper.byName("minecraft:finish_configuration", ConnectionState.CONFIGURATION, PacketFlow.SERVERBOUND);
    }

    @Override
    public int clientboundLoginPacket() {
        return PacketIdHelper.byName("minecraft:login", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientboundLoginFinishedPacket() {
        return PacketIdHelper.byName(VersionHelper.isOrAbove1_21_2 ? "minecraft:login_finished" : "minecraft:game_profile", ConnectionState.LOGIN, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int serverboundLoginAcknowledgedPacket() {
        return PacketIdHelper.byName("minecraft:login_acknowledged", ConnectionState.LOGIN, PacketFlow.SERVERBOUND);
    }

    @Override
    public int clientboundStartConfigurationPacket() {
        return PacketIdHelper.byName("minecraft:start_configuration", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int serverboundConfigurationAcknowledgedPacket() {
        return PacketIdHelper.byName("minecraft:configuration_acknowledged", ConnectionState.PLAY, PacketFlow.SERVERBOUND);
    }

    @Override
    public int clientboundCustomPayloadPacket$play() {
        return PacketIdHelper.byName("minecraft:custom_payload", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientboundCustomPayloadPacket$configuration() {
        return PacketIdHelper.byName("minecraft:custom_payload", ConnectionState.CONFIGURATION, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int serverboundAttackPacket() {
        return PacketIdHelper.byName("minecraft:attack", ConnectionState.PLAY, PacketFlow.SERVERBOUND);
    }

    @Override
    public int clientboundCustomChatCompletionsPacket() {
        return PacketIdHelper.byName("minecraft:custom_chat_completions", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientboundFinishConfigurationPacket() {
        return PacketIdHelper.byName("minecraft:finish_configuration", ConnectionState.CONFIGURATION, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int serverboundChatSessionUpdatePacket() {
        return PacketIdHelper.byName("minecraft:chat_session_update", ConnectionState.PLAY, PacketFlow.SERVERBOUND);
    }

    @Override
    public int clientboundServerDataPacket() {
        return PacketIdHelper.byName("minecraft:server_data", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int serverboundHelloPacket() {
        return PacketIdHelper.byName("minecraft:hello", ConnectionState.LOGIN, PacketFlow.SERVERBOUND);
    }

    @Override
    public int serverboundSwingPacket() {
        return PacketIdHelper.byName("minecraft:swing", ConnectionState.PLAY, PacketFlow.SERVERBOUND);
    }

    @Override
    public int serverboundPlayerActionPacket() {
        return PacketIdHelper.byName("minecraft:player_action", ConnectionState.PLAY, PacketFlow.SERVERBOUND);
    }

    @Override
    public int serverboundUseItemOnPacket() {
        return PacketIdHelper.byName("minecraft:use_item_on", ConnectionState.PLAY, PacketFlow.SERVERBOUND);
    }

    @Override
    public int serverboundPickItemFromBlockPacket() {
        return PacketIdHelper.byName("minecraft:pick_item_from_block", ConnectionState.PLAY, PacketFlow.SERVERBOUND);
    }

    @Override
    public int serverboundPickItemFromEntityPacket() {
        return PacketIdHelper.byName("minecraft:pick_item_from_entity", ConnectionState.PLAY, PacketFlow.SERVERBOUND);
    }

    @Override
    public int clientboundUpdateTagsPacket$play() {
        return PacketIdHelper.byName("minecraft:update_tags", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    public int clientboundupdatetagspacket$configuration() {
        return PacketIdHelper.byName("minecraft:update_tags", ConnectionState.CONFIGURATION, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientboundEntityEventPacket() {
        return PacketIdHelper.byName("minecraft:entity_event", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int serverboundResourcePackPacket$play() {
        return PacketIdHelper.byName("minecraft:resource_pack", ConnectionState.PLAY, PacketFlow.SERVERBOUND);
    }

    @Override
    public int serverboundResourcePackPacket$configuration() {
        return PacketIdHelper.byName("minecraft:resource_pack", ConnectionState.CONFIGURATION, PacketFlow.SERVERBOUND);
    }

    @Override
    public int serverboundRenameItemPacket() {
        return PacketIdHelper.byName("minecraft:rename_item", ConnectionState.PLAY, PacketFlow.SERVERBOUND);
    }

    @Override
    public int serverboundSignUpdatePacket() {
        return PacketIdHelper.byName("minecraft:sign_update", ConnectionState.PLAY, PacketFlow.SERVERBOUND);
    }

    @Override
    public int serverboundEditBookPacket() {
        return PacketIdHelper.byName("minecraft:edit_book", ConnectionState.PLAY, PacketFlow.SERVERBOUND);
    }

    @Override
    public int clientboundEntityPositionSyncPacket() {
        return PacketIdHelper.byName("minecraft:entity_position_sync", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientboundMoveEntityPacket$PosRot() {
        return PacketIdHelper.byName("minecraft:move_entity_pos_rot", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientboundMoveEntityPacket$Pos() {
        return PacketIdHelper.byName("minecraft:move_entity_pos", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientboundRespawnPacket() {
        return PacketIdHelper.byName("minecraft:respawn", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int serverboundClientInformationPacket$play() {
        return PacketIdHelper.byName("minecraft:client_information", ConnectionState.PLAY, PacketFlow.SERVERBOUND);
    }

    @Override
    public int serverboundClientInformationPacket$configuration() {
        return PacketIdHelper.byName("minecraft:client_information", ConnectionState.CONFIGURATION, PacketFlow.SERVERBOUND);
    }

    @Override
    public int clientboundRegistryDataPacket() {
        return PacketIdHelper.byName("minecraft:registry_data", ConnectionState.CONFIGURATION, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientboundShowDialogPacket$play() {
        return PacketIdHelper.byName("minecraft:show_dialog", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    @Override
    public int clientboundShowDialogPacket$configuration() {
        return PacketIdHelper.byName("minecraft:show_dialog", ConnectionState.CONFIGURATION, PacketFlow.CLIENTBOUND);
    }
}
