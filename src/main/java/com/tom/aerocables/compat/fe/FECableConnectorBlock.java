package com.tom.aerocables.compat.fe;

import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;

import com.mojang.serialization.MapCodec;

import com.tom.aerocables.AeroCables;
import com.tom.aerocables.common.CableConnectorBlock;

public class FECableConnectorBlock extends CableConnectorBlock<FECableConnectorBlockEntity> {
	public static final MapCodec<FECableConnectorBlock> CODEC = simpleCodec(FECableConnectorBlock::new);

	public FECableConnectorBlock(Properties properties) {
		super(properties);
	}

	@Override
	public Class<FECableConnectorBlockEntity> getBlockEntityClass() {
		return FECableConnectorBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends FECableConnectorBlockEntity> getBlockEntityType() {
		return AeroCables.FE_CABLE.blockEntityType();
	}

	@Override
	protected MapCodec<? extends DirectionalBlock> codec() {
		return CODEC;
	}
}
