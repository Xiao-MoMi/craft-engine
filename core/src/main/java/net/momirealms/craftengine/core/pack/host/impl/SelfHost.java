package net.momirealms.craftengine.core.pack.host.impl;

import net.momirealms.craftengine.core.pack.host.*;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class SelfHost implements ResourcePackHost {
    public static final ResourcePackHostFactory<SelfHost> FACTORY = new Factory();
    private final String id;
    private final Path storagePath;

    SelfHost(String id, Path storagePath) {
        this.id = id;
        this.storagePath = storagePath.toAbsolutePath().normalize();
    }

    public Path storagePath() {
        return this.storagePath;
    }

    @Override
    public CompletableFuture<List<ResourcePackDownloadData>> requestResourcePackDownloadLink(NetWorkUser user) {
        ResourcePackDownloadData data = SelfHostHttpServer.instance().generateOneTimeUrl(user, this.id);
        if (data == null) return CompletableFuture.completedFuture(List.of());
        return CompletableFuture.completedFuture(List.of(data));
    }

    @Override
    public CompletableFuture<Void> upload(Path resourcePackPath) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        CraftEngine.instance().scheduler().executeAsync(() -> {
            try {
                storeResourcePack(resourcePackPath);
                future.complete(null);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    synchronized void storeResourcePack(Path source) throws IOException {
        Path stored = source.toAbsolutePath().normalize();
        if (!stored.equals(this.storagePath)) {
            Files.createDirectories(this.storagePath.getParent());
            Path temporary = Files.createTempFile(this.storagePath.getParent(), ".pack-", ".zip");
            try {
                Files.copy(stored, temporary, StandardCopyOption.REPLACE_EXISTING);
                Files.move(temporary, this.storagePath, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(temporary);
            }
            stored = this.storagePath;
        }
        SelfHostHttpServer.instance().readResourcePack(this.id, stored);
    }

    @Override
    public boolean canUpload() {
        return true;
    }

    @Override
    public ResourcePackHostType<SelfHost> type() {
        return ResourcePackHosts.SELF;
    }

    private static class Factory implements ResourcePackHostFactory<SelfHost> {
        @Override
        public SelfHost create(String id, ConfigSection section) {
            String file = section.getValue("storage_path", ConfigValue::getAsNonEmptyString, "./cache/hosted/" + id + "/resource_pack.zip");
            Path path = CraftEngine.instance().dataFolderPath().resolve(file);
            return new SelfHost(id, path);
        }
    }
}
