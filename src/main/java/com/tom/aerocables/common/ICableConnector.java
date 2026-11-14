package com.tom.aerocables.common;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;

import dev.simulated_team.simulated.content.blocks.rope.RopeStrandHolderBehavior;

public interface ICableConnector {
	void setLink(BlockPos ropeBlock, BlockPos otherSide, boolean hasItem);
	RopeStrandHolderBehavior getRope();
	ICableConnector getOther();
	CableModel cableModels();
	Direction getFacing();
	BlockPos getPosition();
	void connectorMoving(BlockPos newPos);
	Vec3i getRopeOffset();

	default int getRopePositionUnique() {
		Vec3i vec = getRopeOffset();
		switch (getFacing().getAxis()) {
		case Y:
			return (4 + Mth.sign(vec.getX()) + Mth.sign(vec.getZ()) * 2) % 5;

		default:
			return (4 + Mth.sign(vec.getX()) + Mth.sign(vec.getZ()) + Mth.sign(vec.getY()) * 2) % 5;
		}
	}
}
