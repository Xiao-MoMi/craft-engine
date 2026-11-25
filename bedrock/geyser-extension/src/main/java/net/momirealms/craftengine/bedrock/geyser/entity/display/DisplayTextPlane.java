package net.momirealms.craftengine.bedrock.geyser.entity.display;

import org.cloudburstmc.math.GenericMath;
import org.cloudburstmc.math.imaginary.Quaternionf;
import org.cloudburstmc.math.vector.Vector3f;

import java.util.Arrays;

public final class DisplayTextPlane {
    static final float PIXEL = 0.025f;
    // DebugText 原生字形的横向像素间距，由连续位图和独立位图对照测得。
    private static final float DEBUG_TEXT_PIXEL = PIXEL * (16f / 15f);
    static final float LINE_CENTER = 4.5f;
    private static final int LINE_HEIGHT = 10;
    // 让基岩背景的逐行高度与 Java 的 10 像素行距对齐
    private static final float BACKGROUND_LINE_GAP = -0.625f;
    private static final float BACKGROUND_LINE_OFFSET = LINE_HEIGHT * 0.5f * PIXEL;
    private static final float BACKGROUND_DEPTH = 0.003f;
    private final float backgroundLineGap;
    private final BedrockBitmapFont bitmapFont;
    private DisplayPose pose;
    private Vector3f base;
    private float yaw;
    private float pitch;
    private Quaternionf orientation;
    private Vector3f anchor;
    private Vector3f right;
    private Vector3f up;
    private Vector3f normal;
    private Vector3f rotation;
    private float scale;
    private boolean drawable;
    private DisplayTextLayout layout;
    private float alignment;
    private Vector3f background;
    private Vector3f shadowAnchor;
    private Vector3f[] lines;
    private Vector3f[] shadows;
    private float[] shadowOffsets;

    DisplayTextPlane(boolean supportsLineGap, BedrockBitmapFont bitmapFont) {
        this.backgroundLineGap = supportsLineGap ? BACKGROUND_LINE_GAP : 0;
        this.bitmapFont = bitmapFont;
    }

    boolean prepare(DisplayPose pose, Vector3f base, float yaw, float pitch, DisplayTextLayout layout, float alignment) {
        boolean orientationChanged = this.orientation == null || this.yaw != yaw || this.pitch != pitch;
        boolean planeChanged = orientationChanged || this.pose != pose;
        boolean anchorChanged = planeChanged || !base.equals(this.base);
        boolean layoutChanged = this.layout != layout || this.alignment != alignment;
        if (!anchorChanged && !layoutChanged) {
            return this.drawable;
        }

        if (orientationChanged) {
            this.yaw = yaw;
            this.pitch = pitch;
            this.orientation = Quaternionf.fromAngleDegAxis(-yaw, Vector3f.UNIT_Y).mul(Quaternionf.fromAngleDegAxis(pitch, Vector3f.UNIT_X));
        }
        if (planeChanged) {
            this.pose = pose;
            this.drawable = this.updateAxes();
        }
        if (anchorChanged) {
            this.base = base;
            this.anchor = Vector3f.ZERO.equals(pose.translation()) ? base : base.add(this.orientation.rotate(pose.translation()));
            this.shadowAnchor = null;
        }
        if (this.layout != layout) {
            this.shadowOffsets = null;
        }
        this.layout = layout;
        this.alignment = alignment;
        this.background = null;
        if (this.lines != null) {
            Arrays.fill(this.lines, null);
        }
        if (this.shadows != null) {
            Arrays.fill(this.shadows, null);
        }
        return this.drawable;
    }

    float scale() {
        return this.scale;
    }

    Vector3f rotation() {
        return this.rotation;
    }

    float backgroundLineGap() {
        return this.backgroundLineGap;
    }

    Vector3f background() {
        if (this.background == null) {
            // 基岩沿世界 Y 轴补偿多行文本的居中位置，每多一行就从首行锚点抵消半行距离
            float lineY = this.lineY(0) * PIXEL;
            float offset = (this.layout.lines().size() - 1) * BACKGROUND_LINE_OFFSET;
            this.background = this.anchor.add(
                    this.right.getX() * PIXEL + this.up.getX() * lineY - this.normal.getX() * BACKGROUND_DEPTH,
                    this.right.getY() * PIXEL + this.up.getY() * lineY - offset - this.normal.getY() * BACKGROUND_DEPTH,
                    this.right.getZ() * PIXEL + this.up.getZ() * lineY - this.normal.getZ() * BACKGROUND_DEPTH
            );
        }
        return this.background;
    }

