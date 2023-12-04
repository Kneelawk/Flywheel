package com.jozufozu.flywheel.backend.engine.instancing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL32;

import com.jozufozu.flywheel.Flywheel;
import com.jozufozu.flywheel.api.layout.BufferLayout;
import com.jozufozu.flywheel.api.model.Mesh;
import com.jozufozu.flywheel.api.vertex.VertexType;
import com.jozufozu.flywheel.gl.GlPrimitive;
import com.jozufozu.flywheel.gl.array.GlVertexArray;
import com.jozufozu.flywheel.gl.buffer.GlBuffer;
import com.jozufozu.flywheel.gl.buffer.MappedBuffer;
import com.jozufozu.flywheel.lib.vertex.VertexTypes;

public class InstancedMeshPool {
	private static final VertexType VERTEX_TYPE = VertexTypes.BLOCK;

	private final Map<Mesh, BufferedMesh> meshes = new HashMap<>();
	private final List<BufferedMesh> allBuffered = new ArrayList<>();
	private final List<BufferedMesh> pendingUpload = new ArrayList<>();

	private final GlBuffer vbo;
	private long byteSize;

	private boolean dirty;
	private boolean anyToRemove;

	/**
	 * Create a new mesh pool.
	 */
	public InstancedMeshPool() {
		int stride = VERTEX_TYPE.getLayout()
				.getStride();
		vbo = new GlBuffer();
		vbo.growthFunction(l -> Math.max(l + stride * 128L, (long) (l * 1.6)));
	}

	public VertexType getVertexType() {
		return VERTEX_TYPE;
	}

	/**
	 * Allocate a mesh in the arena.
	 *
	 * @param mesh     The mesh to allocate.
	 * @param eboCache The EBO cache to use.
	 * @return A handle to the allocated mesh.
	 */
	public BufferedMesh alloc(Mesh mesh, EBOCache eboCache) {
		return meshes.computeIfAbsent(mesh, m -> {
			BufferedMesh bufferedMesh = new BufferedMesh(m, byteSize, eboCache);
			byteSize += bufferedMesh.size();
			allBuffered.add(bufferedMesh);
			pendingUpload.add(bufferedMesh);

			dirty = true;
			return bufferedMesh;
		});
	}

	@Nullable
	public BufferedMesh get(Mesh mesh) {
		return meshes.get(mesh);
	}

	public void flush() {
		if (!dirty) {
			return;
		}

		if (anyToRemove) {
			processDeletions();
		}

		vbo.ensureCapacity(byteSize);

		uploadPending();

		dirty = false;
		pendingUpload.clear();
	}

	private void processDeletions() {
		// remove deleted meshes
		allBuffered.removeIf(bufferedMesh -> {
			boolean deleted = bufferedMesh.isDeleted();
			if (deleted) {
				meshes.remove(bufferedMesh.mesh);
			}
			return deleted;
		});

		// re-evaluate first vertex for each mesh
		int byteIndex = 0;
		for (BufferedMesh mesh : allBuffered) {
			if (mesh.byteIndex != byteIndex) {
				pendingUpload.add(mesh);
			}

			mesh.byteIndex = byteIndex;

			byteIndex += mesh.size();
		}

		this.byteSize = byteIndex;
		this.anyToRemove = false;
	}

	private void uploadPending() {
		try (MappedBuffer mapped = vbo.map()) {
			long ptr = mapped.ptr();

			var vertexList = VERTEX_TYPE.createVertexList();

			for (BufferedMesh mesh : pendingUpload) {
				vertexList.ptr(ptr + mesh.byteIndex);
				mesh.mesh.write(vertexList);
				mesh.boundTo.clear();
			}

			pendingUpload.clear();
		} catch (Exception e) {
			Flywheel.LOGGER.error("Error uploading pooled meshes:", e);
		}
	}

	public void delete() {
		vbo.delete();
		meshes.clear();
		allBuffered.clear();
		pendingUpload.clear();
	}

	@Override
	public String toString() {
		return "InstancedMeshPool{" + "vertexType=" + VERTEX_TYPE + ", byteSize=" + byteSize + ", meshCount=" + meshes.size() + '}';
	}

	public class BufferedMesh {
		private final Mesh mesh;
		private final int ebo;
		private long byteIndex;
		private boolean deleted;

		private final Set<GlVertexArray> boundTo = new HashSet<>();

		private BufferedMesh(Mesh mesh, long byteIndex, EBOCache eboCache) {
			this.mesh = mesh;
			this.byteIndex = byteIndex;
			this.ebo = eboCache.get(mesh.indexSequence(), mesh.indexCount());
		}

		public int size() {
			return mesh.vertexCount() * VERTEX_TYPE.getStride();
		}

		public VertexType getVertexType() {
			return VERTEX_TYPE;
		}

		public int getAttributeCount() {
			return VERTEX_TYPE.getLayout()
					.getAttributeCount();
		}

		public boolean isDeleted() {
			return deleted;
		}

		public boolean isEmpty() {
			return mesh.isEmpty() || isDeleted();
		}

		public void setup(GlVertexArray vao) {
			if (boundTo.add(vao)) {
				BufferLayout type = VERTEX_TYPE.getLayout();
				vao.bindVertexBuffer(0, InstancedMeshPool.this.vbo.handle(), byteIndex, type.getStride());
				vao.bindAttributes(0, 0, type.attributes());
				vao.setElementBuffer(ebo);
			}
		}

		public void draw(int instanceCount) {
			if (instanceCount > 1) {
				GL32.glDrawElementsInstanced(GlPrimitive.TRIANGLES.glEnum, mesh.indexCount(), GL32.GL_UNSIGNED_INT, 0, instanceCount);
			} else {
				GL32.glDrawElements(GlPrimitive.TRIANGLES.glEnum, mesh.indexCount(), GL32.GL_UNSIGNED_INT, 0);
			}
		}

		public void delete() {
			deleted = true;
			InstancedMeshPool.this.dirty = true;
			InstancedMeshPool.this.anyToRemove = true;
		}
	}
}
