package com.tom.aerocables.compat.cc;

import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;

import com.mojang.serialization.MapCodec;

import com.tom.aerocables.AeroCables;
import com.tom.aerocables.common.CableConnectorBlock;

public class CCCableConnectorBlock extends CableConnectorBlock<CCCableConnectorBlockEntity> {
	public static final MapCodec<CCCableConnectorBlock> CODEC = simpleCodec(CCCableConnectorBlock::new);

	public CCCableConnectorBlock(Properties properties) {
		super(properties);
	}

	@Override
	public Class<CCCableConnectorBlockEntity> getBlockEntityClass() {
		return CCCableConnectorBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends CCCableConnectorBlockEntity> getBlockEntityType() {
		return AeroCables.CC_CABLE.blockEntityType();
	}

	@Override
	protected MapCodec<? extends DirectionalBlock> codec() {
		return CODEC;
	}
}
