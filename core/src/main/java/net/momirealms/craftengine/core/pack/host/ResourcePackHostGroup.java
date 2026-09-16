package net.momirealms.craftengine.core.pack.host;

import net.momirealms.craftengine.core.plugin.network.NetWorkUser;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class ResourcePackHostGroup implements ResourcePackHost {
    private final List<ResourcePackHost> hosts;
    private final Map<UUID, ResourcePackHost> packHosts = new ConcurrentHashMap<>();

    public ResourcePackHostGroup(Collection<ResourcePackHost> hosts) {
        this.hosts = List.copyOf(hosts);
    }

    @Override
    public CompletableFuture<List<ResourcePackDownloadData>> requestResourcePackDownloadLink(NetWorkUser user) {
        return requestResourcePackDownloadLink(user, this.hosts);
    }

    public CompletableFuture<List<ResourcePackDownloadData>> requestResourcePackDownloadLink(NetWorkUser user, List<ResourcePackHost> selected) {
        List<CompletableFuture<List<ResourcePackDownloadData>>> requests = selected.stream()
                .map(host -> host.requestResourcePackDownloadLink(user))
                .toList();
        return CompletableFuture.allOf(requests.toArray(CompletableFuture[]::new)).thenApply(ignored -> {
            Map<UUID, ResourcePackDownloadData> packs = new LinkedHashMap<>();
            for (int i = 0; i < requests.size(); i++) {
                for (ResourcePackDownloadData data : requests.get(i).join()) {
                    if (packs.putIfAbsent(data.uuid(), data) == null) {
                        this.packHosts.put(data.uuid(), selected.get(i));
                    }
                }
            }
            return List.copyOf(packs.values());
        });
    }

    @Override
    public CompletableFuture<Void> upload(Path path) {
        return CompletableFuture.allOf(this.hosts.stream().filter(ResourcePackHost::canUpload)
                .map(host -> host.upload(path)).toArray(CompletableFuture[]::new));
    }

    public ResourcePackHost sourceOf(UUID packId) {
        return this.packHosts.get(packId);
    }

    @Override
    public boolean canUpload() {
        return this.hosts.stream().anyMatch(ResourcePackHost::canUpload);
    }

    @Override
    public ResourcePackHostType<? extends ResourcePackHost> type() {
        return this.hosts.getFirst().type();
    }
}
