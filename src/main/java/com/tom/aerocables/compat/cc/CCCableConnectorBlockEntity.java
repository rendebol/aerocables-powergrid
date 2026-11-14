package com.tom.aerocables.compat.cc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;

import com.tom.aerocables.AeroCables;
import com.tom.aerocables.common.CableConnectorBlockEntity;
import com.tom.aerocables.common.CableModel;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.network.wired.WiredElement;
import dan200.computercraft.api.network.wired.WiredElementCapability;
import dan200.computercraft.api.network.wired.WiredNetworkChange;
import dan200.computercraft.api.network.wired.WiredNode;

public class CCCableConnectorBlockEntity extends CableConnectorBlockEntity {
	private final CableWiredElement wiredElement = new CableWiredElement();
	private final WiredNode node = ComputerCraftAPI.createWiredNodeForElement(wiredElement);
	private BlockCapabilityCache<WiredElement, Direction> adjacentElement;
	private WiredNode connectedAdjacentNode;
	private WiredNode connectedOtherNode;
	private boolean refreshAdjacent;

	public CCCableConnectorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	public WiredElement getWiredElement(Direction side) {
		return side == null || side == getFacing().getOpposite() ? wiredElement : null;
	}

	@Override
	public void onLoad() {
		super.onLoad();
		if (!level.isClientSide) {
			Direction facing = getFacing();
			adjacentElement = BlockCapabilityCache.create(
					WiredElementCapability.get(),
					(ServerLevel) level,
					worldPosition.relative(facing.getOpposite()),
					facing,
					() -> !isRemoved(),
					this::adjacentInvalidated
			);
			refreshConnections();
		}
	}

	@Override
	public CableModel cableModels() {
		return AeroCables.CC_CABLE.type.cableModel();
	}

	@Override
	protected ItemStack getCableStack() {
		return new ItemStack(AeroCables.CC_CABLE.item());
	}

	@Override
	public void tick() {
		super.tick();
		if (!level.isClientSide) {
			refreshConnections();
		}
	}

	@Override
	public void destroy() {
		disconnectAll();
		node.remove();
		super.destroy();
	}

	@Override
	public void onChunkUnloaded() {
		disconnectAll();
		node.remove();
		super.onChunkUnloaded();
	}

	@Override
	public void invalidate() {
		disconnectAll();
		node.remove();
		super.invalidate();
	}

	@Override
	public void clearRemoved() {
		super.clearRemoved();
		if (level != null && !level.isClientSide) {
			refreshConnections();
		}
	}

	private void refreshConnections() {
		if (level == null || level.isClientSide || isRemoved()) {
			return;
		}

		connectOther();
		connectAdjacent();
		refreshAdjacent = false;
	}

	private void adjacentInvalidated() {
		disconnect(connectedAdjacentNode);
		connectedAdjacentNode = null;
		refreshAdjacent = true;
	}

	private void connectOther() {
		WiredNode otherNode = getOther() instanceof CCCableConnectorBlockEntity other ? other.node : null;
		if (connectedOtherNode != otherNode) {
			disconnect(connectedOtherNode);
			connectedOtherNode = otherNode;
		}
		connect(connectedOtherNode);
	}

	private void connectAdjacent() {
		if (adjacentElement == null || (!refreshAdjacent && connectedAdjacentNode != null)) {
			return;
		}

		WiredElement element = adjacentElement.getCapability();
		WiredNode adjacentNode = element == null || element == wiredElement ? null : element.getNode();
		if (connectedAdjacentNode != adjacentNode) {
			disconnect(connectedAdjacentNode);
			connectedAdjacentNode = adjacentNode;
		}
		connect(connectedAdjacentNode);
	}

	private void disconnectAll() {
		disconnect(connectedAdjacentNode);
		connectedAdjacentNode = null;
		disconnect(connectedOtherNode);
		connectedOtherNode = null;
	}

	private void connect(WiredNode other) {
		if (other != null && other != node) {
			node.connectTo(other);
		}
	}

	private void disconnect(WiredNode other) {
		if (other != null && other != node) {
			node.disconnectFrom(other);
		}
	}

	private class CableWiredElement implements WiredElement {
		@Override
		public WiredNode getNode() {
			return node;
		}

		@Override
		public Level getLevel() {
			return CCCableConnectorBlockEntity.this.getLevel();
		}

		@Override
		public Vec3 getPosition() {
			return Vec3.atCenterOf(worldPosition);
		}

		@Override
		public String getSenderID() {
			return "aerocables_cc_connector";
		}

		@Override
		public void networkChanged(WiredNetworkChange change) {
		}
	}
}
