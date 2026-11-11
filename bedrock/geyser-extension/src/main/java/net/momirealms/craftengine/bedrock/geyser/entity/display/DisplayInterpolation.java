package net.momirealms.craftengine.bedrock.geyser.entity.display;

import org.cloudburstmc.math.imaginary.Quaternionf;
import org.cloudburstmc.math.vector.Vector3f;

final class DisplayInterpolation {
    private DisplayPose target = DisplayPose.IDENTITY;
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
        if (!this.target.translation().equals(value)) {
            this.target = new DisplayPose(value, this.target.left(), this.target.scale(), this.target.right());
            this.change();
        }
    }

    void scale(Vector3f value) {
        if (!this.target.scale().equals(value)) {
            this.target = new DisplayPose(this.target.translation(), this.target.left(), value, this.target.right());
            this.change();
        }
    }

    void left(Quaternionf value) {
        if (!this.target.left().equals(value)) {
            this.target = new DisplayPose(this.target.translation(), value, this.target.scale(), this.target.right());
            this.change();
        }
    }

    void right(Quaternionf value) {
        if (!this.target.right().equals(value)) {
            this.target = new DisplayPose(this.target.translation(), this.target.left(), this.target.scale(), value);
            this.change();
        }
    }

    private void change() {
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
