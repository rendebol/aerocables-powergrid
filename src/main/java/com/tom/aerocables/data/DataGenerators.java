package com.tom.aerocables.data;

import java.util.function.BiConsumer;

import net.createmod.ponder.foundation.PonderIndex;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import com.tterrag.registrate.providers.ProviderType;

import com.tom.aerocables.AeroCables;
import com.tom.aerocables.client.ClientRegistration;

@EventBusSubscriber(modid = AeroCables.MODID, bus = EventBusSubscriber.Bus.MOD)
public class DataGenerators {

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void gatherDataLang(GatherDataEvent event) {
		if (event.getMods().contains(AeroCables.MODID)) {
			AeroCables.registrate().addDataGenerator(ProviderType.LANG, provider -> {
				BiConsumer<String, String> langConsumer = provider::add;
				providePonderLang(langConsumer);
			});
		}
	}

	private static void providePonderLang(BiConsumer<String, String> consumer) {
		// Register this since FMLClientSetupEvent does not run during datagen
		ClientRegistration.register();

		PonderIndex.getLangAccess().provideLang(AeroCables.MODID, consumer);
	}

	@SubscribeEvent
	public static void gatherData(GatherDataEvent event) {
		if (!event.getMods().contains(AeroCables.MODID)) {
			return;
		}
		//DataGenerator generator = event.getGenerator();
		//PackOutput packOutput = generator.getPackOutput();
		//CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

		//generator.addProvider(event.includeServer(), new SBRecipes(packOutput, lookupProvider));
	}
}
