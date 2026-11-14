package com.tom.aerocables.compat.fe;

import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.Tags;

import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.providers.RegistrateRecipeProvider;

import com.tom.aerocables.common.AbstractCableType;
import com.tom.aerocables.common.CommonRegistration;

public class FECableType extends AbstractCableType<FECableConnectorBlockEntity> {

	@Override
	public void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, blockEntity.get(), FECableConnectorBlockEntity::getEnergyStorage);
	}

	@Override
	public void register(CreateRegistrate registrate) {
		super.register(registrate);
		block = block(registrate, FECableConnectorBlock::new)
				.lang("Energy Cable Connector")
				.register();
		blockEntity = blockEntity(registrate, FECableConnectorBlockEntity::new)
				.register();
		item = item(registrate, FECableItem::new)
				.lang("Energy Cable Spool")
				.recipe((c, p) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, c.get(), 1)
						.pattern("CRC")
						.pattern("R-R")
						.pattern("CRC")
						.define('R', Tags.Items.DUSTS_REDSTONE)
						.define('C', AllItems.COPPER_SHEET)
						.define('-', CommonRegistration.CABLE)
						.unlockedBy("has_ingredient", RegistrateRecipeProvider.has(CommonRegistration.CABLE))
						.save(p))
				.register();

		registrate.addRawLang("tooltip.aerocables.feCableTransferRate", "Transfer Rate: %d FE/t");
		registrate.addRawLang("config.aerocables.fe_cable_transfer", "Energy Cable Transfer Rate (FE/t)");
	}

	@Override
	protected String prefix() {
		return "fe";
	}
}
