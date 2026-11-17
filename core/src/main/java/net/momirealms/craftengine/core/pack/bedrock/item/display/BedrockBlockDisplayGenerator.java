package net.momirealms.craftengine.core.pack.bedrock.item.display;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Either;
import net.momirealms.craftengine.bedrock.BlockStateKey;
import net.momirealms.craftengine.core.block.BlockRegistryMirror;
import net.momirealms.craftengine.core.block.BlockStateWrapper;
import net.momirealms.craftengine.core.pack.bedrock.block.JavaBlockModelResolver;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemRenderTarget;
import net.momirealms.craftengine.core.pack.bedrock.model.BedrockModelTransform;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModel;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResources;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureUsage;
import net.momirealms.craftengine.core.pack.model.definition.Transformation;
import net.momirealms.craftengine.core.pack.model.definition.tint.ConstantTint;
import net.momirealms.craftengine.core.pack.model.definition.tint.GrassTint;
import net.momirealms.craftengine.core.pack.model.definition.tint.Tint;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.core.util.VectorUtils;
import org.joml.Matrix3d;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class BedrockBlockDisplayGenerator {
    private BedrockBlockDisplayGenerator() {
    }

    static JsonObject write(JavaModelResources resources, BedrockDisplayAssets assets,
                            Map<List<BedrockItemDisplayGenerator.Part>, Integer> meshes, JsonArray appearances,
                            List<String> selectors) throws IOException {
        JsonObject result = new JsonObject();
        if (selectors.isEmpty()) {
            return result;
        }
        JavaBlockModelResolver resolver = new JavaBlockModelResolver(resources);
        Map<JsonArray, Integer> poses = new HashMap<>();
        for (BlockStateWrapper state : select(selectors, BlockRegistryMirror.blockStates())) {
            String key = BlockStateKey.parse(state.getAsString()).toString();
            try {
                List<JavaBlockModelResolver.Part> resolved = state.isAir() ? List.of() : resolver.resolve(state);
                Matrix4fc shared = sharedRotation(resolved);
                List<BedrockItemDisplayGenerator.Part> parts = parts(state, resolved, shared);
                Integer shape = meshes.get(parts);
                if (shape == null) {
                    shape = assets.add(parts, TextureUsage.BLOCK);
                    if (shape == 0) {
                        throw new IllegalArgumentException("The block model has no renderable materials");
                    }
                    meshes.put(parts, shape);
                }
                JsonArray pose = appearance(shape, shared);
                Integer index = poses.get(pose);
                if (index == null) {
                    index = appearances.size();
                    appearances.add(pose);
                    poses.put(pose, index);
                }
                result.addProperty(key, index);
            } catch (IOException | IllegalArgumentException | UnsupportedOperationException exception) {
                TranslationManager.warn("resource_pack.bedrock.block.display_skipped", key, exception.getLocalizedMessage());
            }
        }
        return result;
    }

    static Set<BlockStateWrapper> select(List<String> selectors, BlockStateWrapper[] states) {
        Set<BlockStateWrapper> selected = new LinkedHashSet<>();
        Map<BlockStateKey, BlockStateWrapper> available = new LinkedHashMap<>();
        for (BlockStateWrapper state : states) {
            if (state.ownerId().isMinecraftNamespace()) {
                available.put(BlockStateKey.parse(state.getAsString()), state);
            }
        }
        for (String value : selectors) {
            try {
                BlockStateKey selector = BlockStateKey.parse(value);
                boolean matched = false;
                for (Map.Entry<BlockStateKey, BlockStateWrapper> entry : available.entrySet()) {
                    if (selector.matches(entry.getKey())) {
                        selected.add(entry.getValue());
                        matched = true;
                    }
                }
                if (!matched) {
                    throw new IllegalArgumentException("No legal vanilla block states match this selector");
                }
            } catch (IllegalArgumentException exception) {
                TranslationManager.warn("resource_pack.bedrock.block.display_skipped", value, exception.getLocalizedMessage());
            }
        }
        return selected;
    }

    private static Matrix4fc sharedRotation(List<JavaBlockModelResolver.Part> parts) {
        if (!parts.isEmpty()) {
            Matrix4fc first = parts.getFirst().transform();
            if (parts.stream().allMatch(part -> first.equals(part.transform()))) {
                return first;
            }
        }
        return new Matrix4f();
    }

    static JsonArray appearance(int shape, Matrix4fc rotation) {
        // 几何以方块中心为原点且 X 反向；展示以角点为原点且 Z 反向。
        // 状态旋转发生在中心，角点补偿在其外层，最后才应用 Display 的 T·L·S·R。
        Matrix4f matrix = new Matrix4f().rotationY((float) Math.PI).scale(-1, 1, 1)
                .mul(new Matrix4f(rotation).setTranslation(0, 0, 0)).scale(-1, 1, 1);
        Vector3f angles = BedrockModelTransform.rotationAngles(new Matrix3d(matrix).transpose()).negate();
        JsonArray pose = new JsonArray();
        pose.add(shape);
        pose.add(VectorUtils.toJson(8, 8, -8));
        pose.add(VectorUtils.toJson(angles));
        pose.add(VectorUtils.toJson(1, 1, 1));
        return pose;
    }

    private static List<BedrockItemDisplayGenerator.Part> parts(BlockStateWrapper state, List<JavaBlockModelResolver.Part> resolved, Matrix4fc shared) {
        List<BedrockItemDisplayGenerator.Part> result = new ArrayList<>(resolved.size());
        for (JavaBlockModelResolver.Part part : resolved) {
            if (part.model().elements().isEmpty()) {
                continue;
            }
            List<Transformation> transforms = List.of();
            if (!shared.equals(part.transform())) {
                Matrix4fc matrix = part.transform();
                List<Float> values = new ArrayList<>(16);
                for (int row = 0; row < 4; row++) {
                    for (int column = 0; column < 4; column++) {
                        values.add(matrix.get(column, row));
                    }
                }
                transforms = List.of(new Transformation(values));
            }
            result.add(new BedrockItemDisplayGenerator.Part(new ItemRenderTarget.Model(part.model().id(), List.of(tint(state))),
                    transforms, JavaModel.Transform.IDENTITY, false, part.model()));
        }
        return List.copyOf(result);
    }

    private static Tint tint(BlockStateWrapper state) {
        int color = switch (state.ownerId().value()) {
            case "grass_block", "short_grass", "grass", "tall_grass", "fern", "large_fern", "potted_fern", "bush" -> -1;
            case "oak_leaves", "jungle_leaves", "acacia_leaves", "dark_oak_leaves", "mangrove_leaves", "vine" -> 0x48b518;
            case "spruce_leaves" -> 0x619961;
            case "birch_leaves" -> 0x80a755;
            case "lily_pad" -> 0x71c35c;
            case "attached_melon_stem", "attached_pumpkin_stem" -> 0xe0c71c;
            case "melon_stem", "pumpkin_stem" -> {
                int age = state.getProperty("age");
                yield (age * 32 << 16) | (255 - age * 8 << 8) | age * 4;
            }
            case "redstone_wire" -> {
                int power = state.getProperty("power");
                float level = power / 15f;
                int red = (int) ((level * 0.6f + (power == 0 ? 0.3f : 0.4f)) * 255);
                int green = (int) (Math.clamp(level * level * 0.7f - 0.5f, 0, 1) * 255);
                int blue = (int) (Math.clamp(level * level * 0.6f - 0.7f, 0, 1) * 255);
                yield (red << 16) | (green << 8) | blue;
            }
            default -> 0xffffff;
        };
        return color == -1 ? new GrassTint(0.5f, 1) : new ConstantTint(Either.left(color));
    }
}
