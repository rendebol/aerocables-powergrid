package com.tom.aerocables.compat.powergrid;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import com.tom.aerocables.AeroCables;
import com.tom.aerocables.Config;
import com.tom.aerocables.common.CableConnectorBlockEntity;
import com.tom.aerocables.common.CableModel;

import org.patryk3211.powergrid.electricity.GlobalElectricNetworks;
import org.patryk3211.powergrid.electricity.base.ElectricBehaviour;
import org.patryk3211.powergrid.electricity.base.IElectricEntity;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.electricity.sim.ElectricalNetwork;
import org.patryk3211.powergrid.electricity.sim.node.OwnedFloatingNode;
import org.patryk3211.powergrid.electricity.wire.BlockWireEndpoint;

import dev.ryanhcode.sable.Sable;

/**
 * Connector block entity for the Power Grid cable.
 *
 * Each connector owns a Power Grid {@link ElectricBehaviour} with two external terminals (+ and -).
 * The connector with the "smaller" position of a linked pair (the master) creates two simple
 * Power Grid wires: +A <-> +B and -A <-> -B. The wires are not saved, they are rebuilt whenever
 * both ends are loaded, and dropped before any of the two ends goes away.
 */
public class PGCableConnectorBlockEntity extends CableConnectorBlockEntity implements IElectricEntity {
	private static final int RESISTANCE_UPDATE_INTERVAL = 20;
	private static final double MIN_LENGTH = 1.0;
	private static final double MIN_RESISTANCE = 1.0E-4;
	private static final float LEAKAGE_RESISTANCE = 1.0E6f;

	// NOTE: no field initializer here! addBehaviours() runs inside the super constructor.
	private ElectricBehaviour electricBehaviour;

	private final ElectricWire[] links = new ElectricWire[PGTerminals.COUNT];
	private double linkResistance = -1;
	private int resistanceTimer;
	private long rebuildWindowStart;
	private int rebuildCount;
	private int rebuildBackoff;

