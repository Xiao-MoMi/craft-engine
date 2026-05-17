package net.momirealms.craftengine.core.pack.host.impl;

import com.google.gson.reflect.TypeToken;
import net.momirealms.craftengine.core.pack.host.*;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.HashUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public final class GitLabHost implements ResourcePackHost {
    public static final ResourcePackHostFactory<GitLabHost> FACTORY = new Factory();
    private final String gitlabUrl;
    private final String accessToken;
    private final String projectId;

    private String cachedUrl;
    private String cachedSha1;

    public GitLabHost(String gitlabUrl, String accessToken, String projectId) {
        this.gitlabUrl = gitlabUrl.endsWith("/") ? gitlabUrl.substring(0, gitlabUrl.length() - 1) : gitlabUrl;
        this.accessToken = accessToken;
        this.projectId = projectId;

        this.readCacheFromDisk();
    }

    @Override
    public boolean canUpload() {
        return true;
    }

    @Override
    public ResourcePackHostType<GitLabHost> type() {
        return ResourcePackHosts.GITLAB;
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public CompletableFuture<List<ResourcePackDownloadData>> requestResourcePackDownloadLink(NetWorkUser user) {
        if (this.cachedUrl == null || this.cachedSha1 == null) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
        UUID uuid = UUID.nameUUIDFromBytes(this.cachedSha1.getBytes(StandardCharsets.UTF_8));
        return CompletableFuture.completedFuture(List.of(new ResourcePackDownloadData(this.cachedUrl, uuid, this.cachedSha1)));
    }

    @Override
    public CompletableFuture<Void> upload(Path resourcePackPath) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        CraftEngine.instance().scheduler().executeAsync(() -> {
            try {
                String localSha1 = HashUtils.calculateLocalFileSha1(resourcePackPath);
                String boundary = "CraftEngineBoundary" + System.currentTimeMillis();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(this.gitlabUrl + "/api/v4/projects/" + this.projectId + "/uploads"))
                        .header("PRIVATE-TOKEN", this.accessToken)
                        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                        .POST(buildMultipartBodyPublisher(resourcePackPath, boundary))
                        .build();

                HttpClientManager.get().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                        .thenAccept(response -> handleUploadResponse(response, localSha1, future))
                        .exceptionally(ex -> {
                            future.completeExceptionally(ex);
                            return null;
                        });
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    private void handleUploadResponse(HttpResponse<String> response, String localSha1, CompletableFuture<Void> future) {
        if (response.statusCode() != 200 && response.statusCode() != 201) {
            fail(future, "Upload HTTP " + response.statusCode(), response.body());
            return;
        }

        try {
            Map<String, Object> json = GsonHelper.parseJsonToMap(response.body());
            if (json.containsKey("full_path")) {
                this.cachedUrl = this.gitlabUrl + json.get("full_path");
                this.cachedSha1 = localSha1;
                saveCacheToDisk();
                future.complete(null);
            } else {
                fail(future, "Missing full_path in response", response.body());
            }
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
    }

    private HttpRequest.BodyPublisher buildMultipartBodyPublisher(Path filePath, String boundary) throws IOException {
        String fileName = filePath.getFileName().toString();
        byte[] start = ("--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n" +
                "Content-Type: application/octet-stream\r\n\r\n").getBytes(StandardCharsets.UTF_8);
        byte[] end = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);
        return HttpRequest.BodyPublishers.ofByteArrays(List.of(
                start,
                Files.readAllBytes(filePath),
                end
        ));
    }

    private void fail(CompletableFuture<?> future, String reason, String body) {
        String msg = "GitLabHost Error: " + reason + (body != null ? " | Body: " + body : "");
        future.completeExceptionally(new RuntimeException(msg));
    }

    private void readCacheFromDisk() {
        Path cachePath = CraftEngine.instance().dataFolderPath().resolve("cache").resolve("gitlab.json");
        if (!Files.exists(cachePath)) return;
        try (InputStreamReader isr = new InputStreamReader(Files.newInputStream(cachePath), StandardCharsets.UTF_8)) {
            Map<String, String> cache = GsonHelper.get().fromJson(isr, new TypeToken<Map<String, String>>(){}.getType());
            this.cachedUrl = cache.get("url");
            this.cachedSha1 = cache.get("sha1");
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to load GitLab cache", e);
        }
    }

    private void saveCacheToDisk() {
        Path cachePath = CraftEngine.instance().dataFolderPath().resolve("cache").resolve("gitlab.json");
        try {
            Files.createDirectories(cachePath.getParent());
            Map<String, String> cache = new HashMap<>();
            cache.put("url", this.cachedUrl != null ? this.cachedUrl : "");
            cache.put("sha1", this.cachedSha1 != null ? this.cachedSha1 : "");
            Files.writeString(cachePath, GsonHelper.get().toJson(cache), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            CraftEngine.instance().logger().warn("Failed to persist GitLab cache", e);
        }
    }

    private static class Factory implements ResourcePackHostFactory<GitLabHost> {
        private static final String[] USE_ENVIRONMENT_VARIABLES = new String[] {"use_environment_variables", "use-environment-variables"};
        private static final String[] GITLAB_URL = new String[] {"gitlab_url", "gitlab-url"};
        private static final String[] ACCESS_TOKEN = new String[] {"access_token", "access-token"};
        private static final String[] PROJECT_ID = new String[] {"project_id", "project-id"};

        @Override
        public GitLabHost create(ConfigSection section) {
            boolean useEnv = section.getBoolean(USE_ENVIRONMENT_VARIABLES);
            String gitlabUrl = section.getNonEmptyString(GITLAB_URL);
            String accessToken = useEnv ? getNonNullEnvironmentVariable(section, "CE_GITLAB_ACCESS_TOKEN") : section.getNonEmptyString(ACCESS_TOKEN);
            String projectId = section.getNonEmptyString(PROJECT_ID);
            projectId = URLEncoder.encode(projectId, StandardCharsets.UTF_8).replace("/", "%2F");
            return new GitLabHost(gitlabUrl, accessToken, projectId);
        }
    }
}
