package com.tom.aerocables.compat.powergrid;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import com.tom.aerocables.AeroCables;
import com.tom.aerocables.Config;
import com.tom.aerocables.common.CableItem;

public class PGCableItem extends CableItem {

	public PGCableItem(Properties properties) {
		super(properties);
	}

	@Override
	protected Block getConnectorBlock() {
		return AeroCables.PG_CABLE.block();
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents,
			TooltipFlag tooltipFlag) {
		tooltipComponents.add(Component.translatable("tooltip.aerocables.pgCable").withStyle(ChatFormatting.GRAY));
		tooltipComponents.add(Component.translatable("tooltip.aerocables.pgCableResistance",
				String.format("%.4f", Config.get().pgCableResistance)).withStyle(ChatFormatting.DARK_GRAY));
	}
}
