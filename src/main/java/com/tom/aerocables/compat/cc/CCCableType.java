package com.tom.aerocables.compat.cc;

import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.providers.RegistrateRecipeProvider;

import com.tom.aerocables.common.AbstractCableType;
import com.tom.aerocables.common.CommonRegistration;

import dan200.computercraft.api.network.wired.WiredElementCapability;
import dan200.computercraft.shared.ModRegistry;

public class CCCableType extends AbstractCableType<CCCableConnectorBlockEntity> {

	@Override
	public void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(WiredElementCapability.get(), blockEntity.get(), CCCableConnectorBlockEntity::getWiredElement);
	}

	@Override
	public void register(CreateRegistrate registrate) {
		super.register(registrate);
		block = block(registrate, CCCableConnectorBlock::new)
				.lang("CC Network Connector")
				.register();
		blockEntity = blockEntity(registrate, CCCableConnectorBlockEntity::new)
				.register();
		item = item(registrate, CCCableItem::new)
				.lang("CC Cable Spool")
				.recipe((c, p) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, c.get(), 1)
						.pattern("CCC")
						.pattern("C-C")
						.pattern("CCC")
						.define('C', ModRegistry.Items.CABLE.get())
						.define('-', CommonRegistration.CABLE)
						.unlockedBy("has_ingredient", RegistrateRecipeProvider.has(CommonRegistration.CABLE))
						.save(p))
				.register();
	}

	@Override
	protected String prefix() {
		return "cc";
	}
}
