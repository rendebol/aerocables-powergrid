package com.tom.aerocables.common;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.simibubi.create.foundation.block.IBE;

import dev.ryanhcode.sable.api.block.BlockSubLevelAssemblyListener;
import dev.simulated_team.simulated.content.blocks.util.AbstractDirectionalAxisBlock;
import dev.simulated_team.simulated.util.DirectionalAxisShaper;

public abstract class CableConnectorBlock<T extends BlockEntity & ICableConnector> extends AbstractDirectionalAxisBlock implements IBE<T>, BlockSubLevelAssemblyListener {
	private static final DirectionalAxisShaper SHAPE = DirectionalAxisShaper.make(Block.box(5, 0, 5, 11, 7, 11));

	public CableConnectorBlock(Properties properties) {
		super(properties);
	}

	@Override
	public void onRemove(final BlockState pState, final Level pLevel, final BlockPos pPos, final BlockState pNewState, final boolean pIsMoving) {
		IBE.onRemove(pState, pLevel, pPos, pNewState);
	}

	@Override
	public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
		return CommonRegistration.CONNECTOR.asStack();
	}

	@Override
	public void beforeMove(ServerLevel originLevel, ServerLevel resultingLevel, BlockState newState, BlockPos oldPos,
			BlockPos newPos) {
		withBlockEntityDo(originLevel, oldPos, be -> {
			be.connectorMoving(newPos);
		});
	}

	@Override
	public void afterMove(ServerLevel originLevel, ServerLevel resultingLevel, BlockState newState, BlockPos oldPos,
			BlockPos newPos) {
	}

	@Override
	public VoxelShape getShape(final BlockState state, final BlockGetter worldIn, final BlockPos pos, final CollisionContext context) {
		return SHAPE.get(state.getValue(FACING), state.getValue(AXIS_ALONG_FIRST_COORDINATE));
	}
}
