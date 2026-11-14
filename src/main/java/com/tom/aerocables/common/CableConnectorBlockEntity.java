package com.tom.aerocables.common;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import com.tom.aerocables.client.ICustomRenderBox;

import dev.simulated_team.simulated.content.blocks.rope.RopeStrandHolderBehavior;
import dev.simulated_team.simulated.content.blocks.rope.RopeStrandHolderBlockEntity;
import dev.simulated_team.simulated.content.blocks.rope.rope_connector.RopeConnectorBlockEntity;
import dev.simulated_team.simulated.content.blocks.rope.strand.client.ClientRopeStrand;
import dev.simulated_team.simulated.content.blocks.rope.strand.server.RopeAttachment;
import dev.simulated_team.simulated.content.blocks.rope.strand.server.RopeAttachmentPoint;
import dev.simulated_team.simulated.content.blocks.rope.strand.server.ServerRopeStrand;

public abstract class CableConnectorBlockEntity extends SmartBlockEntity implements ICableConnector, ICustomRenderBox {
	private BlockPos ropeBlockPos;
	private BlockPos otherSide;
	private boolean cableItem;
	private boolean broken;

	public CableConnectorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
	}

	@Override
	public void setLink(BlockPos ropeBlock, BlockPos otherSide, boolean hasItem) {
		this.ropeBlockPos = ropeBlock.subtract(worldPosition);
		this.otherSide = otherSide;
		this.cableItem = hasItem;
		setChanged();
	}

	@Override
	protected void read(CompoundTag tag, Provider registries, boolean clientPacket) {
		super.read(tag, registries, clientPacket);
		ropeBlockPos = NbtUtils.readBlockPos(tag, "rope").orElse(null);
		otherSide = NbtUtils.readBlockPos(tag, "other").orElse(null);
		cableItem = tag.getBoolean("cable");
	}

	@Override
	protected void write(CompoundTag tag, Provider registries, boolean clientPacket) {
		super.write(tag, registries, clientPacket);
		if (ropeBlockPos != null)tag.put("rope", NbtUtils.writeBlockPos(ropeBlockPos));
		if (otherSide != null)tag.put("other", NbtUtils.writeBlockPos(otherSide));
		tag.putBoolean("cable", cableItem);
	}

	@Override
	public void tick() {
		super.tick();

		if (this.level.isClientSide) {
			this.invalidateRenderBoundingBox();
		} else {
			if (!isValid()) {
				level.setBlock(worldPosition, CommonConnectorBlock.copy(CommonRegistration.CONNECTOR.get(), getBlockState()), 3);
			}
		}
	}

	public boolean isValid() {
		if (ropeBlockPos == null || otherSide == null)
			return false;

		CableConnectorBlockEntity other = null;
		if (level.isLoaded(otherSide)) {
			if (level.getBlockEntity(otherSide) instanceof CableConnectorBlockEntity be) {
				other = be;
				if (!worldPosition.equals(be.otherSide)) {
					return false;
				}
			} else
				return false;
		}

		BlockPos rope = ropeBlockPos.offset(worldPosition);
		if (level.isLoaded(rope)) {
			if (level.getBlockEntity(rope) instanceof RopeStrandHolderBlockEntity be) {
				RopeStrandHolderBehavior ropeB = be.getBehavior();
				ServerRopeStrand strand = ropeB.getOwnedStrand();
				if (strand != null && other != null) {
					if (other.ropeBlockPos == null)
						return false;
					BlockPos otherRope = other.worldPosition.offset(other.ropeBlockPos);
					RopeAttachment point = strand.getAttachment(RopeAttachmentPoint.END);
					if (point == null)
						return false;
					if (!point.blockAttachment().equals(otherRope)) {
						return false;
					}
				} else if(strand == null && !ropeB.isAttached()) {
					return false;
				}
			} else
				return false;
		}

		return true;
	}

	@Override
	public void destroy() {
		if (broken)return;
		broken = true;
		super.destroy();

		CableConnectorBlockEntity other = getOther();

		if (cableItem) {
			Block.popResource(level, worldPosition, getCableStack());
			cableItem = false;
		} else if (other != null && other.cableItem) {
			Block.popResource(level, worldPosition, getCableStack());
			other.cableItem = false;
			other.setChanged();
		}

		if (other != null) {
			BlockState otherState = level.getBlockState(otherSide);
			if (otherState.getBlock() == getBlockState().getBlock())
				level.setBlock(otherSide, CommonConnectorBlock.copy(CommonRegistration.CONNECTOR.get(), otherState), 3);
		}
	}

	protected abstract ItemStack getCableStack();

	@Override
	public RopeStrandHolderBehavior getRope() {
		if (ropeBlockPos != null && level.getBlockEntity(ropeBlockPos.offset(worldPosition)) instanceof RopeStrandHolderBlockEntity be)
			return be.getBehavior();
		return null;
	}

	@Override
	public CableConnectorBlockEntity getOther() {
		if (otherSide == null)return null;
		if (level.getBlockEntity(otherSide) instanceof CableConnectorBlockEntity be)
			return be;
		return null;

	}

	@Override
	public abstract CableModel cableModels();

	@Override
	public AABB getRenderBoundingBox() {
		RopeStrandHolderBehavior ropeHolder = getRope();
		if (ropeHolder == null) {
			return super.getRenderBoundingBox();
		}
		final ClientRopeStrand rope = ropeHolder.getClientStrand();
		if (rope != null && ropeHolder.ownsRope()) {
			final AABB bounds = rope.getBounds();

			if (bounds == null) {
				return super.getRenderBoundingBox();
			}

			return bounds.inflate(RopeConnectorBlockEntity.RENDER_BOUNDING_BOX_INFLATION);
		} else {
			return super.getRenderBoundingBox();
		}
	}

	@Override
	public void connectorMoving(BlockPos newPos) {
		CableConnectorBlockEntity be = getOther();
		if (be != null) {
			be.otherSide = newPos;
			be.notifyUpdate();
		}
		broken = true;
	}

	/** Position of the connector on the other end of the cable (may be unloaded), or null. */
	public BlockPos getOtherSidePos() {
		return otherSide;
	}

	@Override
	public BlockPos getPosition() {
		return worldPosition;
	}

	@Override
	public Direction getFacing() {
		return getBlockState().getValue(CableConnectorBlock.FACING);
	}

	@Override
	public Vec3i getRopeOffset() {
		return ropeBlockPos;
	}

	@Override
	public void clearRemoved() {
		super.clearRemoved();
		broken = false;
	}
}
