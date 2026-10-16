package net.momirealms.craftengine.bedrock.geyser;

import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomBlocksEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomItemsEvent;
import org.geysermc.geyser.api.extension.Extension;
import org.geysermc.geyser.api.event.lifecycle.GeyserPostInitializeEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserPostReloadEvent;

import java.io.IOException;
import java.util.Set;

public final class CraftEngineGeyserExtension implements Extension {
    private BedrockItemRegistrar itemRegistrar;
    private Set<String> locales = Set.of();

    @Subscribe
    public void onDefineCustomBlocks(GeyserDefineCustomBlocksEvent event) {
        this.itemRegistrar = new BedrockItemRegistrar(this.logger());
        this.itemRegistrar.load(this.geyserApi().packDirectory(), event);
        this.syncLanguages();
    }

    @Subscribe
    public void onDefineCustomItems(GeyserDefineCustomItemsEvent event) {
        if (this.itemRegistrar == null) {
            this.itemRegistrar = new BedrockItemRegistrar(this.logger());
            this.itemRegistrar.load(this.geyserApi().packDirectory(), null);
            this.syncLanguages();
        }
        this.itemRegistrar.register(event);
        this.itemRegistrar = null;
    }

    private void syncLanguages() {
        try {
            this.locales = LocaleOverrides.sync(this.geyserApi().configDirectory().resolve("locales/overrides"),
                    this.dataFolder().resolve("managed-languages.json"), this.itemRegistrar.translations());
            this.refreshLanguages();
            this.logger().info("Synchronized CraftEngine language overrides for " + this.locales.size() + " locales");
        } catch (IOException exception) {
            this.logger().error("Could not synchronize CraftEngine language overrides", exception);
        }
    }

    @Subscribe
    public void onInitialized(GeyserPostInitializeEvent event) {
        this.refreshLanguages();
    }

    @Subscribe
    public void onReloaded(GeyserPostReloadEvent event) {
        this.refreshLanguages();
    }

    private void refreshLanguages() {
        try {
            GeyserLocaleBridge.reloadLoaded(this.locales);
        } catch (ReflectiveOperationException exception) {
            this.logger().error("Could not refresh Geyser's loaded languages; restart Geyser to load the written overrides", exception);
        }
    }
}
