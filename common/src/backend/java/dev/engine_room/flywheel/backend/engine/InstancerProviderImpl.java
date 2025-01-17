package dev.engine_room.flywheel.backend.engine;

import dev.engine_room.flywheel.api.event.RenderStage;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.instance.InstanceType;
import dev.engine_room.flywheel.api.instance.Instancer;
import dev.engine_room.flywheel.api.instance.InstancerProvider;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.backend.engine.embed.GlobalEnvironment;

public record InstancerProviderImpl(EngineImpl engine, RenderStage renderStage) implements InstancerProvider {
	@Override
	public <I extends Instance> Instancer<I> instancer(InstanceType<I> type, Model model) {
		return engine.instancer(GlobalEnvironment.INSTANCE, type, model, renderStage);
	}
}
