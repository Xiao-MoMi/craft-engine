package net.momirealms.craftengine.core.pack.validation;

import com.google.common.collect.Multimap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.momirealms.craftengine.core.pack.CubeRotationProcessor;
import net.momirealms.craftengine.core.pack.ResourcePackView;
import net.momirealms.craftengine.core.pack.atlas.SimplifiedModelFile;
import net.momirealms.craftengine.core.pack.atlas.TexturedModel;
import net.momirealms.craftengine.core.pack.model.ModelJsonKeys;
import net.momirealms.craftengine.core.pack.revision.Revision;
import net.momirealms.craftengine.core.pack.revision.Revisions;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MinecraftVersion;
import net.momirealms.craftengine.core.util.VectorUtils;
import org.joml.Vector3f;

import java.nio.file.Path;
import java.util.*;

final class ModelValidation {
    private final ValidationContext context;
    private final ValidationModelResolver resolver;

    ModelValidation(ValidationContext context) {
        this.context = context;
        this.resolver = new ValidationModelResolver(context.logger(), context.resources().modelIds(), context.resources().models());
    }

    Models validate(Path resourcePackPath,
                    Path[] rootPaths,
                    ResourcePackView rpView,
                    ResourceValidation.ModelReferences references,
                    Set<Revision> revisions,
                    boolean v1_21_6,
                    boolean v1_21_11,
                    boolean v26_1) {
        Multimap<Key, Key> modelToItemDefinitions = references.items();
        Multimap<Key, String> modelToBlockStates = references.blocks();
        Set<Path> checkedModels = new HashSet<>();
        // 获取所有能直接从资源包里捕获到的方块和物品模型
        Map<Key, TexturedModel> blockModels = new HashMap<>(256);
        Map<Key, TexturedModel> itemModels = new HashMap<>(256);
        // 获取全部minecraft模型
        Map<Key, TexturedModel> modelsCache = new HashMap<>(256);

        boolean fixMissingTexture = this.context.settings().fixMissingTexture();
        boolean fixTexturesFormat = this.context.settings().fixTexturesFormat() && !v26_1;
        boolean fixRotationAngle = this.context.settings().fixRotationAngle() && !v1_21_11;
        boolean fixElement = fixMissingTexture || fixRotationAngle;
        ModelFixFlags flags = new ModelFixFlags(fixMissingTexture, fixTexturesFormat, fixRotationAngle, fixElement, v1_21_6);

        // 收集全部方块状态的模型贴图
        for (Map.Entry<Key, Collection<String>> entry : modelToBlockStates.asMap().entrySet()) {
            Key modelPath = entry.getKey();
            if (blockModels.containsKey(modelPath)) {
                continue;
            }

            String modelStringPath = "assets/" + modelPath.namespace() + "/models/" + modelPath.value() + ".json";
            Path modelJsonPath = rpView.getExistingReversed(modelStringPath);
            if (modelJsonPath != null) {
                JsonObject modelJson = this.context.readJson(modelJsonPath);
                if (modelJson == null) {
                    continue;
                }
                TexturedModel texturedModel = this.resolver.getTexturedModel(modelPath, modelJson, rootPaths, modelsCache);
                blockModels.put(modelPath, texturedModel);
                if (checkedModels.add(modelJsonPath)) {
                    validateModel(resourcePackPath, modelJson, modelJsonPath, modelStringPath, texturedModel, revisions, flags);
                }
                continue;
            }
            // 资源包中没有模型文件时，回退到原版模型；非原版模型则提示缺失
            if (!this.context.resources().modelIds().contains(modelPath)) {
                modelsCache.put(modelPath, TexturedModel.EMPTY);
                this.context.logger().warn(TranslationManager.instance().plainTranslation("resource_pack.missing_block_model", entry.getValue().toString(), modelStringPath));
            } else {
                SimplifiedModelFile preset = this.context.resources().models().get(modelPath);
                if (preset != null) {
                    blockModels.put(modelPath, this.resolver.getTexturedModel(modelPath, preset, rootPaths, modelsCache));
                }
            }
        }

        // 收集全部物品定义的模型贴图
        for (Map.Entry<Key, Collection<Key>> entry : modelToItemDefinitions.asMap().entrySet()) {
            Key modelPath = entry.getKey();
            if (itemModels.containsKey(modelPath)) {
                continue;
            }
            String modelStringPath = "assets/" + modelPath.namespace() + "/models/" + modelPath.value() + ".json";
            Path modelJsonPath = rpView.getExistingReversed(modelStringPath);
            if (modelJsonPath != null) {
                // A shared block/item model has already been read and validated in this pass.
                // Resolve the path first: validation may have generated a higher-priority overlay.
                TexturedModel blockModel = blockModels.get(modelPath);
                if (blockModel != null && checkedModels.contains(modelJsonPath)) {
                    itemModels.put(modelPath, blockModel);
                    continue;
                }
                JsonObject modelJson = this.context.readJson(modelJsonPath);
                if (modelJson == null) {
                    continue;
                }
                TexturedModel texturedModel = this.resolver.getTexturedModel(modelPath, modelJson, rootPaths, modelsCache);
                itemModels.put(modelPath, texturedModel);
                if (checkedModels.add(modelJsonPath)) {
                    validateModel(resourcePackPath, modelJson, modelJsonPath, modelStringPath, texturedModel, revisions, flags);
                }
                continue;
            }
            if (!this.context.resources().modelIds().contains(modelPath)) {
                modelsCache.put(modelPath, TexturedModel.EMPTY);
                this.context.logger().warn(TranslationManager.instance().plainTranslation("resource_pack.missing_item_model", entry.getValue().toString(), modelStringPath));
            } else {
                SimplifiedModelFile preset = this.context.resources().models().get(modelPath);
                if (preset != null) {
                    itemModels.put(modelPath, this.resolver.getTexturedModel(modelPath, preset, rootPaths, modelsCache));
                }
            }
        }

        Set<TexturedModel> modelsToValidate = new HashSet<>(blockModels.values());
        modelsToValidate.addAll(itemModels.values());
        for (TexturedModel model : modelsToValidate) {
            for (ModelTextureReferenceValidator.Problem problem : ModelTextureReferenceValidator.validate(model)) {
                this.context.logger().warn(TranslationManager.instance().plainTranslation(
                        problem.cycle() ? "resource_pack.cyclic_texture_reference" : "resource_pack.missing_texture_reference",
                        model.path.asString(), problem.location(), problem.chain()
                ));
            }
        }

        return new Models(blockModels, itemModels, modelsCache);
    }

