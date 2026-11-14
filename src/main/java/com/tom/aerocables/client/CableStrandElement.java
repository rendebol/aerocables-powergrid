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
import net.createmod.ponder.api.element.AnimatedSceneElement;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.foundation.element.AnimatedSceneElementBase;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock;

import com.tom.aerocables.common.CableConnectorBlock;
import com.tom.aerocables.common.CableModel;

import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.simulated_team.simulated.content.blocks.rope.strand.client.RopeStrandRenderer;
import dev.simulated_team.simulated.content.blocks.rope.strand.client.RopeStrandRenderer.RopeRenderPoint;
import dev.simulated_team.simulated.index.SimRenderTypes;
import dev.simulated_team.simulated.ponder.elements.rope.PonderRopePose;
import dev.simulated_team.simulated.ponder.elements.rope.RopeStrandElement;
import dev.simulated_team.simulated.util.SimMathUtils;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class CableStrandElement extends AnimatedSceneElementBase implements AnimatedSceneElement {
	private Method buildRenderPoints;
	private final RopeStrandElement rope;
	private final CableModel models;
	private final BlockPos ownerPos;
	private final BlockPos endPos;
	private BlockPos startRenderPos;
	private BlockPos endRenderPos;
	private BlockPos winch;

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

	public CableStrandElement(RopeStrandElement rope, CableModel models, BlockPos ownerPos, BlockPos endPos) {
		this.rope = rope;
		this.models = models;
		this.ownerPos = ownerPos;
		this.endPos = endPos;
		this.startRenderPos = ownerPos;
		this.endRenderPos = endPos;

		try {
			buildRenderPoints = RopeStrandElement.class.getDeclaredMethod("buildRenderPoints", float.class, List.class);
			buildRenderPoints.setAccessible(true);
		} catch (NoSuchMethodException | SecurityException e) {
			throw new RuntimeException("Can't find method buildRenderPoints in RopeStrandRenderer (Create Simulated)", e);
		}
	}

	public void setWinch(BlockPos winch) {
		this.winch = winch;
	}

	public void setStartRenderPos(BlockPos startRenderPos, BlockPos endRenderPos) {
		this.startRenderPos = startRenderPos;
		this.endRenderPos = endRenderPos;
	}

	@Override
	protected void renderLast(PonderLevel world, MultiBufferSource bufferSource, GuiGraphics graphics, float fade, float pt) {
		final SuperByteBuffer middle = CachedBuffers.partialFacing(models.cable(), AllBlocks.ROPE.getDefaultState(), Direction.NORTH);

		final PoseStack ps = graphics.pose();

		final PonderRopePose currentPose = new PonderRopePose();
		currentPose.set(rope.lastPose);
		currentPose.lerp(rope.pose, pt);

		final List<Vector3d> points = new ObjectArrayList<>();

		final Vector3d currentPos = new Vector3d();
		final int knots = (int) Math.ceil(currentPose.length) + 1;
		final double extra = currentPose.length - knots;
		for (int i = 0; i < knots; i++) {
			final double t = (1.0d - i / (knots + extra));

			final Vector3d pos = currentPose.start.lerp(currentPose.end, Math.max(0, t), currentPos);

			final double y = Math.pow(t - .5, 2) * 4;
			pos.sub(0, Mth.clamp(1.0 - y, 0, 1) * currentPose.sog, 0);

			pos.set(pos.x, Math.max(pos.y, currentPose.floorHeight), pos.z);

			points.add(new Vector3d(pos));
		}

		final ObjectArrayList<RopeStrandRenderer.RopeRenderPoint> renderPoints = buildRenderPoints(pt, points);

		boolean isWinch = winch != null;

		if (renderPoints.size() > (isWinch ? 2 : 3)) {
			RopeRenderPoint start = renderPoints.get(1);
			RopeRenderPoint end = renderPoints.get(renderPoints.size() - 2);

			if (isWinch) {
				Direction winchFacing = world.getBlockState(winch).getValue(DirectionalAxisKineticBlock.FACING);
				Vector3d pos = JOMLConversion.atCenterOf(winch);
				pos.sub(winchFacing.step().mul(0.5f));

				Vec3i dir = new Vec3i(0, -1, 0);
				Quaternionf quat = new Quaternionf().lookAlong(new Vector3f(dir.getX(), dir.getY(), dir.getZ()), winchFacing.step());

				end = new RopeRenderPoint(quat, pos);
			}

			ps.pushPose();
			ps.translate(startRenderPos.getX(), startRenderPos.getY(), startRenderPos.getZ());
			final VertexConsumer buffer = bufferSource.getBuffer(SimRenderTypes.spring(models.cableTexture()));
			renderSplineToConnector(world, ps, buffer, end, false);

			renderSplineToConnector(world, ps, buffer, start, true);
			ps.popPose();
		}

		final VertexConsumer vb = bufferSource.getBuffer(RenderType.solid());

		Quaternionf alt = new Quaternionf().fromAxisAngleDeg(0, 1, 0, 180);

		ps.pushPose();
		this.applyFade(ps, pt);
		ps.translate(currentPose.start.x, currentPose.start.y, currentPose.start.z);
		for (int i = 2; i < renderPoints.size() - (isWinch ? 0 : 1); i++) {
			final RopeStrandRenderer.RopeRenderPoint renderPoint0 = renderPoints.get(i - 1);
			final RopeStrandRenderer.RopeRenderPoint renderPoint1 = renderPoints.get(i);
			final Vector3d renderPos = renderPoint0.position();
			final Quaternionf orientation = renderPoint0.orientation();

			final double length = renderPoint1.position().distance(renderPoint0.position());

			ps.pushPose();
			ps.translate(renderPos.x - currentPose.start.x, renderPos.y - currentPose.start.y, renderPos.z - currentPose.start.z);
			ps.mulPose(orientation);

			if (i % 2 == 0)
				ps.mulPose(alt);

			ps.translate(-0.5, -0.5, -0.5);
			final int worldLight = LightTexture.FULL_BLOCK;

			ps.pushPose();
			ps.translate(0.0, 0.5, 0.0);
			ps.scale(1.0f, (float) length, 1.0f);

			middle.light(worldLight)
			.renderInto(ps, vb);
			ps.popPose();

			if(renderPoint1 == renderPoints.getLast()) {
				ps.translate(0, length, 0);
			}

			ps.popPose();
		}
		ps.popPose();
	}

	private void renderSplineToConnector(PonderLevel level, PoseStack ps, VertexConsumer buffer, RopeRenderPoint ropeEnd, boolean transformToTarget) {
		final BlockPos blockPos = transformToTarget ? endPos : ownerPos;
		final BlockPos blockRenderPos = transformToTarget ? endRenderPos : startRenderPos;
		final Direction facing = level.getBlockState(blockPos).getValue(CableConnectorBlock.FACING);
		final Vector3d normalA = JOMLConversion.atLowerCornerOf(facing.getNormal());

		Vector3d renderPos = new Vector3d(ropeEnd.position());
		Quaternionf orientation = new Quaternionf(ropeEnd.orientation());

		final Vector3dc normalB = orientation.transform(new Vector3d(0, transformToTarget ? 1 : -1, 0));

		final Vector3d center = JOMLConversion.atCenterOf(blockRenderPos);
		float d = 1 / 16f;
		final Vector3dc centerOff = orientation.transform(transformToTarget ?
				new Vector3d(1.1 * d, 0, 2 * d) :
					new Vector3d(-d * 1.25, d, -d * 2)
				);
		final Vector3dc otherCenter = renderPos.add(centerOff);

		final List<SplinePoint> splinePoints = this.generateSpline(
				JOMLConversion.ZERO,
				otherCenter.sub(center, new Vector3d()),
				normalA,
				normalB,
				center.distance(otherCenter) / 5.0 + 0.25
				);

		ps.pushPose();

		if (transformToTarget) {
			Vector3d thisVec = JOMLConversion.atLowerCornerOf(ownerPos);
			ps.translate(center.x() - thisVec.x(), center.y() - thisVec.y(), center.z() - thisVec.z());
		} else {
			ps.translate(center.x() - (blockRenderPos.getX()), center.y() - (blockRenderPos.getY()), center.z() - (blockRenderPos.getZ()));
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

		int light = LightTexture.FULL_BLOCK;

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

			final float width = 1.9f;

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
					textureWidth);

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
					textureWidth);
			runningSpringLength += length;
		}

		ps.popPose();
	}

	@SuppressWarnings("unchecked")
	private @NotNull ObjectArrayList<RopeStrandRenderer.RopeRenderPoint> buildRenderPoints(final float partialTick, final List<Vector3d> inputPoints) {
		try {
			return (ObjectArrayList<RopeRenderPoint>) buildRenderPoints.invoke(null, partialTick, inputPoints);
		} catch (IllegalAccessException | InvocationTargetException | ClassCastException e) {
			return null;
		}
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

	private void renderSegment(final PoseStack ms,
			final Vector3dc startDirection,
			final Vector3dc endDirection,
			final Vector3dc inputStartUp,
			final Vector3dc inputEndUp,
			final Vector3dc startPos,
			final Vector3dc endPos,
			final boolean second,
			final float uvStart,
			final float uvEnd,
			final int light,
			final int color,
			final VertexConsumer a,
			final float width,
			final float textureWidth) {
		inputStartUp.cross(startDirection, this.startLeft).normalize();
		inputEndUp.cross(endDirection, this.endLeft).normalize();

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
