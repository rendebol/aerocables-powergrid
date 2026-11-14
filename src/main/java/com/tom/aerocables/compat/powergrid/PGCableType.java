package com.tom.aerocables.compat.powergrid;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import com.simibubi.create.AllTags;
import com.simibubi.create.foundation.data.CreateRegistrate;

import com.tom.aerocables.common.AbstractCableType;

import dev.simulated_team.simulated.index.SimTags;

/**
 * Cable type for Create: Power Grid.
 *
 * Block states, models, loot table, recipe and translations of this type are plain resource files
 * (assets/aerocables/blockstates/pg_connector.json etc.), not data generated: Power Grid is not
 * loaded during runData, so the generators below are intentionally empty.
 */
public class PGCableType extends AbstractCableType<PGCableConnectorBlockEntity> {

	@Override
	public void registerCapabilities(RegisterCapabilitiesEvent event) {
		// Power Grid does not use capabilities, the connector talks to it through IElectric
	}

	@Override
	public void register(CreateRegistrate registrate) {
		// no super.register(): the cable model is a hand made file (models/block/pg_cable.json)
		block = registrate.block(prefix() + "_connector", PGCableConnectorBlock::new)
				.properties(p -> p.mapColor(MapColor.COLOR_ORANGE)
						.strength(1.0F, 3.0F)
						.sound(SoundType.COPPER)
						.noOcclusion())
				.addLayer(() -> RenderType::cutoutMipped)
				.blockstate((c, p) -> {})
				.loot((t, b) -> {})
				.tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.MINEABLE_WITH_AXE, AllTags.AllBlockTags.BRITTLE.tag, SimTags.Blocks.SUPER_LIGHT)
				.lang("Power Grid Cable Connector")
				.register();
		blockEntity = blockEntity(registrate, PGCableConnectorBlockEntity::new)
				.register();
		item = registrate.item(prefix() + "_cable", PGCableItem::new)
				.model((c, p) -> {})
				.lang("Power Grid Cable Spool")
				.register();
	}

	@Override
	protected String prefix() {
		return "pg";
	}

	@Override
	public void clientPreInit() {
		model = new PGCableModel();
	}
}
