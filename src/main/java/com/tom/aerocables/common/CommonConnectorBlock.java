package com.tom.aerocables.common;

import java.util.Arrays;
import java.util.stream.Stream;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.mojang.serialization.MapCodec;

import dev.simulated_team.simulated.content.blocks.util.AbstractDirectionalAxisBlock;
import dev.simulated_team.simulated.util.DirectionalAxisShaper;

public class CommonConnectorBlock extends AbstractDirectionalAxisBlock {
	private static final DirectionalAxisShaper SHAPE = DirectionalAxisShaper.make(Block.box(5, 0, 5, 11, 5, 11));
	public static final MapCodec<CommonConnectorBlock> CODEC = simpleCodec(CommonConnectorBlock::new);

	public CommonConnectorBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected MapCodec<? extends DirectionalBlock> codec() {
		return CODEC;
	}

	@Override
	protected Direction getFacingForPlacement(final BlockPlaceContext context) {
		return context.getClickedFace();
	}

	@Override
	protected boolean getAxisAlignmentForPlacement(final BlockPlaceContext context) {
		return context.getHorizontalDirection()
				.getAxis() != Direction.Axis.X;
	}

	public static Stream<BlockPos> getRopeAttachmentPositions(BlockState state, BlockPos pos) {
		Direction facing = state.getValue(FACING).getOpposite();
		return Arrays.stream(Direction.values()).filter(d -> d != facing).map(pos::relative);
	}

	public static BlockState copy(Block block, BlockState state) {
		return block.defaultBlockState().setValue(FACING, state.getValue(FACING)).
				setValue(AXIS_ALONG_FIRST_COORDINATE, state.getValue(AXIS_ALONG_FIRST_COORDINATE));
	}

	@Override
	public VoxelShape getShape(final BlockState state, final BlockGetter worldIn, final BlockPos pos, final CollisionContext context) {
		return SHAPE.get(state.getValue(FACING), state.getValue(AXIS_ALONG_FIRST_COORDINATE));
	}
}
