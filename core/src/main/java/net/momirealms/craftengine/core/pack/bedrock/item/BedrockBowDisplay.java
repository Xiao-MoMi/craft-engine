package net.momirealms.craftengine.core.pack.bedrock.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.util.VectorUtils;
import org.joml.Matrix4f;
import org.joml.Vector3f;

final class BedrockBowDisplay {
    private static final float RADIANS = (float) (Math.PI / 180);
    private static final float JAVA_BOW_SCALE = 0.68f;
    private static final Matrix4f MODEL_BASIS = modelBasis();
    private static final Matrix4f IDLE_BASE = base(false);
    private static final Matrix4f PULL_BASE = base(true);
    private static final JsonObject POSE = poseAnimation();
    private static final JsonObject MODEL = modelAnimation();

    private BedrockBowDisplay() {
    }

    static JsonObject pose() {
        return POSE;
    }

    static JsonObject model() {
        return MODEL;
    }

    static void previewMatrix(BedrockItemDisplay.Transform display, boolean pulling, Matrix4f target) {
        target.set(pulling ? PULL_BASE : IDLE_BASE);
        BedrockItemDisplay.applyDisplay(target, display);
    }

    private static Matrix4f modelBasis() {
        // Blockbench imports bow.geo.json's texture mesh as origin (-2,-1,-2), rotation (0,135,90), pivot (6,0,-6).
        // Its Java generated mesh uses rotation (90,180,0) and pivot (0,-7.5,-16). Map centered Java geometry back through it.
        Matrix4f mesh = new Matrix4f().translation(-2, -1, -2)
                .rotateZYX(90 * RADIANS, 135 * RADIANS, 0)
                .translate(6, 0, -6).translate(0, 7.5f, 16)
                .rotateX(-90 * RADIANS).rotateY(-180 * RADIANS)
                .translate(8, 8, 8);
        // Retain the source model's display below this bone; remove only the unmodified Java bow's display baseline.
        Matrix4f javaDisplay = new Matrix4f().translation(1.13f, 3.2f, 1.13f)
                .rotateXYZ(0, -90 * RADIANS, 25 * RADIANS).scale(JAVA_BOW_SCALE);
        return mesh.mul(javaDisplay.invert());
    }

    private static Matrix4f base(boolean pulling) {
        Matrix4f result = new Matrix4f().translation(0, 24, 0);
        if (pulling) {
            result.translate(7, -0.5f, -7.8f).rotateZYX(-28 * RADIANS, 112 * RADIANS, 15 * RADIANS)
                    .translate(-0.01f, 0, 1);
        } else {
            result.translate(5.5f, -3, -3).rotateZYX(-63 * RADIANS, 120 * RADIANS, -38 * RADIANS);
        }
        return result.mul(MODEL_BASIS);
    }

    private static JsonObject poseAnimation() {
        JsonObject pose = new JsonObject();
        // Wield and pull are additive channels on vanilla rightitem. The expression-bound root needs +24 Y minus its pivot Y=8.
        JsonArray position = new JsonArray();
        position.add("variable.ce_bow_using ? -7 : -5.5");
        position.add("variable.ce_bow_using ? 15.5 + (variable.ce_bow_ticks >= 10 ? math.sin(query.life_time * 1300) * 0.1 - math.sin(query.life_time * 45) * 0.5 : 0) : 13");
        position.add("variable.ce_bow_using ? -7.8 : -3");
        pose.add("position", position);
        JsonArray rotation = new JsonArray();
        rotation.add("variable.ce_bow_using ? -15 : 38");
        rotation.add("variable.ce_bow_using ? -112 : -120");
        rotation.add("variable.ce_bow_using ? -28 : -63");
        pose.add("rotation", rotation);
        return pose;
    }

    private static JsonObject modelAnimation() {
        Vector3f position = MODEL_BASIS.getTranslation(new Vector3f());
        Vector3f rotation = new Matrix4f(MODEL_BASIS).scale(JAVA_BOW_SCALE).getEulerAnglesZYX(new Vector3f())
                .mul(-1 / RADIANS, -1 / RADIANS, 1 / RADIANS);
        JsonObject model = new JsonObject();
        JsonArray translation = new JsonArray();
        translation.add(-position.x() + " + (variable.ce_bow_using && variable.ce_bow_late_stage ? 0.01 : 0)");
        translation.add(position.y());
        translation.add(position.z() + " + (variable.ce_bow_using ? 1 : 0)");
        model.add("position", translation);
        model.add("rotation", VectorUtils.toJson(rotation));
        model.addProperty("scale", 1 / JAVA_BOW_SCALE);
        return model;
    }
}
