package com.tom.aerocables.compat.powergrid;

import net.minecraft.resources.ResourceLocation;

import com.tom.aerocables.AeroCables;
import com.tom.aerocables.common.CableModel;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;

/**
 * Two thin wires (red + / blue -) instead of one thick cable.
 */
public class PGCableModel implements CableModel {
	private final PartialModel model = CableModel.block("pg_cable");
	private final ResourceLocation[] strands = new ResourceLocation[] {
			texture("pg_cable_positive"),
			texture("pg_cable_negative")
	};

	private static ResourceLocation texture(String name) {
		return ResourceLocation.tryBuild(AeroCables.MODID, "textures/block/" + name + ".png");
	}

	@Override
	public PartialModel cable() {
		return model;
	}

	@Override
	public ResourceLocation cableTexture() {
		return strands[0];
	}

	@Override
	public ResourceLocation[] strandTextures() {
		return strands;
	}
}
