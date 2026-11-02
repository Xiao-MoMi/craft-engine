package net.momirealms.craftengine.core.pack.bedrock.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.momirealms.craftengine.core.pack.bedrock.model.BedrockGeneratedGeometry;
import net.momirealms.craftengine.core.pack.bedrock.model.BedrockModelTransform;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModel;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureData;
import net.momirealms.craftengine.core.pack.model.definition.condition.UsingItemConditionProperty;
import net.momirealms.craftengine.core.pack.model.definition.rangedisptach.UseDurationRangeDispatchProperty;
import net.momirealms.craftengine.core.pack.model.generation.display.DisplayPosition;
import net.momirealms.craftengine.core.util.*;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;

import static net.momirealms.craftengine.bedrock.BedrockPackLayout.FORMAT_VERSION;

public final class BedrockItemAttachables {
    private static final float RADIANS = (float) (Math.PI / 180);
    private static final float PLAYER_SCALE = 0.9375f;
    private static final float MODEL_RADIUS = (float) Math.sqrt(8 * 8 + 8 * 8 + 0.5f * 0.5f);
    private static final String BINDING = "ce_binding";
    private static final String SLOT = "ce_slot";
    private static final String X = "ce_display_x";
    private static final String Y = "ce_display_y";
    private static final String Z = "ce_display_z";
    private static final View[] VIEWS = { View.FIRST_RIGHT, View.FIRST_LEFT, View.THIRD_RIGHT, View.THIRD_LEFT };

    private BedrockItemAttachables() {
    }

    public static void write(Key id, JavaModel model, BufferedImage image, String texture, Path root) throws IOException {
        BedrockItemAttachment attachment = new BedrockItemAttachment(id);
        addStaticPart(attachment, model, image, texture, -1);
        attachment.showInHand("controller.render.item_default");
        attachment.write(root, null);
    }

    static void writeComposite(Key id, List<JavaModel> models, List<BufferedImage> images, Path root) throws IOException {
        BedrockItemAttachment attachment = new BedrockItemAttachment(id);
        JsonObject controllers = new JsonObject();
        for (int i = 0; i < models.size(); i++) {
            String alias = "part_" + i;
            String texture = "textures/" + id.namespace() + '/' + id.value() + "/" + alias;
            BufferedImage image = images.get(i);
            ImageUtils.writePng(image, root.resolve(texture + ".png"));
            addStaticPart(attachment, models.get(i), image, texture, i);
            // 所有部件同时可见，各自的动画只作用于本部件的骨骼。
            String controller = "controller.render." + attachment.prefix + "." + alias;
            controllers.add(controller, renderController("Geometry." + alias, "Texture." + alias));
            attachment.showInHand(controller);
        }
        writeControllers(controllers, attachment.controllerPath(root));
        attachment.write(root, null);
    }

    private static void addStaticPart(BedrockItemAttachment attachment, JavaModel model, BufferedImage image, String texture, int layer) {
        String alias = layer < 0 ? "default" : "part_" + layer;
        String prefix = layer < 0 ? attachment.prefix : attachment.prefix + "_" + alias;
        String bonePrefix = layer < 0 ? "" : alias + "_";
        String geometryId = "geometry." + prefix;
        JsonArray cubes = new JsonArray();
        BedrockGeneratedGeometry.addLayer(cubes, new TextureData(image, null), null, Math.max(0, layer), image.getWidth(), image.getHeight());
        displayAnimations(model, "animation." + prefix + '.', -1, bonePrefix, attachment.animations, attachment.animationNames, attachment.animate);
        attachment.addGeometry(alias, geometryId, texture, displayGeometry(model, cubes, geometryId, MODEL_RADIUS, bonePrefix));
    }

