package net.momirealms.craftengine.core.pack.bedrock.item.display;

import net.momirealms.craftengine.bedrock.BedrockHeldItemType;
import net.momirealms.craftengine.core.pack.bedrock.model.BedrockModelTransform;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModel;
import org.joml.Matrix3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

final class BedrockHeldItemTransforms {
    private static final float RADIANS = (float) (Math.PI / 180);

    private BedrockHeldItemTransforms() {
    }

    static JavaModel.Transform correction(BedrockHeldItemType type) {
        if (type == BedrockHeldItemType.NATIVE) {
            return new JavaModel.Transform(new Vector3f(90, 0, 0), new Vector3f(0, -8, 0), new Vector3f(2));
        }
        Matrix4f attachment = attachment(type);
        if (attachment != null) {
            Matrix4f correction = attachmentTarget(type).mul(attachment.invert());
            Vector3f rotation = BedrockModelTransform.rotationAngles(new Matrix3d(correction));
            return new JavaModel.Transform(rotation, correction.getTranslation(new Vector3f()), correction.getScale(new Vector3f()));
        }
        Matrix4f correction = new Matrix4f();
        if (type != BedrockHeldItemType.BLOCK) {
            // 图标网格的 XZ 对应贴图 UV；CE 几何的 U 增大时 X 减小，V 增大时 Y 减小。
            correction.rotateY((float) Math.PI).translate(-0.5f, 0.5f, -0.03125f)
                    .rotateX((float) Math.PI / 2).scale(1f / 16);
        }
        correction.mul(nativeTransform(type).invert()).rotateX((float) Math.PI);
        Vector3f translation = correction.getTranslation(new Vector3f()).mul(16, 16, -16);
        Vector3f scale = correction.getScale(new Vector3f());
        Quaternionf rotation = correction.getUnnormalizedRotation(new Quaternionf());
        rotation.y = -rotation.y;
        rotation.z = -rotation.z;
        Vector3f angles = rotation.getEulerAnglesZYX(new Vector3f()).mul((float) (180 / Math.PI));
        return new JavaModel.Transform(angles, translation, scale);
    }

    private static Matrix4f attachmentTarget(BedrockHeldItemType type) {
        if (type == BedrockHeldItemType.SHIELD || type == BedrockHeldItemType.TRIDENT) {
            // 原生几何已经朝上；先还原 Java 特殊模型的局部坐标，定义中的翻转稍后随 display 一起应用。
            Matrix4f target = new Matrix4f().translation(0, -16, -16).rotateX((float) Math.PI)
                    .translate(8, -8, -8).rotateY((float) Math.PI);
            return type == BedrockHeldItemType.SHIELD ? target.translate(-1, -28, -1) : target.translate(0, -24, 0);
        }
        Matrix4f target = new Matrix4f().rotationY((float) Math.PI).translate(-8, 8, -0.5f).rotateX((float) Math.PI / 2);
        return type == BedrockHeldItemType.SPEAR ? target.scale(0.5f) : target;
    }

    private static Matrix4f attachment(BedrockHeldItemType type) {
        return switch (type) {
            // binding 以原生手部的 (0,24,0) 为基准，不能直接减去模型自身的 pivot。
            case SHIELD -> new Matrix4f().translation(0.6f, 0.5f, 12.3f)
                    .rotateZYX(-90 * RADIANS, 0, 90 * RADIANS).scale(1, -1, -1).translate(-1, -15.5f, -3);
            case TRIDENT -> new Matrix4f().translation(1.5f, -2.5f, -10.5f)
                    .rotateZYX(49 * RADIANS, -1.5f * RADIANS, -97 * RADIANS).translate(0, -24, 0);
            case BOW -> new Matrix4f().translation(2.5f, -1.5f, -1)
                    .rotateZYX(-90 * RADIANS, -135 * RADIANS, 0).translate(-6, 0, -6);
            case SPEAR -> new Matrix4f().translation(0, 0, -27)
                    .rotateZYX(-90 * RADIANS, -45 * RADIANS, 0).translate(0, -0.5f, 0);
            default -> null;
        };
    }

    static Matrix4f nativeTransform(BedrockHeldItemType type) {
        if (type == BedrockHeldItemType.BLOCK) {
            return new Matrix4f().translation(0, 0.1875f, -0.3125f)
                    .rotateX((float) Math.toRadians(200)).rotateY((float) Math.toRadians(45))
                    .scale(0.375f).rotateY((float) Math.PI);
        }
        Matrix4f matrix = new Matrix4f();
        switch (type) {
            case SPRITE -> matrix.translate(0.3125f, 0.1875f, -0.1875f).scale(0.375f)
                    .rotateZ((float) Math.toRadians(60)).rotateX((float) Math.toRadians(-90)).rotateZ((float) Math.toRadians(20));
            case HANDHELD -> matrix.rotateY((float) Math.PI).translate(0.1f, 0.265f, 0).scale(0.625f)
                    .rotateX((float) Math.toRadians(80)).rotateY((float) Math.toRadians(45));
            case FISHING_ROD -> matrix.rotateX((float) Math.toRadians(165)).translate(0.1f, 0.065f, -0.0625f).scale(0.625f)
                    .rotateX((float) Math.toRadians(80)).rotateY((float) Math.toRadians(45));
            default -> throw new IllegalArgumentException(type.name());
        }
        // 手部挂点之后，原版图标渲染器还会应用自身的持握姿态和像素坐标变换。
        return matrix.scale(1.5f).rotateY((float) Math.toRadians(50)).rotateZ((float) Math.toRadians(-25))
                .translate(0.075f, -0.245f, -0.1f).scale(1f / 16)
                .rotateZ((float) Math.PI / 2).rotateX((float) -Math.PI / 2).rotateY((float) -Math.PI / 2)
                .translate(0, 0, -16);
    }
}
