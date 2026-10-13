package net.momirealms.craftengine.core.pack.validation;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.atlas.SimplifiedModelFile;
import net.momirealms.craftengine.core.pack.atlas.TexturedModel;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.core.plugin.logger.PluginLogger;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourcePackJsonUtils;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

public final class ValidationModelResolver {
    private final PluginLogger logger;
    private final Set<Key> modelIds;
    private final Map<Key, SimplifiedModelFile> models;

    public ValidationModelResolver(PluginLogger logger, Set<Key> modelIds, Map<Key, SimplifiedModelFile> models) {
        this.logger = logger;
        this.modelIds = modelIds;
        this.models = models;
    }

    public TexturedModel getTexturedModel(Key currentRL, @Nullable Key parentRL, Map<String, Key> textures, Path[] rootPaths, Map<Key, TexturedModel> cached) {
        TexturedModel cachedModel = cached.get(currentRL);
        if (cachedModel != null) {
            return cachedModel;
        }
        return inheritTexturedModel(new TexturedModel(currentRL, textures), parentRL, rootPaths, cached);
    }

    public TexturedModel getTexturedModel(Key currentRL, SimplifiedModelFile model, Path[] rootPaths, Map<Key, TexturedModel> cached) {
        TexturedModel cachedModel = cached.get(currentRL);
        if (cachedModel != null) {
            return cachedModel;
        }
        return inheritTexturedModel(new TexturedModel(currentRL, model), model.parent, rootPaths, cached);
    }

    public TexturedModel getTexturedModel(Key currentRL, JsonObject model, Path[] rootPaths, Map<Key, TexturedModel> cached) {
        TexturedModel cachedModel = cached.get(currentRL);
        if (cachedModel != null) {
            return cachedModel;
        }
        return getTexturedModel(currentRL, new SimplifiedModelFile(model), rootPaths, cached);
    }

    private TexturedModel inheritTexturedModel(TexturedModel texturedModel, @Nullable Key parentRL, Path[] rootPaths, Map<Key, TexturedModel> cached) {
        Key currentRL = texturedModel.path;
        // 放这里防止parent互相引用造成死循环
        cached.put(currentRL, texturedModel);
        if (parentRL != null) {
            TexturedModel parentModel = cached.get(parentRL);
            if (parentModel == null) {
                String parentModelStringPath = "assets/" + parentRL.namespace() + "/models/" + parentRL.value() + ".json";
                // 找一下有没有自定义的父模型
                JsonObject parentModelJson = getParentModel(parentModelStringPath, rootPaths);
                if (parentModelJson != null) {
                    parentModel = getTexturedModel(parentRL, parentModelJson, rootPaths, cached);
                } else {
                    if (this.modelIds.contains(parentRL)) {
                        // 可能为空，因为存在built-in模型
                        SimplifiedModelFile simplifiedModelFile = this.models.get(parentRL);
                        if (simplifiedModelFile == null) {
                            parentModel = parentRL.equals(TexturedModel.GENERATED.path) ? TexturedModel.GENERATED : TexturedModel.BUILTIN;
                            cached.put(parentRL, parentModel);
                        } else {
                            parentModel = getTexturedModel(parentRL, simplifiedModelFile, rootPaths, cached);
                        }
                    } else {
                        parentModel = TexturedModel.EMPTY;
                        cached.put(parentRL, parentModel);
                    }
                }
            }
            if (parentModel == TexturedModel.EMPTY) {
                String parentModelStringPath = "assets/" + parentRL.namespace() + "/models/" + parentRL.value() + ".json";
                this.logger.warn(TranslationManager.instance().plainTranslation(
                        "resource_pack.missing_parent_model",
                        currentRL.asString(), parentModelStringPath
                ));
            } else {
                texturedModel.addParent(parentModel);
            }
        }
        return texturedModel;
    }

    private JsonObject getParentModel(String path, Path[] rootPaths) {
        // 倒序遍历数组
        for (int i = rootPaths.length - 1; i >= 0; i--) {
            Path rootPath = rootPaths[i];
            Path modelJsonPath = rootPath.resolve(path);
            if (Files.exists(modelJsonPath)) {
                JsonObject parentModelJson = ResourcePackJsonUtils.readJsonObjectOrWarn(this.logger, modelJsonPath);
                if (parentModelJson != null) {
                    return parentModelJson;
                }
            }
        }
        return null;
    }
}
