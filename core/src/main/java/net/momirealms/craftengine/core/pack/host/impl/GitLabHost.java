package net.momirealms.craftengine.core.pack.host.impl;

import com.google.gson.reflect.TypeToken;
import net.momirealms.craftengine.core.pack.host.ResourcePackDownloadData;
import net.momirealms.craftengine.core.pack.host.ResourcePackHost;
import net.momirealms.craftengine.core.pack.host.ResourcePackHostFactory;
import net.momirealms.craftengine.core.pack.host.ResourcePackHosts;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.locale.LocalizedException;
import net.momirealms.craftengine.core.util.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class GitLabHost implements ResourcePackHost {
    public static final Factory FACTORY = new Factory();
    private final String gitlabUrl;
    private final String accessToken;
    private final String projectId;
    private final ProxySelector proxy;

    private String url;
    private String sha1;
    private UUID uuid;

    public GitLabHost(String gitlabUrl, String accessToken, String projectId, ProxySelector proxy) {
        this.gitlabUrl = gitlabUrl;
        this.accessToken = accessToken;
        this.projectId = projectId;
        this.proxy = proxy;
        this.readCacheFromDisk();
    }

    public void readCacheFromDisk() {
        Path cachePath = CraftEngine.instance().dataFolderPath().resolve("cache").resolve("gitlab.json");
        if (!Files.exists(cachePath) || !Files.isRegularFile(cachePath)) return;
        try (InputStream is = Files.newInputStream(cachePath)) {
            Map<String, String> cache = GsonHelper.get().fromJson(
                    new InputStreamReader(is),
                    new TypeToken<Map<String, String>>(){}.getType()
            );
            this.url = cache.get("url");
            this.sha1 = cache.get("sha1");
            String uuidString = cache.get("uuid");
            if (uuidString != null && !uuidString.isEmpty()) {
                this.uuid = UUID.fromString(uuidString);
            }
            CraftEngine.instance().logger().info("[GitLab] Loaded cached resource pack info");
        } catch (Exception e) {
            CraftEngine.instance().logger().warn(
                    "[GitLab] Failed to read cache file: " + cachePath, e);
        }
    }

    public void saveCacheToDisk() {
        Map<String, String> cache = new HashMap<>();
        cache.put("url", this.url);
        cache.put("sha1", this.sha1);
        cache.put("uuid", this.uuid != null ? this.uuid.toString() : "");
        Path cachePath = CraftEngine.instance().dataFolderPath().resolve("cache").resolve("gitlab.json");
        try {
            Files.createDirectories(cachePath.getParent());
            Files.writeString(
                    cachePath,
                    GsonHelper.get().toJson(cache),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );
        } catch (IOException e) {
            CraftEngine.instance().logger().warn(
                    "[GitLab] Failed to save cache: " + e.getMessage());
        }
    }

    @Override
    public boolean canUpload() {
        return true;
    }

    @Override
    public Key type() {
        return ResourcePackHosts.GITLAB;
    }

    @Override
    public CompletableFuture<List<ResourcePackDownloadData>> requestResourcePackDownloadLink(UUID player) {
        if (url == null) return CompletableFuture.completedFuture(Collections.emptyList());
        return CompletableFuture.completedFuture(List.of(ResourcePackDownloadData.of(this.url, this.uuid, this.sha1)));
    }

    @Override
    public CompletableFuture<Void> upload(Path resourcePackPath) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        CraftEngine.instance().scheduler().executeAsync(() -> {
            this.sha1 = HashUtils.calculateLocalFileSha1(resourcePackPath);
            this.uuid = UUID.nameUUIDFromBytes(this.sha1.getBytes(StandardCharsets.UTF_8));
            try (HttpClient client = HttpClient.newBuilder().proxy(this.proxy).build()) {
                String boundary = UUID.randomUUID().toString();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(this.gitlabUrl + "/api/v4/projects/" + this.projectId + "/uploads"))
                        .header("PRIVATE-TOKEN", this.accessToken)
                        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                        .POST(buildMultipartBody(resourcePackPath, boundary))
                        .build();
                long uploadStart = System.currentTimeMillis();
                CraftEngine.instance().logger().info(
                        "[GitLab] Initiating resource pack upload...");
                client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                        .thenAccept(response -> {
                            long uploadTime = System.currentTimeMillis() - uploadStart;
                            CraftEngine.instance().logger().info(
                                    "[GitLab] Upload request completed in " + uploadTime + "ms");
                            if (response.statusCode() == 200 || response.statusCode() == 201) {
                                Map<String, Object> json = GsonHelper.parseJsonToMap(response.body());
                                if (json.containsKey("full_path")) {
                                    this.url = this.gitlabUrl + json.get("full_path");
                                    future.complete(null);
                                    saveCacheToDisk();
                                    return;
                                }
                            }
                            CraftEngine.instance().logger().warn("[GitLab] Upload failed: " + response.body());
                            future.completeExceptionally(new RuntimeException("Upload failed: " + response.body()));
                        })
                        .exceptionally(ex -> {
                            CraftEngine.instance().logger().warn(
                                    "[GitLab] Upload error: " + ex.getMessage());
                            future.completeExceptionally(ex);
                            return null;
                        });
            } catch (IOException e) {
                CraftEngine.instance().logger().warn(
                        "[GitLab] Failed to upload resource pack: " + e.getMessage());
            }
        });
        return future;
    }

    private HttpRequest.BodyPublisher buildMultipartBody(Path filePath, String boundary) throws IOException {
        List<byte[]> parts = new ArrayList<>();
        String filePartHeader = "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"file\"; filename=\"" + filePath.getFileName() + "\"\r\n" +
                "Content-Type: application/octet-stream\r\n\r\n";
        parts.add(filePartHeader.getBytes());

        parts.add(Files.readAllBytes(filePath));
        parts.add("\r\n".getBytes());

        String endBoundary = "--" + boundary + "--\r\n";
        parts.add(endBoundary.getBytes());

        return HttpRequest.BodyPublishers.ofByteArrays(parts);
    }

    public static class Factory implements ResourcePackHostFactory {

        @Override
        public ResourcePackHost create(Map<String, Object> arguments) {
            boolean useEnv = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("use-environment-variables", false), "use-environment-variables");
            String gitlabUrl = Optional.ofNullable(arguments.get("gitlab-url")).map(String::valueOf).orElse(null);
            if (gitlabUrl == null || gitlabUrl.isEmpty()) {
                throw new LocalizedException("warning.config.host.gitlab.missing_url");
            }
            if (gitlabUrl.endsWith("/")) {
                gitlabUrl = gitlabUrl.substring(0, gitlabUrl.length() - 1);
            }
            String accessToken = useEnv ? System.getenv("CE_GITLAB_ACCESS_TOKEN") : Optional.ofNullable(arguments.get("access-token")).map(String::valueOf).orElse(null);
            if (accessToken == null || accessToken.isEmpty()) {
                throw new LocalizedException("warning.config.host.gitlab.missing_token");
            }
            String projectId = Optional.ofNullable(arguments.get("project-id")).map(String::valueOf).orElse(null);
            if (projectId == null || projectId.isEmpty()) {
                throw new LocalizedException("warning.config.host.gitlab.missing_project");
            }
            projectId = URLEncoder.encode(projectId, StandardCharsets.UTF_8).replace("/", "%2F");
            ProxySelector proxy = getProxySelector(MiscUtils.castToMap(arguments.get("proxy"), true));
            return new GitLabHost(gitlabUrl, accessToken, projectId, proxy);
        }
    }
}
