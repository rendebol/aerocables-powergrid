package com.tom.aerocables.client;

import net.createmod.ponder.foundation.PonderIndex;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

import com.tom.aerocables.AeroCables;

public class ClientRegistration {

	public static void register() {
		AeroCables.ALL_CABLES.forEach(c -> c.type.registerClient());
		PonderIndex.addPlugin(new ACPonderPlugin());
		NeoForge.EVENT_BUS.addListener(ClientRegistration::postClientTick);
	}

	public static void preInit() {
		AeroCables.ALL_CABLES.forEach(c -> c.type.clientPreInit());
	}

	public static void postClientTick(final ClientTickEvent.Post event) {
		ClientCableItemHandler.tick();
	}
}
