package com.tom.aerocables.compat.rs;

import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import com.refinedmods.refinedstorage.neoforge.api.RefinedStorageNeoForgeApi;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.providers.RegistrateRecipeProvider;

import com.tom.aerocables.common.AbstractCableType;
import com.tom.aerocables.common.CommonRegistration;

public class RSCableType extends AbstractCableType<RSCableConnectorBlockEntity>{

	@Override
	public void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(RefinedStorageNeoForgeApi.INSTANCE.getNetworkNodeContainerProviderCapability(), blockEntity.get(), (be, _v) -> be.getContainerProvider());
	}

	@Override
	public void register(CreateRegistrate registrate) {
		super.register(registrate);
		block = block(registrate, RSCableConnectorBlock::new)
				.lang("RS Network Connector")
				.register();
		blockEntity = blockEntity(registrate, RSCableConnectorBlockEntity::new)
				.register();
		item = item(registrate, RSCableItem::new)
				.lang("RS Cable Spool")
				.recipe((c, p) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, c.get(), 1)
						.pattern("CCC")
						.pattern("C-C")
						.pattern("CCC")
						.define('C', com.refinedmods.refinedstorage.common.content.Tags.CABLES)
						.define('-', CommonRegistration.CABLE)
						.unlockedBy("has_ingredient", RegistrateRecipeProvider.has(CommonRegistration.CABLE))
						.save(p))
				.register();
	}

	@Override
	protected String prefix() {
		return "rs";
	}
}
