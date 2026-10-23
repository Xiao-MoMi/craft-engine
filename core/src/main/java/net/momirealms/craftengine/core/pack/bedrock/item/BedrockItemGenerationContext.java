package net.momirealms.craftengine.core.pack.bedrock.item;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.util.Key;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class BedrockItemGenerationContext {
    private final Path root;
    private final JsonObject icons = new JsonObject(); // 图标资源键到 PNG 路径的映射
    private final Map<Key, BedrockItemBlocks.DisplayBlock> displays = new LinkedHashMap<>(); // 分支 ID 到展示方块资源
    private final Map<Key, List<BedrockItemIcons.Variant>> variants = new LinkedHashMap<>(); // 物品 ID 到全部显示分支
    private final Set<Key> attachables = new HashSet<>();
    private final Set<Key> javaSpears = new HashSet<>();
    private final Set<Key> javaShields = new HashSet<>();
    private final BedrockItemBlocks blocks = new BedrockItemBlocks();
    private final BedrockCrossbowPlayerAnimations crossbowPlayerAnimations = new BedrockCrossbowPlayerAnimations();

    public BedrockItemGenerationContext(Path root) {
        this.root = root;
    }

    public Path root() {
        return this.root;
    }

    public JsonObject icons() {
        return this.icons;
    }

    public Map<Key, BedrockItemBlocks.DisplayBlock> displays() {
        return this.displays;
    }

    public Map<Key, List<BedrockItemIcons.Variant>> variants() {
        return this.variants;
    }

    public Set<Key> attachables() {
        return this.attachables;
    }

    public Set<Key> javaSpears() {
        return this.javaSpears;
    }

    public Set<Key> javaShields() {
        return this.javaShields;
    }

    public BedrockItemBlocks blocks() {
        return this.blocks;
    }

    public BedrockCrossbowPlayerAnimations crossbowPlayerAnimations() {
        return this.crossbowPlayerAnimations;
    }
}