    public static void writeBow(Key id, List<Stage> stages, int idleStage, int maximumUseTicks, Path root) throws IOException {
        BedrockItemAttachment attachment = new BedrockItemAttachment(id);
        JsonArray geometryFrames = new JsonArray();
        JsonArray textureFrames = new JsonArray();
        int pullingStages = 0;
        for (int i = 0; i < stages.size(); i++) {
            Stage stage = stages.get(i);
            String alias = "stage_" + i;
            String geometryId = "geometry." + attachment.prefix + "_v" + i;
            // 沿用原版闲置、初拉、后续拉弓的局部位置。
            float x = 2;
            float z = -2;
            if (i != idleStage) {
                z = -1;
                if (pullingStages++ > 0) {
                    x = 2.01f;
                }
            }
            attachment.addGeometry(alias, geometryId, stage.texture(), bowGeometry(geometryId, alias, x, z, stage.transformation()));
            geometryFrames.add("Geometry." + alias);
            textureFrames.add("Texture." + alias);
        }
        bowAnimations(id, attachment.animations, attachment.animationNames, attachment.animate);
        String controller = "controller.render." + attachment.prefix;
        writeController(controller, geometryFrames, textureFrames, attachment.controllerPath(root));
        attachment.controllers.add(GsonHelper.createObject(controller, new JsonPrimitive("(" + BedrockItemAttachment.IN_HAND + ") && variable.ce_stage >= 0")));
        attachment.write(root, bowScripts(stageSelection(stages, Stage::condition), maximumUseTicks));
    }

    static <T> String stageSelection(List<T> stages, Function<T, String> condition) {
        StringBuilder script = new StringBuilder("variable.ce_stage = ");
        for (int i = 0; i < stages.size(); i++) {
            script.append('(').append(condition.apply(stages.get(i))).append(") ? ").append(i).append(" : (");
        }
        script.append("-1");
        for (int i = 0; i < stages.size(); i++) {
            script.append(')');
        }
        return script.append(';').toString();
    }

    static JsonArray bowScripts(String stageSelection, int maximumUseTicks) {
        JsonArray preAnimation = new JsonArray();
        // 原版弓使用主手 tick 查询；通用 item_in_use_duration 在部分客户端的单位与文档不一致。
        preAnimation.add("variable.ce_using_item = context.item_slot == 'main_hand' && query.is_using_item && query.main_hand_item_use_duration > 0;");
        preAnimation.add("variable.ce_use_ticks = variable.ce_using_item ? math.max(0, query.main_hand_item_max_duration - query.main_hand_item_use_duration) : 0;");
        preAnimation.add("variable.ce_charge_amount = variable.ce_using_item ? math.clamp((variable.ce_use_ticks + query.frame_alpha - 1) / 10, 0, 1) : 0;");
        // Geyser 的最大使用时长可能不同于 Java；remaining 必须按来源物品计算。
        preAnimation.add("variable.ce_remaining_ticks = variable.ce_using_item ? math.max(0, " + maximumUseTicks + " - variable.ce_use_ticks) : 0;");
        preAnimation.add(stageSelection);
        return preAnimation;
    }

    static int findBowIdle(List<ItemBindingRule> rules) {
        boolean hasUseCondition = false;
        int idle = -1;
        for (int i = 0; i < rules.size(); i++) {
            ItemBindingRule rule = rules.get(i);
            hasUseCondition = hasUseCondition || rule.hasCondition(BedrockItemAttachables::isUseCondition);
            if (idle == -1 && matchesIdle(rule)) {
                idle = i;
            }
            if (hasUseCondition && idle != -1) {
                break;
            }
        }
        if (!hasUseCondition || idle == -1) {
            return -1;
        }
        List<ItemBindingCondition> conditions = rules.get(idle).conditionsExcept(BedrockItemAttachables::isUseCondition);
        // 先找到真正的待机，再检查所有阶段的非使用条件，避免吞掉耐久等注册分支。
        for (ItemBindingRule rule : rules) {
            if (!(rule.target() instanceof ItemRenderTarget.Model)
                    || !rule.matchesConditionsExcept(conditions, BedrockItemAttachables::isUseCondition)) {
                return -1;
            }
        }
        return idle;
    }

    static boolean isUseCondition(ItemBindingCondition condition) {
        return (condition instanceof ItemBindingCondition.BooleanMatch match && match.property() instanceof UsingItemConditionProperty)
                || (condition instanceof ItemBindingCondition.NumericRange range && range.property() instanceof UseDurationRangeDispatchProperty);
    }

