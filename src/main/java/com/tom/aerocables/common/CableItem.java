package com.tom.aerocables.common;

import java.util.List;
import java.util.stream.Stream;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import dev.simulated_team.simulated.content.blocks.rope.RopeStrandHolderBlockEntity;
import dev.simulated_team.simulated.content.blocks.rope.strand.server.RopeAttachment;
import dev.simulated_team.simulated.content.blocks.rope.strand.server.RopeAttachmentPoint;
import dev.simulated_team.simulated.content.blocks.rope.strand.server.ServerRopeStrand;

public abstract class CableItem extends Item {

	public CableItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		BlockState state = context.getLevel().getBlockState(context.getClickedPos());
		if (CommonRegistration.CONNECTOR.has(state)) {
			if (!context.getLevel().isClientSide) {
				BlockPos otherSide = context.getItemInHand().get(CommonRegistration.CONNECTOR_SELECTED);
				if (otherSide != null) {
					if (tryConnecting(context.getLevel(), context.getClickedPos(), otherSide, state, context)) {
						context.getItemInHand().set(CommonRegistration.CONNECTOR_SELECTED, null);
						context.getItemInHand().consume(1, context.getPlayer());
					}
				} else {
					context.getItemInHand().set(CommonRegistration.CONNECTOR_SELECTED, context.getClickedPos());
				}
			}
			return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
		} else if (context.isSecondaryUseActive()) {
			if (!context.getLevel().isClientSide)
				context.getItemInHand().set(CommonRegistration.CONNECTOR_SELECTED, null);
			return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
		}
		return InteractionResult.PASS;
	}

	private boolean tryConnecting(Level level, BlockPos clickedPos, BlockPos otherSide, BlockState state, UseOnContext context) {
		BlockState otherState = level.getBlockState(otherSide);
		if (!CommonRegistration.CONNECTOR.has(otherState))return false;

		List<BlockPos> thisPos = CommonConnectorBlock.getRopeAttachmentPositions(state, clickedPos).toList();
		List<BlockPos> otherPos = CommonConnectorBlock.getRopeAttachmentPositions(otherState, otherSide).toList();

		List<ServerRopeStrand> strands = Stream.concat(thisPos.stream(), otherPos.stream())
				.map(level::getBlockEntity).filter(e -> e instanceof RopeStrandHolderBlockEntity)
				.map(e -> ((RopeStrandHolderBlockEntity) e).getBehavior().getOwnedStrand())
				.filter(s -> s != null).toList();

		if (strands.isEmpty())return false;

		for (ServerRopeStrand serverRopeStrand : strands) {
			RopeAttachment startAttachment = serverRopeStrand.getAttachment(RopeAttachmentPoint.START);
			RopeAttachment endAttachment = serverRopeStrand.getAttachment(RopeAttachmentPoint.END);

			if (startAttachment == null || endAttachment == null)continue;

			BlockPos start = startAttachment.blockAttachment();
			BlockPos end = endAttachment.blockAttachment();

			BlockPos thisRopePos, otherRopePos;

			if (thisPos.contains(start) && otherPos.contains(end)) {
				thisRopePos = start;
				otherRopePos = end;
			} else if (otherPos.contains(start) && thisPos.contains(end)) {
				thisRopePos = end;
				otherRopePos = start;
			} else continue;

			replaceConnector(level, clickedPos, state, thisRopePos, otherSide, true, context);
			replaceConnector(level, otherSide, otherState, otherRopePos, clickedPos, false, context);

			return true;
		}

		return true;
	}

	private void replaceConnector(Level level, BlockPos pos, BlockState state, BlockPos ropeBlock, BlockPos otherSide, boolean hasItem, UseOnContext context) {
		level.setBlock(pos, CommonConnectorBlock.copy(getConnectorBlock(), state), 3);
		BlockEntity be = level.getBlockEntity(pos);
		if (be instanceof ICableConnector cc) {
			cc.setLink(ropeBlock, otherSide, hasItem);
			setupConnector(cc, context);
		}
	}

	protected void setupConnector(ICableConnector cc, UseOnContext context) {
	}

	protected abstract Block getConnectorBlock();
}
