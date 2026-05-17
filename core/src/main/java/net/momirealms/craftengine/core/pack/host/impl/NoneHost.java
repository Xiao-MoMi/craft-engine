package net.momirealms.craftengine.core.pack.host.impl;

import net.momirealms.craftengine.core.pack.host.*;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class NoneHost implements ResourcePackHost {
    public static final ResourcePackHostFactory<NoneHost> FACTORY = new Factory();
    public static final NoneHost INSTANCE = new NoneHost();

    private NoneHost() {}

    @Override
    public CompletableFuture<List<ResourcePackDownloadData>> requestResourcePackDownloadLink(NetWorkUser user) {
        return CompletableFuture.completedFuture(List.of());
    }

    @Override
    public CompletableFuture<Void> upload(Path resourcePackPath) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public ResourcePackHostType<NoneHost> type() {
        return ResourcePackHosts.NONE;
    }

    @Override
    public boolean canUpload() {
        return false;
    }

    private static class Factory implements ResourcePackHostFactory<NoneHost> {

        @Override
        public NoneHost create(ConfigSection section) {
            return INSTANCE;
        }
    }
}
