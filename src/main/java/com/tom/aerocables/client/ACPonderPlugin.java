package com.tom.aerocables.client;

import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredHolder;

import com.tterrag.registrate.util.entry.ItemProviderEntry;

import com.tom.aerocables.AeroCables;
import com.tom.aerocables.common.CommonRegistration;

import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.index.SimBlocks;
import dev.simulated_team.simulated.index.SimItems;

public class ACPonderPlugin implements PonderPlugin {

	@Override
	public String getModId() {
		return AeroCables.MODID;
	}

	@Override
	public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> registry) {
		final PonderSceneRegistrationHelper<ItemProviderEntry<?, ?>> helper = registry.withKeyFunction(DeferredHolder::getId);

		helper.forComponents(CommonRegistration.CONNECTOR, SimBlocks.ROPE_CONNECTOR, SimBlocks.ROPE_WINCH, SimItems.ROPE_COUPLING)
		.addStoryBoard("ac_connector", CablePonder::connector, board -> {
			board.orderAfter(Simulated.MOD_ID, "rope_connections");
		});
	}
}
