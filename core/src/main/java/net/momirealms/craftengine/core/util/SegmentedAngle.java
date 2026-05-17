package net.momirealms.craftengine.core.util;

public final class SegmentedAngle {
    private final int segments;
    private final float degreeToAngle;
    private final float angleToDegree;

    public SegmentedAngle(int segments) {
        if (segments < 2) {
            throw new IllegalArgumentException("Segments cannot be less than 2");
        } else if (segments > 360) {
            throw new IllegalArgumentException("Segments cannot be greater than 360");
        } else {
            this.segments = segments;
            this.degreeToAngle = (float) segments / 360.0F;
            this.angleToDegree = 360.0F / (float) segments;
        }
    }

    public boolean isSameAxis(int alpha, int beta) {
        int halfSegments = this.segments / 2;
        return (alpha % halfSegments) == (beta % halfSegments);
    }

    public int fromDirection(Direction direction) {
        if (direction.axis().isVertical()) {
            return 0;
        } else {
            int i = direction.data2d();
            return i * (this.segments / 4);
        }
    }

    public int fromDegreesWithTurns(float degrees) {
        return Math.round(degrees * this.degreeToAngle);
    }

    public int fromDegrees(float degrees) {
        return this.normalize(this.fromDegreesWithTurns(degrees));
    }

    public float toDegreesWithTurns(int rotation) {
        return (float) rotation * this.angleToDegree;
    }

    public float toDegrees(int rotation) {
        float f = this.toDegreesWithTurns(this.normalize(rotation));
        return f >= 180.0F ? f - 360.0F : f;
    }

    public int normalize(int rotationBits) {
        return ((rotationBits % this.segments) + this.segments) % this.segments;
    }

    public int getSegments() {
        return this.segments;
    }
}