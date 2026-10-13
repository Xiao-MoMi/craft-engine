package net.momirealms.craftengine.core.pack.validation;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.ResourcePackView;
import net.momirealms.craftengine.core.pack.atlas.Atlas;
import net.momirealms.craftengine.core.pack.atlas.TextureStatus;
import net.momirealms.craftengine.core.pack.atlas.TexturedModel;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ListUtils;

import java.util.*;

final class AtlasValidation {
    private final ValidationContext context;
    private final JsonObject lastBlocksAtlas;
    private final JsonObject lastItemAtlas;
    private final Atlas blockAtlas;
    private final Atlas itemAtlas;

    AtlasValidation(ValidationContext context, ResourcePackView rpView, boolean modern) {
        this.context = context;
        this.lastBlocksAtlas = rpView.getExistingReversed("assets/minecraft/atlases/blocks.json", context::readJson);
        this.lastItemAtlas = modern ? rpView.getExistingReversed("assets/minecraft/atlases/items.json", context::readJson) : null;
        if (modern) {
            this.blockAtlas = new Atlas(ListUtils.newNonNullList(context.resources().blockAtlas(), this.lastBlocksAtlas));
            this.itemAtlas = new Atlas(ListUtils.newNonNullList(context.resources().itemAtlas(), this.lastItemAtlas));
        } else {
            this.blockAtlas = new Atlas(ListUtils.newNonNullList(context.resources().blockAtlas(), context.resources().itemAtlas(), this.lastBlocksAtlas));
            this.itemAtlas = null;
        }
    }

    private static void connectTextures(Map<Key, Key> parent, Collection<Key> textures) {
        Key first = null;
        for (Key texture : textures) {
            parent.putIfAbsent(texture, texture);
            if (first == null) {
                first = texture;
            } else {
                union(parent, first, texture);
            }
        }
    }

    private static Key find(Map<Key, Key> parent, Key i) {
        if (parent.get(i).equals(i)) {
            return i;
        }
        Key root = find(parent, parent.get(i));
        parent.put(i, root); // 路径压缩
        return root;
    }

    private static void union(Map<Key, Key> parent, Key i, Key j) {
        Key rootI = find(parent, i);
        Key rootJ = find(parent, j);
        if (!rootI.equals(rootJ)) {
            parent.put(rootI, rootJ);
        }
    }

