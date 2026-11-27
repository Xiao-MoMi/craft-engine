package net.momirealms.craftengine.core.util;

import org.joml.Matrix3d;
import org.joml.Matrix3dc;
import org.joml.Vector3d;

public final class MatrixDecomposition {
    private MatrixDecomposition() {
    }

    public static Result decompose(Matrix3dc matrix) {
        Matrix3d work = new Matrix3d(matrix);
        Matrix3d right = new Matrix3d();
        // 单边 Jacobi SVD：只旋转列，使 A·V 的列相互正交；不求逆，因此零缩放也可分解。
        for (int sweep = 0; sweep < 32; sweep++) {
            boolean changed = false;
            for (int p = 0; p < 2; p++) {
                for (int q = p + 1; q < 3; q++) {
                    Vector3d a = work.getColumn(p, new Vector3d());
                    Vector3d b = work.getColumn(q, new Vector3d());
                    double cross = a.dot(b);
                    if (Math.abs(cross) <= 1e-14 * Math.sqrt(a.lengthSquared() * b.lengthSquared())) {
                        continue;
                    }
                    double angle = 0.5 * Math.atan2(2 * cross, b.lengthSquared() - a.lengthSquared());
                    double cosine = Math.cos(angle);
                    double sine = Math.sin(angle);
                    work.setColumn(p, new Vector3d(a).mul(cosine).sub(new Vector3d(b).mul(sine)));
                    work.setColumn(q, a.mul(sine).add(b.mul(cosine)));
                    right.getColumn(p, a);
                    right.getColumn(q, b);
                    right.setColumn(p, new Vector3d(a).mul(cosine).sub(new Vector3d(b).mul(sine)));
                    right.setColumn(q, a.mul(sine).add(b.mul(cosine)));
                    changed = true;
                }
            }
            if (!changed) {
                break;
            }
        }
        Vector3d scale = new Vector3d();
        Matrix3d left = new Matrix3d().zero();
        boolean[] present = new boolean[3];
        double largest = 0;
        for (int axis = 0; axis < 3; axis++) {
            double length = work.getColumn(axis, new Vector3d()).length();
            scale.setComponent(axis, length);
            largest = Math.max(largest, length);
        }
        for (int axis = 0; axis < 3; axis++) {
            double length = scale.get(axis);
            if (length > largest * 1e-12) {
                left.setColumn(axis, work.getColumn(axis, new Vector3d()).div(length));
                present[axis] = true;
            } else {
                scale.setComponent(axis, 0);
            }
        }
        // 奇异矩阵缺失的轴只补齐正交基，不改变零奇异值对应的几何。
        for (int axis = 0; axis < 3; axis++) {
            if (present[axis]) {
                continue;
            }
            Vector3d best = new Vector3d();
            for (int candidate = 0; candidate < 3; candidate++) {
                Vector3d vector = new Vector3d().setComponent(candidate, 1);
                for (int other = 0; other < 3; other++) {
                    if (present[other]) {
                        Vector3d basis = left.getColumn(other, new Vector3d());
                        vector.sub(basis.mul(vector.dot(basis)));
                    }
                }
                if (vector.lengthSquared() > best.lengthSquared()) {
                    best.set(vector);
                }
            }
            left.setColumn(axis, best.normalize());
            present[axis] = true;
        }
        // 骨骼旋转必须是正旋转；将反射符号留在缩放中，保留原来的 UV 和面对应关系。
        if (left.determinant() < 0) {
            left.setColumn(2, left.getColumn(2, new Vector3d()).negate());
            scale.z = -scale.z;
        }
        return new Result(left, scale, right.transpose());
    }

    public record Result(Matrix3d left, Vector3d scale, Matrix3d right) {
    }
}
