package dev.engine_room.flywheel.api.vertex;

import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * A read only view of a vertex buffer.
 *
 * <p>
 *     VertexList assumes nothing about the layout of the vertices. Implementations should feel free to return constants
 *     for values that are unused in their layout.
 * </p>
 */
public interface VertexList {
	float x(int index);

	float y(int index);

	float z(int index);

	float r(int index);

	float g(int index);

	float b(int index);

	float a(int index);

	float u(int index);

	float v(int index);

	int overlay(int index);

	int light(int index);

	float normalX(int index);

	float normalY(int index);

	float normalZ(int index);

	default Vector4f getPos(int i, Vector4f dest) {
		return dest.set(x(i), y(i), z(i));
	}

	default Vector3f getNormal(int i, Vector3f dest) {
		return dest.set(normalX(i), normalY(i), normalZ(i));
	}

	default void write(MutableVertexList dst, int srcIndex, int dstIndex) {
		dst.x(dstIndex, x(srcIndex));
		dst.y(dstIndex, y(srcIndex));
		dst.z(dstIndex, z(srcIndex));

		dst.r(dstIndex, r(srcIndex));
		dst.g(dstIndex, g(srcIndex));
		dst.b(dstIndex, b(srcIndex));
		dst.a(dstIndex, a(srcIndex));

		dst.u(dstIndex, u(srcIndex));
		dst.v(dstIndex, v(srcIndex));

		dst.overlay(dstIndex, overlay(srcIndex));
		dst.light(dstIndex, light(srcIndex));

		dst.normalX(dstIndex, normalX(srcIndex));
		dst.normalY(dstIndex, normalY(srcIndex));
		dst.normalZ(dstIndex, normalZ(srcIndex));
	}

	default void write(MutableVertexList dst, int srcStartIndex, int dstStartIndex, int vertexCount) {
		for (int i = 0; i < vertexCount; i++) {
			write(dst, srcStartIndex + i, dstStartIndex + i);
		}
	}

	default void writeAll(MutableVertexList dst) {
		write(dst, 0, 0, Math.min(vertexCount(), dst.vertexCount()));
	}

	int vertexCount();

	default boolean isEmpty() {
		return vertexCount() == 0;
	}
}
