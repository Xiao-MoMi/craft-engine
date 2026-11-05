package net.momirealms.craftengine.bedrock.geyser;

import net.momirealms.craftengine.bedrock.geyser.block.GeyserBlockManager;
import net.momirealms.craftengine.bedrock.geyser.display.GeyserItemDisplays;
import net.momirealms.craftengine.bedrock.geyser.gui.GeyserContainerCloseHandler;
import net.momirealms.craftengine.bedrock.geyser.gui.GeyserGuiMenus;
import net.momirealms.craftengine.bedrock.geyser.item.GeyserItemManager;
import net.momirealms.craftengine.bedrock.geyser.network.GeyserCustomPayloads;
import net.momirealms.craftengine.bedrock.geyser.pack.GeyserPackManager;
import net.momirealms.craftengine.bedrock.geyser.text.GeyserTextManager;
import org.geysermc.event.PostOrder;
import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.api.event.bedrock.SessionDisconnectEvent;
import org.geysermc.geyser.api.event.bedrock.SessionInitializeEvent;
import org.geysermc.geyser.api.event.bedrock.SessionLoadResourcePacksEvent;
import org.geysermc.geyser.api.event.java.ServerSpawnEntityEvent;
import org.geysermc.geyser.api.event.lifecycle.*;
import org.geysermc.geyser.api.extension.Extension;
import org.geysermc.geyser.session.GeyserSession;

import java.io.IOException;

@SuppressWarnings("UnstableApiUsage")
public final class CraftEngineGeyserExtension implements Extension {
    private final GeyserItemDisplays itemDisplays = new GeyserItemDisplays();
    private final GeyserTextManager textManager = new GeyserTextManager();
    private GeyserBlockManager blockManager;
    private GeyserItemManager itemManager;
    private GeyserPackManager packManager;
    private GeyserCustomPayloads customPayloads;

    @Subscribe
    public void onPreInitialize(GeyserPreInitializeEvent event) {
        this.textManager.init();
        this.blockManager = new GeyserBlockManager(this.logger());
        this.customPayloads = new GeyserCustomPayloads();
        this.itemManager = new GeyserItemManager(this.logger(), this.blockManager);
        try {
            this.packManager = new GeyserPackManager(
                    this.geyserApi().packDirectory(),
                    this.dataFolder().resolve("data"),
                    this.dataFolder().resolve("pack-snapshots")
            );
            this.textManager.load(this.packManager.text(), this.geyserApi().configDirectory());
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load CraftEngine pack/data pairs", exception);
        }
    }

    @Subscribe
    public void onPostInitialize(GeyserPostInitializeEvent event) {
        this.itemDisplays.install();
        this.itemManager.init();
        this.blockManager.init();
        this.customPayloads.install();
        GeyserGuiMenus.install(this.logger(), this.textManager.data().layout());
        this.blockManager.initPlacementHandler();
        this.packManager.prepare();
    }

    @Subscribe
    public void onFurnitureShulkerSpawn(ServerSpawnEntityEvent event) {
        this.itemDisplays.onFurnitureSpawn(event);
    }

    @Subscribe
    public void onLoadPacks(SessionLoadResourcePacksEvent event) {
        this.packManager.load(event);
    }

    @Subscribe
    public void onDisconnect(SessionDisconnectEvent event) {
        this.blockManager.disconnect(event.connection());
    }

    @Subscribe
    public void onShutdown(GeyserShutdownEvent event) {
        if (this.customPayloads != null) {
            this.customPayloads.uninstall();
        }
        if (this.blockManager != null) {
            this.blockManager.unload();
        }
        if (this.itemManager != null) {
            this.itemManager.disable();
        }
        this.itemDisplays.uninstall();
        if (this.blockManager != null) {
            this.blockManager.disable();
        }
        this.textManager.disable();
    }

    // Geyser installs its built-in mushroom mappings after extensions, at NORMAL order.
    // Apply CE's state overrides afterwards so their geometry and mining components survive.
    @Subscribe(postOrder = PostOrder.LAST)
    public void onDefineCustomBlocks(GeyserDefineCustomBlocksEvent event) {
        this.blockManager.registerBlocks(this.packManager.registrationFiles(), event);
    }

    @Subscribe
    public void onDefineEntities(GeyserDefineEntitiesEvent event) {
        this.itemDisplays.registerEntities(this.packManager.registrationFiles(), event, this.logger());
    }

    @Subscribe
    public void onDefineEntityProperties(GeyserDefineEntityPropertiesEvent event) {
        this.itemDisplays.properties(event);
        this.itemManager.registerProperties(event);
    }

    @Subscribe
    public void onDefineCustomItems(GeyserDefineCustomItemsEvent event) {
        this.itemManager.registerItems(this.packManager.registrationFiles(), event);
    }

    @Subscribe
    public void onSessionInitialize(SessionInitializeEvent event) {
        GeyserSession session = (GeyserSession) event.connection();
        this.blockManager.initSession(session);
        this.itemManager.initSessionDye(session);
        GeyserContainerCloseHandler.install(session);
        this.itemManager.initSession(session);
    }
}
