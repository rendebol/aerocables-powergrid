package com.tom.aerocables.compat.rs;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import com.mojang.serialization.MapCodec;
import com.refinedmods.refinedstorage.common.support.AbstractBlockEntityTicker;
import com.refinedmods.refinedstorage.common.support.network.NetworkNodeBlockEntityTicker;

import com.tom.aerocables.AeroCables;
import com.tom.aerocables.common.CableConnectorBlock;


public class RSCableConnectorBlock extends CableConnectorBlock<RSCableConnectorBlockEntity> {
	private static final AbstractBlockEntityTicker<RSCableConnectorBlockEntity> TICKER = new NetworkNodeBlockEntityTicker(AeroCables.RS_CABLE::blockEntityType);
	public static final MapCodec<RSCableConnectorBlock> CODEC = simpleCodec(RSCableConnectorBlock::new);

	public RSCableConnectorBlock(Properties properties) {
		super(properties);
	}

	@Override
	public <S extends BlockEntity> BlockEntityTicker<S> getTicker(Level p_153212_, BlockState p_153213_,
			BlockEntityType<S> p_153214_) {
		return TICKER.get(p_153212_, p_153214_);
	}

	@Override
	public Class<RSCableConnectorBlockEntity> getBlockEntityClass() {
		return RSCableConnectorBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends RSCableConnectorBlockEntity> getBlockEntityType() {
		return AeroCables.RS_CABLE.blockEntityType();
	}

	@Override
	protected MapCodec<? extends DirectionalBlock> codec() {
		return CODEC;
	}

	@Override
	public void onRemove(BlockState blockState, Level level, BlockPos pos, BlockState newBlockState, final boolean pIsMoving) {
		if (!blockState.hasBlockEntity())
			return;
		if (blockState.is(newBlockState.getBlock()) && newBlockState.hasBlockEntity())
			return;
		BlockEntity blockEntity = level.getBlockEntity(pos);
		if (blockEntity instanceof RSCableConnectorBlockEntity sbe)
			sbe.destroy();
		level.removeBlockEntity(pos);
	}
}
