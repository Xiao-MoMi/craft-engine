package net.momirealms.craftengine.bedrock.geyser;

import net.momirealms.craftengine.bedrock.geyser.block.GeyserBlockManager;
import net.momirealms.craftengine.bedrock.geyser.config.GeyserConfig;
import net.momirealms.craftengine.bedrock.geyser.entity.display.GeyserItemDisplays;
import net.momirealms.craftengine.bedrock.geyser.entity.display.GeyserTextDisplayManager;
import net.momirealms.craftengine.bedrock.geyser.entity.hitbox.GeyserHardHitboxManager;
import net.momirealms.craftengine.bedrock.geyser.glyph.GeyserGlyphManager;
import net.momirealms.craftengine.bedrock.geyser.gui.GeyserGuiMenus;
import net.momirealms.craftengine.bedrock.geyser.item.GeyserItemManager;
import net.momirealms.craftengine.bedrock.geyser.locale.GeyserTranslationManager;
import net.momirealms.craftengine.bedrock.geyser.locale.GeyserTranslationManagerImpl;
import net.momirealms.craftengine.bedrock.geyser.network.GeyserCustomPayloads;
import net.momirealms.craftengine.bedrock.geyser.pack.GeyserExtensionData;
import net.momirealms.craftengine.bedrock.geyser.player.GeyserPlayerManager;
import net.momirealms.craftengine.bedrock.geyser.scoreboard.GeyserSidebarManager;
import net.momirealms.craftengine.bedrock.geyser.translation.GeyserLanguageManager;
import org.geysermc.event.PostOrder;
import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.api.event.bedrock.SessionDisconnectEvent;
import org.geysermc.geyser.api.event.bedrock.SessionInitializeEvent;
import org.geysermc.geyser.api.event.java.ServerSpawnEntityEvent;
import org.geysermc.geyser.api.event.lifecycle.*;
import org.geysermc.geyser.api.extension.Extension;
import org.geysermc.geyser.session.GeyserSession;

import java.io.IOException;
import java.nio.file.Path;

@SuppressWarnings("UnstableApiUsage")
public final class CraftEngineGeyserExtension implements Extension {
    private static CraftEngineGeyserExtension instance;

    private GeyserPlayerManager playerManager;
    private GeyserSidebarManager sidebarManager;
    private GeyserConfig config;
    private GeyserTranslationManager translations;
    private GeyserItemDisplays itemDisplays;
    private GeyserTextDisplayManager textDisplayManager;
    private GeyserHardHitboxManager hardHitboxManager;
    private GeyserGlyphManager glyphManager;
    private GeyserLanguageManager languageManager;
    private GeyserBlockManager blockManager;
    private GeyserItemManager itemManager;
    private GeyserExtensionData extensionData;
    private GeyserCustomPayloads customPayloads;

    @Subscribe
    public void onPreInitialize(GeyserPreInitializeEvent event) {
        instance = this;
        this.playerManager = new GeyserPlayerManager(this);
        this.config = new GeyserConfig(this.dataFolder(), this.logger());
        this.config.load();
        try {
            this.translations = new GeyserTranslationManagerImpl(this.dataFolder(), this.logger());
            this.translations.load();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to initialize translations", exception);
        }
        this.itemDisplays = new GeyserItemDisplays();
        this.hardHitboxManager = new GeyserHardHitboxManager(this);
        this.blockManager = new GeyserBlockManager();
        this.customPayloads = new GeyserCustomPayloads();
        this.glyphManager = new GeyserGlyphManager();
        this.languageManager = new GeyserLanguageManager();
        this.itemManager = new GeyserItemManager(this);
        try {
            this.extensionData = GeyserExtensionData.read(this.dataFile());
            this.languageManager.load(this.extensionData.languages());
            this.glyphManager.load(this.extensionData.glyphs());
        } catch (IOException exception) {
            throw new IllegalStateException(GeyserTranslationManager.message("extension.data_failed"), exception);
        }
    }

    private Path dataFile() {
        return this.dataFolder().resolve("data/craftengine.cedata");
    }