    private void validateModel(Path resourcePackPath,
                               JsonObject modelJson,
                               Path modelJsonPath,
                               String modelStringPath,
                               TexturedModel texturedModel,
                               Set<Revision> revisions,
                               ModelFixFlags flags) {
        boolean changed = this.context.settings().fixMissingParticleTexture() && ModelTextureReferenceValidator.fixMissingParticleTexture(modelJson, texturedModel);
        boolean illegalRotation225 = false;
        boolean illegalRotation45 = false;
        boolean illegalNewRotationFormat = false;
        boolean illegalTextures = false;
        boolean repairedMissingTexture = false;

        List<ModelDisplayBoundsValidator.Problem> displayProblems = ModelDisplayBoundsValidator.validate(modelJson);
        if (!displayProblems.isEmpty()) {
            if (this.context.settings().fixModelDisplayOutOfBounds()) {
                if (ModelDisplayBoundsValidator.fix(modelJson, displayProblems) > 0) {
                    changed = true;
                }
            } else {
                for (ModelDisplayBoundsValidator.Problem problem : displayProblems) {
                    this.context.logger().warn(TranslationManager.instance().plainTranslation(
                            "resource_pack.model_display_out_of_bounds",
                            texturedModel.path.asString(),
                            problem.position(),
                            problem.field(),
                            problem.vector(),
                            String.valueOf(problem.limit())
                    ));
                }
            }
        }

        List<ModelUvBoundsValidator.Problem> uvProblems = ModelUvBoundsValidator.validate(modelJson, texturedModel.textures);
        if (!uvProblems.isEmpty()) {
            if (this.context.settings().fixModelUvOutOfBounds()) {
                if (ModelUvBoundsValidator.fix(modelJson, uvProblems) > 0) {
                    changed = true;
                }
            } else {
                for (ModelUvBoundsValidator.Problem problem : uvProblems) {
                    this.context.logger().warn(TranslationManager.instance().plainTranslation(
                            "resource_pack.model_uv_out_of_bounds",
                            texturedModel.path.asString(),
                            String.valueOf(problem.elementIndex() + 1),
                            problem.face(),
                            problem.uv(),
                            problem.texture()
                    ));
                }
            }
        }

        if (flags.fixTexturesFormat && modelJson.get(ModelJsonKeys.TEXTURES) instanceof JsonObject textures) {
            for (Map.Entry<String, JsonElement> textureEntry : textures.entrySet()) {
                if (textureEntry.getValue() instanceof JsonObject texture && texture.has(ModelJsonKeys.SPRITE)) {
                    illegalTextures = true;
                    break;
                }
            }
        }

        if (flags.fixElement && modelJson.get(ModelJsonKeys.ELEMENTS) instanceof JsonArray elements) {
            for (JsonElement e : elements) {
                if (e instanceof JsonObject element) {

                    // 修复含有 #missing 的贴图的元素
                    if (flags.fixMissingTexture) {
                        if (element.get(ModelJsonKeys.FACES) instanceof JsonObject faces) {
                            for (Map.Entry<String, JsonElement> faceEntry : faces.entrySet()) {
                                if (faceEntry.getValue() instanceof JsonObject face) {
                                    if (face.get(ModelJsonKeys.TEXTURE) instanceof JsonPrimitive texture && texture.getAsString().equals("#missing")) {
                                        Optional<String> first = texturedModel.textures.keySet().stream().findFirst();
                                        if (first.isPresent()) {
                                            face.addProperty(ModelJsonKeys.TEXTURE, "#" + first.get());
                                            repairedMissingTexture = true;
                                            changed = true;
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 检查含有非法旋转角度的元素
                    if (flags.fixRotationAngle) {
                        if (element.get(ModelJsonKeys.ROTATION) instanceof JsonObject rotation) {
                            if (rotation.get(ModelJsonKeys.ANGLE) instanceof JsonPrimitive angle) {
                                float angleValue = angle.getAsFloat();
                                if (angleValue < -45 || angleValue > 45) {
                                    illegalRotation45 = true;
                                }
                                if (!flags.v1_21_6 && angleValue % 22.5f != 0) {
                                    illegalRotation225 = true;
                                }
                            } else if (!rotation.has(ModelJsonKeys.AXIS)) {
                                illegalNewRotationFormat = true;
                            }
                        }
                    }
                }
            }
        }

        if (repairedMissingTexture) {
            texturedModel.refreshFaces(modelJson);
        }

        if (illegalTextures) {
            // 如果资源包支持 26.1
            if (this.context.settings().packMaxVersion().isAtOrAbove(MinecraftVersion.V26_1)) {
                // 先把高版本格式写到 overlay 内
                Path overlayModelPath = resourcePackPath
                        .resolve(this.context.settings().createOverlayFolderName(Revisions.SINCE_26_1.versionString()))
                        .resolve(modelStringPath);
                revisions.add(Revisions.SINCE_26_1);
                this.context.writeJson(modelJson, overlayModelPath);
            }

            // 再进行修复
            JsonObject newTextures = new JsonObject();
            if (modelJson.get(ModelJsonKeys.TEXTURES) instanceof JsonObject textures) {
                for (Map.Entry<String, JsonElement> textureEntry : textures.entrySet()) {
                    if (textureEntry.getValue() instanceof JsonObject texture) {
                        newTextures.addProperty(textureEntry.getKey(), texture.getAsJsonPrimitive(ModelJsonKeys.SPRITE).getAsString());
                    } else if (textureEntry.getValue() instanceof JsonPrimitive texture) {
                        newTextures.addProperty(textureEntry.getKey(), texture.getAsString());
                    }
                }
            }
            modelJson.add(ModelJsonKeys.TEXTURES, newTextures);
            changed = true;
        }

        if (illegalRotation45 || illegalNewRotationFormat) {
            // 如果支持 1.21.11
            if (this.context.settings().packMaxVersion().isAtOrAbove(MinecraftVersion.V1_21_11)) {
                // 先把高版本格式写到 overlay 内
                Path overlayModelPath = resourcePackPath
                        .resolve(this.context.settings().createOverlayFolderName(Revisions.SINCE_1_21_11.versionString()))
                        .resolve(modelStringPath);
                revisions.add(Revisions.SINCE_1_21_11);
                this.context.writeJson(modelJson, overlayModelPath);
            }

            if (modelJson.get(ModelJsonKeys.ELEMENTS) instanceof JsonArray elements) {
                for (JsonElement e : elements) {
                    if (e instanceof JsonObject element) {
                        if (element.get(ModelJsonKeys.ROTATION) instanceof JsonObject rotation) {

                            if (!rotation.has(ModelJsonKeys.ANGLE)) {
                                float x = 0f;
                                float y = 0f;
                                float z = 0f;

                                if (rotation.remove(ModelJsonKeys.X) instanceof JsonPrimitive xp) {
                                    x = xp.getAsFloat();
                                }
                                if (rotation.remove(ModelJsonKeys.Y) instanceof JsonPrimitive yp) {
                                    y = yp.getAsFloat();
                                }
                                if (rotation.remove(ModelJsonKeys.Z) instanceof JsonPrimitive zp) {
                                    z = zp.getAsFloat();
                                }

                                float absX = Math.abs(x);
                                float absY = Math.abs(y);
                                float absZ = Math.abs(z);

                                float finalAngle;
                                String finalAxis;

                                if (absX >= absY && absX >= absZ) {
                                    finalAngle = x;
                                    finalAxis = "x";
                                } else if (absY >= absZ) {
                                    finalAngle = y;
                                    finalAxis = "y";
                                } else {
                                    finalAngle = z;
                                    finalAxis = "z";
                                }

                                rotation.addProperty(ModelJsonKeys.AXIS, finalAxis);
                                rotation.addProperty(ModelJsonKeys.ANGLE, finalAngle);
                            }

                            if (rotation.get(ModelJsonKeys.ANGLE) instanceof JsonPrimitive angle) {
                                float angleValue = angle.getAsFloat();
                                if (angleValue < -45 || angleValue > 45) {
                                    Vector3f from = VectorUtils.vector3f(element.getAsJsonArray(ModelJsonKeys.FROM));
                                    Vector3f to = VectorUtils.vector3f(element.getAsJsonArray(ModelJsonKeys.TO));
                                    Vector3f origin = VectorUtils.vector3f(rotation.getAsJsonArray(ModelJsonKeys.ORIGIN));
                                    String axis = rotation.getAsJsonPrimitive(ModelJsonKeys.AXIS).getAsString();

                                    CubeRotationProcessor.RotationResult result = CubeRotationProcessor.process(from, to, origin, axis, angleValue);

                                    element.add(ModelJsonKeys.FROM, VectorUtils.toJson(result.from()));
                                    element.add(ModelJsonKeys.TO, VectorUtils.toJson(result.to()));
                                    rotation.add(ModelJsonKeys.ORIGIN, VectorUtils.toJson(result.origin()));
                                    rotation.addProperty(ModelJsonKeys.ANGLE, result.angle());

                                    if (element.has(ModelJsonKeys.FACES)) {
                                        JsonObject oldFaces = element.getAsJsonObject(ModelJsonKeys.FACES);
                                        JsonObject newFaces = new JsonObject();

                                        for (CubeRotationProcessor.Face sourceFace : CubeRotationProcessor.Face.values()) {
                                            if (oldFaces.has(sourceFace.name())) {
                                                JsonObject faceObj = oldFaces.getAsJsonObject(sourceFace.name()).deepCopy();
                                                CubeRotationProcessor.FaceData mappingData = result.faceChanges().get(sourceFace);

                                                int oldRot = faceObj.has(ModelJsonKeys.ROTATION) ? faceObj.get(ModelJsonKeys.ROTATION).getAsInt() : 0;
                                                int finalRot = (oldRot + mappingData.uvRotation()) % 360;

                                                if (finalRot != 0) {
                                                    faceObj.addProperty(ModelJsonKeys.ROTATION, finalRot);
                                                } else {
                                                    faceObj.remove(ModelJsonKeys.ROTATION);
                                                }

                                                newFaces.add(mappingData.targetFace().name(), faceObj);
                                            }
                                        }
                                        element.add(ModelJsonKeys.FACES, newFaces);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            changed = true;
        }

        // 遵循 22.5 且在 [-45, 45]
        if (illegalRotation225) {
            // 如果支持 1.21.6
            if (this.context.settings().packMaxVersion().isAtOrAbove(MinecraftVersion.V1_21_6)) {
                // 先把高版本格式写到 overlay 内
                Path overlayModelPath = resourcePackPath
                        .resolve(this.context.settings().createOverlayFolderName(Revisions.SINCE_1_21_6.versionString()))
                        .resolve(modelStringPath);
                revisions.add(Revisions.SINCE_1_21_6);
                this.context.writeJson(modelJson, overlayModelPath);
            }

            if (modelJson.get(ModelJsonKeys.ELEMENTS) instanceof JsonArray elements) {
                for (JsonElement e : elements) {
                    if (e instanceof JsonObject element) {
                        if (element.get(ModelJsonKeys.ROTATION) instanceof JsonObject rotation && rotation.get(ModelJsonKeys.ANGLE) instanceof JsonPrimitive angle) {
                            float finalValue = Math.round(angle.getAsFloat() / 22.5f) * 22.5f;
                            rotation.addProperty(ModelJsonKeys.ANGLE, finalValue);
                        }
                    }
                }
            }
            changed = true;
        }

        if (changed) {
            this.context.writeJson(modelJson, modelJsonPath);
        }
    }

    record Models(Map<Key, TexturedModel> blocks, Map<Key, TexturedModel> items, Map<Key, TexturedModel> all) {
    }

    private record ModelFixFlags(boolean fixMissingTexture,
                                 boolean fixTexturesFormat,
                                 boolean fixRotationAngle,
                                 boolean fixElement,
                                 boolean v1_21_6) {
    }
}
