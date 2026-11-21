package net.momirealms.craftengine.bedrock.geyser.entity.display;

import org.cloudburstmc.math.imaginary.Quaternionf;
import org.cloudburstmc.math.vector.Vector3f;

public final class TextDisplayInterpolation {
    private static final State INITIAL = new State(DisplayPose.IDENTITY, 0x40000000, 255);
    private int duration;
    private int delay;
    private long tick;
    private long start;
    private long sampledTick;
    private DisplayPose pose = DisplayPose.IDENTITY;
    private int background = INITIAL.background();
    private int opacity = INITIAL.opacity();
    private State previous = INITIAL;
    private State target = INITIAL;
    private DisplayPose currentPose = INITIAL.pose();
    private int currentBackground = INITIAL.background();
    private int currentOpacity = INITIAL.opacity();
    private boolean dirty = true;
    private boolean restart;
    private boolean interpolating;

    void tick() {
        this.tick++;
    }

    boolean needsUpdate() {
        return this.dirty || (this.interpolating && this.tick != this.sampledTick);
    }

    void sample() {
        // 同一 tick 的元数据和渲染更新共用采样结果，收到变更时重新采样
        if (!this.needsUpdate()) {
            return;
        }
        this.sampledTick = this.tick;
        if (this.dirty) {
            boolean targetChanged = this.background != this.target.background() || this.opacity != this.target.opacity() || !this.pose.equals(this.target.pose());
            if (this.restart || (this.interpolating && targetChanged)) {
                this.previous = this.currentBackground == this.target.background() && this.currentOpacity == this.target.opacity() && this.currentPose.equals(this.target.pose()) ? this.target : new State(this.currentPose, this.currentBackground, this.currentOpacity);
                this.start = this.tick + (this.restart ? this.delay : 0);
                this.interpolating = this.duration > 0 && (this.currentBackground != this.background || this.currentOpacity != this.opacity || !this.currentPose.equals(this.pose));
            }
            if (targetChanged) {
                this.target = new State(this.pose, this.background, this.opacity);
            }
            this.dirty = false;
            this.restart = false;
        }
        if (this.interpolating && this.duration > 0) {
            float progress = (this.tick - this.start) / (float) this.duration;
            if (progress <= 0) {
                this.setCurrent(this.previous);
                return;
            }
            if (progress < 1) {
                DisplayPose from = this.previous.pose();
                DisplayPose to = this.target.pose();
                this.currentPose = from.equals(to) ? from : from.interpolate(to, progress);
                this.currentBackground = interpolateColor(this.previous.background(), this.target.background(), progress);
                this.currentOpacity = interpolateChannel(this.previous.opacity(), this.target.opacity(), progress);
                return;
            }
        }
        this.interpolating = false;
        this.setCurrent(this.target);
    }

    private void setCurrent(State state) {
        this.currentPose = state.pose();
        this.currentBackground = state.background();
        this.currentOpacity = state.opacity();
    }

    DisplayPose currentPose() {
        return this.currentPose;
    }

    int currentBackground() {
        return this.currentBackground;
    }

    int currentOpacity() {
        return this.currentOpacity;
    }

    void startInterpolation(int delay) {
        this.delay = delay;
        this.restart = true;
        this.dirty = true;
    }

    void duration(int ticks) {
        this.dirty = true;
        this.duration = Math.max(0, ticks);
    }

    void translation(Vector3f value) {
        if (!this.pose.translation().equals(value)) {
            this.pose = new DisplayPose(value, this.pose.left(), this.pose.scale(), this.pose.right());
            this.dirty = true;
        }
    }

    void scale(Vector3f value) {
        if (!this.pose.scale().equals(value)) {
            this.pose = new DisplayPose(this.pose.translation(), this.pose.left(), value, this.pose.right());
            this.dirty = true;
        }
    }

    void left(Quaternionf value) {
        if (!this.pose.left().equals(value)) {
            this.pose = new DisplayPose(this.pose.translation(), value, this.pose.scale(), this.pose.right());
            this.dirty = true;
        }
    }

    void right(Quaternionf value) {
        if (!this.pose.right().equals(value)) {
            this.pose = new DisplayPose(this.pose.translation(), this.pose.left(), this.pose.scale(), value);
            this.dirty = true;
        }
    }

    void background(int value) {
        if (this.background != value) {
            this.background = value;
            this.dirty = true;
        }
    }

    void opacity(int value) {
        if (this.opacity != value) {
            this.opacity = value;
            this.dirty = true;
        }
    }

    private static int interpolateColor(int from, int to, float progress) {
        if (from == to) {
            return from;
        }
        return interpolateChannel(from >>> 24, to >>> 24, progress) << 24
                | interpolateChannel((from >>> 16) & 255, (to >>> 16) & 255, progress) << 16
                | interpolateChannel((from >>> 8) & 255, (to >>> 8) & 255, progress) << 8
                | interpolateChannel(from & 255, to & 255, progress);
    }

    private static int interpolateChannel(int from, int to, float progress) {
        return Math.round(from + (to - from) * progress);
    }

    private record State(DisplayPose pose, int background, int opacity) {
    }
}
