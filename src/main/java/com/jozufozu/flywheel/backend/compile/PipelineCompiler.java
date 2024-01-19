package com.jozufozu.flywheel.backend.compile;

import java.util.List;

import com.jozufozu.flywheel.backend.InternalVertex;
import com.jozufozu.flywheel.backend.compile.core.CompilationHarness;
import com.jozufozu.flywheel.backend.compile.core.Compile;
import com.jozufozu.flywheel.backend.gl.shader.ShaderType;
import com.jozufozu.flywheel.backend.glsl.ShaderSources;
import com.jozufozu.flywheel.backend.glsl.SourceComponent;

public class PipelineCompiler {
	private static final Compile<PipelineProgramKey> PIPELINE = new Compile<>();

	static CompilationHarness<PipelineProgramKey> create(ShaderSources sources, Pipeline pipeline, List<SourceComponent> vertexComponents, List<SourceComponent> fragmentComponents) {
		return PIPELINE.program()
				.link(PIPELINE.shader(pipeline.glslVersion(), ShaderType.VERTEX)
						.withResource(pipeline.vertexApiImpl())
						.withResource(InternalVertex.LAYOUT_SHADER)
						.withComponent(key -> pipeline.assembler()
								.assemble(new Pipeline.InstanceAssemblerContext(InternalVertex.ATTRIBUTE_COUNT, key.instanceType())))
						.withComponents(vertexComponents)
						.withResource(key -> key.instanceType()
								.vertexShader())
						.withResource(key -> key.contextShader()
								.vertexShader())
						.withResource(pipeline.vertexMain()))
				.link(PIPELINE.shader(pipeline.glslVersion(), ShaderType.FRAGMENT)
						.enableExtension("GL_ARB_conservative_depth")
						.withResource(pipeline.fragmentApiImpl())
						.withComponents(fragmentComponents)
						.withResource(key -> key.contextShader()
								.fragmentShader())
						.withResource(pipeline.fragmentMain()))
				.then((key, program) -> {
					key.contextShader()
							.onProgramLink(program);
					program.setUniformBlockBinding("_FlwFrameUniforms", 0);
					program.setUniformBlockBinding("_FlwFogUniforms", 1);
				})
				.harness(pipeline.compilerMarker(), sources);
	}
}
