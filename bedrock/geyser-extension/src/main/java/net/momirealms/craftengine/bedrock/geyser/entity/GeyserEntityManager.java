package net.momirealms.craftengine.bedrock.geyser.entity;

import com.google.gson.JsonObject;
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
    private final GeyserModelDisplayManager modelDisplays = new GeyserModelDisplayManager();
    private final GeyserNativeModels nativeModels;
    private GeyserEntitySpawnTranslator spawnTranslator;

    public GeyserEntityManager(GeyserPlayerManager players, JsonObject nativeModels) {
        this.players = players;
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
        this.spawnTranslator = GeyserEntitySpawnTranslator.install(this.modelDisplays, textDisplays, this.players);
    }

    public void acceptNativeModel(GeyserPlayer player, NativeModelPayload state) {
        this.nativeModels.accept(player, state);
    }

    public void disconnect(GeyserSession session) {
        this.nativeModels.disconnect(session);
    }

    public void disable() {
        if (this.spawnTranslator != null) {
            this.spawnTranslator.uninstall();
            this.spawnTranslator = null;
        }
        this.modelDisplays.disable();
    }
}
