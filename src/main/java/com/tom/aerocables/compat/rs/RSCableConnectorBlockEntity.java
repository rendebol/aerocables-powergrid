package com.tom.aerocables.compat.rs;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import com.refinedmods.refinedstorage.api.network.impl.node.SimpleNetworkNode;
import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.refinedmods.refinedstorage.common.api.support.network.ConnectionSink;
import com.refinedmods.refinedstorage.common.api.support.network.InWorldNetworkNodeContainer;
import com.refinedmods.refinedstorage.common.support.network.AbstractBaseNetworkNodeContainerBlockEntity;
import com.refinedmods.refinedstorage.common.support.network.SimpleConnectionStrategy;

import com.tom.aerocables.AeroCables;
import com.tom.aerocables.client.ICustomRenderBox;
import com.tom.aerocables.common.CableConnectorBlock;
import com.tom.aerocables.common.CableModel;
import com.tom.aerocables.common.CommonConnectorBlock;
import com.tom.aerocables.common.CommonRegistration;
import com.tom.aerocables.common.ICableConnector;

import dev.simulated_team.simulated.content.blocks.rope.RopeStrandHolderBehavior;
import dev.simulated_team.simulated.content.blocks.rope.RopeStrandHolderBlockEntity;
import dev.simulated_team.simulated.content.blocks.rope.rope_connector.RopeConnectorBlockEntity;
import dev.simulated_team.simulated.content.blocks.rope.strand.client.ClientRopeStrand;
import dev.simulated_team.simulated.content.blocks.rope.strand.server.RopeAttachment;
import dev.simulated_team.simulated.content.blocks.rope.strand.server.RopeAttachmentPoint;
import dev.simulated_team.simulated.content.blocks.rope.strand.server.ServerRopeStrand;

public class RSCableConnectorBlockEntity extends AbstractBaseNetworkNodeContainerBlockEntity<SimpleNetworkNode> implements ICableConnector, ICustomRenderBox {
	private BlockPos ropeBlockPos;
	private BlockPos otherSide;
	private boolean cableItem;
	private boolean broken;

