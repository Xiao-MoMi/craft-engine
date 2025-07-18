package net.momirealms.craftengine.core.pack.host.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import net.momirealms.craftengine.core.pack.host.ResourcePackDownloadData;
import net.momirealms.craftengine.core.pack.host.ResourcePackHost;
import net.momirealms.craftengine.core.pack.host.ResourcePackHostFactory;
import net.momirealms.craftengine.core.pack.host.ResourcePackHosts;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.locale.LocalizedException;
import net.momirealms.craftengine.core.util.*;
import org.jetbrains.annotations.Nullable;

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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class AlistHost implements ResourcePackHost {
    public static final Factory FACTORY = new Factory();
    private final String apiUrl;
    private final String userName;
    private final String password;
    private final String filePassword;
    private final String otpCode;
    private final Duration jwtTokenExpiration;
    private final String uploadPath;
    private final boolean disableUpload;
    private final ProxySelector proxy;
    private Pair<String, Date> jwtToken;
    private String cachedSha1;

    public AlistHost(String apiUrl,
                     String userName,
                     String password,
                     String filePassword,
                     String otpCode,
                     Duration jwtTokenExpiration,
                     String uploadPath,
                     boolean disableUpload,
                     ProxySelector proxy) {
        this.apiUrl = apiUrl;
        this.userName = userName;
        this.password = password;
        this.filePassword = filePassword;
        this.otpCode = otpCode;
        this.jwtTokenExpiration = jwtTokenExpiration;
        this.uploadPath = uploadPath;
        this.disableUpload = disableUpload;
        this.proxy = proxy;
        this.readCacheFromDisk();
    }

    @Override
    public boolean canUpload() {
        return true;
    }

    @Override
    public Key type() {
        return ResourcePackHosts.ALIST;
    }

    private void readCacheFromDisk() {
        Path cachePath = CraftEngine.instance().dataFolderPath().resolve("cache").resolve("alist.json");
        if (!Files.exists(cachePath) || !Files.isRegularFile(cachePath)) return;
        try (InputStream is = Files.newInputStream(cachePath)) {
            Map<String, String> cache = GsonHelper.get().fromJson(
                    new InputStreamReader(is),
                    new TypeToken<Map<String, String>>(){}.getType()
            );
            this.cachedSha1 = cache.get("sha1");
            CraftEngine.instance().logger().info("[Alist] Loaded cached resource pack metadata");
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("[Alist] Failed to load cache " + cachePath, e);
        }
    }

    private void saveCacheToDisk() {
        Map<String, String> cache = new HashMap<>();
        cache.put("sha1", this.cachedSha1 != null ? this.cachedSha1 : "");
        Path cachePath = CraftEngine.instance().dataFolderPath().resolve("cache").resolve("alist.json");
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
                    "[Alist] Failed to persist cache to disk: " + e.getMessage());
        }
    }

    @Override
    public CompletableFuture<List<ResourcePackDownloadData>> requestResourcePackDownloadLink(UUID player) {
        CompletableFuture<List<ResourcePackDownloadData>> future = new CompletableFuture<>();
        CraftEngine.instance().scheduler().executeAsync(() -> {
            try (HttpClient client = HttpClient.newBuilder().proxy(this.proxy).build()) {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(this.apiUrl + "/api/fs/get"))
                        .header("Authorization", getOrRefreshJwtToken())
                        .header("Content-Type", "application/json")
                        .POST(getRequestResourcePackDownloadLinkPost())
                        .build();
                client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                        .thenAccept(response -> handleResourcePackDownloadLinkResponse(response, future))
                        .exceptionally(ex -> {
                            CraftEngine.instance().logger().severe("[Alist] Failed to retrieve resource pack download URL", ex);
                            future.completeExceptionally(ex);
                            return null;
                        });
            }
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> upload(Path resourcePackPath) {
        if (this.disableUpload) {
            this.cachedSha1 = "";
            saveCacheToDisk();
            return CompletableFuture.completedFuture(null);
        }
        CompletableFuture<Void> future = new CompletableFuture<>();
        CraftEngine.instance().scheduler().executeAsync(() -> {
            try (HttpClient client = HttpClient.newBuilder().proxy(this.proxy).build()) {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(this.apiUrl + "/api/fs/put"))
                        .header("Authorization", getOrRefreshJwtToken())
                        .header("File-Path", URLEncoder.encode(this.uploadPath, StandardCharsets.UTF_8)
                                .replace("/", "%2F"))
                        .header("overwrite", "true")
                        .header("password", this.filePassword)
                        .header("Content-Type", "application/x-zip-compressed")
                        .PUT(HttpRequest.BodyPublishers.ofFile(resourcePackPath))
                        .build();
                long requestStart = System.currentTimeMillis();
                CraftEngine.instance().logger().info("[Alist] Initiating resource pack upload...");
                client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                        .thenAccept(response -> {
                            long uploadTime = System.currentTimeMillis() - requestStart;
                            if (response.statusCode() == 200) {
                                this.cachedSha1 = HashUtils.calculateLocalFileSha1(resourcePackPath);
                                saveCacheToDisk();
                                CraftEngine.instance().logger().info("[Alist] Successfully uploaded resource pack in " + uploadTime + " ms");
                                future.complete(null);
                            } else {
                                future.completeExceptionally(new RuntimeException("Upload failed with status code: " + response.statusCode()));
                            }
                        })
                        .exceptionally(ex -> {
                            long uploadTime = System.currentTimeMillis() - requestStart;
                            CraftEngine.instance().logger().severe(
                                    "[Alist] Resource pack upload failed after " + uploadTime + " ms", ex);
                            future.completeExceptionally(ex);
                            return null;
                        });
            } catch (IOException e) {
                CraftEngine.instance().logger().warn("[Alist] Failed to upload resource pack: " + e.getMessage());
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    @Nullable
    private String getOrRefreshJwtToken() {
        if (this.jwtToken == null || this.jwtToken.right().before(new Date())) {
            try (HttpClient client = HttpClient.newBuilder().proxy(this.proxy).build()) {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(this.apiUrl + "/api/auth/login"))
                        .header("Content-Type", "application/json")
                        .POST(getLoginPost())
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    CraftEngine.instance().logger().warn("[Alist] Authentication failed (HTTP " + response.statusCode() + "): " + response.body());
                    return null;
                }
                JsonObject jsonData = GsonHelper.parseJsonToJsonObject(response.body());
                JsonElement code = jsonData.get("code");
                if (code.isJsonPrimitive() && code.getAsJsonPrimitive().isNumber() && code.getAsJsonPrimitive().getAsInt() == 200) {
                    JsonElement data = jsonData.get("data");
                    if (data.isJsonObject()) {
                        JsonObject jsonObj = data.getAsJsonObject();
                        this.jwtToken = Pair.of(
                                jsonObj.getAsJsonPrimitive("token").getAsString(),
                                new Date(System.currentTimeMillis() + this.jwtTokenExpiration.toMillis())
                        );
                        return this.jwtToken.left();
                    }
                    CraftEngine.instance().logger().warn("[Alist] Invalid JWT response format: " + response.body());
                    return null;
                }
                CraftEngine.instance().logger().warn("[Alist] Authentication rejected: " + response.body());
                return null;
            } catch (IOException | InterruptedException e) {
                CraftEngine.instance().logger().warn("[Alist] JWT token acquisition failed", e);
                return null;
            }
        }
        return this.jwtToken.left();
    }

    private HttpRequest.BodyPublisher getLoginPost() {
        String body = "{\"username\":\"" + this.userName + "\",\"password\":\"" + this.password + "\"";
        if (this.otpCode != null && !this.otpCode.isEmpty()) {
            body += ",\"otp_code\":\"" + this.otpCode + "\"";
        }
        body += "}";
        return HttpRequest.BodyPublishers.ofString(body);
    }

    private HttpRequest.BodyPublisher getRequestResourcePackDownloadLinkPost() {
        String body = "{\"path\":\"" + this.uploadPath + "\",\"password\":\"" + this.filePassword + "\"}";
        return HttpRequest.BodyPublishers.ofString(body);
    }

    private void handleResourcePackDownloadLinkResponse(
            HttpResponse<String> response, CompletableFuture<List<ResourcePackDownloadData>> future) {
        if (response.statusCode() == 200) {
            JsonObject json = GsonHelper.parseJsonToJsonObject(response.body());
            JsonElement code = json.get("code");
            if (code.isJsonPrimitive() && code.getAsJsonPrimitive().isNumber() && code.getAsJsonPrimitive().getAsInt() == 200) {
                JsonElement data = json.get("data");
                if (data.isJsonObject()) {
                    JsonObject dataObj = data.getAsJsonObject();
                    boolean isDir = dataObj.getAsJsonPrimitive("is_dir").getAsBoolean();
                    if (!isDir) {
                        String url = dataObj.getAsJsonPrimitive("raw_url").getAsString();
                        if ((this.cachedSha1 == null || this.cachedSha1.isEmpty()) && this.disableUpload) {
                            try (HttpClient client = HttpClient.newBuilder().proxy(this.proxy).build()) {
                                HttpRequest request = HttpRequest.newBuilder()
                                        .uri(URI.create(url))
                                        .GET()
                                        .build();
                                HttpResponse<InputStream> responseHash = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
                                try (InputStream inputStream = responseHash.body()) {
                                    MessageDigest md = MessageDigest.getInstance("SHA-1");
                                    byte[] buffer = new byte[8192];
                                    int len;
                                    while ((len = inputStream.read(buffer)) != -1) {
                                        md.update(buffer, 0, len);
                                    }
                                    byte[] digest = md.digest();
                                    this.cachedSha1 = HexFormat.of().formatHex(digest);
                                    saveCacheToDisk();
                                } catch (NoSuchAlgorithmException e) {
                                    future.completeExceptionally(new RuntimeException("Failed to calculate SHA-1 hash algorithm", e));
                                    return;
                                }
                            } catch (IOException | InterruptedException e) {
                                future.completeExceptionally(new RuntimeException("Failed to retrieve remote resource pack for hashing", e));
                                return;
                            }
                        }
                        UUID uuid = UUID.nameUUIDFromBytes(Objects.requireNonNull(this.cachedSha1).getBytes(StandardCharsets.UTF_8));
                        future.complete(List.of(new ResourcePackDownloadData(url, uuid, this.cachedSha1)));
                        return;
                    }
                }
            }
        }
        future.completeExceptionally(
                new RuntimeException("Failed to obtain resource pack download URL (HTTP " + response.statusCode() + "): " + response.body()));
    }

    public static class Factory implements ResourcePackHostFactory {

        @Override
        public ResourcePackHost create(Map<String, Object> arguments) {
            boolean useEnv = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("use-environment-variables", false), "use-environment-variables");
            String apiUrl = ResourceConfigUtils.requireNonEmptyStringOrThrow(arguments.get("api-url"), () -> new LocalizedException("warning.config.host.alist.missing_api_url"));
            String userName = useEnv ? System.getenv("CE_ALIST_USERNAME") : Optional.ofNullable(arguments.get("username")).map(String::valueOf).orElse(null);
            if (userName == null || userName.isEmpty()) {
                throw new LocalizedException("warning.config.host.alist.missing_username");
            }
            String password =  useEnv ? System.getenv("CE_ALIST_PASSWORD") : Optional.ofNullable(arguments.get("password")).map(String::valueOf).orElse(null);
            if (password == null || password.isEmpty()) {
                throw new LocalizedException("warning.config.host.alist.missing_password");
            }
            String filePassword = useEnv ? System.getenv("CE_ALIST_FILE_PASSWORD") : arguments.getOrDefault("file-password", "").toString();
            String otpCode = Optional.ofNullable(arguments.get("otp-code")).map(String::valueOf).orElse(null);
            Duration jwtTokenExpiration = Duration.ofHours((int) arguments.getOrDefault("jwt-token-expiration", 48));
            String uploadPath = ResourceConfigUtils.requireNonEmptyStringOrThrow(arguments.get("upload-path"), () -> new LocalizedException("warning.config.host.alist.missing_upload_path"));
            boolean disableUpload = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("disable-upload", false), "disable-upload");
            ProxySelector proxy = getProxySelector(MiscUtils.castToMap(arguments.get("proxy"), true));
            return new AlistHost(apiUrl, userName, password, filePassword, otpCode, jwtTokenExpiration, uploadPath, disableUpload, proxy);
        }
    }
}