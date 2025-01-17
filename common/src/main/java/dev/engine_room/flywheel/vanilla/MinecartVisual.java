package dev.engine_room.flywheel.vanilla;

import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visual.TickableVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.Materials;
import dev.engine_room.flywheel.lib.model.ModelHolder;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import dev.engine_room.flywheel.lib.model.part.ModelPartConverter;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import dev.engine_room.flywheel.lib.visual.SimpleEntityVisual;
import dev.engine_room.flywheel.lib.visual.SimpleTickableVisual;
import dev.engine_room.flywheel.lib.visual.component.FireComponent;
import dev.engine_room.flywheel.lib.visual.component.HitboxComponent;
import dev.engine_room.flywheel.lib.visual.component.ShadowComponent;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class MinecartVisual<T extends AbstractMinecart> extends SimpleEntityVisual<T> implements SimpleTickableVisual, SimpleDynamicVisual {
	public static final ModelHolder CHEST_BODY_MODEL = createBodyModelHolder(ModelLayers.CHEST_MINECART);
	public static final ModelHolder COMMAND_BLOCK_BODY_MODEL = createBodyModelHolder(ModelLayers.COMMAND_BLOCK_MINECART);
	public static final ModelHolder FURNACE_BODY_MODEL = createBodyModelHolder(ModelLayers.FURNACE_MINECART);
	public static final ModelHolder HOPPER_BODY_MODEL = createBodyModelHolder(ModelLayers.HOPPER_MINECART);
	public static final ModelHolder STANDARD_BODY_MODEL = createBodyModelHolder(ModelLayers.MINECART);
	public static final ModelHolder SPAWNER_BODY_MODEL = createBodyModelHolder(ModelLayers.SPAWNER_MINECART);
	public static final ModelHolder TNT_BODY_MODEL = createBodyModelHolder(ModelLayers.TNT_MINECART);

	private final ModelHolder bodyModel;

	private TransformedInstance body;
	@Nullable
	private TransformedInstance contents;
	private BlockState blockState;
	private boolean active;

	private final PoseStack stack = new PoseStack();

	public MinecartVisual(VisualizationContext ctx, T entity, ModelHolder bodyModel) {
		super(ctx, entity);
		this.bodyModel = bodyModel;
	}

	private static ModelHolder createBodyModelHolder(ModelLayerLocation layer) {
		return new ModelHolder(() -> {
			return new SingleMeshModel(ModelPartConverter.convert(layer), Materials.MINECART);
		});
	}

	@Override
	public void init(float partialTick) {
		addComponent(new ShadowComponent(visualizationContext, entity).radius(0.7f));
		addComponent(new FireComponent(visualizationContext, entity));
		addComponent(new HitboxComponent(visualizationContext, entity));

		body = createBodyInstance();
		blockState = entity.getDisplayBlockState();
		contents = createContentsInstance();

		updateInstances(partialTick);
		updateLight();

		super.init(partialTick);
	}

	private TransformedInstance createBodyInstance() {
		return instancerProvider.instancer(InstanceTypes.TRANSFORMED, bodyModel.get())
				.createInstance();
	}

	@Nullable
	private TransformedInstance createContentsInstance() {
		RenderShape shape = blockState.getRenderShape();

		if (shape == RenderShape.ENTITYBLOCK_ANIMATED) {
			body.setEmptyTransform();
			active = false;
			return null;
		}
		active = true;

		if (shape == RenderShape.INVISIBLE) {
			return null;
		}

		return instancerProvider.instancer(InstanceTypes.TRANSFORMED, Models.block(blockState))
				.createInstance();
	}

	@Override
	public void tick(TickableVisual.Context context) {
		BlockState displayBlockState = entity.getDisplayBlockState();

		if (displayBlockState != blockState) {
			blockState = displayBlockState;
			if (contents != null) {
				contents.delete();
			}
			contents = createContentsInstance();
		}

		updateLight();
	}

	@Override
	public void beginFrame(DynamicVisual.Context context) {
		super.beginFrame(context);

		if (!isVisible(context.frustum())) {
			return;
		}

		// TODO: add proper way to temporarily disable rendering a specific instance
		if (!active) {
			return;
		}

		updateInstances(context.partialTick());
	}

	private void updateInstances(float partialTick) {
		stack.setIdentity();

		double posX = Mth.lerp(partialTick, entity.xOld, entity.getX());
		double posY = Mth.lerp(partialTick, entity.yOld, entity.getY());
		double posZ = Mth.lerp(partialTick, entity.zOld, entity.getZ());

		stack.translate(posX - renderOrigin.getX(), posY - renderOrigin.getY(), posZ - renderOrigin.getZ());
		float yaw = Mth.lerp(partialTick, entity.yRotO, entity.getYRot());

		long randomBits = entity.getId() * 493286711L;
		randomBits = randomBits * randomBits * 4392167121L + randomBits * 98761L;
		float nudgeX = (((float) (randomBits >> 16 & 7L) + 0.5f) / 8.0f - 0.5F) * 0.004f;
		float nudgeY = (((float) (randomBits >> 20 & 7L) + 0.5f) / 8.0f - 0.5F) * 0.004f;
		float nudgeZ = (((float) (randomBits >> 24 & 7L) + 0.5f) / 8.0f - 0.5F) * 0.004f;
		stack.translate(nudgeX, nudgeY, nudgeZ);

		Vec3 pos = entity.getPos(posX, posY, posZ);
		float pitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
		if (pos != null) {
			Vec3 offset1 = entity.getPosOffs(posX, posY, posZ, 0.3F);
			Vec3 offset2 = entity.getPosOffs(posX, posY, posZ, -0.3F);

			if (offset1 == null) {
				offset1 = pos;
			}

			if (offset2 == null) {
				offset2 = pos;
			}

			stack.translate(pos.x - posX, (offset1.y + offset2.y) / 2.0D - posY, pos.z - posZ);
			Vec3 vec = offset2.add(-offset1.x, -offset1.y, -offset1.z);
			if (vec.length() != 0.0D) {
				vec = vec.normalize();
				yaw = (float) (Math.atan2(vec.z, vec.x) * 180.0D / Math.PI);
				pitch = (float) (Math.atan(vec.y) * 73.0D);
			}
		}

		stack.translate(0.0D, 0.375D, 0.0D);
		stack.mulPose(Axis.YP.rotationDegrees(180 - yaw));
		stack.mulPose(Axis.ZP.rotationDegrees(-pitch));

		float hurtTime = entity.getHurtTime() - partialTick;
		float damage = entity.getDamage() - partialTick;

		if (damage < 0) {
			damage = 0;
		}

		if (hurtTime > 0) {
			stack.mulPose(Axis.XP.rotationDegrees(Mth.sin(hurtTime) * hurtTime * damage / 10.0F * (float) entity.getHurtDir()));
		}

		int displayOffset = entity.getDisplayOffset();
		if (contents != null) {
			stack.pushPose();
			stack.scale(0.75F, 0.75F, 0.75F);
			stack.translate(-0.5F, (float) (displayOffset - 8) / 16, 0.5F);
			stack.mulPose(Axis.YP.rotationDegrees(90));
			updateContents(contents, stack, partialTick);
			stack.popPose();
		}

		stack.scale(-1.0F, -1.0F, 1.0F);
		body.setTransform(stack)
				.setChanged();
	}

	protected void updateContents(TransformedInstance contents, PoseStack stack, float partialTick) {
		contents.setTransform(stack)
				.setChanged();
	}

	public void updateLight() {
		relight(entity.blockPosition(), body, contents);
	}

	@Override
	protected void _delete() {
		super._delete();
		body.delete();
		if (contents != null) {
			contents.delete();
		}
	}

	public static boolean shouldSkipRender(AbstractMinecart minecart) {
		return minecart.getDisplayBlockState().getRenderShape() != RenderShape.ENTITYBLOCK_ANIMATED;
	}
}
