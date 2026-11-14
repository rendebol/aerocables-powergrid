package com.tom.aerocables.common;

import net.minecraft.resources.ResourceLocation;

import com.tom.aerocables.AeroCables;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;

public interface CableModel {
	PartialModel cable();
	ResourceLocation cableTexture();

	/**
	 * Textures of the individual thin strands drawn between the rope and the connectors.
	 * null (default) draws one thick cable with {@link #cableTexture()}.
	 */
	default ResourceLocation[] strandTextures() {
		return null;
	}

	public static PartialModel block(final String path) {
		return PartialModel.of(ResourceLocation.tryBuild(AeroCables.MODID, "block/" + path));
	}
}
