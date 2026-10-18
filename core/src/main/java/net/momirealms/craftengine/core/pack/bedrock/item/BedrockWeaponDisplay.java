package net.momirealms.craftengine.core.pack.bedrock.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.bedrock.model.BedrockGeometry;
import net.momirealms.craftengine.core.pack.bedrock.texture.BedrockAnimatedTextures;
import net.momirealms.craftengine.core.util.VectorUtils;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

final class BedrockWeaponDisplay {
    static final String ACTION = "ce_weapon_action";
    static final String POSE = "ce_weapon_pose";
    private static final String[] BONES = {ACTION, POSE, BedrockItemDisplay.SLOT_BONE, "ce_base_scale", "ce_base_z", "ce_base_y", "ce_base_x", "ce_base_translation"};
    private static final float RADIANS = (float) (Math.PI / 180);
    private static final Vector3f PIVOT = new Vector3f(0, 8, 0);
    static final BedrockWeaponDisplay CROSSBOW = new BedrockWeaponDisplay(false);
    static final BedrockWeaponDisplay SPEAR = new BedrockWeaponDisplay(true);
    private final Map<BedrockItemDisplay.View, List<JsonObject>> animations = new EnumMap<>(BedrockItemDisplay.View.class);
    private final Map<BedrockItemDisplay.View, Matrix4f> bases = new EnumMap<>(BedrockItemDisplay.View.class);

    private BedrockWeaponDisplay(boolean spear) {
        // TextureMesh's Bedrock import flips position X/Y and rotation X/Y. The generated Java mesh is centered at (8,8,8).
        Matrix4f mesh = spear
                ? new Matrix4f().rotateZYX(90 * RADIANS, 45 * RADIANS, 0).translate(0, 0.5f, 0).scale(2, 1, 2)
                : new Matrix4f().translation(0, 5, -3).rotateZYX(-49 * RADIANS, 0, -90 * RADIANS);
        mesh.translate(0, 7.5f, 16).rotateX(-90 * RADIANS).rotateY(-180 * RADIANS).translate(8, 8, 8);
        Vector3f meshScale = mesh.getScale(new Vector3f());
        JsonObject meshAnimation = transform(mesh.getTranslation(new Vector3f()),
                new Matrix4f(mesh).scale(1 / meshScale.x(), 1 / meshScale.y(), 1 / meshScale.z()).getEulerAnglesZYX(new Vector3f()).div(RADIANS), meshScale);
        for (BedrockItemDisplay.View view : BedrockItemDisplay.VIEWS) {
            boolean first = view == BedrockItemDisplay.View.FIRST_PERSON_RIGHT || view == BedrockItemDisplay.View.FIRST_PERSON_LEFT;
            boolean left = view == BedrockItemDisplay.View.FIRST_PERSON_LEFT || view == BedrockItemDisplay.View.THIRD_PERSON_LEFT;
            Vector3f rotation;
            Vector3f translation;
            Vector3f scale;
            if (spear) {
                rotation = new Vector3f(first ? -20 : 5, first ? 90 : 270, first ? -35 : -40);
                translation = first ? new Vector3f(3.13f, 2, 0.13f) : new Vector3f(0, 2, 2);
                scale = first ? new Vector3f(1.36f, 1.36f, 0.68f) : new Vector3f(1.7f, 1.7f, 0.85f);
            } else {
                rotation = new Vector3f(-90, 0, first ? (left ? -35 : -55) : (left ? -30 : -60));
                translation = first ? new Vector3f(1.13f, 3.2f, 1.13f) : new Vector3f(2, 0.1f, -3);
                scale = new Vector3f(first ? 0.68f : 0.9f);
            }
            if (left) {
                translation.x = -translation.x;
            }
            Matrix4f baseline = new Matrix4f().translation(translation).rotateXYZ(rotation.x * RADIANS, rotation.y * RADIANS, rotation.z * RADIANS).scale(scale);
            this.bases.put(view, new Matrix4f(mesh).mul(baseline.invert()));
            List<JsonObject> bones = new ArrayList<>(BONES.length);
            // Crossbow's native bound pivot is zero; compensate our Y=8 pivot without repeating the player's use animation.
            bones.add(position(0, spear ? 16 : -8, 0));
            bones.add(spear ? spearPose(first) : crossbowPose(first));
            bones.add(meshAnimation);
            // Keep inverse scale before inverse rotation. Collapsing this to one TRS would lose shear for the spear's nonuniform scale.
            JsonObject inverseScale = new JsonObject();
            inverseScale.add("scale", VectorUtils.toJson(new Vector3f(1 / scale.x, 1 / scale.y, 1 / scale.z)));
            bones.add(inverseScale);
            bones.add(rotation(0, 0, -rotation.z));
            bones.add(rotation(0, -rotation.y, 0));
            bones.add(rotation(-rotation.x, 0, 0));
            bones.add(position(translation.x, -translation.y, -translation.z));
            this.animations.put(view, List.copyOf(bones));
        }
    }

