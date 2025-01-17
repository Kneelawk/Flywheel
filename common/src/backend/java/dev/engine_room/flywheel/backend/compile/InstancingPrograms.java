package dev.engine_room.flywheel.backend.compile;

import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.google.common.collect.ImmutableList;

import dev.engine_room.flywheel.api.instance.InstanceType;
import dev.engine_room.flywheel.backend.gl.GlCompat;
import dev.engine_room.flywheel.backend.gl.shader.GlProgram;
import dev.engine_room.flywheel.backend.glsl.GlslVersion;
import dev.engine_room.flywheel.backend.glsl.ShaderSources;
import dev.engine_room.flywheel.backend.glsl.SourceComponent;
import dev.engine_room.flywheel.backend.util.AtomicReferenceCounted;

public class InstancingPrograms extends AtomicReferenceCounted {
	private static final List<String> EXTENSIONS = getExtensions(GlCompat.MAX_GLSL_VERSION);

	@Nullable
	private static InstancingPrograms instance;

	private final Map<PipelineProgramKey, GlProgram> pipeline;

	private InstancingPrograms(Map<PipelineProgramKey, GlProgram> pipeline) {
		this.pipeline = pipeline;
	}

	private static List<String> getExtensions(GlslVersion glslVersion) {
		var extensions = ImmutableList.<String>builder();
		if (glslVersion.compareTo(GlslVersion.V330) < 0) {
			extensions.add("GL_ARB_shader_bit_encoding");
		}
		return extensions.build();
	}

	static void reload(ShaderSources sources, ImmutableList<PipelineProgramKey> pipelineKeys, List<SourceComponent> vertexComponents, List<SourceComponent> fragmentComponents) {
		if (!GlCompat.SUPPORTS_INSTANCING) {
			return;
		}

		InstancingPrograms newInstance = null;

		var pipelineCompiler = PipelineCompiler.create(sources, Pipelines.INSTANCING, vertexComponents, fragmentComponents, EXTENSIONS);

		try {
			var pipelineResult = pipelineCompiler.compileAndReportErrors(pipelineKeys);

			if (pipelineResult != null) {
				newInstance = new InstancingPrograms(pipelineResult);
			}
		} catch (Throwable t) {
			FlwPrograms.LOGGER.error("Failed to compile instancing programs", t);
		}

		pipelineCompiler.delete();

		setInstance(newInstance);
	}

	static void setInstance(@Nullable InstancingPrograms newInstance) {
		if (instance != null) {
			instance.release();
		}
		if (newInstance != null) {
			newInstance.acquire();
		}
		instance = newInstance;
	}

	@Nullable
	public static InstancingPrograms get() {
		return instance;
	}

	public static boolean allLoaded() {
		return instance != null;
	}

	public GlProgram get(InstanceType<?> instanceType, ContextShader contextShader) {
		return pipeline.get(new PipelineProgramKey(instanceType, contextShader));
	}

	@Override
	protected void _delete() {
		pipeline.values()
				.forEach(GlProgram::delete);
	}
}
