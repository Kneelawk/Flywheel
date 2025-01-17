package dev.engine_room.flywheel.impl.visualization.manager;

import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import dev.engine_room.flywheel.api.visual.Visual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.impl.visualization.storage.Storage;
import dev.engine_room.flywheel.lib.visual.VisualizationHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public class EntityStorage extends Storage<Entity> {
	public EntityStorage(Supplier<VisualizationContext> visualizationContextSupplier) {
		super(visualizationContextSupplier);
	}

	@Override
	@Nullable
	protected Visual createRaw(Entity obj) {
		var visualizer = VisualizationHelper.getVisualizer(obj);
		if (visualizer == null) {
			return null;
		}

		return visualizer.createVisual(visualizationContextSupplier.get(), obj);
	}

	@Override
	public boolean willAccept(Entity entity) {
		if (!entity.isAlive()) {
			return false;
		}

		if (!VisualizationHelper.canVisualize(entity)) {
			return false;
		}

		Level level = entity.level();
		return level != null;
	}
}