    List<BedrockGeometry.Bone> rig(BedrockGeometry geometry, String prefix) {
        List<BedrockGeometry.Bone> bones = new ArrayList<>(BONES.length + 3);
        String parent = null;
        for (String name : BONES) {
            String bone = prefix + name;
            bones.add(new BedrockGeometry.Bone(bone, parent, PIVOT, parent == null ? "query.item_slot_to_bone_name(context.item_slot)" : null, List.of()));
            parent = bone;
        }
        bones.add(new BedrockGeometry.Bone(prefix + BedrockItemDisplay.X_BONE, parent, PIVOT, null, List.of()));
        bones.add(new BedrockGeometry.Bone(prefix + BedrockItemDisplay.Y_BONE, prefix + BedrockItemDisplay.X_BONE, PIVOT, null, List.of()));
        bones.add(new BedrockGeometry.Bone(prefix + BedrockItemDisplay.Z_BONE, prefix + BedrockItemDisplay.Y_BONE, PIVOT, null, geometry.cubes()));
        return bones;
    }

    void animate(JsonObject bones, BedrockItemDisplay.View view, String prefix, List<BedrockAnimatedTextures.Track> tracks) {
        List<JsonObject> values = this.animations.get(view);
        for (int index = 0; index < BONES.length; index++) {
            BedrockItemWriter.addBoneAnimation(bones, prefix, BONES[index], values.get(index), tracks);
        }
    }

    void modelMatrix(BedrockItemDisplay.View view, BedrockItemDisplay.Transform display, Matrix4f target) {
        target.set(this.bases.get(view));
        BedrockItemDisplay.applyDisplay(target, display);
    }

    BedrockGeometry.Bounds bounds(BedrockGeometry geometry, Map<BedrockItemDisplay.View, BedrockItemDisplay.Transform> transforms) {
        BedrockGeometry.Bounds original = geometry.bounds();
        float radius = original.width() * 8;
        float bottom = (original.offset().y() - original.height() / 2) * 16;
        float top = (original.offset().y() + original.height() / 2) * 16;
        float reach = 0;
        Matrix4f matrix = new Matrix4f();
        Vector3f corner = new Vector3f();
        for (BedrockItemDisplay.View view : BedrockItemDisplay.VIEWS) {
            this.modelMatrix(view, transforms.get(view), matrix);
            for (int index = 0; index < 8; index++) {
                corner.set((index & 1) == 0 ? -radius : radius, (index & 2) == 0 ? bottom : top, (index & 4) == 0 ? -radius : radius);
                reach = Math.max(reach, matrix.transformPosition(corner).length());
            }
        }
        // Enclose every rotation plus spear reach/hit recoil and crossbow's animated translation/scale.
        float extent = (float) Math.ceil((reach * 1.15f + 40) / 16);
        return new BedrockGeometry.Bounds(extent * 2, extent * 2, new Vector3f(0, 24f / 16, 0));
    }

    private static JsonObject crossbowPose(boolean first) {
        JsonObject pose = position(first ? 13 : 0.5f, first ? 7 : -0.5f, first ? 2 : -1.5f);
        pose.add("rotation", VectorUtils.toJson(first ? new Vector3f(-9, 60, -47) : new Vector3f(0, -3, 3.25f)));
        if (first) {
            pose.add("position", expressions("13", "7 + (variable.ce_crossbow_using && variable.ce_crossbow_pull >= 1 ? math.sin(query.life_time * 1300) * 0.1 - math.sin(query.life_time * 45) * 0.25 : 0)", "2"));
        }
        return pose;
    }

    private static JsonObject spearPose(boolean first) {
        JsonObject pose = position(0, 0, -27);
        pose.add("rotation", expressions("0", "0", first
                ? "-20 + context.owning_entity->variable.fp_melee_spear_use_attachable_rotation_z + context.owning_entity->variable.fp_melee_spear_attack_attachable_rotation_z"
                : "-context.owning_entity->variable.tp_melee_spear_use_attachable_rotation_z"));
        if (!first) {
            pose.add("position", expressions("0", "0", "-27 + context.owning_entity->variable.tp_melee_spear_attack_attachable_position_z"));
        }
        return pose;
    }

    private static JsonObject transform(Vector3f translation, Vector3f rotation, Vector3f scale) {
        JsonObject result = position(-translation.x, translation.y, translation.z);
        result.add("rotation", VectorUtils.toJson(new Vector3f(-rotation.x, -rotation.y, rotation.z)));
        result.add("scale", VectorUtils.toJson(scale));
        return result;
    }

    private static JsonObject position(float x, float y, float z) {
        JsonObject result = new JsonObject();
        result.add("position", VectorUtils.toJson(new Vector3f(x, y, z)));
        return result;
    }

    private static JsonObject rotation(float x, float y, float z) {
        JsonObject result = new JsonObject();
        result.add("rotation", VectorUtils.toJson(new Vector3f(-x, -y, z)));
        return result;
    }

    static JsonArray expressions(String x, String y, String z) {
        JsonArray array = new JsonArray();
        array.add(x);
        array.add(y);
        array.add(z);
        return array;
    }
}
