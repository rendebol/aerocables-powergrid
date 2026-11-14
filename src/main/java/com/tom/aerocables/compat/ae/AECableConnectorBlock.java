package com.tom.aerocables.compat.ae;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import com.mojang.serialization.MapCodec;

import com.tom.aerocables.AeroCables;
import com.tom.aerocables.common.CableConnectorBlock;

public class AECableConnectorBlock extends CableConnectorBlock<AECableConnectorBlockEntity> {
	public static final MapCodec<AECableConnectorBlock> CODEC = simpleCodec(AECableConnectorBlock::new);
	public static final BooleanProperty POWERED = BooleanProperty.create("powered");

	public AECableConnectorBlock(Properties properties) {
		super(properties);
	}

	@Override
	public Class<AECableConnectorBlockEntity> getBlockEntityClass() {
		return AECableConnectorBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends AECableConnectorBlockEntity> getBlockEntityType() {
		return AeroCables.AE_CABLE.blockEntityType();
	}

	@Override
	protected MapCodec<? extends DirectionalBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> pBuilder) {
		super.createBlockStateDefinition(pBuilder);
		pBuilder.add(POWERED);
	}
}
