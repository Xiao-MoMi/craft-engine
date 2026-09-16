package net.momirealms.craftengine.core.pack.host.impl;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.host.*;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.util.GsonHelper;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class SelfForwardHost implements ResourcePackHost {
    public static final ResourcePackHostFactory<SelfForwardHost> FACTORY = new Factory();
    private final String server;
    private final String secret;
    private final String packId;

    private SelfForwardHost(String server, String secret, String packId) {
        this.server = server;
        this.secret = secret;
        this.packId = packId;
    }

    @Override
    public CompletableFuture<List<ResourcePackDownloadData>> requestResourcePackDownloadLink(NetWorkUser user) {
        CompletableFuture<List<ResourcePackDownloadData>> future = new CompletableFuture<>();
        CraftEngine.instance().scheduler().executeAsync(() -> {
            try {
                HttpRequest request = HttpClientManager.requestBuilder()
                        .uri(URI.create(this.server + "/forward/" + this.packId))
                        .header("secret", this.secret)
                        .header("uuid", user.uuid().toString())
                        .build();
                HttpResponse<String> response = HttpClientManager.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    future.completeExceptionally(new RuntimeException("Failed to request resource pack download link | Body: " + response.body()));
                    return;
                }
                JsonObject json = GsonHelper.parseJsonToJsonObject(response.body());
                String url = json.get("url").getAsString();
                String uuid = json.get("uuid").getAsString();
                String hash = json.get("hash").getAsString();
                future.complete(List.of(ResourcePackDownloadData.of(url, UUID.fromString(uuid), hash)));
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
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
    public ResourcePackHostType<? extends ResourcePackHost> type() {
        return ResourcePackHosts.SELF_FORWARD;
    }

    private static class Factory implements ResourcePackHostFactory<SelfForwardHost> {

        @Override
        public SelfForwardHost create(String id, ConfigSection section) {
            String server = section.getNonEmptyString("server");
            String secret = section.getNonEmptyString("secret");
            return new SelfForwardHost(server, secret, section.getNonEmptyString("pack"));
        }
    }
}
