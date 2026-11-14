package com.tom.aerocables.compat.powergrid;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import com.tom.aerocables.common.CableConnectorBlock;

import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;

import dev.simulated_team.simulated.content.blocks.util.AbstractDirectionalAxisBlock;

/**
 * Terminal (+ / -) hitboxes of the Power Grid cable connector.
 *
 * The boxes are defined in the frame of the connector block model
 * (models/block/pg_connector/horizontal.json, pixels, model faces south)
 * and rotated exactly like the blockstate rotates the model
 * (same rules as Simulated's directionalAxisBlock blockstate generator),
 * so the clickable terminals always line up with the visible studs.
 */
public final class PGTerminals {
	public static final int POSITIVE = 0;
	public static final int NEGATIVE = 1;
	public static final int COUNT = 2;

	// minX, minY, minZ, maxX, maxY, maxZ in model pixels (horizontal model variant)
	private static final double[] POSITIVE_BOX = {2.5, 6.5, 0.5, 5.0, 9.5, 3.5};
	private static final double[] NEGATIVE_BOX = {11.0, 6.5, 0.5, 13.5, 9.5, 3.5};

	private PGTerminals() {
	}

	public static TerminalBoundingBox[] forState(BlockState state) {
		Direction facing = state.getValue(CableConnectorBlock.FACING);
		boolean alongFirst = state.getValue(AbstractDirectionalAxisBlock.AXIS_ALONG_FIRST_COORDINATE);

		// Same logic as the blockstate generator (NeoForgeSimBlockStateService#directionalAxisBlock)
		boolean vertical = facing.getAxis().isHorizontal() && (facing.getAxis() == Direction.Axis.X) == alongFirst;
		int xRot = facing == Direction.DOWN ? 270 : facing == Direction.UP ? 90 : 0;
		int yRot = facing.getAxis().isVertical() ? (alongFirst ? 0 : 90) : (int) facing.toYRot();

		TerminalBoundingBox[] terminals = new TerminalBoundingBox[COUNT];
		terminals[POSITIVE] = new TerminalBoundingBox(IDecoratedTerminal.POSITIVE, transform(POSITIVE_BOX, vertical, xRot, yRot))
				.withColor(IDecoratedTerminal.RED);
		terminals[NEGATIVE] = new TerminalBoundingBox(IDecoratedTerminal.NEGATIVE, transform(NEGATIVE_BOX, vertical, xRot, yRot))
				.withColor(IDecoratedTerminal.BLUE);
		return terminals;
	}

	/**
	 * Rotates a model-space box (pixels) into block space (0..1) the same way Minecraft rotates
	 * a block model: optional 90 degree turn around Z for the "vertical" variant, then the
	 * blockstate X rotation, then the blockstate Y rotation.
	 */
	private static AABB transform(double[] box, boolean vertical, int xRot, int yRot) {
		double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY, minZ = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;

		double ax = Math.toRadians(-xRot);
		double cx = Math.cos(ax), sx = Math.sin(ax);
		double ay = Math.toRadians(-yRot);
		double cy = Math.cos(ay), sy = Math.sin(ay);

		for (int i = 0; i < 8; i++) {
			double x = ((i & 1) == 0 ? box[0] : box[3]) / 16.0 - 0.5;
			double y = ((i & 2) == 0 ? box[1] : box[4]) / 16.0 - 0.5;
			double z = ((i & 4) == 0 ? box[2] : box[5]) / 16.0 - 0.5;

			if (vertical) {
				// 90 degrees around Z: (x, y) -> (-y, x)
				double t = x;
				x = -y;
				y = t;
			}

			// around X
			double y2 = y * cx - z * sx;
			double z2 = y * sx + z * cx;
			y = y2;
			z = z2;

			// around Y
			double x3 = x * cy + z * sy;
			double z3 = -x * sy + z * cy;
			x = x3;
			z = z3;

			minX = Math.min(minX, x);
			minY = Math.min(minY, y);
			minZ = Math.min(minZ, z);
			maxX = Math.max(maxX, x);
			maxY = Math.max(maxY, y);
			maxZ = Math.max(maxZ, z);
		}

		return new AABB(round(minX + 0.5), round(minY + 0.5), round(minZ + 0.5),
				round(maxX + 0.5), round(maxY + 0.5), round(maxZ + 0.5));
	}

	private static double round(double v) {
		return Math.round(v * 1024.0) / 1024.0;
	}
}
