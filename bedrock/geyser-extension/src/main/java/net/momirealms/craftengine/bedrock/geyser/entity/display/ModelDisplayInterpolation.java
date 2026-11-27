package net.momirealms.craftengine.bedrock.geyser.entity.display;

import org.cloudburstmc.math.imaginary.Quaternionf;
import org.cloudburstmc.math.vector.Vector3f;

public final class ModelDisplayInterpolation {
    private DisplayPose target = DisplayPose.IDENTITY;
    private Vector3f translation = Vector3f.ZERO;
    private Vector3f scale = Vector3f.ONE;
    private Quaternionf left = Quaternionf.IDENTITY;
    private Quaternionf right = Quaternionf.IDENTITY;
    private int duration;
    private int delay;
    private int sequence;
    private boolean dirty = true;
    private boolean restart;
    private boolean changed = true;

    boolean dirty() {
        return this.dirty;
    }

    DisplayPose target() {
        if (this.target == null) {
            this.target = new DisplayPose(this.translation, this.left, this.scale, this.right);
        }
        return this.target;
    }

    float duration() {
        return this.duration / 20f;
    }

    float delay() {
        return this.delay / 20f;
    }

    void start(int delay) {
        this.delay = delay;
        this.restart = true;
        this.dirty = true;
    }

    void duration(int ticks) {
        int duration = Math.max(0, ticks);
        this.dirty |= this.duration != duration;
        this.duration = duration;
    }

    void translation(Vector3f value) {
        if (!this.translation.equals(value)) {
            this.translation = value;
            this.change();
        }
    }

    void scale(Vector3f value) {
        if (!this.scale.equals(value)) {
            this.scale = value;
            this.change();
        }
    }

    void left(Quaternionf value) {
        if (!this.left.equals(value)) {
            this.left = value;
            this.change();
        }
    }

    void right(Quaternionf value) {
        if (!this.right.equals(value)) {
            this.right = value;
            this.change();
        }
    }

    private void change() {
        this.target = null;
        this.dirty = true;
        this.changed = true;
    }

    int consume(boolean snap) {
        int flags = (this.restart ? 1 : 0) | (this.changed ? 2 : 0) | (snap ? 4 : 0);
        this.sequence = (this.sequence + 1) % 125000;
        this.dirty = false;
        this.restart = false;
        this.changed = false;
        return this.sequence * 8 + flags;
    }
}
