package com.tom.aerocables.compat.fe;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import com.tom.aerocables.AeroCables;
import com.tom.aerocables.Config;
import com.tom.aerocables.common.CableItem;

public class FECableItem extends CableItem {

	public FECableItem(Properties properties) {
		super(properties);
	}

	@Override
	protected Block getConnectorBlock() {
		return AeroCables.FE_CABLE.block();
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents,
			TooltipFlag tooltipFlag) {
		int transfer = Config.get().feCableTransfer;
		tooltipComponents.add(Component.translatable("tooltip.aerocables.feCableTransferRate", transfer).withStyle(ChatFormatting.GRAY));
	}
}
