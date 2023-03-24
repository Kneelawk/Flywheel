package com.jozufozu.flywheel.vanilla;

import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.jozufozu.flywheel.api.instance.DynamicInstance;
import com.jozufozu.flywheel.api.instancer.InstancedPart;
import com.jozufozu.flywheel.api.instancer.InstancerManager;
import com.jozufozu.flywheel.backend.instancing.blockentity.BlockEntityInstance;
import com.jozufozu.flywheel.core.Materials;
import com.jozufozu.flywheel.core.hardcoded.ModelPart;
import com.jozufozu.flywheel.core.model.SimpleLazyModel;
import com.jozufozu.flywheel.core.structs.StructTypes;
import com.jozufozu.flywheel.core.structs.oriented.OrientedPart;
import com.jozufozu.flywheel.util.AnimationTickHolder;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;

import net.minecraft.client.renderer.blockentity.BellRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BellBlockEntity;

public class BellInstance extends BlockEntityInstance<BellBlockEntity> implements DynamicInstance {

	private static final SimpleLazyModel MODEL = new SimpleLazyModel(BellInstance::createBellModel, Materials.BELL);

	private final OrientedPart bell;

	private float lastRingTime = Float.NaN;

	public BellInstance(InstancerManager instancerManager, BellBlockEntity blockEntity) {
		super(instancerManager, blockEntity);

		bell = createBellInstance()
				.setPivot(0.5f, 0.75f, 0.5f)
				.setPosition(getInstancePosition());
	}

	@Override
	public void beginFrame() {
		float ringTime = (float)blockEntity.ticks + AnimationTickHolder.getPartialTicks();

		if (ringTime == lastRingTime) return;
		lastRingTime = ringTime;

		if (blockEntity.shaking) {
			float angle = Mth.sin(ringTime / (float) Math.PI) / (4.0F + ringTime / 3.0F);

			Vector3f ringAxis = blockEntity.clickDirection.getCounterClockWise().step();

			bell.setRotation(ringAxis.rotation(angle));
		} else {
			bell.setRotation(Quaternion.ONE);
		}
	}

	@Override
	public void updateLight() {
		relight(getWorldPosition(), bell);
	}

	@Override
	public void addCrumblingParts(List<InstancedPart> data) {
		Collections.addAll(data, bell);
	}

	@Override
	public void remove() {
		bell.delete();
	}

	private OrientedPart createBellInstance() {
		return instancerManager.instancer(StructTypes.ORIENTED, MODEL)
				.createInstance();
	}

	@NotNull
	private static ModelPart createBellModel() {
		return ModelPart.builder("bell", 32, 32)
				.sprite(BellRenderer.BELL_RESOURCE_LOCATION.sprite())
				.cuboid()
				.start(5.0F, 6.0F, 5.0F)
				.size(6.0F, 7.0F, 6.0F)
				.endCuboid()
				.cuboid()
				.textureOffset(0, 13)
				.start(4.0F, 4.0F, 4.0F)
				.size(8.0F, 2.0F, 8.0F)
				.endCuboid()
				.build();
	}
}
