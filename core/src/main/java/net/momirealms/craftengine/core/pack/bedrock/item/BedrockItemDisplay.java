package net.momirealms.craftengine.core.pack.bedrock.item;

import net.momirealms.craftengine.core.pack.bedrock.model.BedrockGeometry;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModel;
import net.momirealms.craftengine.core.pack.model.generation.display.DisplayPosition;
import net.momirealms.craftengine.core.util.VectorUtils;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class BedrockItemDisplay {
    static final String SLOT_BONE = "ce_slot";
    static final String X_BONE = "ce_display_x";
    static final String Y_BONE = "ce_display_y";
    static final String Z_BONE = "ce_display_z";
    private static final float MODEL_CENTER_Y = 8;
    private static final Vector3f PIVOT = new Vector3f(0, MODEL_CENTER_Y, 0);
    private static final String BINDING = "context.item_slot == 'head' ? 'head' : query.item_slot_to_bone_name(context.item_slot)";
    private static final float DEGREES_TO_RADIANS = (float) (Math.PI / 180);
    static final View[] VIEWS = View.values();

    private BedrockItemDisplay() {
    }

    public static Map<View, Transform> convert(JavaModel model) {
        Map<View, Transform> transforms = new EnumMap<>(View.class);
        for (View view : VIEWS) {
            JavaModel.Transform source = model.display().getOrDefault(view.position, JavaModel.Transform.identity());
            Vector3f rotation = source.rotation();
            Vector3f translation = source.translation();
            // Blockbench display_mode.js mirrors left-hand Y/Z angles and X translation; keyframe.js exports the signs.
            float side = view.leftHand ? -1 : 1;
            transforms.put(view, new Transform(
                    new Vector3f(-rotation.x(), -rotation.y() * side, rotation.z() * side),
                    new Vector3f(-translation.x() * side, translation.y(), translation.z()),
                    source.scale()
            ));
        }
        return transforms;
    }

    static List<BedrockGeometry.Bone> rig(BedrockGeometry geometry) {
        // Nested X -> Y -> Z bones retain Java's XYZ rotation order; scale belongs on the final bone.
        return List.of(
                new BedrockGeometry.Bone(SLOT_BONE, null, PIVOT, BINDING, List.of()),
                new BedrockGeometry.Bone(X_BONE, SLOT_BONE, PIVOT, null, List.of()),
                new BedrockGeometry.Bone(Y_BONE, X_BONE, PIVOT, null, List.of()),
                new BedrockGeometry.Bone(Z_BONE, Y_BONE, PIVOT, null, geometry.cubes())
        );
    }

    static BedrockGeometry.Bounds bounds(BedrockGeometry geometry, Map<View, Transform> transforms) {
        if (geometry.cubes().isEmpty()) {
            return geometry.bounds();
        }
        BedrockGeometry.Bounds original = geometry.bounds();
        float radius = original.width() * 8;
        float bottom = (original.offset().y() - original.height() / 2) * 16;
        float top = (original.offset().y() + original.height() / 2) * 16;
        float minY = bottom;
        float maxY = top;
        float maxRadius = radius;
        Vector3f corner = new Vector3f();
        Matrix4f matrix = new Matrix4f();
        for (View view : VIEWS) {
            previewMatrix(view.pose, transforms.get(view), matrix);
            for (int index = 0; index < 8; index++) {
                corner.set((index & 1) == 0 ? -radius : radius, (index & 2) == 0 ? bottom : top, (index & 4) == 0 ? -radius : radius);
                matrix.transformPosition(corner);
                maxRadius = Math.max(maxRadius, Math.max(Math.abs(corner.x()), Math.abs(corner.z())));
                minY = Math.min(minY, corner.y());
                maxY = Math.max(maxY, corner.y());
            }
        }
        bottom = (float) Math.floor(minY / 16);
        top = (float) Math.ceil(maxY / 16);
        return new BedrockGeometry.Bounds((float) Math.ceil(maxRadius / 8), top - bottom, new Vector3f(0, (bottom + top) / 2, 0));
    }

    static void previewMatrix(SlotPose pose, Transform transform, Matrix4f target) {
        Vector3f slotRotation = pose.rotation();
        Vector3f slotPosition = pose.position();
        Vector3f rotation = transform.rotation();
        Vector3f translation = transform.translation();
        // Undo Bedrock codec signs, then compose in Blockbench preview coordinates around the shared model center.
        target.translation(-slotPosition.x(), PIVOT.y() + slotPosition.y(), slotPosition.z())
                .rotateZYX(slotRotation.z() * DEGREES_TO_RADIANS, -slotRotation.y() * DEGREES_TO_RADIANS, -slotRotation.x() * DEGREES_TO_RADIANS)
                .scale(pose.scale())
                .translate(-translation.x(), translation.y(), translation.z())
                .rotateXYZ(-rotation.x() * DEGREES_TO_RADIANS, -rotation.y() * DEGREES_TO_RADIANS, rotation.z() * DEGREES_TO_RADIANS)
                .scale(transform.scale())
                .translate(0, -PIVOT.y(), 0);
    }

    public record Transform(Vector3f rotation, Vector3f translation, Vector3f scale) {
    }

    record SlotPose(Vector3f rotation, Vector3f position, Vector3f scale) {
        private static final float BEDROCK_PLAYER_SCALE = 0.9375f;
        private static final float BEDROCK_EYE_HEIGHT = 1.62001f;
        private static final float BEDROCK_CAMERA_Y_SHIFT = 24.125f;
        private static final float JAVA_HAND_X = 0.56f;
        private static final float JAVA_HAND_Y = -0.52f;
        private static final float JAVA_HAND_Z = -0.72f;
        // Third-person offsets account for the bound item's -24 Y origin and Java's hand/head attachment points.
        private static final SlotPose HAND = new SlotPose(new Vector3f(90, 0, 0), new Vector3f(0, 13, -3), VectorUtils.ONE);
        private static final SlotPose HEAD = new SlotPose(VectorUtils.ZERO, new Vector3f(0, 20, 0), new Vector3f(0.625f));
        private static final SlotPose FIRST_RIGHT = firstPersonPose(false);
        private static final SlotPose FIRST_LEFT = firstPersonPose(true);

        private static SlotPose firstPersonPose(boolean leftHand) {
            // Standard idle rig with matching 70-degree hand projections; verified with preview pack 1.0.4.
            // Camera origin follows Dofes/blockbench-bedrock-attachable (7141126); scale follows vanilla player.entity.json.
            float cameraScale = BEDROCK_PLAYER_SCALE / 16;
            Matrix4f slot = new Matrix4f()
                    .translation(0, cameraScale * (BEDROCK_CAMERA_Y_SHIFT - 24) - BEDROCK_EYE_HEIGHT, 0)
                    .scale(-cameraScale, cameraScale, -cameraScale);
            if (leftHand) {
                slot.translate(-6, -9, 0);
            } else {
                // Vanilla empty_hand animates only the right arm; expression binding adds the -24 Y offset.
                slot.translate(5, 22, 0).translate(-13.5f, -10, 12)
                        .rotateZYX(115 * DEGREES_TO_RADIANS, 45 * DEGREES_TO_RADIANS, -95 * DEGREES_TO_RADIANS)
                        .translate(1, -31, 0);
            }
            // Slot = inverse(camera * binding) * Java hand base. Source display is applied separately.
            slot.invert().translate(leftHand ? -JAVA_HAND_X : JAVA_HAND_X, JAVA_HAND_Y, JAVA_HAND_Z).scale(1f / 16);
            Vector3f offset = slot.getTranslation(new Vector3f());
            offset.set(-offset.x(), offset.y() - MODEL_CENTER_Y, offset.z());
            // Remove the uniform 16/15 scale before extracting the Bedrock ZYX Euler angles.
            slot.scale(BEDROCK_PLAYER_SCALE);
            Vector3f angles = slot.getEulerAnglesZYX(new Vector3f()).mul(-1 / DEGREES_TO_RADIANS, -1 / DEGREES_TO_RADIANS, 1 / DEGREES_TO_RADIANS);
            return new SlotPose(angles, offset, new Vector3f(1 / BEDROCK_PLAYER_SCALE));
        }
    }

    public enum View {
        THIRD_PERSON_RIGHT(DisplayPosition.THIRDPERSON_RIGHTHAND, false, SlotPose.HAND, "thirdperson_righthand", "!context.is_first_person && context.item_slot == 'main_hand'"),
        THIRD_PERSON_LEFT(DisplayPosition.THIRDPERSON_LEFTHAND, true, SlotPose.HAND, "thirdperson_lefthand", "!context.is_first_person && context.item_slot == 'off_hand'"),
        FIRST_PERSON_RIGHT(DisplayPosition.FIRSTPERSON_RIGHTHAND, false, SlotPose.FIRST_RIGHT, "firstperson_righthand", "context.is_first_person && context.item_slot == 'main_hand'"),
        FIRST_PERSON_LEFT(DisplayPosition.FIRSTPERSON_LEFTHAND, true, SlotPose.FIRST_LEFT, "firstperson_lefthand", "context.is_first_person && context.item_slot == 'off_hand'"),
        HEAD(DisplayPosition.HEAD, false, SlotPose.HEAD, "head", "!context.is_first_person && context.item_slot == 'head'");

        private final DisplayPosition position;
        private final boolean leftHand;
        final SlotPose pose;
        final String animation;
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