	protected RSCableConnectorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state, new SimpleNetworkNode(0));
	}

	@Override
	protected InWorldNetworkNodeContainer createMainContainer(final SimpleNetworkNode networkNode) {
		return RefinedStorageApi.INSTANCE.createNetworkNodeContainer(this, networkNode)
				.connectionStrategy(new SimpleConnectionStrategy(this.getBlockPos()) {

					@Override
					public void addOutgoingConnections(ConnectionSink sink) {
						Direction direction = getFacing();
						sink.tryConnectInSameDimension(this.origin.relative(direction.getOpposite()), direction);

						RSCableConnectorBlockEntity other = getOther();
						if (other != null)
							sink.tryConnect(other.getGlobalPos(), RSCableConnectorBlock.class);
					}

					@Override
					public boolean canAcceptIncomingConnection(Direction incomingDirection,
							BlockState connectingState) {
						return incomingDirection == getFacing().getOpposite();
					}
				})
				.build();
	}

	@Override
	public void doWork() {
		super.doWork();

		if (!this.level.isClientSide) {
			if (!isValid()) {
				level.setBlock(worldPosition, CommonConnectorBlock.copy(CommonRegistration.CONNECTOR.get(), getBlockState()), 3);
			}
		}
	}

	public boolean isValid() {
		if (ropeBlockPos == null || otherSide == null)
			return false;

		RSCableConnectorBlockEntity other = null;
		if (level.isLoaded(otherSide)) {
			if (level.getBlockEntity(otherSide) instanceof RSCableConnectorBlockEntity be) {
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
	public void saveAdditional(CompoundTag tag, Provider provider) {
		super.saveAdditional(tag, provider);
		if (ropeBlockPos != null)tag.put("rope", NbtUtils.writeBlockPos(ropeBlockPos));
		if (otherSide != null)tag.put("other", NbtUtils.writeBlockPos(otherSide));
		tag.putBoolean("cable", cableItem);
	}

	@Override
	public void loadAdditional(CompoundTag tag, Provider provider) {
		super.loadAdditional(tag, provider);
		ropeBlockPos = NbtUtils.readBlockPos(tag, "rope").orElse(null);
		otherSide = NbtUtils.readBlockPos(tag, "other").orElse(null);
		cableItem = tag.getBoolean("cable");
	}

	@Override
	public Component getName() {
		return AeroCables.RS_CABLE.block().getName();
	}

	@Override
	public void setLink(BlockPos ropeBlock, BlockPos otherSide, boolean hasItem) {
		this.ropeBlockPos = ropeBlock.subtract(worldPosition);
		this.otherSide = otherSide;
		this.cableItem = hasItem;
		setChanged();
	}

	@Override
	public RopeStrandHolderBehavior getRope() {
		if (ropeBlockPos != null && level.getBlockEntity(ropeBlockPos.offset(worldPosition)) instanceof RopeStrandHolderBlockEntity be)
			return be.getBehavior();
		return null;
	}

	@Override
	public RSCableConnectorBlockEntity getOther() {
		if (otherSide == null || isRemoved())return null;
		if (level.isLoaded(otherSide) && level.getBlockEntity(otherSide) instanceof RSCableConnectorBlockEntity be)
			return be;
		return null;
	}

	@Override
	public CableModel cableModels() {
		return AeroCables.RS_CABLE.type.cableModel();
	}

	@Override
	public Direction getFacing() {
		return getBlockState().getValue(CableConnectorBlock.FACING);
	}

	@Override
	public BlockPos getPosition() {
		return worldPosition;
	}

	@Override
	public void connectorMoving(BlockPos newPos) {
		RSCableConnectorBlockEntity be = getOther();
		if (be != null) {
			be.otherSide = newPos;
			be.notifyUpdate();
		}
		broken = true;
	}

	private void notifyUpdate() {
		setChanged();

		if (level instanceof ServerLevel serverLevel)
			serverLevel.getChunkSource().blockChanged(getBlockPos());
	}

	@Override
	public Vec3i getRopeOffset() {
		return ropeBlockPos;
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(final HolderLookup.Provider provider) {
		CompoundTag tag = new CompoundTag();
		saveAdditional(tag, provider);
		return tag;
	}

	public void destroy() {
		if (broken)return;
		broken = true;

		RSCableConnectorBlockEntity other = getOther();

		if (cableItem) {
			Block.popResource(level, worldPosition, new ItemStack(AeroCables.RS_CABLE.item()));
			cableItem = false;
		} else if (other != null && other.cableItem) {
			Block.popResource(level, worldPosition, new ItemStack(AeroCables.RS_CABLE.item()));
			other.cableItem = false;
			other.setChanged();
		}

		if (other != null) {
			BlockState otherState = level.getBlockState(otherSide);
			if (otherState.getBlock() == getBlockState().getBlock())
				level.setBlock(otherSide, CommonConnectorBlock.copy(CommonRegistration.CONNECTOR.get(), otherState), 3);
		}
	}

	@Override
	public AABB getRenderBoundingBox() {
		RopeStrandHolderBehavior ropeHolder = getRope();
		if (ropeHolder == null) {
			return new AABB(getBlockPos());
		}
		final ClientRopeStrand rope = ropeHolder.getClientStrand();
		if (rope != null && ropeHolder.ownsRope()) {
			final AABB bounds = rope.getBounds();

			if (bounds == null) {
				return new AABB(getBlockPos());
			}

			return bounds.inflate(RopeConnectorBlockEntity.RENDER_BOUNDING_BOX_INFLATION);
		} else {
			return new AABB(getBlockPos());
		}
	}

	public GlobalPos getGlobalPos() {
		return new GlobalPos(level.dimension(), worldPosition);
	}
}
