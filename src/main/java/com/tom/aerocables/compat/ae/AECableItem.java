package com.tom.aerocables.compat.ae;

import net.minecraft.world.level.block.Block;

import com.tom.aerocables.AeroCables;
import com.tom.aerocables.common.CableItem;

public class AECableItem extends CableItem {

	public AECableItem(Properties properties) {
		super(properties);
	}

	@Override
	protected Block getConnectorBlock() {
		return AeroCables.AE_CABLE.block();
	}
}
