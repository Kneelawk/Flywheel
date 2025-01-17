package dev.engine_room.flywheel.api.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

@FunctionalInterface
public interface RenderStageCallback {
	Event<RenderStageCallback> EVENT =
			EventFactory.createArrayBacked(RenderStageCallback.class, callbacks -> (context, stage) -> {
				for (RenderStageCallback callback : callbacks) {
					callback.onRenderStage(context, stage);
				}
			});

	void onRenderStage(RenderContext context, RenderStage stage);
}
