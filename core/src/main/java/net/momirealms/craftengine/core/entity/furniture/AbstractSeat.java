package net.momirealms.craftengine.core.entity.furniture;

import org.joml.Vector3f;

import java.util.Objects;

public abstract class AbstractSeat implements Seat{
	protected final Vector3f offset;
	protected final float yaw;

	public AbstractSeat(Vector3f offset, float yaw) {
		this.offset = offset;
		this.yaw = yaw;
	}

	@Override
	public Vector3f offset() {
		return offset;
	}

	@Override
	public float yaw() {
		return yaw;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof AbstractSeat seat)) return false;
		return Float.compare(yaw, seat.yaw()) == 0 && offset.equals(seat.offset());
	}

	@Override
	public int hashCode() {
		int result = Objects.hash(offset);
		result = 31 * result + Float.hashCode(yaw);
		return result;
	}
}
