package dev.engine_room.flywheel.backend.compile.core;

import static org.lwjgl.opengl.GL11.GL_TRUE;
import static org.lwjgl.opengl.GL20.GL_LINK_STATUS;
import static org.lwjgl.opengl.GL20.glAttachShader;
import static org.lwjgl.opengl.GL20.glCreateProgram;
import static org.lwjgl.opengl.GL20.glGetProgramInfoLog;
import static org.lwjgl.opengl.GL20.glGetProgrami;
import static org.lwjgl.opengl.GL20.glLinkProgram;

import java.util.List;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import dev.engine_room.flywheel.backend.gl.shader.GlProgram;
import dev.engine_room.flywheel.backend.gl.shader.GlShader;

public class ProgramLinker {
	private final CompilerStats stats;

	public ProgramLinker(CompilerStats stats) {
		this.stats = stats;
	}

	@Nullable
	public GlProgram link(List<GlShader> shaders, Consumer<GlProgram> preLink) {
		// this probably doesn't need caching
		var linkResult = linkInternal(shaders, preLink);
		stats.linkResult(linkResult);
		return linkResult.unwrap();
	}

	private LinkResult linkInternal(List<GlShader> shaders, Consumer<GlProgram> preLink) {
		int handle = glCreateProgram();
		var out = new GlProgram(handle);

		for (GlShader shader : shaders) {
			glAttachShader(handle, shader.handle());
		}

		preLink.accept(out);

		glLinkProgram(handle);
		String log = glGetProgramInfoLog(handle);

		if (linkSuccessful(handle)) {
			return LinkResult.success(out, log);
		} else {
			out.delete();
			return LinkResult.failure(log);
		}
	}

	private static boolean linkSuccessful(int handle) {
		return glGetProgrami(handle, GL_LINK_STATUS) == GL_TRUE;
	}

}
