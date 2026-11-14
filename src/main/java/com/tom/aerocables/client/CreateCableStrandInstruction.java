package com.tom.aerocables.client;

import net.createmod.ponder.foundation.instruction.FadeIntoSceneInstruction;
import net.minecraft.core.Direction;

public class CreateCableStrandInstruction extends FadeIntoSceneInstruction<CableStrandElement> {
	public CreateCableStrandInstruction(final int fadeInTicks, final Direction fadeInFrom, final CableStrandElement element) {
		super(fadeInTicks, fadeInFrom, element);
	}

	public CreateCableStrandInstruction(final CableStrandElement element) {
		super(0, Direction.DOWN, element);
	}

	@Override
	protected Class<CableStrandElement> getElementClass() {
		return CableStrandElement.class;
	}
}