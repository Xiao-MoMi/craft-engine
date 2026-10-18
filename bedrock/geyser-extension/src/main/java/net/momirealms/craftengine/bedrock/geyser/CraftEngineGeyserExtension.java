package net.momirealms.craftengine.bedrock.geyser;

import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomBlocksEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomItemsEvent;
import org.geysermc.geyser.api.extension.Extension;
import org.geysermc.geyser.api.event.lifecycle.GeyserPostInitializeEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserPostReloadEvent;

import java.io.IOException;
import java.util.Set;
import java.util.Map;

public final class CraftEngineGeyserExtension implements Extension {
    private BedrockItemRegistrar itemRegistrar;
    private Set<String> locales = Set.of();
    private Map<String, String> spears = Map.of();

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
        this.spears = this.itemRegistrar.spears();
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
        this.patchSpears();
    }

    @Subscribe
    public void onReloaded(GeyserPostReloadEvent event) {
        this.refreshLanguages();
        this.patchSpears();
    }

    private void patchSpears() {
        try {
            int patched = ExperimentalSpearItemAdapter.apply(this.spears);
            if (patched != 0) {
                this.logger().info("Patched " + patched + " CraftEngine spear definitions across Bedrock protocol versions");
            }
        } catch (ReflectiveOperationException exception) {
            this.logger().error("Could not apply the experimental Geyser spear component adapter", exception);
        }
    }

    private void refreshLanguages() {
        try {
            GeyserLocaleBridge.reloadLoaded(this.locales);
        } catch (ReflectiveOperationException exception) {
            this.logger().error("Could not refresh Geyser's loaded languages; restart Geyser to load the written overrides", exception);
        }
    }
}