	public PGCableConnectorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		super.addBehaviours(behaviours);
		electricBehaviour = new ElectricBehaviour(this);
		behaviours.add(electricBehaviour);
	}

	@Override
	public void buildCircuit(CircuitBuilder builder) {
		// Two external terminals. The cable resistance lives in the link wires (see updateLinks).
		builder.setTerminalCount(PGTerminals.COUNT);
		// Tiny leakage between + and -. It is electrically negligible, but it gives the connector
		// "internals", so Power Grid treats its terminals like device terminals and never folds
		// them into the middle of a transmission line (our link wires must stay attached to them).
		builder.connect(LEAKAGE_RESISTANCE, builder.terminalNode(PGTerminals.POSITIVE), builder.terminalNode(PGTerminals.NEGATIVE));
	}

	public ElectricBehaviour getElectricBehaviour() {
		return electricBehaviour;
	}

	@Override
	public CableModel cableModels() {
		return AeroCables.PG_CABLE.type.cableModel();
	}

	@Override
	protected ItemStack getCableStack() {
		return new ItemStack(AeroCables.PG_CABLE.item());
	}

	@Override
	public void tick() {
		super.tick();
		if (level == null || level.isClientSide || isRemoved())
			return;
		updateLinks();
	}

	// ---- link management (server only) ----

	private void updateLinks() {
		PGCableConnectorBlockEntity other = getLinkedPartner();
		if (other == null || !isMaster(other)) {
			dropLinks();
			return;
		}

		ElectricBehaviour own = electricBehaviour;
		ElectricBehaviour remote = other.electricBehaviour;
		if (own == null || remote == null)
			return;

		boolean intact = true;
		for (int i = 0; i < PGTerminals.COUNT; i++) {
			if (!isLinkIntact(i, own, remote)) {
				intact = false;
				break;
			}
		}

		if (!intact) {
			dropLinks();
			// Only (re)build while both ends are fully running
			if (own.isPaused() || remote.isPaused())
				return;
			// Safety net: never rebuild the links every single tick
			if (rebuildBackoff > 0) {
				rebuildBackoff--;
				return;
			}
			long now = level.getGameTime();
			if (now - rebuildWindowStart > 100) {
				rebuildWindowStart = now;
				rebuildCount = 0;
			}
			if (++rebuildCount > 20) {
				rebuildBackoff = 100;
				AeroCables.LOGGER.warn("Power Grid cable at {} keeps losing its link, retrying in 5 seconds", getBlockPos());
				return;
			}
			double resistance = computeResistance(other);
			for (int i = 0; i < PGTerminals.COUNT; i++) {
				ElectricWire wire = GlobalElectricNetworks.makeSimpleConnection(level,
						new BlockWireEndpoint(getBlockPos(), i),
						new BlockWireEndpoint(other.getBlockPos(), i),
						(float) resistance);
				if (wire == null) {
					AeroCables.LOGGER.debug("Power Grid cable link {} between {} and {} could not be created", i, getBlockPos(), other.getBlockPos());
					dropLinks();
					return;
				}
				links[i] = wire;
			}
			linkResistance = resistance;
			resistanceTimer = 0;
			return;
		}

		// Links are fine, keep the resistance in sync with the cable length (ropes extend and retract)
		if (++resistanceTimer >= RESISTANCE_UPDATE_INTERVAL) {
			resistanceTimer = 0;
			double resistance = computeResistance(other);
			if (linkResistance <= 0 || Math.abs(resistance - linkResistance) / linkResistance > 0.05) {
				for (ElectricWire wire : links) {
					if (wire != null)
						wire.setResistance(resistance);
				}
				linkResistance = resistance;
			}
		}
	}

	private boolean isLinkIntact(int index, ElectricBehaviour own, ElectricBehaviour remote) {
		ElectricWire wire = links[index];
		if (wire == null)
			return false;
		Object network = wire.getNetwork();
		if (network == null)
			return false;
		OwnedFloatingNode a = own.getTerminal(index);
		OwnedFloatingNode b = remote.getTerminal(index);
		if (a == null || b == null)
			return false;
		if ((Object) wire.getNode1() != a || (Object) wire.getNode2() != b)
			return false;
		return a.getNetwork() == network && b.getNetwork() == network;
	}

	private double computeResistance(PGCableConnectorBlockEntity other) {
		double perBlock = Config.get().pgCableResistance;
		if (!(perBlock > 0))
			perBlock = 0.001;
		double length = Math.max(MIN_LENGTH, cableLength(other));
		return Math.max(MIN_RESISTANCE, length * perBlock);
	}

	private double cableLength(PGCableConnectorBlockEntity other) {
		Vec3 a = Vec3.atCenterOf(getBlockPos());
		Vec3 b = Vec3.atCenterOf(other.getBlockPos());
		try {
			// Connectors on a ship live in a sub-level far away; project both into the real world.
			Vec3 pa = Sable.HELPER.projectOutOfSubLevel(level, a);
			Vec3 pb = Sable.HELPER.projectOutOfSubLevel(level, b);
			return pa.distanceTo(pb);
		} catch (Throwable e) {
			return a.distanceTo(b);
		}
	}

	private boolean isMaster(PGCableConnectorBlockEntity other) {
		return getBlockPos().compareTo(other.getBlockPos()) < 0;
	}

	/**
	 * @return the connector on the other end, only if it is loaded, alive and linked back to us
	 */
	private PGCableConnectorBlockEntity getLinkedPartner() {
		BlockPos otherPos = getOtherSidePos();
		if (otherPos == null || level == null || !level.isLoaded(otherPos))
			return null;
		if (level.getBlockEntity(otherPos) instanceof PGCableConnectorBlockEntity other && !other.isRemoved()
				&& getBlockPos().equals(other.getOtherSidePos()))
			return other;
		return null;
	}

	/** Removes the wires this connector created. */
	void dropLinks() {
		for (int i = 0; i < links.length; i++) {
			ElectricWire wire = links[i];
			if (wire != null) {
				ElectricalNetwork network = wire.getNetwork();
				wire.remove();
				links[i] = null;
				// the network may have fallen apart into two pieces now
				if (network != null && level != null && !level.isClientSide) {
					try {
						GlobalElectricNetworks.getWorldNetworks(level).scheduleIslandDiscovery(network);
					} catch (RuntimeException e) {
						AeroCables.LOGGER.debug("Power Grid island discovery failed", e);
					}
				}
			}
		}
		linkResistance = -1;
	}

	/** Removes the wires of this pair no matter which side created them. Must run before our nodes go away. */
	private void dropAllLinks() {
		dropLinks();
		if (level == null || level.isClientSide)
			return;
		BlockPos otherPos = getOtherSidePos();
		if (otherPos != null && level.isLoaded(otherPos) && level.getBlockEntity(otherPos) instanceof PGCableConnectorBlockEntity other)
			other.dropLinks();
	}

	@Override
	public void invalidate() {
		// chunk unload or block removal: wires must go before the nodes are unloaded
		dropAllLinks();
		super.invalidate();
	}

	@Override
	public void remove() {
		// block broken / replaced / moved onto a ship
		dropAllLinks();
		super.remove();
		if (electricBehaviour != null)
			electricBehaviour.remove();
	}
}
