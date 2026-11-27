package net.momirealms.craftengine.bedrock.geyser.entity;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.bedrock.geyser.entity.display.GeyserDisplayModelOwners;
import net.momirealms.craftengine.bedrock.geyser.entity.display.GeyserModelDisplayManager;
import net.momirealms.craftengine.bedrock.geyser.entity.display.GeyserTextDisplayManager;
import net.momirealms.craftengine.bedrock.geyser.entity.model.GeyserNativeModels;
import net.momirealms.craftengine.bedrock.geyser.glyph.GeyserGlyphData;
import net.momirealms.craftengine.bedrock.geyser.player.GeyserPlayer;
import net.momirealms.craftengine.bedrock.geyser.player.GeyserPlayerManager;
import net.momirealms.craftengine.bedrock.network.NativeModelPayload;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineEntitiesEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineEntityPropertiesEvent;
import org.geysermc.geyser.session.GeyserSession;
import org.jetbrains.annotations.Nullable;

public final class GeyserEntityManager {
    private final GeyserPlayerManager players;
    private final GeyserModelDisplayManager modelDisplays;
    private final GeyserNativeModels nativeModels;
    private GeyserEntitySpawnTranslator spawnTranslator;

    public GeyserEntityManager(GeyserPlayerManager players, JsonObject nativeModels) {
        this.players = players;
        this.modelDisplays = new GeyserModelDisplayManager(players);
        this.nativeModels = new GeyserNativeModels(nativeModels);
    }

    @SuppressWarnings("UnstableApiUsage")
    public void registerEntities(@Nullable JsonObject modelDisplayData, GeyserDefineEntitiesEvent event) {
        this.nativeModels.register(event);
        this.modelDisplays.registerEntities(modelDisplayData, event);
    }

    public void registerProperties(GeyserDefineEntityPropertiesEvent event) {
        this.nativeModels.properties(event);
        this.modelDisplays.properties(event);
    }

    public void initialize(GeyserGlyphData glyphs) {
        this.modelDisplays.initialize();
        GeyserTextDisplayManager textDisplays = new GeyserTextDisplayManager(this.players, glyphs);
        this.spawnTranslator = new GeyserEntitySpawnTranslator(this.modelDisplays, textDisplays);
    }

    public GeyserEntitySpawnTranslator spawnTranslator() {
        return this.spawnTranslator;
    }

    public void acceptNativeModel(GeyserPlayer player, NativeModelPayload state) {
        switch (state) {
            case NativeModelPayload.DisplayBones bones -> this.modelDisplays.updateOwner(player, bones);
            case NativeModelPayload.RemoveDisplay remove -> this.modelDisplays.removeOwner(player, remove.instance());
            case NativeModelPayload.NativeMessage message -> player.session().ensureInEventLoop(() -> {
                if (!player.isClosed() && player.nativeModels() != null) {
                    player.nativeModels().accept(message);
                }
            });
        }
    }

    public GeyserNativeModels.Connection createNativeConnection(GeyserSession session) {
        return this.nativeModels.createConnection(session);
    }

    public GeyserDisplayModelOwners createDisplayOwners(GeyserSession session) {
        return this.modelDisplays.createOwners(session);
    }

    public void disable() {
        this.spawnTranslator = null;
        this.modelDisplays.disable();
    }
}
