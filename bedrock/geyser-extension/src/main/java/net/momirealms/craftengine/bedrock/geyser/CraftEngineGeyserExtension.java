package net.momirealms.craftengine.bedrock.geyser;

import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomBlocksEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomItemsEvent;
import org.geysermc.geyser.api.extension.Extension;

public final class CraftEngineGeyserExtension implements Extension {
    private BedrockItemRegistrar itemRegistrar;

    @Subscribe
    public void onDefineCustomBlocks(GeyserDefineCustomBlocksEvent event) {
        this.itemRegistrar = new BedrockItemRegistrar(this.logger());
        this.itemRegistrar.load(this.geyserApi().packDirectory(), event);
    }

    @Subscribe
    public void onDefineCustomItems(GeyserDefineCustomItemsEvent event) {
        if (this.itemRegistrar == null) {
            this.itemRegistrar = new BedrockItemRegistrar(this.logger());
            this.itemRegistrar.load(this.geyserApi().packDirectory(), null);
        }
        this.itemRegistrar.register(event);
        this.itemRegistrar = null;
    }
}
