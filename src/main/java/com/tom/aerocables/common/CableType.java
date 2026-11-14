package com.tom.aerocables.common;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import com.simibubi.create.foundation.data.CreateRegistrate;

public interface CableType<T extends BlockEntity & ICableConnector> {
	void registerCapabilities(RegisterCapabilitiesEvent event);
	void register(CreateRegistrate registrate);
	BlockEntityType<T> blockEntityType();
	Block block();
	default void registerClient() {}
	void clientPreInit();
	Item item();
	CableModel cableModel();
}
