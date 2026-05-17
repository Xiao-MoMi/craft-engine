package net.momirealms.craftengine.core.pack;

import org.joml.Vector3f;

import java.util.EnumMap;
import java.util.Map;

public final class CubeRotationProcessor {
    private static final float MIN = -16.0f;
    private static final float MAX = 32.0f;

    public enum Face { down, up, north, south, west, east }

    public record FaceData(Face targetFace, int uvRotation) {}

    public record RotationResult(
            Vector3f from, Vector3f to, Vector3f origin,
            float angle, Map<Face, FaceData> faceChanges
    ) {}

    private static final Map<Face, FaceData> ROT_X = Map.of(
            Face.up, new FaceData(Face.south, 0),
            Face.south, new FaceData(Face.down, 0),
            Face.down, new FaceData(Face.north, 180),
            Face.north, new FaceData(Face.up, 180),
            Face.east, new FaceData(Face.east, 270),
            Face.west, new FaceData(Face.west, 90)
    );

    private static final Map<Face, FaceData> ROT_Y = Map.of(
            Face.north, new FaceData(Face.west, 0),
            Face.west, new FaceData(Face.south, 0),
            Face.south, new FaceData(Face.east, 0),
            Face.east, new FaceData(Face.north, 0),
            Face.up, new FaceData(Face.up, 270),
            Face.down, new FaceData(Face.down, 90)
    );

    private static final Map<Face, FaceData> ROT_Z = Map.of(
            Face.up, new FaceData(Face.east, 90),
            Face.east, new FaceData(Face.down, 90),
            Face.down, new FaceData(Face.west, 90),
            Face.west, new FaceData(Face.up, 90),
            Face.north, new FaceData(Face.north, 270),
            Face.south, new FaceData(Face.south, 90)
    );

    public static RotationResult process(Vector3f from, Vector3f to, Vector3f origin, String axis, float angle) {
        float normalized = ((angle + 180) % 360 + 360) % 360 - 180;
        int steps = Math.round(normalized / 90.0f);
        float finalAngle = Math.clamp(normalized - (steps * 90.0f), -45, 45);

        Vector3f fFrom = new Vector3f(from);
        Vector3f fTo = new Vector3f(to);
        Vector3f fOrigin = new Vector3f(origin);

        Map<Face, FaceData> faceMap = new EnumMap<>(Face.class);
        for (Face f : Face.values()) faceMap.put(f, new FaceData(f, 0));

        if (steps != 0) {
            int positiveSteps = (steps % 4 + 4) % 4;
            for (int i = 0; i < positiveSteps; i++) {
                rotatePoint90(fFrom, fOrigin, axis);
                rotatePoint90(fTo, fOrigin, axis);
                updateMapping(faceMap, axis);
            }
        }

        float x1 = Math.min(fFrom.x, fTo.x), x2 = Math.max(fFrom.x, fTo.x);
        float y1 = Math.min(fFrom.y, fTo.y), y2 = Math.max(fFrom.y, fTo.y);
        float z1 = Math.min(fFrom.z, fTo.z), z2 = Math.max(fFrom.z, fTo.z);
        fFrom.set(x1, y1, z1);
        fTo.set(x2, y2, z2);

        Vector3f shift = new Vector3f(0);
        if (fFrom.x < MIN) shift.x = MIN - fFrom.x; else if (fTo.x > MAX) shift.x = MAX - fTo.x;
        if (fFrom.y < MIN) shift.y = MIN - fFrom.y; else if (fTo.y > MAX) shift.y = MAX - fTo.y;
        if (fFrom.z < MIN) shift.z = MIN - fFrom.z; else if (fTo.z > MAX) shift.z = MAX - fTo.z;

        fFrom.add(shift);
        fTo.add(shift);
        fOrigin.add(shift);

        fFrom.set(Math.clamp(fFrom.x, MIN, MAX), Math.clamp(fFrom.y, MIN, MAX), Math.clamp(fFrom.z, MIN, MAX));
        fTo.set(Math.clamp(fTo.x, MIN, MAX), Math.clamp(fTo.y, MIN, MAX), Math.clamp(fTo.z, MIN, MAX));

        return new RotationResult(fFrom, fTo, fOrigin, finalAngle, faceMap);
    }

    private static void updateMapping(Map<Face, FaceData> map, String axis) {
        Map<Face, FaceData> matrix = switch (axis.toLowerCase()) {
            case "x" -> ROT_X;
            case "y" -> ROT_Y;
            case "z" -> ROT_Z;
            default -> throw new IllegalArgumentException("Invalid axis: " + axis);
        };

        Map<Face, FaceData> nextStep = new EnumMap<>(Face.class);
        for (var entry : map.entrySet()) {
            FaceData currentData = entry.getValue();
            FaceData translation = matrix.get(currentData.targetFace());

            int combinedRotation = (currentData.uvRotation() + translation.uvRotation()) % 360;
            nextStep.put(entry.getKey(), new FaceData(translation.targetFace(), combinedRotation));
        }
        map.putAll(nextStep);
    }

    private static void rotatePoint90(Vector3f p, Vector3f o, String axis) {
        float x = p.x - o.x, y = p.y - o.y, z = p.z - o.z;
        switch (axis.toLowerCase()) {
            case "x" -> p.set(o.x + x, o.y - z, o.z + y);
            case "y" -> p.set(o.x + z, o.y + y, o.z - x);
            case "z" -> p.set(o.x + y, o.y - x, o.z + z);
        }
    }
}