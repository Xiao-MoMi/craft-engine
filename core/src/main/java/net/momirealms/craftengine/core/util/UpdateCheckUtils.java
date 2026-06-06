package net.momirealms.craftengine.core.util;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public final class UpdateCheckUtils {
    private static final String MODRINTH_API_URL = "https://api.modrinth.com/v2/version_files/update";
    private static final String MODRINTH_CN_API_URL = "https://mod.mcimirror.top/modrinth/v2/version_files/update";

    private UpdateCheckUtils() {}

    public static void checkUpdates(CraftEngine plugin, String buildByBit, String polymart) {
        boolean downloadFromPolymart = polymart.equals("1");
        boolean downloadFromBBB = buildByBit.equals("true");
        String link;
        if (VersionHelper.PREMIUM) {
            if (downloadFromPolymart) {
                link = "https://voxel.shop/product/7624/";
            } else if (downloadFromBBB) {
                link = "https://builtbybit.com/resources/82674/";
            } else {
                if (Locale.getDefault() == Locale.SIMPLIFIED_CHINESE) {
                    link = "QQ群[1039968907]";
                } else {
                    return;
                }
            }
        } else {
            link = "https://modrinth.com/plugin/craftengine/";
        }
        try {
            String lv = null;
            for (String url : getUpdateUrls()) {
                try {
                    lv = getLatestVersion(url);
                    if (lv != null) break;
                } catch (Exception ignored) {
                }
            }
            if (lv == null) return;
            if (compareVer(lv, plugin.pluginVersion()) > 0) {
                plugin.logger().warn(TranslationManager.instance().plainTranslation("update.available", lv, link));
            } else {
                plugin.logger().info(TranslationManager.instance().plainTranslation("update.is_latest"));
            }
        } catch (Exception ignored) {
        }
    }

    private static int compareVer(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");
        int maxLength = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < maxLength; i++) {
            int num1 = i < parts1.length ? parseInt(parts1[i]) : 0;
            int num2 = i < parts2.length ? parseInt(parts2[i]) : 0;
            if (num1 != num2) return num1 - num2;
        }
        return 0;
    }

    @Nullable
    private static String getLatestVersion(String apiUrl) throws Exception {
        HttpURLConnection connection = getConnection(apiUrl);
        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            return GsonHelper.get().fromJson(response.toString(), JsonObject.class)
                    .asMap().values().iterator().next()
                    .getAsJsonObject()
                    .get("version_number")
                    .getAsString();
        }
        return null;
    }

    private static @NonNull HttpURLConnection getConnection(String apiUrl) throws URISyntaxException, IOException {
        URL url = new URI(apiUrl).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        String requestBody = "{\"algorithm\":\"sha512\",\"game_versions\":[],\"hashes\":[\"" +
                // 这里使用 26.6 的 sha512 来获取最新版本
                "357da69552ef525b790d3f09cf2c9782b5642cc4bae7fbdb50aaecbc675035154c913771e0736a596a523033cd74fc8934dd14014d3683780f36e15c0e5d2c73" +
                "\"],\"loaders\":[]}";
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        return connection;
    }

    private static String[] getUpdateUrls() {
        boolean isChinese = Locale.getDefault() == Locale.SIMPLIFIED_CHINESE;
        return new String[]{
                isChinese ? MODRINTH_CN_API_URL : MODRINTH_API_URL,
                isChinese ? MODRINTH_API_URL : MODRINTH_CN_API_URL
        };
    }

    private static int parseInt(String s) {
        if (s == null || s.isEmpty()) return 0;
        int dashIdx = s.indexOf('-');
        if (dashIdx != -1) s = s.substring(0, dashIdx);
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
