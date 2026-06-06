package net.momirealms.craftengine.core.util;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Locale;

public final class UpdateCheckUtils {
    private static final String MODRINTH_API_URL = "https://api.modrinth.com/v2/project/craftengine/version";
    private static final String MODRINTH_CN_API_URL = "https://mod.mcimirror.top/modrinth/v2/project/craftengine/version";

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
        URL url = new URI(apiUrl).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            List<JsonObject> versions = GsonHelper.get().fromJson(
                    response.toString(),
                    new TypeToken<List<JsonObject>>(){}.getType()
            );
            return versions.stream()
                    .map(v -> v.get("version_number").getAsString())
                    .max(UpdateCheckUtils::compareVer)
                    .orElse(null);
        }
        return null;
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
