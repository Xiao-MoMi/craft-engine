package net.momirealms.craftengine.core.pack.bedrock.model;

import net.momirealms.craftengine.core.util.Direction;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class BedrockBlockGeometry {
    private static final Direction[] DIRECTIONS = Direction.values();

    private BedrockBlockGeometry() {
    }

    static BedrockGeometry convert(BedrockGeometry geometry) {
        List<BedrockGeometry.Cube> source = geometry.cubes();
        List<BedrockGeometry.Cube> cubes = null;
        for (int index = 0; index < source.size(); index++) {
            BedrockGeometry.Cube cube = source.get(index);
            Vector3f size = cube.size();
            if (size.x() < 0 || size.y() < 0 || size.z() < 0) {
                if (cubes == null) {
                    cubes = new ArrayList<>(source.size() + 5);
                    for (int previous = 0; previous < index; previous++) {
                        cubes.add(source.get(previous));
                    }
                }
                appendPlanes(cube, cubes);
            } else if (cubes != null) {
                cubes.add(cube);
            }
        }
        return cubes == null ? geometry : new BedrockGeometry(geometry.identifier(), geometry.textureWidth(),
                geometry.textureHeight(), cubes, geometry.bounds());
    }

    private static void appendPlanes(BedrockGeometry.Cube cube, List<BedrockGeometry.Cube> output) {
        Vector3f origin = cube.origin();
        Vector3f size = cube.size();
        for (Direction direction : DIRECTIONS) {
            BedrockGeometry.Face face = cube.faces().get(direction);
            if (face == null) {
                continue;
            }
            int axis = switch (direction.axis()) {
                case X -> 0;
                case Y -> 1;
                case Z -> 2;
            };
            float horizontal = axis == 0 ? size.z() : size.x();
            float vertical = axis == 1 ? size.z() : size.y();
            if (horizontal == 0 || vertical == 0) {
                continue;
            }
            // Block geometry does not preserve signed cube winding. Each plane carries its actual facing explicitly.
            Direction facing = (horizontal < 0) != (vertical < 0) ? direction.opposite() : direction;
            Vector3f planeOrigin = new Vector3f(origin.x() + Math.min(size.x(), 0),
                    origin.y() + Math.min(size.y(), 0), origin.z() + Math.min(size.z(), 0));
            // Bedrock reflects X, so the Java west face lies at origin.x + size.x.
            float position = switch (direction) {
                case WEST -> origin.x() + size.x();
                case EAST -> origin.x();
                case UP -> origin.y() + size.y();
                case DOWN -> origin.y();
                case SOUTH -> origin.z() + size.z();
                case NORTH -> origin.z();
            };
            planeOrigin.setComponent(axis, position);
            Vector3f planeSize = new Vector3f(Math.abs(size.x()), Math.abs(size.y()), Math.abs(size.z()));
            planeSize.setComponent(axis, 0);
            // With the facing corrected, positive extents shift the UV vertex order by either zero or two corners.
            boolean rotateUv = axis == 1 ? size.x() < 0 : size.y() < 0;
            BedrockGeometry.Face planeFace = rotateUv
                    ? new BedrockGeometry.Face(face.u(), face.v(), face.width(), face.height(), (face.rotation() + 180) % 360, face.material())
                    : face;
            output.add(new BedrockGeometry.Cube(planeOrigin, planeSize, cube.rotation(), Map.of(facing, planeFace)));
        }
    }
}
