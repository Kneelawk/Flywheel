package com.jozufozu.flywheel.backend;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import com.jozufozu.flywheel.api.FlywheelWorld;
import com.jozufozu.flywheel.backend.instancing.ParallelTaskEngine;
import com.jozufozu.flywheel.config.FlwConfig;
import com.jozufozu.flywheel.core.BackendTypes;
import com.mojang.logging.LogUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

public class Backend {
	public static final Logger LOGGER = LogUtils.getLogger();

	public static final boolean DUMP_SHADER_SOURCE = System.getProperty("flw.dumpShaderSource") != null;

	private static BackendType TYPE;

	private static ParallelTaskEngine EXECUTOR;

	private static final Loader LOADER = new Loader();

	/**
	 * Get the current Flywheel backend type.
	 */
	public static BackendType getBackendType() {
		return TYPE;
	}

	/**
	 * Get a thread pool for running Flywheel related work in parallel.
	 * @return A global Flywheel thread pool.
	 */
	public static ParallelTaskEngine getTaskEngine() {
		if (EXECUTOR == null) {
			EXECUTOR = new ParallelTaskEngine("Flywheel");
			EXECUTOR.startWorkers();
		}

		return EXECUTOR;
	}

	/**
	 * Get a string describing the Flywheel backend. When there are eventually multiple backends
	 * (Meshlet, MDI, GL31 Draw Instanced are planned), this will name which one is in use.
	 */
	public static String getBackendDescriptor() {
		return TYPE == null ? "Uninitialized" : TYPE.getProperName();
	}

	public static void refresh() {
		TYPE = chooseEngine();
	}

	public static boolean isOn() {
		return TYPE != BackendTypes.OFF;
	}

	public static boolean canUseInstancing(@Nullable Level world) {
		return isOn() && isFlywheelWorld(world);
	}

	/**
	 * Used to avoid calling Flywheel functions on (fake) worlds that don't specifically support it.
	 */
	public static boolean isFlywheelWorld(@Nullable LevelAccessor world) {
		if (world == null) return false;

		if (!world.isClientSide()) return false;

		if (world instanceof FlywheelWorld && ((FlywheelWorld) world).supportsFlywheel()) return true;

		return world == Minecraft.getInstance().level;
	}

	public static boolean isGameActive() {
		return !(Minecraft.getInstance().level == null || Minecraft.getInstance().player == null);
	}

	public static void reloadWorldRenderers() {
		RenderWork.enqueue(Minecraft.getInstance().levelRenderer::allChanged);
	}

	private static BackendType chooseEngine() {
		var preferred = FlwConfig.get()
				.getBackendType();
		var actual = preferred.findFallback();

		if (preferred != actual) {
			LOGGER.warn("Flywheel backend fell back from '{}' to '{}'", preferred.getShortName(), actual.getShortName());
		}

		return actual;
	}

	public static void init() {
		// noop
	}

	private Backend() {
		throw new UnsupportedOperationException("Backend is a static class!");
	}

}