    Vector3f line(int index) {
        int lineCount = this.layout.lines().size();
        if (this.lines == null || this.lines.length < lineCount) {
            this.lines = new Vector3f[lineCount];
        }
        Vector3f position = this.lines[index];
        if (position == null) {
            position = this.position(this.anchor, this.lineX(index) + 1, this.lineY(index));
            this.lines[index] = position;
        }
        return position;
    }

    Vector3f shadow(int index) {
        int lineCount = this.layout.lines().size();
        if (this.shadows == null || this.shadows.length < lineCount) {
            this.shadows = new Vector3f[lineCount];
        }
        Vector3f position = this.shadows[index];
        if (position == null) {
            if (this.shadowAnchor == null) {
                this.shadowAnchor = this.anchor.sub(this.normal.mul(0.001f));
            }
            if (this.shadowOffsets == null) {
                this.shadowOffsets = new float[lineCount];
                for (int i = 0; i < lineCount; i++) {
                    this.shadowOffsets[i] = this.bitmapFont.shadowOffset(this.layout.lines().get(i).text());
                }
            }
            float offset = this.shadowOffsets[index];
            position = this.position(this.shadowAnchor, this.lineX(index) + 1 + offset, this.lineY(index) - offset);
            this.shadows[index] = position;
        }
        return position;
    }

    Vector3f piecePosition(Vector3f line, float center) {
        float x = center * DEBUG_TEXT_PIXEL;
        return line.add(this.right.getX() * x, this.right.getY() * x, this.right.getZ() * x);
    }

    private float lineX(int index) {
        return (this.layout.width() - this.layout.lines().get(index).width()) * this.alignment;
    }

    private float lineY(int index) {
        return (this.layout.lines().size() - 1 - index) * LINE_HEIGHT + LINE_CENTER;
    }

    private boolean updateAxes() {
        float quaternionScale = this.pose.left().lengthSquared() * this.pose.right().lengthSquared();
        if (quaternionScale < 1e-8f) {
            return false;
        }
        this.right = this.axis(Vector3f.UNIT_X, quaternionScale);
        this.up = this.axis(Vector3f.UNIT_Y, quaternionScale);
        float rightLength = this.right.length();
        float upLength = this.up.length();
        if (rightLength < 0.0001f || upLength < 0.0001f) {
            return false;
        }
        Vector3f normal = this.right.cross(this.up);
        float normalLength = normal.length();
        if (normalLength < 1e-8f) {
            return false;
        }
        this.normal = normal.div(normalLength);
        Vector3f unitRight = this.right.div(rightLength);
        this.rotation = textRotation(unitRight, this.normal.cross(unitRight), this.normal);
        this.scale = (float) Math.sqrt(rightLength * upLength);
        return true;
    }

    private Vector3f axis(Vector3f axis, float quaternionScale) {
        // Java 按右旋转、逐轴缩放、左旋转的顺序计算文本平面
        Vector3f local = Quaternionf.IDENTITY.equals(this.pose.right()) ? axis : this.pose.right().rotate(axis);
        local = Vector3f.ONE.equals(this.pose.scale()) ? local : local.mul(this.pose.scale());
        local = Quaternionf.IDENTITY.equals(this.pose.left()) ? local : this.pose.left().rotate(local);
        Vector3f world = this.orientation.rotate(local);
        return quaternionScale == 1 ? world : world.mul(quaternionScale);
    }

    private Vector3f position(Vector3f origin, float x, float y) {
        x *= PIXEL;
        y *= PIXEL;
        return origin.add(
                this.right.getX() * x + this.up.getX() * y,
                this.right.getY() * x + this.up.getY() * y,
                this.right.getZ() * x + this.up.getZ() * y
        );
    }

    private static Vector3f textRotation(Vector3f right, Vector3f up, Vector3f normal) {
        double sinYaw = Math.clamp(right.getZ(), -1, 1);
        double pitch;
        double yaw;
        double roll;
        // DebugText 的正面沿 -Z，且采用 XYZ 欧拉旋转
        if (Math.abs(sinYaw) < 0.9999) {
            pitch = Math.toDegrees(Math.atan2(up.getZ(), -normal.getZ()));
            yaw = Math.toDegrees(Math.asin(sinYaw));
            roll = Math.toDegrees(Math.atan2(-right.getY(), -right.getX()));
        } else {
            pitch = Math.toDegrees(Math.atan2(normal.getY(), up.getY()));
            yaw = Math.copySign(90, sinYaw);
            roll = 0;
        }
        if (Math.abs(pitch) > 90) {
            pitch = GenericMath.wrapAngleDeg((float) (pitch + 180));
            yaw = GenericMath.wrapAngleDeg((float) (180 - yaw));
            roll = GenericMath.wrapAngleDeg((float) (roll + 180));
        }
        return Vector3f.from(pitch, yaw, roll);
    }
}
