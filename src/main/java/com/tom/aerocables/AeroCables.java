package com.tom.aerocables;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.slf4j.Logger;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import com.mojang.logging.LogUtils;
import com.simibubi.create.foundation.data.CreateRegistrate;

import com.tom.aerocables.client.ClientRegistration;
import com.tom.aerocables.common.CableType;
import com.tom.aerocables.common.CommonRegistration;
import com.tom.aerocables.compat.ae.AECableType;
import com.tom.aerocables.compat.fe.FECableType;
import com.tom.aerocables.compat.rs.RSCableType;
import com.tom.aerocables.compat.cc.CCCableType;
import com.tom.aerocables.compat.powergrid.PGCableType;

@Mod(AeroCables.MODID)
public class AeroCables {
	public static final String MODID = "aerocables";
	public static final Logger LOGGER = LogUtils.getLogger();

	public static final List<Cable> ALL_CABLES = new ArrayList<>();

	public static final String AE = "ae2";
	public static final String RS = "refinedstorage";
	public static final String ID = "integrateddynamics";
	public static final String CC = "computercraft";
	public static final String PG = "powergrid";

	private static CreateRegistrate registrate;

	public static final Cable FE_CABLE = new Cable("neoforge", () -> FECableType::new);
	public static final Cable AE_CABLE = new Cable(AE, () -> AECableType::new);
	public static final Cable RS_CABLE = new Cable(RS, () -> RSCableType::new);
	public static final Cable CC_CABLE = new Cable(CC, () -> CCCableType::new);
	public static final Cable PG_CABLE = new Cable(PG, () -> PGCableType::new);

	public AeroCables(IEventBus modEventBus, ModContainer modContainer) {
		modEventBus.addListener(this::commonSetup);
		modEventBus.addListener(this::doClientStuff);
		modEventBus.addListener(this::registerCapabilities);

		modContainer.registerConfig(ModConfig.Type.COMMON, Config.commonSpec);
		modContainer.registerConfig(ModConfig.Type.SERVER, Config.serverSpec);
		modEventBus.register(Config.get());

		registrate = CreateRegistrate.create(MODID).registerEventListeners(modEventBus);

		CommonRegistration.register(modEventBus);

		ALL_CABLES.forEach(c -> c.type.register(registrate));

		if (FMLEnvironment.dist == Dist.CLIENT)ClientRegistration.preInit();
	}

	private void commonSetup(FMLCommonSetupEvent event) {
		LOGGER.info("Create Aero Cables starting");
	}

	private void doClientStuff(final FMLClientSetupEvent event) {
		event.enqueueWork(ClientRegistration::register);
	}

	private void registerCapabilities(RegisterCapabilitiesEvent event) {
		ALL_CABLES.forEach(c -> c.type.registerCapabilities(event));
	}

	public static CreateRegistrate registrate() {
		return registrate;
	}

	public static class Cable {
		public final String modid;
		public CableType<?> type;

		public Cable(String modid, Supplier<Supplier<CableType<?>>> typeFactory) {
			this.modid = modid;
			if(ModList.get().isLoaded(modid)) {
				this.type = typeFactory.get().get();
				ALL_CABLES.add(this);
			}
		}

		public <T extends BlockEntity> BlockEntityType<T> blockEntityType() {
			return (BlockEntityType<T>) type.blockEntityType();
		}

		public Block block() {
			return type.block();
		}

		public Item item() {
			return type.item();
		}
	}
}
