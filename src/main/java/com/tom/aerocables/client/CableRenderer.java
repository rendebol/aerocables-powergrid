package com.tom.aerocables.client;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3d;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3f;

import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import com.tom.aerocables.common.CableModel;
import com.tom.aerocables.common.ICableConnector;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.simulated_team.simulated.content.blocks.rope.RopeStrandHolderBehavior;
import dev.simulated_team.simulated.content.blocks.rope.rope_winch.RopeWinchBlockEntity;
import dev.simulated_team.simulated.content.blocks.rope.strand.client.ClientRopePoint;
import dev.simulated_team.simulated.content.blocks.rope.strand.client.ClientRopeStrand;
import dev.simulated_team.simulated.content.blocks.rope.strand.client.RopeStrandRenderer;
import dev.simulated_team.simulated.content.blocks.rope.strand.client.RopeStrandRenderer.RopeRenderPoint;
import dev.simulated_team.simulated.index.SimRenderTypes;
import dev.simulated_team.simulated.util.SimMathUtils;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class CableRenderer<T extends BlockEntity & ICableConnector> extends SafeBlockEntityRenderer<T> {
	/** Width of one thin strand in pixels. */
	private static final float STRAND_WIDTH = 1.0f;
	/** Distance between the centers of two thin strands in blocks. */
	private static final double STRAND_SPACING = 1.3 / 16.0;

	private Method buildRenderPoints;

	private final Vector3d controlPointA = new Vector3d();
	private final Vector3d controlPointB = new Vector3d();
	private final Vector3d segmentALerp = new Vector3d();
	private final Vector3d segmentBLerp = new Vector3d();
	private final Vector3d segmentCLerp = new Vector3d();
	private final Vector3d startUp = new Vector3d();
	private final Vector3d endUp = new Vector3d();
	private final Vector3d startLeft = new Vector3d();
	private final Vector3d endLeft = new Vector3d();
	private final Vector3d normalizedNormal = new Vector3d();
	private final Vector3d vertex = new Vector3d();

	public CableRenderer(final BlockEntityRendererProvider.Context context) {
		try {
			buildRenderPoints = RopeStrandRenderer.class.getDeclaredMethod("buildRenderPoints", float.class, List.class);
			buildRenderPoints.setAccessible(true);
		} catch (NoSuchMethodException | SecurityException e) {
			throw new RuntimeException("Can't find method buildRenderPoints in RopeStrandRenderer (Create Simulated)", e);
		}
	}

	@Override
	public boolean shouldRenderOffScreen(final T blockEntity) {
		return true;
	}

	@Override
	public boolean shouldRender(final T blockEntity, final Vec3 cameraPos) {
		return true;
	}

	@Override
	public @NotNull AABB getRenderBoundingBox(@NotNull T blockEntity) {
		if (blockEntity instanceof ICustomRenderBox cbe)
			return cbe.getRenderBoundingBox();

		return super.getRenderBoundingBox(blockEntity);
	}

	@Override
	protected void renderSafe(T be, float partialTicks, PoseStack ps,
			MultiBufferSource bufferSource, int light, int overlay) {
		RopeStrandHolderBehavior ropeHolder = be.getRope();
		if (ropeHolder == null)return;
		ICableConnector otherBe = be.getOther();
		if (otherBe == null)return;

		final Level level = be.getLevel();
		assert level != null;

		final BlockPos ownerPos = be.getBlockPos();

		final SubLevel subLevel = Sable.HELPER.getContaining(be);
		Pose3dc containingPose = null;

		if (subLevel instanceof final ClientSubLevel clientSubLevel) {
			containingPose = clientSubLevel.renderPose();
		}

		final ClientRopeStrand rope = ropeHolder.getClientStrand();

		CableModel models = be.cableModels();

		final SuperByteBuffer middle = CachedBuffers.partialFacing(models.cable(), AllBlocks.ROPE.getDefaultState(), Direction.NORTH);

		if (ropeHolder.ownsRope() && rope != null && ropeHolder.getClientStrand() != null) {
			final ClientRopeStrand clientStrand = ropeHolder.getClientStrand();

			final List<ClientRopePoint> points = clientStrand.getPoints();

			if (points.size() <= 1) {
				return;
			}

			final ObjectArrayList<RopeRenderPoint> ropeRenderPoints = buildRenderPoints(partialTicks, points);

			if (ropeRenderPoints.isEmpty()) return;

			float rot = be.getRopePositionUnique() * 90;
			Quaternionf norm = new Quaternionf().fromAxisAngleDeg(0, 1, 0, rot);
			Quaternionf alt = new Quaternionf().fromAxisAngleDeg(0, 1, 0, rot + 180);
			ps.pushPose();

			boolean isWinch = ropeHolder.blockEntity instanceof RopeWinchBlockEntity;

			if (ropeRenderPoints.size() > (isWinch ? 2 : 3)) {
				RopeRenderPoint start = ropeRenderPoints.get(1);
				RopeRenderPoint end = ropeRenderPoints.get(ropeRenderPoints.size() - 2);

				if (ropeHolder.blockEntity instanceof RopeWinchBlockEntity w) {
					Direction winchFacing = w.getBlockState().getValue(DirectionalAxisKineticBlock.FACING);
					Vector3d pos = JOMLConversion.atCenterOf(w.getBlockPos());
					pos.sub(winchFacing.step().mul(0.5f));

					Vec3i dir = be.getRopeOffset();
					Quaternionf quat = new Quaternionf().lookAlong(new Vector3f(dir.getX(), dir.getY(), dir.getZ()), winchFacing.step());

					if (containingPose != null) {
						containingPose.transformPosition(pos);
					}

					start = new RopeRenderPoint(quat, pos);
				}

				final ClientSubLevel otherSubLevel = Sable.HELPER.getContainingClient((BlockEntity) otherBe);
				final Pose3dc otherRenderPose = otherSubLevel != null ? otherSubLevel.renderPose() : null;

				final ResourceLocation[] strands = models.strandTextures();
				if (strands == null || strands.length == 0) {
					final VertexConsumer buffer = bufferSource.getBuffer(SimRenderTypes.spring(models.cableTexture()));
					renderSplineToConnector(ps, buffer, ownerPos, be, start, light, false, containingPose, containingPose, rot, 0.0, 1.9f);

					renderSplineToConnector(ps, buffer, ownerPos, otherBe, end, light, true, containingPose, otherRenderPose, rot, 0.0, 1.9f);
				} else {
					// several thin wires side by side (e.g. Power Grid + and -)
					for (int s = 0; s < strands.length; s++) {
						final double sideOffset = (s - (strands.length - 1) / 2.0) * STRAND_SPACING;
						final VertexConsumer buffer = bufferSource.getBuffer(SimRenderTypes.spring(strands[s]));
						renderSplineToConnector(ps, buffer, ownerPos, be, start, light, false, containingPose, containingPose, rot, sideOffset, STRAND_WIDTH);

						renderSplineToConnector(ps, buffer, ownerPos, otherBe, end, light, true, containingPose, otherRenderPose, rot, sideOffset, STRAND_WIDTH);
					}
				}
			}

			final VertexConsumer vb = bufferSource.getBuffer(RenderType.solid());

			int rotateSeg = ropeRenderPoints.size() % 2;

			for (int i = isWinch ? 1 : 2; i < ropeRenderPoints.size() - 1; i++) {
				final RopeRenderPoint renderPoint0 = ropeRenderPoints.get(i - 1);
				final RopeRenderPoint renderPoint1 = ropeRenderPoints.get(i);
				final Vector3d globalRenderPos = new Vector3d(renderPoint0.position());
				final Vector3d renderPos = renderPoint0.position();
				final Quaternionf orientation = renderPoint0.orientation();

				final double length = renderPoint1.position().distance(renderPoint0.position());

				if (containingPose != null) {
					containingPose.transformPositionInverse(renderPos);
					orientation.premul(new Quaternionf(containingPose.orientation()).conjugate());
				}

				ps.pushPose();
				ps.translate(renderPos.x - (ownerPos.getX()), renderPos.y - (ownerPos.getY()), renderPos.z - (ownerPos.getZ()));
				ps.mulPose(orientation);

				if (i % 2 == rotateSeg)
					ps.mulPose(alt);
				else
					ps.mulPose(norm);

				ps.translate(-0.5, -0.5, -0.5);
				final BlockPos pos = BlockPos.containing(globalRenderPos.x, globalRenderPos.y, globalRenderPos.z);
				final int worldLight = LevelRenderer.getLightColor(level, pos);

				ps.translate(0.0, 0.5, 0.0);
				ps.scale(1.0f, (float) length, 1.0f);

				middle.light(worldLight)
				.renderInto(ps, vb);
				ps.popPose();
			}

			ps.popPose();
		}
	}

	private void renderSplineToConnector(PoseStack ps, VertexConsumer buffer, BlockPos thisPos, ICableConnector be, RopeRenderPoint ropeEnd, int light, boolean transformToTarget, Pose3dc renderPose, Pose3dc otherRenderPose, float rot, double sideOffset, float width) {
		final Direction facing = be.getFacing();
		final Vector3d normalA = JOMLConversion.atLowerCornerOf(facing.getNormal());

		Vector3d renderPos = new Vector3d(ropeEnd.position());
		Quaternionf orientation = new Quaternionf(ropeEnd.orientation());
		if (renderPose != null) {
			renderPose.transformPositionInverse(renderPos);
			orientation.premul(new Quaternionf(renderPose.orientation()).conjugate());
		}

		orientation.rotateAxis((float) Math.toRadians(rot), 0, transformToTarget ? 1 : -1, 0);

		final Vector3dc normalB = orientation.transform(new Vector3d(0, transformToTarget ? 1 : -1, 0));

		final BlockPos blockPos = be.getPosition();

		final Vector3d center = JOMLConversion.atCenterOf(blockPos);
		float d = 1 / 16f;
		final Vector3dc centerOff = orientation.transform(transformToTarget ?
				new Vector3d(1.1 * d, 0, 2 * d) :
					new Vector3d(-d * 1.25, d, -d * 2)
				);
		final Vector3dc otherCenter = renderPos.add(centerOff);

		if (otherRenderPose != null) {
			otherRenderPose.transformNormal(normalA);
			otherRenderPose.transformPosition(center);
		}

		if (renderPose != null) {
			renderPose.transformNormalInverse(normalA);
			renderPose.transformPositionInverse(center);
		}

		final List<SplinePoint> splinePoints = this.generateSpline(
				JOMLConversion.ZERO,
				otherCenter.sub(center, new Vector3d()),
				normalA,
				normalB,
				center.distance(otherCenter) / 5.0 + 0.25
				);

		ps.pushPose();

		if (transformToTarget) {
			Vector3d thisVec = JOMLConversion.atLowerCornerOf(thisPos);
			ps.translate(center.x() - thisVec.x(), center.y() - thisVec.y(), center.z() - thisVec.z());
		} else {
			ps.translate(center.x() - (blockPos.getX()), center.y() - (blockPos.getY()), center.z() - (blockPos.getZ()));
		}

		final int totalPoints = splinePoints.size();

		double runningSpringLength = 0.0;

		final Vector3d pointNormal = new Vector3d();

		pointNormal.set(splinePoints.getFirst().normal);

		final double twist = 0;

		final Vector3d startUpDir = JOMLConversion.toJOML(this.getUpDirection(facing, otherCenter.sub(center, new Vector3d())));

		final Matrix3d matrix = new Matrix3d(
				startUpDir,
				pointNormal,
				startUpDir.cross(pointNormal, new Vector3d())
				);

		for (int i = 0; i < totalPoints - 1; i++) {
			final SplinePoint point = splinePoints.get(i);
			final SplinePoint nextPoint = splinePoints.get(i + 1);

			final Vector3dc upDir = matrix.getColumn(0, new Vector3d());

			matrix.rotateLocal(
					SimMathUtils.getQuaternionfFromVectorRotation(point.normal, nextPoint.normal)
					);
			matrix.rotateY(-twist / (totalPoints - 1));

			final Vector3dc nextUpDir = matrix.getColumn(0, new Vector3d());

			final double length = point.point.distance(nextPoint.point);

			final float textureWidth = 16f;

			int color = 0;

			this.renderSegment(ps,
					point.normal,
					nextPoint.normal,
					upDir,
					nextUpDir,
					point.point,
					nextPoint.point,
					false,
					(float) runningSpringLength,
					(float) (runningSpringLength + length),
					light,
					color,
					buffer,
					width,
					textureWidth,
					sideOffset);

			// render inside
			this.renderSegment(ps,
					point.normal.negate(new Vector3d()),
					nextPoint.normal.negate(new Vector3d()),
					upDir.negate(new Vector3d()),
					nextUpDir.negate(new Vector3d()),
					point.point,
					nextPoint.point,
					true,
					0.0f - (float) runningSpringLength,
					0.0f - (float) (runningSpringLength + length),
					light,
					color,
					buffer,
					width,
					textureWidth,
					sideOffset);
			runningSpringLength += length;
		}

		ps.popPose();
	}

	private Vec3 getUpDirection(final Direction facing, final Vector3dc directionToSpring) {
		final Vec3 normal = Vec3.atLowerCornerOf(facing.getNormal());
		final double dot = directionToSpring.dot(normal.x, normal.y, normal.z);
		final Vector3d dir = directionToSpring.sub(normal.x * dot, normal.y * dot, normal.z * dot, new Vector3d());

		if (dir.lengthSquared() < 1e-6) {
			// default to north or up if we're in a configuration where we can't pick anything
			return facing.getAxis().isHorizontal() ? new Vec3(0, 1, 0) : new Vec3(0, 0, -1);
		}

		return Vec3.atLowerCornerOf(Direction.getNearest(dir.x, dir.y, dir.z).getOpposite().getNormal());
	}

	@SuppressWarnings("unchecked")
	private ObjectArrayList<RopeRenderPoint> buildRenderPoints(final float partialTick, final List<ClientRopePoint> inputPoints) {
		try {
			return (ObjectArrayList<RopeRenderPoint>) buildRenderPoints.invoke(null, partialTick, inputPoints);
		} catch (IllegalAccessException | InvocationTargetException | ClassCastException e) {
			return null;
		}
	}

	private void renderSegment(final PoseStack ms,
			final Vector3dc startDirection,
			final Vector3dc endDirection,
			final Vector3dc inputStartUp,
			final Vector3dc inputEndUp,
			final Vector3dc startPosIn,
			final Vector3dc endPosIn,
			final boolean second,
			final float uvStart,
			final float uvEnd,
			final int light,
			final int color,
			final VertexConsumer a,
			final float width,
			final float textureWidth,
			final double sideOffset) {
		inputStartUp.cross(startDirection, this.startLeft).normalize();
		inputEndUp.cross(endDirection, this.endLeft).normalize();

		// shift the whole strand sideways (used to draw several thin wires next to each other)
		final Vector3d startPos = new Vector3d(startPosIn).fma(sideOffset, this.startLeft);
		final Vector3d endPos = new Vector3d(endPosIn).fma(sideOffset, this.endLeft);

		final float texW = width / textureWidth;
		final double scale = width / 16.0 / 2.0;

		this.startLeft.mul(scale);
		inputStartUp.mul(scale, this.startUp);
		this.endLeft.mul(scale);
		inputEndUp.mul(scale, this.endUp);

		final Vector3d startDown = this.startUp.negate(new Vector3d());
		final Vector3d endDown = this.endUp.negate(new Vector3d());
		final Vector3d startRight = this.startLeft.negate(new Vector3d());
		final Vector3d endRight = this.endLeft.negate(new Vector3d());

		final float uvScale = 16.0f / textureWidth;
		final float uvXOffset = second ? width / textureWidth : 0.0f;
		this.vert(ms, a, startPos.add(this.startLeft, this.vertex).sub(this.startUp), color, 0.0f + uvXOffset, uvStart * uvScale, startDown, light);
		this.vert(ms, a, endPos.add(this.endLeft, this.vertex).sub(this.endUp), color, 0.0f + uvXOffset, uvEnd * uvScale, endDown, light);
		this.vert(ms, a, endPos.sub(this.endLeft, this.vertex).sub(this.endUp), color, texW + uvXOffset, uvEnd * uvScale, endDown, light);
		this.vert(ms, a, startPos.sub(this.startLeft, this.vertex).sub(this.startUp), color, texW + uvXOffset, uvStart * uvScale, startDown, light);

		this.vert(ms, a, startPos.sub(this.startLeft, this.vertex).add(this.startUp), color, 0.0f + uvXOffset, uvStart * uvScale, this.startUp, light);
		this.vert(ms, a, endPos.sub(this.endLeft, this.vertex).add(this.endUp), color, 0.0f + uvXOffset, uvEnd * uvScale, this.endUp, light);
		this.vert(ms, a, endPos.add(this.endLeft, this.vertex).add(this.endUp), color, texW + uvXOffset, uvEnd * uvScale, this.endUp, light);
		this.vert(ms, a, startPos.add(this.startLeft, this.vertex).add(this.startUp), color, texW + uvXOffset, uvStart * uvScale, this.startUp, light);

		this.vert(ms, a, startPos.sub(this.startLeft, this.vertex).sub(this.startUp), color, 0.0f + uvXOffset, uvStart * uvScale, startRight, light);
		this.vert(ms, a, endPos.sub(this.endLeft, this.vertex).sub(this.endUp), color, 0.0f + uvXOffset, uvEnd * uvScale, endRight, light);
		this.vert(ms, a, endPos.sub(this.endLeft, this.vertex).add(this.endUp), color, texW + uvXOffset, uvEnd * uvScale, endRight, light);
		this.vert(ms, a, startPos.sub(this.startLeft, this.vertex).add(this.startUp), color, texW + uvXOffset, uvStart * uvScale, startRight, light);

		this.vert(ms, a, startPos.add(this.startLeft, this.vertex).add(this.startUp), color, 0.0f + uvXOffset, uvStart * uvScale, this.startLeft, light);
		this.vert(ms, a, endPos.add(this.endLeft, this.vertex).add(this.endUp), color, 0.0f + uvXOffset, uvEnd * uvScale, this.endLeft, light);
		this.vert(ms, a, endPos.add(this.endLeft, this.vertex).sub(this.endUp), color, texW + uvXOffset, uvEnd * uvScale, this.endLeft, light);
		this.vert(ms, a, startPos.add(this.startLeft, this.vertex).sub(this.startUp), color, texW + uvXOffset, uvStart * uvScale, this.startLeft, light);
	}

	private void vert(final PoseStack ms, final VertexConsumer a, final Vector3dc pos, final int color, final float u1, final float v1, final Vector3dc normal, final int light) {
		normal.normalize(this.normalizedNormal);
		a.addVertex(ms.last().pose(), (float) pos.x(), (float) pos.y(), (float) pos.z())
		.setColor(color)
		.setUv(u1, v1)
		.setLight(light)
		.setNormal(ms.last(), (float) this.normalizedNormal.x(), (float) this.normalizedNormal.y(), (float) this.normalizedNormal.z());
	}

	private List<SplinePoint> generateSpline(final Vector3dc pointA, final Vector3dc pointB, final Vector3dc normalA, final Vector3dc normalB, final double controlPointLength) {
		final List<SplinePoint> list = new ObjectArrayList<>();

		final double influence = controlPointLength;
		pointA.fma(influence, normalA, this.controlPointA);
		pointB.fma(influence, normalB, this.controlPointB);

		final double len = pointA.distance(pointB);
		final int initialPointCount = Mth.clamp(Mth.ceil(len), 5, 8);
		for (int i = 0; i <= initialPointCount; i++) {
			final double t = (double) i / initialPointCount;
			pointA.lerp(this.controlPointA, t, this.segmentALerp);
			this.controlPointA.lerp(this.controlPointB, t, this.segmentBLerp);
			this.controlPointB.lerp(pointB, t, this.segmentCLerp);

			final Vector3d point = new Vector3d(this.segmentALerp
					.lerp(this.segmentBLerp, t)
					.lerp(this.segmentBLerp.lerp(this.segmentCLerp, t), t));

			final Vector3d normal = new Vector3d();

			if (list.isEmpty()) {
				normal.set(normalA);
			} else if (list.size() == initialPointCount) {
				normal.set(normalB).negate();
			} else {
				point.sub(list.get(list.size() - 1).point, normal).normalize();
			}

			list.add(new SplinePoint(point, normal));
		}

		return list;
	}

	record SplinePoint(Vector3dc point, Vector3dc normal) {
	}
}
