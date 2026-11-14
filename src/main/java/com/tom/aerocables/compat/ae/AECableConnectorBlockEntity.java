package com.tom.aerocables.compat.ae;

import java.util.EnumSet;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import com.tom.aerocables.AeroCables;
import com.tom.aerocables.common.CableConnectorBlockEntity;
import com.tom.aerocables.common.CableModel;
import com.tom.aerocables.common.CommonRegistration;

import appeng.api.networking.GridFlags;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGridNodeListener.State;
import appeng.api.networking.IManagedGridNode;
import appeng.api.orientation.BlockOrientation;
import appeng.api.util.AECableType;
import appeng.hooks.ticking.TickHandler;
import appeng.me.helpers.BlockEntityNodeListener;
import appeng.me.helpers.IGridConnectedBlockEntity;

public class AECableConnectorBlockEntity extends CableConnectorBlockEntity implements IGridConnectedBlockEntity {
	protected final IManagedGridNode mainNode;
	private boolean setChangedQueued;
	private AENetworkHandler network;

	public AECableConnectorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		this.mainNode = this.createMainNode().setVisualRepresentation(CommonRegistration.CONNECTOR.asItem())
				.setInWorldNode(true).setTagName("proxy").setIdlePowerUsage(0.0D).setFlags(new GridFlags[0])
				.setExposedOnSides(EnumSet.noneOf(Direction.class));
		this.onGridConnectableSidesChanged();
	}

	protected IManagedGridNode createMainNode() {
		return GridHelper.createManagedNode(this, BlockEntityNodeListener.INSTANCE);
	}

	@Override
	public Set<Direction> getGridConnectableSides(BlockOrientation orientation) {
		return EnumSet.of(getFacing().getOpposite());
	}

	@Override
	public CableModel cableModels() {
		return AeroCables.AE_CABLE.type.cableModel();
	}

	@Override
	protected ItemStack getCableStack() {
		return new ItemStack(AeroCables.AE_CABLE.item());
	}

	@Override
	public IManagedGridNode getMainNode() {
		return mainNode;
	}

	@Override
	protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.write(tag, registries, clientPacket);
		if(!clientPacket) {
			this.getMainNode().saveToNBT(tag);
		}
	}

	@Override
	protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(tag, registries, clientPacket);
		if(!clientPacket) {
			this.getMainNode().loadFromNBT(tag);
		}
	}

	@Override
	public AECableType getCableConnectionType(final Direction dir) {
		return AECableType.SMART;
	}

	@Override
	public void onChunkUnloaded() {
		super.onChunkUnloaded();
		if (network != null)
			network.destroy();
		this.getMainNode().destroy();
	}

	public void onReady() {
		this.getMainNode().setExposedOnSides(EnumSet.of(getFacing().getOpposite()));
		this.getMainNode().create(this.getLevel(), this.getBlockPos());

		if (getOther() instanceof AECableConnectorBlockEntity be && be.network == null) {
			network = new AENetworkHandler();
			network.init(this);
			be.network = network;
		} else if(network != null) {
			network.connect(this);
		}
	}

	@Override
	public void destroy() {
		super.destroy();
		if (network != null)
			network.destroy();
	}

	protected final void onGridConnectableSidesChanged() {
		this.getMainNode().setExposedOnSides(this.getGridConnectableSides(null));
	}

	@Override
	public void invalidate() {
		super.invalidate();
		if (network != null)
			network.destroy();
		this.getMainNode().destroy();
	}

	@Override
	public void clearRemoved() {
		super.clearRemoved();
		this.scheduleInit();
	}

	@Override
	public void saveChanges() {
		if (this.level == null) {
			return;
		}
		if (this.level.isClientSide) {
			this.setChanged();
		} else {
			this.level.blockEntityChanged(this.worldPosition);
			if (!this.setChangedQueued) {
				TickHandler.instance().addCallable((LevelAccessor) null, this::setChangedAtEndOfTick);
				this.setChangedQueued = true;
			}
		}
	}

	protected void scheduleInit() {
		GridHelper.onFirstTick(this, AECableConnectorBlockEntity::onReady);
	}

	private Object setChangedAtEndOfTick(final Level level) {
		this.setChanged();
		this.setChangedQueued = false;
		return null;
	}

	@Override
	public void onMainNodeStateChanged(State reason) {
		boolean p = getMainNode().isPowered();
		if (!remove && getBlockState().getValue(AECableConnectorBlock.POWERED) != p && level.getBlockState(worldPosition).getBlock() == getBlockState().getBlock()) {
			level.setBlock(worldPosition, getBlockState().setValue(AECableConnectorBlock.POWERED, p), 3);
		}
	}
}
