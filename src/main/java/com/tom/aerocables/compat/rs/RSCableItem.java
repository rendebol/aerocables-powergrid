package com.tom.aerocables.compat.rs;

import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;

import com.tom.aerocables.AeroCables;
import com.tom.aerocables.common.CableItem;
import com.tom.aerocables.common.ICableConnector;

public class RSCableItem extends CableItem {

	public RSCableItem(Properties properties) {
		super(properties);
	}

	@Override
	protected Block getConnectorBlock() {
		return AeroCables.RS_CABLE.block();
	}

	@Override
	protected void setupConnector(ICableConnector cc, UseOnContext context) {
		if (context.getPlayer() != null && cc instanceof RSCableConnectorBlockEntity be) {
			be.setPlacedBy(context.getPlayer().getGameProfile().getId());
		}
	}
}
