package com.tom.aerocables.common;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.generators.ModelFile;

import com.simibubi.create.AllTags;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.data.SharedProperties;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.builders.BlockEntityBuilder;
import com.tterrag.registrate.builders.BlockEntityBuilder.BlockEntityFactory;
import com.tterrag.registrate.builders.ItemBuilder;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.entry.ItemEntry;
import com.tterrag.registrate.util.nullness.NonNullFunction;

import com.tom.aerocables.client.CableRenderer;

import dev.simulated_team.simulated.index.SimTags;
import dev.simulated_team.simulated.service.SimBlockStateService;

public abstract class AbstractCableType<T extends BlockEntity & ICableConnector> implements CableType<T> {
	protected BlockEntry<?> block;
	protected BlockEntityEntry<T> blockEntity;
	protected ItemEntry<?> item;
	protected CableModel model;

	protected abstract String prefix();

	@Override
	public BlockEntityType<T> blockEntityType() {
		return blockEntity.get();
	}

	protected <B extends CableConnectorBlock<T>> BlockBuilder<B, CreateRegistrate> block(CreateRegistrate registrate, NonNullFunction<BlockBehaviour.Properties, B> factory) {
		return registrate.block(prefix() + "_connector", factory)
				.initialProperties(SharedProperties::stone)
				.addLayer(() -> RenderType::cutoutMipped)
				.blockstate(this::blockstate)
				.properties(Block.Properties::noOcclusion)
				.tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.MINEABLE_WITH_AXE, AllTags.AllBlockTags.BRITTLE.tag, SimTags.Blocks.SUPER_LIGHT)
				.asOptional()
				.loot((l, b) -> l.dropOther(b, CommonRegistration.CONNECTOR));
	}

	protected <B extends CableConnectorBlock<T>> void blockstate(final DataGenContext<Block, B> ctx, final RegistrateBlockstateProvider prov) {
		SimBlockStateService.INSTANCE.directionalAxisBlock(ctx, prov, (blockState, vertical) -> blockmodel(ctx, prov, blockState, vertical));
	}

	protected <B extends CableConnectorBlock<T>> ModelFile blockmodel(final DataGenContext<Block, B> ctx, final RegistrateBlockstateProvider prov, BlockState blockState, boolean vertical) {
		return prov.models().withExistingParent(ctx.getName() + (vertical ? "_vertical" : "_horizontal"), prov.modLoc("block/connected_connector_" + (vertical ? "vertical" : "horizontal")));
	}

	protected BlockEntityBuilder<T, CreateRegistrate> blockEntity(CreateRegistrate registrate, BlockEntityFactory<T> factory) {
		return registrate.blockEntity(prefix() + "_connector", factory)
				.renderer(() -> CableRenderer<T>::new)
				.validBlock(block);
	}

	protected <I extends CableItem> ItemBuilder<I, CreateRegistrate> item(CreateRegistrate registrate, NonNullFunction<Item.Properties, I> factory) {
		return registrate.item(prefix() + "_cable", factory)
				.model((c, p) -> p.withExistingParent(prefix() + "_cable", p.modLoc("item/cable_base")).texture("cable", p.modLoc("block/" + prefix() + "_cable")))
				.asOptional();
	}

	@Override
	public void register(CreateRegistrate registrate) {
		registrate.addDataGenerator(ProviderType.BLOCKSTATE, (m -> m.models().withExistingParent(prefix() + "_cable", m.modLoc("block/cable")).texture("cable", m.modLoc("block/" + prefix() + "_cable"))));
	}

	@Override
	public Block block() {
		return block.get();
	}

	@Override
	public Item item() {
		return item.get();
	}

	@Override
	public void clientPreInit() {
		model = new SimpleCableModel(prefix());
	}

	@Override
	public CableModel cableModel() {
		return model;
	}
}
