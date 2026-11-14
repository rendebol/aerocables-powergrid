package com.tom.aerocables.compat.cc;

import net.minecraft.world.level.block.Block;

import com.tom.aerocables.AeroCables;
import com.tom.aerocables.common.CableItem;

public class CCCableItem extends CableItem {
	public CCCableItem(Properties properties) {
		super(properties);
	}

	@Override
	protected Block getConnectorBlock() {
		return AeroCables.CC_CABLE.block();
	}
}
