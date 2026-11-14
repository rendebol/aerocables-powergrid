package com.tom.aerocables.client;

import java.util.ArrayList;
import java.util.List;

import net.createmod.catnip.outliner.Outliner;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import com.tom.aerocables.common.CableItem;
import com.tom.aerocables.common.CommonConnectorBlock;
import com.tom.aerocables.common.CommonRegistration;

import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.simulated_team.simulated.content.blocks.rope.strand.client.ClientLevelRopeManager;
import dev.simulated_team.simulated.content.blocks.rope.strand.client.ClientRopePoint;
import dev.simulated_team.simulated.content.blocks.rope.strand.client.ClientRopeStrand;
import dev.simulated_team.simulated.util.SimColors;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ClientCableItemHandler {

	public static void tick() {
		final Player player = Minecraft.getInstance().player;
		final Level level = Minecraft.getInstance().level;

		if (player == null || level == null)
			return;
		if (Minecraft.getInstance().screen != null)
			return;

		for (final InteractionHand hand : InteractionHand.values()) {
			final ItemStack heldItem = player.getItemInHand(hand);

			if (!(heldItem.getItem() instanceof CableItem))
				continue;

			if (!heldItem.has(CommonRegistration.CONNECTOR_SELECTED))
				continue;

			final BlockPos firstBlock = heldItem.get(CommonRegistration.CONNECTOR_SELECTED);
			final HitResult rayTrace = Minecraft.getInstance().hitResult;

			if (rayTrace instanceof final BlockHitResult hitResult) {
				final BlockPos hitBlock = hitResult.getBlockPos();

				BlockState state = level.getBlockState(firstBlock);
				BlockState otherState = level.getBlockState(hitBlock);

				if (!state.is(CommonRegistration.CONNECTOR)) {
					final Color color = new Color(SimColors.NUH_UH_RED);

					final Vec3 firstPoint = firstBlock.getCenter();
					Outliner.getInstance().chaseAABB("FirstCableAttachmentPoint", new AABB(firstPoint, firstPoint))
					.colored(color)
					.lineWidth(1 / 3f)
					.disableLineNormals();

					return;
				}

				List<BlockPos> thisPos = CommonConnectorBlock.getRopeAttachmentPositions(state, firstBlock).toList();
				List<BlockPos> otherPos = null;
				List<ClientRopeStrand> strands = new ArrayList<>();
				boolean valid = otherState.is(CommonRegistration.CONNECTOR) && !hitBlock.equals(firstBlock);
				if (valid) {
					otherPos = CommonConnectorBlock.getRopeAttachmentPositions(otherState, hitBlock).toList();
				}
				ClientRopeStrand connecting = null;

				ClientLevelRopeManager ropeManager = ClientLevelRopeManager.getOrCreate(level);
				for (final ClientRopeStrand strand : ropeManager.getAllStrands()) {
					if (strand.startAttachment == null || strand.endAttachment == null)
						continue;

					BlockPos start = BlockPos.containing(strand.startAttachment);
					BlockPos end = BlockPos.containing(strand.endAttachment);
					if (thisPos.contains(start) || thisPos.contains(end)) {
						strands.add(strand);

						if (valid && (otherPos.contains(start) || otherPos.contains(end))) {
							connecting = strand;
						}
					}
				}

				final Color color;
				if (connecting != null) {
					color = new Color(SimColors.SUCCESS_LIME);
				} else {
					color = new Color(SimColors.NUH_UH_RED);
				}

				final Vec3 firstPoint = firstBlock.getCenter();
				Outliner.getInstance().chaseAABB("FirstCableAttachmentPoint", new AABB(firstPoint, firstPoint))
				.colored(color)
				.lineWidth(1 / 3f)
				.disableLineNormals();

				if (connecting != null) {
					final Vec3 target = valid ? hitBlock.getCenter() : hitResult.getLocation();

					Outliner.getInstance().chaseAABB("SecondCableAttachmentPoint", new AABB(target, target))
					.colored(color)
					.lineWidth(1 / 3f)
					.disableLineNormals();

					highlightStrand(connecting, color, strands.indexOf(connecting));
				} else {
					int i = 0;
					for (final ClientRopeStrand strand : strands) {
						highlightStrand(strand, color, i++);
					}
				}
			}
		}
	}

	private static void highlightStrand(ClientRopeStrand strand, Color color, int r) {
		final ObjectArrayList<ClientRopePoint> points = strand.getPoints();

		for (int i = 0; i < points.size() - 1; i++) {
			final ClientRopePoint point0 = points.get(i);
			final ClientRopePoint point1 = points.get(i + 1);

			Outliner.getInstance().chaseAABB("RopePoint" + i + "_" + r, new AABB(JOMLConversion.toMojang(point0.position()), JOMLConversion.toMojang(point1.position())))
			.colored(color)
			.lineWidth(1 / 4f)
			.disableLineNormals();
		}
	}
}
