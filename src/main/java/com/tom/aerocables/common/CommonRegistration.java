package com.tom.aerocables.common;

import static com.simibubi.create.foundation.data.ModelGen.customItemModel;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.simibubi.create.AllItems;
import com.simibubi.create.AllTags;
import com.simibubi.create.foundation.data.SharedProperties;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateItemModelProvider;
import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.entry.ItemEntry;
import com.tterrag.registrate.util.entry.RegistryEntry;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;

import com.tom.aerocables.AeroCables;

import dev.simulated_team.simulated.data.SimBlockStateGen;
import dev.simulated_team.simulated.index.SimBlocks;
import dev.simulated_team.simulated.index.SimItems;
import dev.simulated_team.simulated.index.SimTags;

public class CommonRegistration {
	private static final DeferredRegister.DataComponents DATA_COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, AeroCables.ID);

	public static final RegistryEntry<CreativeModeTab, CreativeModeTab> TAB = AeroCables.registrate().defaultCreativeTab("aerocables",
			c -> c.icon(CommonRegistration.CABLE::asStack)
			).register();

	public static final ItemEntry<Item> CABLE = AeroCables.registrate().item("cable", Item::new)
			.recipe((c, p) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, c.get(), 4)
					.pattern("CRR")
					.pattern("RSR")
					.pattern("RRC")
					.define('R', SimItems.ROPE_COUPLING)
					.define('C', AllItems.COPPER_SHEET)
					.define('S', Tags.Items.RODS_WOODEN)
					.unlockedBy("has_ingredient", RegistrateRecipeProvider.has(SimBlocks.ROPE_CONNECTOR))
					.save(p))
			.lang("Cable Spool")
			.model(item2d("item/cable"))
			.register();

	public static final BlockEntry<CommonConnectorBlock> CONNECTOR = AeroCables.registrate().block("connector", CommonConnectorBlock::new)
			.initialProperties(SharedProperties::stone)
			.addLayer(() -> RenderType::cutoutMipped)
			.blockstate(SimBlockStateGen::directionalAxisBlock)
			.properties(Block.Properties::noOcclusion)
			.tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.MINEABLE_WITH_AXE, AllTags.AllBlockTags.BRITTLE.tag, SimTags.Blocks.SUPER_LIGHT)
			.recipe((c, p) -> ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, c.get(), 1)
					.requires(AllItems.COPPER_SHEET)
					.requires(SimBlocks.ROPE_CONNECTOR)
					.requires(ItemTags.TERRACOTTA)
					.unlockedBy("has_ingredient", RegistrateRecipeProvider.has(SimBlocks.ROPE_CONNECTOR))
					.save(p))
			.item()
			.transform(customItemModel())
			.lang("Cable Connector")
			.register();

	public static final DeferredHolder<DataComponentType<?>, DataComponentType<BlockPos>> CONNECTOR_SELECTED = DATA_COMPONENTS.register("selected_connector", () -> DataComponentType.<BlockPos>builder().persistent(BlockPos.CODEC).build());

	public static void register(IEventBus modEventBus) {
		DATA_COMPONENTS.register(modEventBus);
	}

	public static <T extends Item> NonNullBiConsumer<DataGenContext<Item, T>, RegistrateItemModelProvider> item2d(String texture) {
		return (c, p) -> p.singleTexture(c.getName(), p.mcLoc("item/generated"), "layer0", p.modLoc(texture));
	}
}
