package net.momirealms.craftengine.bedrock.geyser.entity;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.bedrock.geyser.entity.display.GeyserDisplayEntityTranslator;
import net.momirealms.craftengine.bedrock.geyser.entity.display.GeyserItemDisplays;
import net.momirealms.craftengine.bedrock.geyser.entity.display.GeyserTextDisplayManager;
import net.momirealms.craftengine.bedrock.geyser.entity.hitbox.GeyserHardHitboxManager;
import net.momirealms.craftengine.bedrock.geyser.entity.model.GeyserNativeModels;
import net.momirealms.craftengine.bedrock.geyser.glyph.GeyserGlyphData;
import net.momirealms.craftengine.bedrock.geyser.player.GeyserPlayer;
import net.momirealms.craftengine.bedrock.geyser.player.GeyserPlayerManager;
import net.momirealms.craftengine.bedrock.network.NativeModelPayload;
import org.geysermc.geyser.api.event.java.ServerSpawnEntityEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineEntitiesEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineEntityPropertiesEvent;
import org.geysermc.geyser.session.GeyserSession;

public final class GeyserEntityManager {
    private final GeyserPlayerManager players;
    private final GeyserItemDisplays itemDisplays = new GeyserItemDisplays();
    private final GeyserHardHitboxManager hardHitboxes;
    private final GeyserNativeModels nativeModels;
    private GeyserDisplayEntityTranslator displayTranslator;

    public GeyserEntityManager(GeyserPlayerManager players, JsonObject nativeModels) {
        this.players = players;
        this.hardHitboxes = new GeyserHardHitboxManager(players, this.itemDisplays);
        this.nativeModels = new GeyserNativeModels(nativeModels);
    }

    @SuppressWarnings("UnstableApiUsage")
    public void registerEntities(JsonObject items, GeyserDefineEntitiesEvent event) {
        this.nativeModels.register(event);
        this.itemDisplays.registerEntities(items, event);
    }

    public void registerProperties(GeyserDefineEntityPropertiesEvent event) {
        this.nativeModels.properties(event);
        this.itemDisplays.properties(event);
    }

    public void initialize(GeyserGlyphData glyphs) {
        this.itemDisplays.initialize();
        GeyserTextDisplayManager textDisplays = new GeyserTextDisplayManager(this.players, glyphs);
        this.displayTranslator = GeyserDisplayEntityTranslator.install(this.itemDisplays, textDisplays);
    }

    @SuppressWarnings("UnstableApiUsage")
    public void onSpawn(ServerSpawnEntityEvent event) {
        this.hardHitboxes.onSpawn(event);
    }

    public void acceptNativeModel(GeyserPlayer player, NativeModelPayload state) {
        this.nativeModels.accept(player, state);
    }

    public void disconnect(GeyserSession session) {
        this.nativeModels.disconnect(session);
    }

    public void disable() {
        if (this.displayTranslator != null) {
            this.displayTranslator.uninstall();
            this.displayTranslator = null;
        }
        this.itemDisplays.disable();
    }
}
