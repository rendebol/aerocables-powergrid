package com.tom.aerocables;

import org.apache.commons.lang3.tuple.Pair;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

public class Config {
	private static final Config INSTANCE = new Config();
	public int feCableTransfer;
	public double pgCableResistance = 0.001;

	public static Config get() {
		return INSTANCE;
	}

	public static class Server {
		public IntValue feCableTransfer;
		public DoubleValue pgCableResistance;

		private Server(ModConfigSpec.Builder builder) {
			feCableTransfer = builder.comment("FE Cable Transfer Rate (FE/t)").
					translation("config.aerocables.fe_cable_transfer").
					worldRestart().
					defineInRange("feCableTransfer", 1024, 1, Integer.MAX_VALUE / 2);
			pgCableResistance = builder.comment("Power Grid Cable resistance per block of cable length, in ohms (applies to each of the two wires)").
					translation("config.aerocables.pg_cable_resistance").
					defineInRange("pgCableResistancePerBlock", 0.001, 0.00001, 100.0);
		}
	}

	public static class Common {
		public Common(ModConfigSpec.Builder builder) {
			builder.comment("IMPORTANT NOTICE:",
					"THIS IS ONLY THE COMMON CONFIG. It does not contain all the values adjustable for Create: Aero Cables",
					"The settings have been moved to aerocables-server.toml",
					"That file is PER WORLD, meaning you have to go into 'saves/<world name>/serverconfig' to adjust it. Those changes will then only apply for THAT WORLD.",
					"You can then take that config file and put it in the 'defaultconfigs' folder to make it apply automatically to all NEW worlds you generate FROM THERE ON.",
					"This may appear confusing to many of you, but it is a new sensible way to handle configuration, because the server configuration is synced when playing multiplayer.").
			define("importantInfo", true);
		}
	}

	static final ModConfigSpec commonSpec;
	public static final Common COMMON;
	static {
		final Pair<Common, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(Common::new);
		commonSpec = specPair.getRight();
		COMMON = specPair.getLeft();
	}

	static final ModConfigSpec serverSpec;
	public static final Server SERVER;
	static {
		final Pair<Server, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(Server::new);
		serverSpec = specPair.getRight();
		SERVER = specPair.getLeft();
	}

	private void load(ModConfig modConfig) {
		if (modConfig.getType() == Type.SERVER) {
			feCableTransfer = SERVER.feCableTransfer.getAsInt();
			pgCableResistance = SERVER.pgCableResistance.getAsDouble();
		}
	}

	@SubscribeEvent
	public void onLoad(final ModConfigEvent.Loading configEvent) {
		AeroCables.LOGGER.info("Loaded Create: Aero Cables config file {}", configEvent.getConfig().getFileName());
		load(configEvent.getConfig());
	}

	@SubscribeEvent
	public void onFileChange(final ModConfigEvent.Reloading configEvent) {
		AeroCables.LOGGER.info("Create: Aero Cables config just got changed on the file system!");
		load(configEvent.getConfig());
	}
}