    ValidationResult validate(ModelValidation.Models models, ResourcePackView rpView, boolean v1_21_11, boolean obfuscation) {
        Map<Key, TexturedModel> blockModels = models.blocks();
        Map<Key, TexturedModel> itemModels = models.items();
        Map<Key, TexturedModel> modelsCache = models.all();
        MissingResourceReferences textureToModels = new MissingResourceReferences(rpView, this.context.resources().textures(), "textures", ".png", 8);
        ValidationResult result = null;

        if (!obfuscation) {

            if (v1_21_11) {

                // 并查集联通贴图
                boolean fixAtlas = this.context.settings().fixTextureAtlas();
                Map<Key, Key> parent = fixAtlas ? new HashMap<>(512) : Map.of();
                Map<Key, TextureStatus> textureStatuses = new HashMap<>(512);
                Set<Key> notInAtlasTextures = new HashSet<>();

                // 对全部方块状态的模型的贴图进行归类
                for (Map.Entry<Key, TexturedModel> entry : blockModels.entrySet()) {
                    Map<String, Key> textures = entry.getValue().textures;
                    Collection<Key> paths = textures.values();
                    for (Key spritePath : paths) {
                        textureStatuses.compute(spritePath, (path, status) -> {
                            if (status == null) {
                                status = new TextureStatus();
                                if (this.itemAtlas.isDefined(spritePath)) {
                                    status.setInItemAtlas(true);
                                }
                                if (this.blockAtlas.isDefined(spritePath)) {
                                    status.setInBlockAtlas(true);
                                }
                            }
                            status.setShouldBeInBlockAtlas(true);
                            if (!this.context.settings().fixTextureAtlas()) {
                                // 不修复就骂你
                                if (status.inItemAtlas()) {
                                    if (status.inBlockAtlas()) {
                                        this.context.logger().warn(TranslationManager.instance().plainTranslation(
                                                "resource_pack.duplicated_sprite",
                                                entry.getKey().asString(), spritePath.asString(),
                                                "minecraft:textures/atlas/items.png", "minecraft:textures/atlas/blocks.png")
                                        );
                                    } else {
                                        this.context.logger().warn(TranslationManager.instance().plainTranslation(
                                                "resource_pack.bad_sprite",
                                                entry.getKey().asString(), spritePath.asString(),
                                                "minecraft:textures/atlas/blocks.png", "minecraft:textures/atlas/items.png"
                                        ));
                                    }
                                } else {
                                    if (!status.inBlockAtlas() && notInAtlasTextures.add(spritePath)) {
                                        this.context.logger().warn(TranslationManager.instance().plainTranslation(
                                                "resource_pack.texture_not_in_atlas",
                                                spritePath.asString()
                                        ));
                                    }
                                }
                            }
                            return status;
                        });
                    }
                    if (fixAtlas) {
                        connectTextures(parent, paths);
                    }
                }

                // 对全部物品的模型的贴图进行归类
                for (Map.Entry<Key, TexturedModel> entry : itemModels.entrySet()) {
                    Map<String, Key> textures = entry.getValue().textures;
                    Collection<Key> paths = textures.values();

                    boolean hasBlockAtlas = false;
                    for (Key spritePath : paths) {
                        TextureStatus status = textureStatuses.computeIfAbsent(spritePath, (path) -> {
                            TextureStatus newStatus = new TextureStatus();
                            if (this.itemAtlas.isDefined(spritePath)) {
                                newStatus.setInItemAtlas(true);
                            }
                            if (this.blockAtlas.isDefined(spritePath)) {
                                newStatus.setInBlockAtlas(true);
                            }
                            return newStatus;
                        });
                        if (status.inBlockAtlas()) {
                            hasBlockAtlas = true;
                            break;
                        }
                    }

                    boolean finalHasBlockAtlas = hasBlockAtlas;
                    for (Key spritePath : paths) {
                        textureStatuses.compute(spritePath, (path, status) -> {
                            if (status == null) {
                                status = new TextureStatus();
                                if (this.itemAtlas.isDefined(spritePath)) {
                                    status.setInItemAtlas(true);
                                }
                                if (this.blockAtlas.isDefined(spritePath)) {
                                    status.setInBlockAtlas(true);
                                }
                            }
                            if (finalHasBlockAtlas && !status.shouldBeInBlockAtlas()) {
                                status.setShouldBeInBlockAtlas(true);
                            }
                            if (!this.context.settings().fixTextureAtlas()) {
                                // 不修复就骂你
                                if (status.inItemAtlas()) {
                                    if (status.inBlockAtlas()) {
                                        this.context.logger().warn(TranslationManager.instance().plainTranslation(
                                                "resource_pack.duplicated_sprite",
                                                entry.getKey().asString(), spritePath.asString(),
                                                "minecraft:textures/atlas/items.png", "minecraft:textures/atlas/blocks.png"
                                        ));
                                    } else if (finalHasBlockAtlas) {
                                        this.context.logger().warn(TranslationManager.instance().plainTranslation(
                                                "resource_pack.multiple_atlases",
                                                entry.getKey().asString(),
                                                "minecraft:textures/atlas/items.png", "minecraft:textures/atlas/blocks.png"
                                        ));
                                    }
                                } else {
                                    if (!status.inBlockAtlas() && notInAtlasTextures.add(spritePath)) {
                                        this.context.logger().warn(TranslationManager.instance().plainTranslation(
                                                "resource_pack.texture_not_in_atlas",
                                                spritePath.asString()
                                        ));
                                    }
                                }
                            }
                            return status;
                        });
                    }
                    if (fixAtlas) {
                        connectTextures(parent, paths);
                    }
                }

                if (this.context.settings().fixTextureAtlas()) {

                    Set<Key> blockAtlasGroups = new HashSet<>();

                    Set<Key> blockAtlasToAdd = new HashSet<>();
                    Set<Key> itemAtlasToAdd = new HashSet<>();
                    Set<Key> itemAtlasToRemove = new HashSet<>();

                    // 分配不在图集内的贴图到适合的图集内
                    List<Key> lateInitTexture = new ArrayList<>();
                    for (Map.Entry<Key, TextureStatus> entry : textureStatuses.entrySet()) {
                        Key spritePath = entry.getKey();
                        TextureStatus status = entry.getValue();
                        if (status.shouldBeInBlockAtlas()) {
                            if (status.inItemAtlas()) {
                                // 此贴图需要链式影响
                                itemAtlasToRemove.add(spritePath);
                            }
                            if (!status.inBlockAtlas()) {
                                blockAtlasToAdd.add(spritePath);
                                blockAtlasGroups.add(find(parent, spritePath));
                            }
                        } else {
                            if (status.inItemAtlas()) {
                                continue;
                            }
                            if (status.inBlockAtlas()) {
                                continue;
                            }
                            lateInitTexture.add(spritePath);
                        }
                    }

                    for (Key spritePath : lateInitTexture) {
                        if (blockAtlasGroups.contains(find(parent, spritePath))) {
                            blockAtlasToAdd.add(spritePath);
                        } else {
                            itemAtlasToAdd.add(spritePath);
                        }
                    }

                    if (!itemAtlasToRemove.isEmpty()) {
                        // 创建所有贴图到模型的索引
                        Multimap<Key, TexturedModel> textureToModel = HashMultimap.create(512, 4);
                        for (Map.Entry<Key, TexturedModel> entry : modelsCache.entrySet()) {
                            TexturedModel texturedModel = entry.getValue();
                            for (Key texture : entry.getValue().textures.values()) {
                                textureToModel.put(texture, texturedModel);
                            }
                        }

                        Stack<Key> chainDetection = new Stack<>();
                        chainDetection.addAll(itemAtlasToRemove);
                        Set<TexturedModel> expandedModels = new HashSet<>();
                        while (!chainDetection.isEmpty()) {
                            Key spritePath = chainDetection.pop();
                            Collection<TexturedModel> texturedModels = textureToModel.get(spritePath);
                            for (TexturedModel texturedModel : texturedModels) {
                                if (!expandedModels.add(texturedModel)) {
                                    continue;
                                }
                                for (Key texture : texturedModel.textures.values()) {
                                    if (this.itemAtlas.isDefined(texture)) {
                                        if (itemAtlasToRemove.add(texture)) {
                                            blockAtlasToAdd.add(texture);
                                            chainDetection.push(texture);
                                        }
                                    }
                                }
                            }
                        }
                    }

                    itemAtlasToAdd.remove(TexturedModel.EMPTY.path);
                    itemAtlasToRemove.remove(TexturedModel.EMPTY.path);
                    blockAtlasToAdd.remove(TexturedModel.EMPTY.path);

                    Map<Key, Key> sourceTextures = new HashMap<>();
                    for (Key texture : blockAtlasToAdd) {
                        Key resource = this.itemAtlas.getSourceTexturePath(texture);
                        if (resource != null) {
                            sourceTextures.put(texture, resource);
                        }
                    }

                    JsonObject fixedItemAtlas = null;
                    if (!itemAtlasToAdd.isEmpty() || !itemAtlasToRemove.isEmpty()) {
                        fixedItemAtlas = this.lastItemAtlas == null ? new JsonObject() : this.lastItemAtlas;
                        JsonArray sources = fixedItemAtlas.getAsJsonArray("sources");
                        if (sources == null) {
                            sources = new JsonArray();
                            fixedItemAtlas.add("sources", sources);
                        }
                        for (Key texture : itemAtlasToAdd) {
                            JsonObject source = new JsonObject();
                            source.addProperty("type", "single");
                            source.addProperty("resource", texture.asString());
                            sources.add(source);
                            this.itemAtlas.addSingle(texture);
                        }
                        for (Key texture : itemAtlasToRemove) {
                            JsonObject source = new JsonObject();
                            source.addProperty("type", "filter");
                            JsonObject pattern = new JsonObject();
                            pattern.addProperty("namespace", "^" + texture.namespace() + "$");
                            pattern.addProperty("path", "^" + texture.value() + "$");
                            source.add("pattern", pattern);
                            sources.add(source);
                            this.itemAtlas.addDeleted(texture);
                        }
                    }

                    JsonObject fixedBlockAtlas = null;
                    if (!blockAtlasToAdd.isEmpty()) {
                        fixedBlockAtlas = this.lastBlocksAtlas == null ? new JsonObject() : this.lastBlocksAtlas;
                        JsonArray sources = fixedBlockAtlas.getAsJsonArray("sources");
                        if (sources == null) {
                            sources = new JsonArray();
                            fixedBlockAtlas.add("sources", sources);
                        }
                        for (Key texture : blockAtlasToAdd) {
                            Key resource = sourceTextures.getOrDefault(texture, texture);
                            JsonObject source = new JsonObject();
                            source.addProperty("type", "single");
                            source.addProperty("resource", resource.asString());
                            if (!texture.equals(resource)) {
                                source.addProperty("sprite", texture.asString());
                            }
                            sources.add(source);
                            this.blockAtlas.addSingle(texture, resource);
                        }
                    }

                    // 至少有一个修复
                    if (fixedBlockAtlas != null || fixedItemAtlas != null) {
                        result = new ValidationResult(fixedItemAtlas, this.lastItemAtlas, fixedBlockAtlas, this.lastBlocksAtlas);
                    }
                }
            } else {

                Set<Key> blockAtlasToAdd = new HashSet<>();

                // 只需要收集即可
                for (Map.Entry<Key, TexturedModel> entry : blockModels.entrySet()) {
                    Map<String, Key> textures = entry.getValue().textures;
                    for (Map.Entry<String, Key> texture : textures.entrySet()) {
                        Key spritePath = texture.getValue();
                        if (!this.blockAtlas.isDefined(spritePath)) {
                            blockAtlasToAdd.add(spritePath);
                        }
                    }
                }
                for (Map.Entry<Key, TexturedModel> entry : itemModels.entrySet()) {
                    Map<String, Key> textures = entry.getValue().textures;
                    for (Map.Entry<String, Key> texture : textures.entrySet()) {
                        Key spritePath = texture.getValue();
                        if (!this.blockAtlas.isDefined(spritePath)) {
                            blockAtlasToAdd.add(spritePath);
                        }
                    }
                }

                blockAtlasToAdd.remove(TexturedModel.EMPTY.path);

                if (this.context.settings().fixTextureAtlas()) {
                    JsonObject fixedBlockAtlas = null;
                    if (!blockAtlasToAdd.isEmpty()) {
                        fixedBlockAtlas = this.lastBlocksAtlas == null ? new JsonObject() : this.lastBlocksAtlas;
                        JsonArray sources = fixedBlockAtlas.getAsJsonArray("sources");
                        if (sources == null) {
                            sources = new JsonArray();
                            fixedBlockAtlas.add("sources", sources);
                        }
                        for (Key texture : blockAtlasToAdd) {
                            JsonObject source = new JsonObject();
                            source.addProperty("type", "single");
                            source.addProperty("resource", texture.asString());
                            sources.add(source);
                            this.blockAtlas.addSingle(texture);
                        }
                    }

                    // 至少有一个修复
                    if (fixedBlockAtlas != null) {
                        result = new ValidationResult(null, null, fixedBlockAtlas, this.lastBlocksAtlas);
                    }
                } else {
                    // 骂死你臭杂鱼
                    for (Key spritePath : blockAtlasToAdd) {
                        this.context.logger().warn(TranslationManager.instance().plainTranslation(
                                "resource_pack.texture_not_in_atlas",
                                spritePath.asString()
                        ));
                    }
                }
            }
        }

        // 再次遍历验证贴图
        // 只允许在图集内的进行下一步操作
        for (Map.Entry<Key, TexturedModel> entry : blockModels.entrySet()) {
            Map<String, Key> textures = entry.getValue().textures;
            for (Map.Entry<String, Key> texture : textures.entrySet()) {
                Key spritePath = texture.getValue();
                Key sourceTexturePath = this.blockAtlas.getSourceTexturePath(spritePath);
                if (sourceTexturePath != null) {
                    textureToModels.add(sourceTexturePath, entry.getKey());
                }
            }
        }

        for (Map.Entry<Key, TexturedModel> entry : itemModels.entrySet()) {
            Map<String, Key> textures = entry.getValue().textures;
            for (Map.Entry<String, Key> texture : textures.entrySet()) {
                Key spritePath = texture.getValue();
                Key sourceTexturePath = this.itemAtlas == null ? null : this.itemAtlas.getSourceTexturePath(spritePath);
                if (sourceTexturePath != null) {
                    textureToModels.add(sourceTexturePath, entry.getKey());
                } else {
                    sourceTexturePath = this.blockAtlas.getSourceTexturePath(spritePath);
                    if (sourceTexturePath != null) {
                        textureToModels.add(sourceTexturePath, entry.getKey());
                    }
                }
            }
        }

        textureToModels.report(this.context.logger(), "resource_pack.missing_model_texture");

        if (result == null) {
            result = new ValidationResult(null, this.lastItemAtlas, null, this.lastBlocksAtlas);
        }

        return result;
    }

    record ValidationResult(JsonObject fixedItemAtlas,
                            JsonObject originalItemAtlas,
                            JsonObject fixedBlockAtlas,
                            JsonObject originalBlockAtlas) {
    }
}
