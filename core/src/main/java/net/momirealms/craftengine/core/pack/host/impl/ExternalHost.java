package net.momirealms.craftengine.core.pack.host.impl;

import net.momirealms.craftengine.core.pack.host.*;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class ExternalHost implements ResourcePackHost {
    public static final ResourcePackHostFactory<ExternalHost> FACTORY = new Factory();
    private final ResourcePackDownloadData downloadData;

    public ExternalHost(ResourcePackDownloadData downloadData) {
        this.downloadData = downloadData;
    }

    @Override
    public CompletableFuture<List<ResourcePackDownloadData>> requestResourcePackDownloadLink(NetWorkUser user) {
        return CompletableFuture.completedFuture(List.of(this.downloadData));
    }

    @Override
    public CompletableFuture<Void> upload(Path resourcePackPath) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public boolean canUpload() {
        return false;
    }

    @Override
    public ResourcePackHostType<ExternalHost> type() {
        return ResourcePackHosts.EXTERNAL;
    }

    private static class Factory implements ResourcePackHostFactory<ExternalHost> {

        @Override
        public ExternalHost create(ConfigSection section) {
            String url = section.getNonEmptyString("url");
            UUID uuid = section.getValue("uuid", ConfigValue::getAsUUID, UUID.nameUUIDFromBytes(url.getBytes(StandardCharsets.UTF_8)));
            return new ExternalHost(new ResourcePackDownloadData(url, uuid, section.getString("sha1", "")));
        }
    }
}
