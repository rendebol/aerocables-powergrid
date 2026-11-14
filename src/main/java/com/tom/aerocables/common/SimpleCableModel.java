package com.tom.aerocables.common;

import net.minecraft.resources.ResourceLocation;

import com.tom.aerocables.AeroCables;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;

public class SimpleCableModel implements CableModel {
	private final PartialModel model;
	private final ResourceLocation texture;

	public SimpleCableModel(String prefix) {
		model = CableModel.block(prefix + "_cable");
		texture = ResourceLocation.tryBuild(AeroCables.MODID, "textures/block/" + prefix + "_cable.png");
	}

	@Override
	public PartialModel cable() {
		return model;
	}

	@Override
	public ResourceLocation cableTexture() {
		return texture;
	}

}
