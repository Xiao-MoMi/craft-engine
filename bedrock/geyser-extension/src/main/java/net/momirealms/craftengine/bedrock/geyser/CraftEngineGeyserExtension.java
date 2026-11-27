package net.momirealms.craftengine.bedrock.geyser;

import net.momirealms.craftengine.bedrock.geyser.block.GeyserBlockManager;
import net.momirealms.craftengine.bedrock.geyser.config.GeyserConfig;
import net.momirealms.craftengine.bedrock.geyser.entity.GeyserEntityManager;
import net.momirealms.craftengine.bedrock.geyser.glyph.GeyserGlyphManager;
import net.momirealms.craftengine.bedrock.geyser.item.GeyserItemManager;
import net.momirealms.craftengine.bedrock.geyser.locale.GeyserTranslationManager;
import net.momirealms.craftengine.bedrock.geyser.locale.GeyserTranslationManagerImpl;
import net.momirealms.craftengine.bedrock.geyser.network.GeyserBedrockPackets;
import net.momirealms.craftengine.bedrock.geyser.network.GeyserCustomPayloads;
import net.momirealms.craftengine.bedrock.geyser.network.GeyserJavaPackets;
import net.momirealms.craftengine.bedrock.geyser.pack.GeyserExtensionData;
import net.momirealms.craftengine.bedrock.geyser.pack.GeyserPocketItemSlots;
import net.momirealms.craftengine.bedrock.geyser.player.GeyserPlayerManager;
import net.momirealms.craftengine.bedrock.geyser.scoreboard.GeyserSidebarManager;
import net.momirealms.craftengine.bedrock.geyser.sound.GeyserSoundMappings;
import net.momirealms.craftengine.bedrock.geyser.translation.GeyserLanguageManager;
import org.geysermc.event.PostOrder;
import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.api.event.bedrock.SessionDisconnectEvent;
import org.geysermc.geyser.api.event.bedrock.SessionInitializeEvent;
import org.geysermc.geyser.api.event.bedrock.SessionLoadResourcePacksEvent;
import org.geysermc.geyser.api.event.lifecycle.*;
import org.geysermc.geyser.api.extension.Extension;
import org.geysermc.geyser.api.pack.PackCodec;
import org.geysermc.geyser.api.pack.ResourcePack;
import org.geysermc.geyser.api.pack.option.PriorityOption;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.registry.type.ItemMappings;
import org.geysermc.geyser.session.GeyserSession;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

@SuppressWarnings("UnstableApiUsage")
public final class CraftEngineGeyserExtension implements Extension {
    private static CraftEngineGeyserExtension instance;

    private GeyserPlayerManager playerManager;
    private GeyserJavaPackets javaPackets;
    private GeyserBedrockPackets bedrockPackets;
    private GeyserSidebarManager sidebarManager;
    private GeyserConfig config;
    private GeyserTranslationManager translations;
    private GeyserEntityManager entityManager;
    private GeyserGlyphManager glyphManager;
    private GeyserLanguageManager languageManager;
    private GeyserBlockManager blockManager;
    private GeyserItemManager itemManager;
    private GeyserExtensionData extensionData;
    private GeyserSoundMappings sounds;
    private GeyserCustomPayloads customPayloads;
    private ResourcePack clientPack;
    private Map<ItemMappings, ResourcePack> pocketSlotPacks = Map.of();

    @Subscribe
    public void onPreInitialize(GeyserPreInitializeEvent event) {
        instance = this;
        this.sounds = new GeyserSoundMappings();
        this.playerManager = new GeyserPlayerManager(this);
        this.config = new GeyserConfig(this.dataFolder(), this.logger());
        this.config.load();
        try {
            this.translations = new GeyserTranslationManagerImpl(this.dataFolder(), this.logger());
            this.translations.load();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to initialize translations", exception);
        }
        this.blockManager = new GeyserBlockManager();
        this.customPayloads = new GeyserCustomPayloads();
        this.glyphManager = new GeyserGlyphManager();
        this.languageManager = new GeyserLanguageManager();
        this.itemManager = new GeyserItemManager(this);
        try {
            this.extensionData = GeyserExtensionData.read(this.config.dataFile(), this.dataFolder().resolve("cache"));
            this.entityManager = new GeyserEntityManager(this.playerManager, this.extensionData.nativeModels());
            this.languageManager.load(this.extensionData.languages());
            this.glyphManager.load(this.extensionData.glyphs());
        } catch (IOException exception) {
            throw new IllegalStateException(GeyserTranslationManager.message("extension.data_failed"), exception);
        } catch (RuntimeException | Error failure) {
            this.glyphManager.disable();
            this.languageManager.disable();
            throw failure;
        }
    }

