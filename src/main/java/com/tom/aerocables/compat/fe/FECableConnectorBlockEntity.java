package com.tom.aerocables.compat.fe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;

import com.tom.aerocables.AeroCables;
import com.tom.aerocables.Config;
import com.tom.aerocables.common.CableConnectorBlockEntity;
import com.tom.aerocables.common.CableModel;

public class FECableConnectorBlockEntity extends CableConnectorBlockEntity {
	private ConnectorEnergyStorage storage;
	private BlockCapabilityCache<IEnergyStorage, Direction> energyReceiver;

	public FECableConnectorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		int transfer = Config.get().feCableTransfer;
		storage = new ConnectorEnergyStorage(transfer * 2, transfer);
	}

	public IEnergyStorage getEnergyStorage(Direction side) {
		return side == getFacing().getOpposite() ? storage : null;
	}

	@Override
	public void onLoad() {
		super.onLoad();
		Direction facing = getFacing();
		if (!level.isClientSide)
			energyReceiver = BlockCapabilityCache.create(Capabilities.EnergyStorage.BLOCK, (ServerLevel) level, worldPosition.relative(facing.getOpposite()), facing);
	}

	@Override
	public CableModel cableModels() {
		return AeroCables.FE_CABLE.type.cableModel();
	}

	@Override
	protected ItemStack getCableStack() {
		return new ItemStack(AeroCables.FE_CABLE.item());
	}

	private class ConnectorEnergyStorage extends EnergyStorage {

		public ConnectorEnergyStorage(int capacity, int maxTransfer) {
			super(capacity, maxTransfer);
		}

		public void transfer(ConnectorEnergyStorage other) {
			int avg = (energy + other.energy) / 2;
			int transfer = energy - avg;
			if (energy - avg > 0) {
				int extracted = extractEnergy(transfer, false);
				other.receiveEnergy(extracted, false);
			}
		}

		public int getMaxEnergyTransfer() {
			return maxReceive;
		}
	}

	@Override
	public void tick() {
		super.tick();

		if (!level.isClientSide) {
			if (getOther() instanceof FECableConnectorBlockEntity be) {
				storage.transfer(be.storage);
			}

			IEnergyStorage pushTo = energyReceiver.getCapability();
			if (pushTo != null) {
				int transfer = Math.min(storage.getEnergyStored(), storage.getMaxEnergyTransfer());
				int ext = storage.extractEnergy(transfer, true);
				int rec = pushTo.receiveEnergy(ext, false);
				if (rec > 0) {
					storage.extractEnergy(rec, false);
				}
			}
		}
	}
}