    @SuppressWarnings("DeconstructionCanBeUsed")
    static boolean matchesIdle(ItemBindingRule rule) {
        for (ItemBindingCondition condition : rule.conditions()) {
            if (condition instanceof ItemBindingCondition.BooleanMatch match && match.property() instanceof UsingItemConditionProperty && match.expected()) {
                return false;
            }
            if (condition instanceof ItemBindingCondition.NumericRange range && range.property() instanceof UseDurationRangeDispatchProperty && !range.matches(0)) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("DeconstructionCanBeUsed")
    static String useCondition(ItemBindingRule rule) {
        StringBuilder expression = new StringBuilder("1");
        for (ItemBindingCondition condition : rule.conditions()) {
            if (condition instanceof ItemBindingCondition.BooleanMatch match && match.property() instanceof UsingItemConditionProperty) {
                expression.append(match.expected() ? " && variable.ce_using_item" : " && !variable.ce_using_item");
            } else if (condition instanceof ItemBindingCondition.NumericRange range && range.property() instanceof UseDurationRangeDispatchProperty duration) {
                String ticks = duration.remaining() ? "variable.ce_remaining_ticks" : "variable.ce_use_ticks";
                String value = "(" + ticks + " * " + range.scale() + ")";
                if (range.minInclusive() != null) {
                    expression.append(" && ").append(value).append(" >= ").append(range.minInclusive());
                }
                if (range.maxExclusive() != null) {
                    expression.append(" && ").append(value).append(" < ").append(range.maxExclusive());
                }
            }
        }
        return expression.toString();
    }

    static void writeController(String id, JsonArray geometryFrames, JsonArray textureFrames, Path path) throws IOException {
        JsonObject controller = new JsonObject();
        JsonObject arrays = new JsonObject();
        JsonObject geometryArray = new JsonObject();
        geometryArray.add("array.stage_geometries", geometryFrames);
        arrays.add("geometries", geometryArray);
        JsonObject textureArray = new JsonObject();
        textureArray.add("array.stage_textures", textureFrames);
        arrays.add("textures", textureArray);
        controller.add("arrays", arrays);
        addRenderBindings(controller, "array.stage_geometries[variable.ce_stage]", "array.stage_textures[variable.ce_stage]");
        JsonObject definitions = new JsonObject();
        definitions.add(id, controller);
        writeControllers(definitions, path);
    }

    private static JsonObject renderController(String geometry, String texture) {
        JsonObject controller = new JsonObject();
        addRenderBindings(controller, geometry, texture);
        return controller;
    }

    private static void addRenderBindings(JsonObject controller, String geometry, String texture) {
        controller.addProperty("geometry", geometry);
        JsonArray materials = new JsonArray();
        JsonObject material = new JsonObject();
        material.addProperty("*", "variable.is_enchanted ? Material.enchanted : Material.default");
        materials.add(material);
        controller.add("materials", materials);
        JsonArray textures = new JsonArray();
        textures.add(texture);
        textures.add("Texture.enchanted");
        controller.add("textures", textures);
    }

    private static void writeControllers(JsonObject definitions, Path path) throws IOException {
        JsonObject file = new JsonObject();
        file.addProperty(FORMAT_VERSION, "1.10.0");
        file.add("render_controllers", definitions);
        BedrockItemAttachment.writeJson(file, path);
    }

    static void displayAnimations(JavaModel model, String prefix, int stage, JsonObject animations, JsonObject names, JsonArray animate) {
        displayAnimations(model, prefix, stage, "", animations, names, animate);
    }

    private static void displayAnimations(JavaModel model, String prefix, int stage, String bonePrefix,
                                          JsonObject animations, JsonObject names, JsonArray animate) {
        for (View view : VIEWS) {
            JavaModel.Transform source = model.display().getOrDefault(view.position, JavaModel.Transform.IDENTITY);
            String name = bonePrefix + view.animation;
            String expression = view.condition;
            if (stage >= 0) {
                name = "stage_" + stage + '_' + name;
                expression = "(" + expression + ") && variable.ce_stage == " + stage;
            }
            animations.add(prefix + name, animation(view, source, bonePrefix));
            names.addProperty(name, prefix + name);
            JsonObject condition = new JsonObject();
            condition.addProperty(name, expression);
            animate.add(condition);
        }
    }

    static JsonObject displayGeometry(JavaModel model, JsonArray cubes, String geometryId, float modelRadius) {
        return displayGeometry(model, cubes, geometryId, modelRadius, "");
    }

    static void contextAnimation(View view, JavaModel model, String animationPrefix, String bonePrefix, String active, JsonObject animations, JsonObject names, JsonArray animate) {
        String alias = bonePrefix + view.animation;
        String name = animationPrefix + alias;
        animations.add(name, animation(view, model.display().getOrDefault(view.position, JavaModel.Transform.IDENTITY), bonePrefix));
        names.addProperty(alias, name);
        JsonObject condition = new JsonObject();
        condition.addProperty(alias, active);
        animate.add(condition);
    }

    static JsonObject displayGeometry(JavaModel model, JsonArray cubes, String geometryId, float modelRadius, String bonePrefix) {
        JsonObject binding = bone(bonePrefix + BINDING, null);
        // 绑定骨骼保持恒等变换，避免多材质绘制时在缓存矩阵上重复叠加姿态。
        binding.add("pivot", VectorUtils.toJson(0, 24, 0));
        binding.addProperty("binding", "query.item_slot_to_bone_name(context.item_slot)");
        JsonObject slot = bone(bonePrefix + SLOT, bonePrefix + BINDING);
        JsonObject mesh = bone(bonePrefix + Z, bonePrefix + Y);
        mesh.add("cubes", cubes);
        JsonArray bones = new JsonArray();
        bones.add(binding);
        bones.add(slot);
        bones.add(bone(bonePrefix + X, bonePrefix + SLOT));
        bones.add(bone(bonePrefix + Y, bonePrefix + X));
        bones.add(mesh);

        JsonObject description = new JsonObject();
        description.addProperty("identifier", geometryId);
        description.addProperty("texture_width", 16);
        description.addProperty("texture_height", 16);
        expandDisplayBounds(description, model, modelRadius, bonePrefix.isEmpty() ? VIEWS : View.values());
        description.add("visible_bounds_offset", VectorUtils.toJson(0, 0, 0));
        JsonObject geometry = new JsonObject();
        geometry.add("description", description);
        geometry.add("bones", bones);
        return geometry;
    }

    static void expandDisplayBounds(JsonObject description, JavaModel model, float modelRadius) {
        expandDisplayBounds(description, model, modelRadius, VIEWS);
    }

    private static void expandDisplayBounds(JsonObject description, JavaModel model, float modelRadius, View[] views) {
        float radius = 0;
        for (View view : views) {
            JavaModel.Transform source = model.display().getOrDefault(view.position, JavaModel.Transform.IDENTITY);
            float scale = Math.max(Math.abs(source.scale().x), Math.max(Math.abs(source.scale().y), Math.abs(source.scale().z)));
            radius = Math.max(radius, (modelRadius * scale + source.translation().length()) * view.pose.scale + view.pose.translation.length() + 8);
        }
        double size = Math.ceil(radius / 8);
        if (!description.has("visible_bounds_width") || size > description.get("visible_bounds_width").getAsDouble()) {
            description.addProperty("visible_bounds_width", size);
            description.addProperty("visible_bounds_height", size);
        }
    }

    private static JsonObject bowGeometry(String identifier, String texture, float x, float z, Matrix4fc transformation) {
        JsonObject mesh = new JsonObject();
        mesh.add("local_pivot", VectorUtils.toJson(6, 0, 6));
        mesh.add("position", VectorUtils.toJson(x, 1, z));
        mesh.add("rotation", VectorUtils.toJson(0, -135, 90));
        if (!BedrockModelTransform.isIdentity(transformation)) {
            float scale = BedrockModelTransform.uniformScale(transformation);
            // texture_mesh 的纹理平面在 XZ，Y 为厚度；先转入纹理坐标，再叠加原版弓的局部姿态。
            Matrix4f coordinates = new Matrix4f().translation(16, 8, 0).scale(-16, 16, 16).rotateX((float) (Math.PI / 2));
            Matrix4f local = new Matrix4f(coordinates).mul(transformation).mul(new Matrix4f(coordinates).invert());
            Matrix4f nativePose = bowPose(x, z);
            Matrix4f result = nativePose.mul(local);
            Vector3f position = result.transformPosition(new Vector3f(6, 0, 6));
            Vector3f rotation = result.getUnnormalizedRotation(new Quaternionf()).getEulerAnglesZYX(new Vector3f()).div(RADIANS);
            mesh.add("position", VectorUtils.toJson(position));
            mesh.add("rotation", VectorUtils.toJson(rotation));
            mesh.add("scale", VectorUtils.toJson(scale, scale, scale));
        }
        mesh.addProperty("texture", texture);
        JsonArray meshes = new JsonArray();
        meshes.add(mesh);
        JsonObject bone = new JsonObject();
        bone.addProperty("name", "rightitem");
        bone.add("texture_meshes", meshes);
        JsonArray bones = new JsonArray();
        bones.add(bone);
        JsonObject description = new JsonObject();
        description.addProperty("identifier", identifier);
        description.addProperty("texture_width", 16);
        description.addProperty("texture_height", 16);
        JsonObject geometry = new JsonObject();
        geometry.add("description", description);
        geometry.add("bones", bones);
        return geometry;
    }

    public record Stage(JavaModel model, BufferedImage image, String texture, String condition, Matrix4fc transformation) {
    }

    static Matrix4f bowPose(float x, float z) {
        return new Matrix4f().translation(x, 1, z).rotateZYX(90 * RADIANS, -135 * RADIANS, 0).translate(-6, 0, -6);
    }

    static void bowAnimations(Key id, JsonObject animations, JsonObject names, JsonArray animate) {
        String prefix = "animation." + ResourcePackUtils.bedrockResourceIdentifier(id);
        animations.add(prefix + ".bow_wield", bowWieldAnimation());
        names.addProperty("bow_wield", prefix + ".bow_wield");
        animate.add("bow_wield");
        animations.add(prefix + ".bow_pull", bowPullAnimation());
        names.addProperty("bow_pull", prefix + ".bow_pull");
        JsonObject condition = new JsonObject();
        condition.addProperty("bow_pull", "context.is_first_person && variable.ce_using_item");
        animate.add(condition);
    }

    private static JsonObject bone(String name, @Nullable String parent) {
        JsonObject bone = new JsonObject();
        bone.addProperty("name", name);
        if (parent != null) {
            bone.addProperty("parent", parent);
        }
        bone.add("pivot", VectorUtils.toJson(0, 8, 0));
        return bone;
    }

    private static JsonObject animation(View view, JavaModel.Transform source, String bonePrefix) {
        float side = view.leftHand ? -1 : 1;
        JsonObject slot = new JsonObject();
        slot.add("rotation", VectorUtils.toJson(view.pose.rotation));
        slot.add("position", VectorUtils.toJson(view.pose.translation));
        slot.addProperty("scale", view.pose.scale);
        // Java 左手翻转 Y/Z 旋转和 X 平移；嵌套骨骼保留 XYZ 顺序，缩放最后应用。
        JsonObject x = new JsonObject();
        x.add("position", VectorUtils.toJson(-source.translation().x * side, source.translation().y, source.translation().z));
        x.add("rotation", VectorUtils.toJson(-source.rotation().x, 0, 0));
        JsonObject y = new JsonObject();
        y.add("rotation", VectorUtils.toJson(0, -source.rotation().y * side, 0));
        JsonObject z = new JsonObject();
        z.add("rotation", VectorUtils.toJson(0, 0, source.rotation().z * side));
        z.add("scale", VectorUtils.toJson(source.scale()));
        JsonObject bones = new JsonObject();
        bones.add(bonePrefix + SLOT, slot);
        bones.add(bonePrefix + X, x);
        bones.add(bonePrefix + Y, y);
        bones.add(bonePrefix + Z, z);
        JsonObject animation = new JsonObject();
        animation.addProperty("loop", true);
        animation.add("bones", bones);
        return animation;
    }

    private static JsonObject bowWieldAnimation() {
        JsonArray position = new JsonArray();
        position.add("c.is_first_person ? -5.5 : 0.5");
        position.add("c.is_first_person ? -3.0 : -2.5");
        position.add("c.is_first_person ? -3.0 : 1.0");
        JsonArray rotation = new JsonArray();
        rotation.add("c.is_first_person ? 38.0 : 0.0");
        rotation.add("c.is_first_person ? -120.0 : 0.0");
        rotation.add("c.is_first_person ? -63.0 : 0.0");
        return bowAnimation(position, rotation);
    }

    private static JsonObject bowPullAnimation() {
        JsonArray position = new JsonArray();
        position.add(-1.5f);
        position.add("2.5 + (variable.ce_charge_amount >= 1 ? math.sin(query.life_time * 1300) * 0.1 - math.sin(query.life_time * 45) * 0.5 : 0)");
        position.add(-4.8f);
        // wield 与 pull 在同一原版骨骼上叠加，不再经过 Java 挂点补偿。
        return bowAnimation(position, VectorUtils.toJson(-53, 8, 35));
    }

    private static JsonObject bowAnimation(JsonArray position, JsonArray rotation) {
        JsonObject bone = new JsonObject();
        bone.add("position", position);
        bone.add("rotation", rotation);
        JsonObject bones = new JsonObject();
        bones.add("rightitem", bone);
        JsonObject animation = new JsonObject();
        animation.addProperty("loop", true);
        animation.add("bones", bones);
        return animation;
    }

    static Matrix4f firstPersonSlotMatrix() {
        SlotPose pose = SlotPose.firstPerson(false);
        // 原生骨骼坐标反转 Y；slot 的 pivot 相对绑定骨骼低 16 个模型单位。
        return new Matrix4f().translation(pose.translation.x, 16 - pose.translation.y, pose.translation.z)
                .rotateZYX(pose.rotation.z * RADIANS, pose.rotation.y * RADIANS, pose.rotation.x * RADIANS)
                .scale(pose.scale);
    }

    private record SlotPose(Vector3f rotation, Vector3f translation, float scale) {
        private static final SlotPose THIRD_PERSON = new SlotPose(new Vector3f(90, 0, 0), new Vector3f(0, 13, -3), 1);

        private static SlotPose firstPerson(boolean leftHand) {
            float cameraScale = PLAYER_SCALE / 16;
            Matrix4f matrix = new Matrix4f()
                    .translation(0, cameraScale * (24.125f - 24) - 1.62f, 0)
                    .scale(-cameraScale, cameraScale, -cameraScale);
            if (leftHand) {
                matrix.translate(-6, -9, 0);
            } else {
                matrix.translate(5, 22, 0)
                        .translate(-13.5f, -10, 12)
                        .rotateZYX(115 * RADIANS, 45 * RADIANS, -95 * RADIANS)
                        .translate(1, -31, 0);
            }
            matrix.invert().translate(leftHand ? -0.56f : 0.56f, -0.52f, -0.72f).scale(1f / 16);
            Vector3f translation = matrix.getTranslation(new Vector3f());
            translation.set(-translation.x, translation.y - 8, translation.z);
            matrix.scale(PLAYER_SCALE);
            Vector3f rotation = matrix.getEulerAnglesZYX(new Vector3f()).mul(-1 / RADIANS, -1 / RADIANS, 1 / RADIANS);
            return new SlotPose(rotation, translation, 1 / PLAYER_SCALE);
        }
    }

    enum View {
        FIRST_RIGHT(DisplayPosition.FIRSTPERSON_RIGHTHAND, false, SlotPose.firstPerson(false), "firstperson_righthand", "context.is_first_person && context.item_slot == 'main_hand'"),
        FIRST_LEFT(DisplayPosition.FIRSTPERSON_LEFTHAND, true, SlotPose.firstPerson(true), "firstperson_lefthand", "context.is_first_person && context.item_slot == 'off_hand'"),
        THIRD_RIGHT(DisplayPosition.THIRDPERSON_RIGHTHAND, false, SlotPose.THIRD_PERSON, "thirdperson_righthand", "!context.is_first_person && context.item_slot == 'main_hand'"),
        THIRD_LEFT(DisplayPosition.THIRDPERSON_LEFTHAND, true, SlotPose.THIRD_PERSON, "thirdperson_lefthand", "!context.is_first_person && context.item_slot == 'off_hand'"),
        HEAD(DisplayPosition.HEAD, false, new SlotPose(new Vector3f(0, 180, 0), new Vector3f(0, 16, 0), 0.625f), "head", "context.item_slot == 'head'");

        private final DisplayPosition position;
        private final boolean leftHand;
        private final SlotPose pose;
        private final String animation;
        final String condition;

        View(DisplayPosition position, boolean leftHand, SlotPose pose, String animation, String condition) {
            this.position = position;
            this.leftHand = leftHand;
            this.pose = pose;
            this.animation = animation;
            this.condition = condition;
        }
    }
}