    @Subscribe
    public void onPostInitialize(GeyserPostInitializeEvent event) {
        this.itemDisplays.install();
        if (this.extensionData.scoreboard().enabled() && this.glyphManager.glyphs().layout() != null) {
            this.sidebarManager = new GeyserSidebarManager(this.glyphManager.glyphs().layout(), this.extensionData.scoreboard());
            this.sidebarManager.install();
        }
        if (this.extensionData.convertText()) {
            this.textDisplayManager = new GeyserTextDisplayManager();
            this.textDisplayManager.install();
        }
        this.itemManager.init();
        this.blockManager.init();
        this.customPayloads.install();
        GeyserGuiMenus.install(this.glyphManager.glyphs().layout());
        this.blockManager.initPlacementHandler();
    }

    @Subscribe
    public void onHardHitboxSpawn(ServerSpawnEntityEvent event) {
        this.hardHitboxManager.onSpawn(event);
    }

    @Subscribe
    public void onDisconnect(SessionDisconnectEvent event) {
        this.playerManager.remove(event.connection());
    }

    @Subscribe
    public void onShutdown(GeyserShutdownEvent event) {
        if (this.playerManager != null) {
            this.playerManager.disable();
        }
        if (this.sidebarManager != null) {
            this.sidebarManager.uninstall();
        }
        if (this.customPayloads != null) {
            this.customPayloads.uninstall();
        }
        if (this.blockManager != null) {
            this.blockManager.unload();
        }
        if (this.itemManager != null) {
            this.itemManager.disable();
        }
        if (this.textDisplayManager != null) {
            this.textDisplayManager.uninstall();
        }
        this.itemDisplays.uninstall();
        if (this.blockManager != null) {
            this.blockManager.disable();
        }
        this.glyphManager.disable();
        if (this.languageManager != null) {
            this.languageManager.disable();
        }
    }

    // Geyser installs its built-in mushroom mappings after extensions, at NORMAL order.
    // Apply CE's state overrides afterwards so their geometry and mining components survive.
    @Subscribe(postOrder = PostOrder.LAST)
    public void onDefineCustomBlocks(GeyserDefineCustomBlocksEvent event) {
        this.blockManager.registerBlocks(this.extensionData.blocks(), event);
    }

    @Subscribe
    public void onDefineEntities(GeyserDefineEntitiesEvent event) {
        this.itemDisplays.registerEntities(this.extensionData.items(), event);
    }

    @Subscribe
    public void onDefineEntityProperties(GeyserDefineEntityPropertiesEvent event) {
        this.itemDisplays.properties(event);
        this.itemManager.registerProperties(event);
    }

    @Subscribe
    public void onDefineCustomItems(GeyserDefineCustomItemsEvent event) {
        this.itemManager.registerItems(this.extensionData.items(), event);
    }

    @Subscribe
    public void onSessionInitialize(SessionInitializeEvent event) {
        this.playerManager.add((GeyserSession) event.connection());
    }

    public static CraftEngineGeyserExtension instance() {
        return instance;
    }

    public GeyserPlayerManager playerManager() {
        return this.playerManager;
    }

    public GeyserSidebarManager sidebarManager() {
        return this.sidebarManager;
    }

    public GeyserConfig config() {
        return this.config;
    }

    public GeyserTranslationManager translationManager() {
        return this.translations;
    }

    public GeyserHardHitboxManager hardHitboxManager() {
        return this.hardHitboxManager;
    }

    public GeyserTextDisplayManager textDisplayManager() {
        return this.textDisplayManager;
    }

    public GeyserItemDisplays itemDisplays() {
        return this.itemDisplays;
    }

    public GeyserGlyphManager glyphManager() {
        return this.glyphManager;
    }

    public GeyserLanguageManager languageManager() {
        return this.languageManager;
    }

    public GeyserBlockManager blockManager() {
        return this.blockManager;
    }

    public GeyserItemManager itemManager() {
        return this.itemManager;
    }

    public GeyserExtensionData extensionData() {
        return this.extensionData;
    }

    public GeyserCustomPayloads customPayloads() {
        return this.customPayloads;
    }
}