    @Subscribe
    public void onDefineResourcePacks(GeyserDefineResourcePacksEvent event) {
        this.clientPack = null;
        this.pocketSlotPacks = Map.of();
        Path pack = this.extensionData.clientPack();
        if (pack != null) {
            ResourcePack resourcePack = ResourcePack.create(PackCodec.path(pack));
            event.register(resourcePack);
            this.clientPack = resourcePack;
            try {
                this.pocketSlotPacks = GeyserPocketItemSlots.createPacks(pack, this.itemManager::usesBlockIcon, this.extensionData.playerPack());
            } catch (IOException exception) {
                throw new IllegalStateException("Failed to create Pocket inventory resource packs", exception);
            }
        }
    }

    @Subscribe(postOrder = PostOrder.LAST)
    public void onSessionLoadResourcePacks(SessionLoadResourcePacksEvent event) {
        if (this.pocketSlotPacks.isEmpty()) {
            return;
        }
        ItemMappings palette = Registries.ITEMS.forVersion(event.connection().protocolVersion());
        ResourcePack slots = this.pocketSlotPacks.get(palette);
        if (slots != null && event.resourcePacks().stream().anyMatch(pack -> pack.uuid().equals(this.clientPack.uuid()))) {
            event.register(slots, PriorityOption.NORMAL);
        }
    }

    @Nullable
    public String clientPackId() {
        return this.clientPack == null ? null : this.clientPack.uuid().toString();
    }

    @Subscribe
    public void onPostInitialize(GeyserPostInitializeEvent event) {
        try {
            this.sounds.install(this.extensionData.soundMappings());
            this.entityManager.initialize(this.glyphManager.glyphs());
            this.sidebarManager = this.extensionData.scoreboard().enabled() && this.glyphManager.glyphs().layout() != null
                    ? new GeyserSidebarManager(this.glyphManager.glyphs().layout(), this.extensionData.scoreboard(), this.extensionData.verticalOffsets())
                    : null;
            this.bedrockPackets = new GeyserBedrockPackets(this.sidebarManager != null);
            this.javaPackets = new GeyserJavaPackets(this.playerManager, Registries.JAVA_PACKET_TRANSLATORS);
            this.itemManager.init();
            this.blockManager.init();
            this.javaPackets.install(this);
        } catch (RuntimeException | Error failure) {
            try {
                this.uninstall();
            } catch (RuntimeException | Error cleanup) {
                failure.addSuppressed(cleanup);
            }
            throw failure;
        }
    }

    @Subscribe
    public void onDisconnect(SessionDisconnectEvent event) {
        this.playerManager.remove(event.connection());
    }

    @Subscribe
    public void onShutdown(GeyserShutdownEvent event) {
        this.uninstall();
    }

    private void uninstall() {
        if (this.playerManager != null) {
            this.playerManager.disable();
        }
        if (this.javaPackets != null) {
            this.javaPackets.uninstall();
        }
        if (this.blockManager != null) {
            this.blockManager.unload();
        }
        if (this.itemManager != null) {
            this.itemManager.disable();
        }
        if (this.entityManager != null) {
            this.entityManager.disable();
        }
        if (this.sounds != null) {
            this.sounds.uninstall();
        }
        if (this.glyphManager != null) {
            this.glyphManager.disable();
        }
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
        this.entityManager.registerEntities(this.extensionData.modelDisplays(), event);
    }

    @Subscribe
    public void onDefineEntityProperties(GeyserDefineEntityPropertiesEvent event) {
        this.entityManager.registerProperties(event);
        this.itemManager.registerProperties(event);
    }

    @Subscribe
    public void onDefineCustomItems(GeyserDefineCustomItemsEvent event) {
        this.itemManager.registerItems(this.extensionData.items(), this.extensionData.elytraVariants(), event);
    }

    @Subscribe
    public void onSessionInitialize(SessionInitializeEvent event) {
        this.playerManager.add((GeyserSession) event.connection());
    }

    public static CraftEngineGeyserExtension instance() {
        return instance;
    }

    public GeyserConfig config() {
        return this.config;
    }

    public GeyserJavaPackets javaPackets() {
        return this.javaPackets;
    }

    public GeyserBedrockPackets bedrockPackets() {
        return this.bedrockPackets;
    }

    public GeyserPlayerManager playerManager() {
        return this.playerManager;
    }

    public GeyserSidebarManager sidebarManager() {
        return this.sidebarManager;
    }

    public GeyserTranslationManager translationManager() {
        return this.translations;
    }

    public GeyserEntityManager entityManager() {
        return this.entityManager;
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
