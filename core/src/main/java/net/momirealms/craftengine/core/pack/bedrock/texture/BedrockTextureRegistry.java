package net.momirealms.craftengine.core.pack.bedrock.texture;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.GsonHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 汇总一次基岩版资源包生成中的纹理登记信息，防止输出冲突。
 * 保存方块、物品的纹理名称映射和逐帧动画配置，最后统一写出 JSON。
 * 图片本身由调用方生成和写入。
 */
public final class BedrockTextureRegistry {
    // 方块纹理名称到图片路径的映射。
    private final JsonObject terrain = new JsonObject();
    // 物品图标名称到图片路径的映射。
    private final JsonObject items = new JsonObject();
    // 按图集名称和变体索引去重的动画配置，保留登记顺序。
    private final Map<String, JsonObject> flipbooks = new LinkedHashMap<>();
    // 已占用的图片输出路径及其来源，用于避免重复写入。
    private final Map<String, String> outputs = new HashMap<>();

    /**
     * 登记纹理输出路径，供调用方避免重复写入；首次登记的来源优先。
     * 同一来源重复登记时静默跳过，不同来源占用同一路径时记录冲突警告。
     * 此方法仅登记路径，不执行文件写入。
     *
     * @param path 图片在资源包中的输出路径，包含扩展名
     * @param owner 生成来源标识，用于判断和报告冲突
     * @return 首次登记为 true，路径已被占用为 false
     */
    public boolean claim(String path, String owner) {
        String previous = this.outputs.putIfAbsent(path, owner);
        if (previous == null) {
            return true;
        }
        if (!previous.equals(owner)) {
            CraftEngine.instance().logger().warn("Conflicting Bedrock texture " + path + ": " + previous + " and " + owner);
        }
        return false;
    }

    /**
     * 判断输出路径是否已被登记，不检查实际文件是否存在。
     */
    public boolean contains(String path) {
        return this.outputs.containsKey(path);
    }

    /**
     * 登记方块纹理名称及图片路径；路径相对于资源包根目录，不含扩展名。
     */
    public void terrain(String name, String path) {
        this.add(this.terrain, name, path);
    }

    /**
     * 登记物品图标名称及图片路径；路径相对于资源包根目录，不含扩展名。
     */
    public void item(String name, String path) {
        this.add(this.items, name, path);
    }

    /**
     * 添加图集条目；同名同路径允许重复登记，同名不同路径抛出异常。
     */
    private void add(JsonObject atlas, String name, String path) {
        JsonObject entry = new JsonObject();
        entry.addProperty("textures", path);
        if (atlas.has(name) && !atlas.get(name).equals(entry)) {
            throw new IllegalArgumentException("Conflicting Bedrock atlas entry " + name);
        }
        atlas.add(name, entry);
    }

    /**
     * 登记按图片帧顺序播放的动画，不插值、不额外平铺。
     */
    public void flipbook(String tile, int index, String path, int interval) {
        this.flipbook(tile, index, path, interval, 1);
    }

    /**
     * 登记按图片帧顺序播放的动画，指定平铺倍率，不插值。
     */
    public void flipbook(String tile, int index, String path, int interval, int replicate) {
        this.flipbook(tile, index, path, interval, replicate, List.of(), false);
    }

    /**
     * 登记逐帧动画；同一图集条目和变体的配置不同则抛出异常。
     */
    public void flipbook(String tile, int index, String path, int interval, int replicate, List<Integer> frames, boolean blend) {
        JsonObject entry = new JsonObject();
        entry.addProperty("atlas_tile", tile);
        if (index >= 0) {
            entry.addProperty("atlas_index", index);
            entry.addProperty("atlas_tile_variant", index);
        }
        entry.addProperty("flipbook_texture", path);
        entry.addProperty("ticks_per_frame", interval);
        entry.addProperty("blend_frames", blend);
        if (!frames.isEmpty()) {
            JsonArray sequence = new JsonArray(frames.size());
            for (int frame : frames) {
                sequence.add(frame);
            }
            entry.add("frames", sequence);
        }
        if (replicate != 1) {
            entry.addProperty("replicate", replicate);
        }
        String key = tile + ':' + Math.max(0, index);
        JsonObject previous = this.flipbooks.putIfAbsent(key, entry);
        if (previous != null && !previous.equals(entry)) {
            throw new IllegalArgumentException("Conflicting Bedrock flipbook " + key);
        }
    }

    /**
     * 将登记信息写入资源包根目录下的 textures 文件夹。
     * 始终写出 item_texture.json，方块图集和动画配置仅在非空时写出。
     */
    public void write(Path root) throws IOException {
        Path directory = Files.createDirectories(root.resolve("textures"));
        this.writeAtlas(directory.resolve("item_texture.json"), "atlas.items", this.items);
        if (!this.terrain.isEmpty()) {
            this.writeAtlas(directory.resolve("terrain_texture.json"), "atlas.terrain", this.terrain);
        }
        if (!this.flipbooks.isEmpty()) {
            JsonArray entries = new JsonArray();
            this.flipbooks.values().forEach(entries::add);
            GsonHelper.writeJsonFile(entries, directory.resolve("flipbook_textures.json"));
        }
    }

    /**
     * 写出图集 JSON；地形图集额外设置边缘填充和 mipmap 层数。
     */
    private void writeAtlas(Path path, String name, JsonObject entries) throws IOException {
        JsonObject atlas = new JsonObject();
        atlas.addProperty("resource_pack_name", "craftengine");
        atlas.addProperty("texture_name", name);
        if (name.equals("atlas.terrain")) {
            atlas.addProperty("padding", 8);
            atlas.addProperty("num_mip_levels", 4);
        }
        atlas.add("texture_data", entries);
        GsonHelper.writeJsonFile(atlas, path);
    }
}
