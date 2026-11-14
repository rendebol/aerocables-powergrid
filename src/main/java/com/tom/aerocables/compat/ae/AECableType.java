package com.tom.aerocables.compat.ae;

import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.model.generators.ModelFile;

import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.providers.RegistrateRecipeProvider;

import com.tom.aerocables.common.AbstractCableType;
import com.tom.aerocables.common.CableConnectorBlock;
import com.tom.aerocables.common.CommonRegistration;

import appeng.api.AECapabilities;
import appeng.datagen.providers.tags.ConventionTags;

public class AECableType extends AbstractCableType<AECableConnectorBlockEntity>{

	@Override
	public void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST, blockEntity.get(), (be, _v) -> be);
	}

	@Override
	public void register(CreateRegistrate registrate) {
		super.register(registrate);
		block = block(registrate, AECableConnectorBlock::new)
				.lang("ME Cable Connector")
				.register();
		blockEntity = blockEntity(registrate, AECableConnectorBlockEntity::new)
				.register();
		item = item(registrate, AECableItem::new)
				.lang("ME Cable Spool")
				.recipe((c, p) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, c.get(), 1)
						.pattern("CCC")
						.pattern("C-C")
						.pattern("CCC")
						.define('C', ConventionTags.GLASS_CABLE)
						.define('-', CommonRegistration.CABLE)
						.unlockedBy("has_ingredient", RegistrateRecipeProvider.has(CommonRegistration.CABLE))
						.save(p))
				.register();
	}

	@Override
	protected String prefix() {
		return "ae";
	}

	@Override
	protected <B extends CableConnectorBlock<AECableConnectorBlockEntity>> ModelFile blockmodel(
			DataGenContext<Block, B> ctx, RegistrateBlockstateProvider prov, BlockState blockState, boolean vertical) {
		boolean powered = blockState.getValue(AECableConnectorBlock.POWERED);
		return prov.models().withExistingParent(ctx.getName() + (vertical ? "_vertical" : "_horizontal") + (powered ? "_on" : "_off"),
				prov.modLoc("block/ae_connector/" + (vertical ? "vertical" : "horizontal")))
				.texture("base", prov.modLoc("block/ae_connector" + (powered ? "_on" : "_off")));
	}
}
