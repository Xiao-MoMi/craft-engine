package net.momirealms.craftengine.core.pack.host.impl;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import net.momirealms.craftengine.core.pack.host.*;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.HashUtils;
import net.momirealms.craftengine.core.util.Pair;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public final class OpenListHost implements ResourcePackHost {
    public static final ResourcePackHostFactory<OpenListHost> FACTORY = new Factory();
    private final String apiUrl;
    private final String userName;
    private final String password;
    private final String filePassword;
    private final String otpCode;
    private final Duration jwtTokenExpiration;
    private final String uploadPath;
    private final boolean disableUpload;
    private final boolean isAlist;
    private Pair<String, Date> jwtToken;
    private String cachedSha1;

    public OpenListHost(String apiUrl, String userName, String password, String filePassword, String otpCode,
                        Duration jwtTokenExpiration, String uploadPath, boolean disableUpload, boolean isAlist) {
        this.apiUrl = apiUrl;
        this.userName = userName;
        this.password = password;
        this.filePassword = filePassword;
        this.otpCode = otpCode;
        this.jwtTokenExpiration = jwtTokenExpiration;
        this.uploadPath = uploadPath;
        this.disableUpload = disableUpload;
        this.isAlist = isAlist;

        this.readCacheFromDisk();
    }

    @Override
    public boolean canUpload() {
        return true;
    }

    @Override
    public ResourcePackHostType<OpenListHost> type() {
        return this.isAlist ? ResourcePackHosts.ALIST : ResourcePackHosts.OPENLIST;
    }

    @Override
    public CompletableFuture<List<ResourcePackDownloadData>> requestResourcePackDownloadLink(NetWorkUser user) {
        CompletableFuture<List<ResourcePackDownloadData>> future = new CompletableFuture<>();

        CraftEngine.instance().scheduler().executeAsync(() -> {
            try {
                String token = getOrRefreshJwtToken();
                if (token == null) {
                    future.completeExceptionally(new RuntimeException("Failed to obtain JWT token"));
                    return;
                }

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(this.apiUrl + "/api/fs/get"))
                        .header("Authorization", token)
                        .header("Content-Type", "application/json")
                        .POST(getRequestResourcePackDownloadLinkPost())
                        .build();

                HttpClientManager.get().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                        .thenAccept(response -> handleResourcePackDownloadLinkResponse(response, future))
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

    private void handleResourcePackDownloadLinkResponse(
            HttpResponse<String> response, CompletableFuture<List<ResourcePackDownloadData>> future) {

        if (response.statusCode() != 200) {
            fail(future, "HTTP " + response.statusCode(), response.body());
            return;
        }

        try {
            JsonObject json = GsonHelper.parseJsonToJsonObject(response.body());

            if (!isResponseSuccess(json)) {
                fail(future, "Business code error", response.body());
                return;
            }

            JsonObject dataObj = json.getAsJsonObject("data");
            if (dataObj == null || dataObj.get("is_dir").getAsBoolean()) {
                fail(future, "Invalid data or target is a directory", null);
                return;
            }

            String url = dataObj.get("raw_url").getAsString();

            if (shouldUpdateCache()) {
                this.cachedSha1 = fetchRemoteSha1(url);
                saveCacheToDisk();
            }

            if (this.cachedSha1 == null) throw new IllegalStateException("SHA1 is still null");
            UUID uuid = UUID.nameUUIDFromBytes(this.cachedSha1.getBytes(StandardCharsets.UTF_8));
            future.complete(List.of(new ResourcePackDownloadData(url, uuid, this.cachedSha1)));

        } catch (Exception e) {
            future.completeExceptionally(e);
        }
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
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(this.apiUrl + "/api/fs/put"))
                        .header("Authorization", getOrRefreshJwtToken())
                        .header("File-Path", URLEncoder.encode(this.uploadPath, StandardCharsets.UTF_8).replace("/", "%2F"))
                        .header("overwrite", "true")
                        .header("password", this.filePassword)
                        .header("Content-Type", "application/x-zip-compressed")
                        .PUT(HttpRequest.BodyPublishers.ofFile(resourcePackPath))
                        .build();

                HttpClientManager.get().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                        .thenAccept(resp -> {
                            if (resp.statusCode() == 200) {
                                this.cachedSha1 = HashUtils.calculateLocalFileSha1(resourcePackPath);
                                saveCacheToDisk();
                                future.complete(null);
                            } else {
                                future.completeExceptionally(new RuntimeException("Upload failed: " + resp.statusCode()));
                            }
                        }).exceptionally(t -> {
                            future.completeExceptionally(t);
                            return null;
                        });
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private String fetchRemoteSha1(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        HttpResponse<InputStream> response = HttpClientManager.get().send(request, HttpResponse.BodyHandlers.ofInputStream());

        try (InputStream is = response.body()) {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] buffer = new byte[8192];
            int len;
            while ((len = is.read(buffer)) != -1) {
                md.update(buffer, 0, len);
            }
            return HexFormat.of().formatHex(md.digest());
        }
    }

    private boolean isResponseSuccess(JsonObject json) {
        return json.has("code") && json.get("code").getAsInt() == 200;
    }

    private boolean shouldUpdateCache() {
        return (this.cachedSha1 == null || this.cachedSha1.isEmpty()) && this.disableUpload;
    }

    private void fail(CompletableFuture<?> future, String reason, String body) {
        String msg = "AlistHost Error: " + reason + (body != null ? " | Body: " + body : "");
        future.completeExceptionally(new RuntimeException(msg));
    }

    @Nullable
    private synchronized String getOrRefreshJwtToken() throws IOException, InterruptedException {
        if (this.jwtToken != null && this.jwtToken.right().after(new Date())) {
            return this.jwtToken.left();
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(this.apiUrl + "/api/auth/login"))
                .header("Content-Type", "application/json")
                .POST(getLoginPost())
                .build();

        HttpResponse<String> response = HttpClientManager.get().send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IllegalStateException("Authentication failed (HTTP " + response.statusCode() + "): " + response.body());
        }

        JsonObject json = GsonHelper.parseJsonToJsonObject(response.body());
        if (isResponseSuccess(json)) {
            String token = json.getAsJsonObject("data").get("token").getAsString();
            this.jwtToken = Pair.of(token, new Date(System.currentTimeMillis() + this.jwtTokenExpiration.toMillis()));
            return token;
        }

        throw new IllegalStateException("Authentication failed: " + response.body());
    }

    private HttpRequest.BodyPublisher getLoginPost() {
        Map<String, String> map = new HashMap<>();
        map.put("username", this.userName);
        map.put("password", this.password);
        if (this.otpCode != null && !this.otpCode.isEmpty()) {
            map.put("otp_code", this.otpCode);
        }
        return HttpRequest.BodyPublishers.ofString(GsonHelper.get().toJson(map));
    }

    private HttpRequest.BodyPublisher getRequestResourcePackDownloadLinkPost() {
        Map<String, String> map = new HashMap<>();
        map.put("path", this.uploadPath);
        map.put("password", this.filePassword);
        return HttpRequest.BodyPublishers.ofString(GsonHelper.get().toJson(map));
    }

    private void readCacheFromDisk() {
        Path cachePath = CraftEngine.instance().dataFolderPath().resolve("cache")
                .resolve(this.isAlist ? "alist.json" : "openlist.json");
        if (!Files.exists(cachePath) || !Files.isRegularFile(cachePath)) return;
        try (InputStreamReader isr = new InputStreamReader(Files.newInputStream(cachePath), StandardCharsets.UTF_8)) {
            Map<String, String> cache = GsonHelper.get().fromJson(isr, new TypeToken<Map<String, String>>(){}.getType());
            this.cachedSha1 = cache.get("sha1");
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to load OpenList cache " + cachePath, e);
        }
    }

    private void saveCacheToDisk() {
        Path cachePath = CraftEngine.instance().dataFolderPath().resolve("cache")
                .resolve(this.isAlist ? "alist.json" : "openlist.json");
        try {
            Files.createDirectories(cachePath.getParent());
            Map<String, String> cache = Collections.singletonMap("sha1", this.cachedSha1 != null ? this.cachedSha1 : "");
            Files.writeString(cachePath, GsonHelper.get().toJson(cache), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            CraftEngine.instance().logger().warn("Failed to persist OpenList cache", e);
        }
    }

    private static class Factory implements ResourcePackHostFactory<OpenListHost> {
        private static final String[] USE_ENVIRONMENT_VARIABLES = new String[] {"use_environment_variables", "use-environment-variables"};
        private static final String[] API_URL = new String[] {"api_url", "api-url"};
        private static final String[] JWT_TOKEN_EXPIRATION = new String[] {"jwt_token_expiration", "jwt-token-expiration"};
        private static final String[] UPLOAD_PATH = new String[] {"upload_path", "upload-path"};
        private static final String[] DISABLE_UPLOAD = new String[] {"disable_upload", "disable-upload"};
        private static final String[] OPT_CODE = new String[] {"otp_code", "otp-code"};

        @Override
        public OpenListHost create(ConfigSection section) {
            boolean useEnv = section.getBoolean(USE_ENVIRONMENT_VARIABLES);
            boolean isAlist = "alist".equals(section.get("type")); // 简单的判断是否为 alist 以便兼容旧版本环境变量及缓存读取
            String apiUrl = section.getNonEmptyString(API_URL);
            String userName = useEnv ? getNonNullEnvironmentVariable(section, isAlist ? "CE_ALIST_USERNAME" : "CE_OPENLIST_USERNAME") : section.getNonEmptyString("username");
            String password = useEnv ? getNonNullEnvironmentVariable(section, isAlist ? "CE_ALIST_PASSWORD" : "CE_OPENLIST_PASSWORD") : section.getNonEmptyString("password");
            String filePassword = useEnv ? getNonNullEnvironmentVariable(section, isAlist ? "CE_ALIST_FILE_PASSWORD" : "CE_OPENLIST_FILE_PASSWORD") : section.getString("file_password", "");
            String otpCode = section.getString(OPT_CODE, "");
            Duration jwtTokenExpiration = Duration.ofHours(section.getInt(JWT_TOKEN_EXPIRATION, 48));
            String uploadPath = section.getNonEmptyString(UPLOAD_PATH);
            boolean disableUpload = section.getBoolean(DISABLE_UPLOAD);
            return new OpenListHost(apiUrl, userName, password, filePassword, otpCode, jwtTokenExpiration, uploadPath, disableUpload, isAlist);
        }
    }
}