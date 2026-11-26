package net.momirealms.craftengine.core.pack.bedrock.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.bedrock.BedrockPackLayout;
import net.momirealms.craftengine.bedrock.text.BossBarStyle;
import net.momirealms.craftengine.bedrock.text.BossBarTitleCodec;
import net.momirealms.craftengine.core.pack.bedrock.JavaResourcePack;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ClientPngReader;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.ImageUtils;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class BedrockBossBars {
    private static final String SOURCE = "assets/minecraft/textures/gui/sprites/boss_bar/";
    private static final String TEXTURES = "textures/ce/boss_bar/";
    private static final List<String> COLORS = List.of("pink", "blue", "red", "green", "yellow", "purple", "white");
    private static final List<String> DIVISIONS = List.of("none", "notched_6", "notched_10", "notched_12", "notched_20");

    private BedrockBossBars() {
    }

    public static void write(JavaResourcePack resources, Path pack, Path data) throws IOException {
        JsonObject styles = new JsonObject();
        JsonArray panels = new JsonArray();
        Map<String, Boolean> sprites = new HashMap<>();
        for (String color : COLORS) {
            boolean customColor = overridden(resources, color);
            for (String division : DIVISIONS) {
                boolean notched = !division.equals("none");
                // 颜色贴图或对应的分段贴图被覆盖时，才为这个组合生成自定义样式
                if (!customColor && !(notched && overridden(resources, division))) {
                    continue;
                }
                int id = styles.size();
                styles.addProperty(color + "/" + division, id);
                JsonArray layers = new JsonArray();
                boolean background = writeSprite(resources, pack, sprites, color + "_background");
                boolean progress = writeSprite(resources, pack, sprites, color + "_progress");
                // 背景和进度贴图都透明时，保留样式编号来隐藏原版条。即使有分段，也只显示标题
                if (!background && !progress) {
                    continue;
                }
                if (background) {
                    addLayer(layers, color + "_background", false);
                }
                if (notched && writeSprite(resources, pack, sprites, division + "_background")) {
                    addLayer(layers, division + "_background", false);
                }
                if (progress) {
                    addLayer(layers, color + "_progress", true);
                }
                if (notched && writeSprite(resources, pack, sprites, division + "_progress")) {
                    addLayer(layers, division + "_progress", true);
                }
                JsonObject panel = new JsonObject();
                panel.addProperty("type", "panel");
                panel.addProperty("anchor_from", "top_middle");
                panel.addProperty("anchor_to", "top_middle");
                panel.add("size", GsonHelper.createArray(182, 5));
                panel.add("offset", GsonHelper.createArray(0, 10));
                // HUD 通过 #bossName 判断标题是否带有当前样式的完整标记
                panel.add("bindings", visibility(BossBarStyle.titleMatches(BossBarStyle.marker(id))));
                panel.add("controls", layers);
                panels.add(GsonHelper.createObject("ce_boss_bar_" + id, panel));
            }
        }
        boolean hasStyles = !styles.isEmpty();
        Map<Key, Integer> offsets = Config.bedrockFontVerticalOffsets();
        List<Integer> baselines = offsets.values().stream().filter(value -> value != 0).distinct().sorted().toList();
        JsonObject fontBaselines = new JsonObject();
        for (Map.Entry<Key, Integer> entry : offsets.entrySet()) {
            if (entry.getValue() != 0) {
                fontBaselines.addProperty(entry.getKey().asString(), 1 + Collections.binarySearch(baselines, entry.getValue()));
            }
        }
        if (!fontBaselines.isEmpty()) {
            styles.add(BedrockPackLayout.BOSS_BAR_FONT_BASELINES, fontBaselines);
        }
        styles.addProperty(BedrockPackLayout.BOSS_BAR_TITLE_LAYOUT, BossBarTitleCodec.COMPACT_POSITION_VERSION);
        GsonHelper.writeJsonFile(styles, data);
        Path hudPath = pack.resolve("ui/hud_screen.json");
        Files.createDirectories(hudPath.getParent());
        JsonObject hud = Files.exists(hudPath) ? GsonHelper.readJsonObjectFromFile(hudPath) : new JsonObject();
        hud.addProperty("namespace", "hud");
        JsonArray modifications = new JsonArray();
        modifications.add(replaceControl("boss_name", "boss_name", BedrockBossBarTitle.panel(hud, baselines)));
        if (hasStyles) {
            JsonObject nativeBar = new JsonObject();
            nativeBar.add("offset", GsonHelper.createArray(0, 10));
            nativeBar.add("bindings", visibility("(not " + BossBarStyle.titleMatches(BossBarStyle.PREFIX) + ")"));
            modifications.add(replaceControl("progress_bar_for_collections", "progress_bar_for_collections@common.progress_bar_for_collections", nativeBar));
        }
        if (!panels.isEmpty()) {
            JsonObject append = new JsonObject();
            append.addProperty("array_name", "controls");
            append.addProperty("operation", "insert_back");
            append.add("value", panels);
            modifications.add(append);
        }
        hud.add("boss_health_panel", GsonHelper.createObject("modifications", modifications));
        GsonHelper.writeJsonFile(hud, hudPath);
    }

    private static JsonObject replaceControl(String name, String template, JsonObject control) {
        JsonObject replace = new JsonObject();
        replace.addProperty("array_name", "controls");
        replace.addProperty("operation", "replace");
        replace.addProperty("control_name", name);
        replace.add("value", GsonHelper.createObject(template, control));
        return replace;
    }

    private static boolean overridden(JavaResourcePack resources, String name) {
        return resources.hasCustomResource(SOURCE + name + "_background.png") || resources.hasCustomResource(SOURCE + name + "_progress.png");
    }

    private static boolean writeSprite(JavaResourcePack resources, Path pack, Map<String, Boolean> sprites, String name) throws IOException {
        // 同一张贴图可供多个样式共用，缓存它是否含有可见像素并已写入资源包。
        Boolean previous = sprites.get(name);
        if (previous != null) {
            return previous;
        }
        String source = SOURCE + name + ".png";
        try (InputStream stream = resources.openResource(source)) {
            if (stream == null) {
                throw new IOException("Missing BossBar sprite " + source);
            }
            BufferedImage image = ClientPngReader.read(stream);
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    if ((image.getRGB(x, y) >>> 24) != 0) {
                        Path output = pack.resolve(TEXTURES + name + ".png");
                        Files.createDirectories(output.getParent());
                        ImageUtils.writePng(image, output);
                        sprites.put(name, true);
                        return true;
                    }
                }
            }
        }
        sprites.put(name, false);
        return false;
    }

    private static void addLayer(JsonArray layers, String sprite, boolean progress) {
        JsonObject image = new JsonObject();
        image.addProperty("type", "image");
        image.addProperty("texture", TEXTURES + sprite);
        image.addProperty("layer", layers.size() + 1);
        image.add("size", GsonHelper.createArray(182, 5));
        if (progress) {
            // 用 BossBar 当前进度裁剪前景贴图，背景层始终绘制完整宽度
            image.addProperty("clip_direction", "left");
            image.addProperty("clip_pixelperfect", false);
            JsonObject clip = collection("#progress_percentage");
            clip.addProperty("binding_name_override", "#clip_ratio");
            JsonArray bindings = new JsonArray();
            bindings.add(clip);
            image.add("bindings", bindings);
        }
        layers.add(GsonHelper.createObject(sprite, image));
    }

    private static JsonArray visibility(String expression) {
        JsonArray bindings = new JsonArray();
        bindings.add(collection("#bossName"));
        JsonObject view = new JsonObject();
        view.addProperty("binding_type", "view");
        view.addProperty("source_property_name", expression);
        view.addProperty("target_property_name", "#visible");
        bindings.add(view);
        return bindings;
    }

    static JsonObject collection(String name) {
        JsonObject binding = new JsonObject();
        binding.addProperty("binding_type", "collection");
        binding.addProperty("binding_collection_name", "boss_bars");
        binding.addProperty("binding_name", name);
        return binding;
    }
}
