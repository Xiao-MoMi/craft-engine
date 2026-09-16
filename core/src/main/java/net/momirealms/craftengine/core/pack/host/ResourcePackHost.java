package net.momirealms.craftengine.core.pack.host;

import net.momirealms.craftengine.core.plugin.network.NetWorkUser;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface ResourcePackHost {

    CompletableFuture<List<ResourcePackDownloadData>> requestResourcePackDownloadLink(NetWorkUser user);

    CompletableFuture<Void> upload(Path resourcePackPath);

    boolean canUpload();

    ResourcePackHostType<? extends ResourcePackHost> type();
}
