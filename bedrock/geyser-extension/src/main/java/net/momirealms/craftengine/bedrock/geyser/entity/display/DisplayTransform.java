package net.momirealms.craftengine.bedrock.geyser.entity.display;

import org.cloudburstmc.math.imaginary.Quaternionf;
import org.cloudburstmc.math.vector.Vector3f;

final class DisplayTransform {
    private int duration;
    private long start;
    private DisplayPose previous = DisplayPose.IDENTITY;
    private DisplayPose target = DisplayPose.IDENTITY;
    private boolean dirty = true;

    boolean needsUpdate() {
        return this.dirty || this.start != 0;
    }

    DisplayPose sample() {
        this.dirty = false;
        return this.start == 0 ? this.target : this.sample(System.nanoTime());
    }

    void startInterpolation(int delay) {
        this.dirty = true;
        long now = System.nanoTime();
        this.previous = this.sample(now);
        this.start = now + delay * 50000000L;
    }

    void duration(int ticks) {
        this.dirty = true;
        this.duration = Math.max(0, ticks);
    }

    void translation(Vector3f value) {
        this.dirty = true;
        this.target = new DisplayPose(value, this.target.left(), this.target.scale(), this.target.right());
    }

    void scale(Vector3f value) {
        this.dirty = true;
        this.target = new DisplayPose(this.target.translation(), this.target.left(), value, this.target.right());
    }

    void left(Quaternionf value) {
        this.dirty = true;
        this.target = new DisplayPose(this.target.translation(), value, this.target.scale(), this.target.right());
    }

    void right(Quaternionf value) {
        this.dirty = true;
        this.target = new DisplayPose(this.target.translation(), this.target.left(), this.target.scale(), value);
    }

    DisplayPose sample(long now) {
        if (this.duration == 0 || this.start == 0) {
            this.start = 0;
            return this.target;
        }
        float fraction = (now - this.start) / (50000000f * this.duration);
        if (fraction >= 1) {
            this.start = 0;
            return this.target;
        }
        return fraction <= 0 ? this.previous : this.previous.interpolate(this.target, fraction);
    }
}
