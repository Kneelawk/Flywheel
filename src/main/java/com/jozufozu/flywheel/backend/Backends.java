package com.jozufozu.flywheel.backend;

import com.jozufozu.flywheel.Flywheel;
import com.jozufozu.flywheel.api.backend.Backend;
import com.jozufozu.flywheel.backend.engine.batching.BatchingEngine;
import com.jozufozu.flywheel.backend.engine.indirect.IndirectEngine;
import com.jozufozu.flywheel.backend.engine.instancing.InstancingEngine;
import com.jozufozu.flywheel.gl.versioned.GlCompat;
import com.jozufozu.flywheel.lib.backend.SimpleBackend;
import com.jozufozu.flywheel.lib.context.Contexts;
import com.jozufozu.flywheel.lib.util.ShadersModHandler;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.TextComponent;

public class Backends {
	/**
	 * Use a thread pool to buffer instances in parallel on the CPU.
	 */
	public static final Backend BATCHING = SimpleBackend.builder()
			.engineMessage(new TextComponent("Using Batching Engine").withStyle(ChatFormatting.GREEN))
			.engineFactory(level -> new BatchingEngine())
			.supported(() -> !ShadersModHandler.isShaderPackInUse())
			.register(Flywheel.rl("batching"));

	/**
	 * Use GPU instancing to render everything.
	 */
	public static final Backend INSTANCING = SimpleBackend.builder()
			.engineMessage(new TextComponent("Using Instancing Engine").withStyle(ChatFormatting.GREEN))
			.engineFactory(level -> new InstancingEngine(Contexts.WORLD, 100))
			.fallback(() -> Backends.BATCHING)
			.supported(() -> !ShadersModHandler.isShaderPackInUse() && GlCompat.getInstance()
					.instancedArraysSupported())
			.pipelineShader(Pipelines.INSTANCED_ARRAYS)
			.register(Flywheel.rl("instancing"));

	/**
	 * Use Compute shaders to cull instances.
	 */
	public static final Backend INDIRECT = SimpleBackend.builder()
			.engineMessage(new TextComponent("Using Indirect Engine").withStyle(ChatFormatting.GREEN))
			.engineFactory(level -> new IndirectEngine(100))
			.fallback(() -> Backends.INSTANCING)
			.supported(() -> !ShadersModHandler.isShaderPackInUse() && GlCompat.getInstance()
					.supportsIndirect())
			.pipelineShader(Pipelines.INDIRECT)
			.register(Flywheel.rl("indirect"));

	public static void init() {
	}
}