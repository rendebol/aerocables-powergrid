package com.tom.aerocables.compat.powergrid;

import java.util.IdentityHashMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.mojang.serialization.MapCodec;

import com.tom.aerocables.AeroCables;
import com.tom.aerocables.common.CableConnectorBlock;

import org.patryk3211.powergrid.electricity.base.IElectric;
import org.patryk3211.powergrid.electricity.base.ITerminalPlacement;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;

/**
 * Cable connector for Create: Power Grid.
 * Has two Power Grid terminals: index 0 = positive (red), index 1 = negative (blue).
 * Terminal N of one connector is wired to terminal N of the connector on the other end of the rope.
 */
public class PGCableConnectorBlock extends CableConnectorBlock<PGCableConnectorBlockEntity> implements IElectric {
	public static final MapCodec<PGCableConnectorBlock> CODEC = simpleCodec(PGCableConnectorBlock::new);

	private final Map<BlockState, TerminalBoundingBox[]> terminals = new IdentityHashMap<>();
	private final Map<BlockState, VoxelShape> shapes = new IdentityHashMap<>();

	public PGCableConnectorBlock(Properties properties) {
		super(properties);
		for (BlockState state : getStateDefinition().getPossibleStates()) {
			TerminalBoundingBox[] stateTerminals = PGTerminals.forState(state);
			terminals.put(state, stateTerminals);

			VoxelShape shape = super.getShape(state, EmptyBlockGetter.INSTANCE, BlockPos.ZERO, CollisionContext.empty());
			for (TerminalBoundingBox terminal : stateTerminals) {
				shape = Shapes.or(shape, terminal.getShape());
			}
			shapes.put(state, shape.optimize());
		}
	}

	@Override
	public Class<PGCableConnectorBlockEntity> getBlockEntityClass() {
		return PGCableConnectorBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends PGCableConnectorBlockEntity> getBlockEntityType() {
		return AeroCables.PG_CABLE.blockEntityType();
	}

	@Override
	protected MapCodec<? extends DirectionalBlock> codec() {
		return CODEC;
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
		VoxelShape shape = shapes.get(state);
		return shape != null ? shape : super.getShape(state, worldIn, pos, context);
	}

	// ---- Power Grid ----

	@Override
	public int terminalCount() {
		return PGTerminals.COUNT;
	}

	@Override
	public ITerminalPlacement terminal(BlockState state, int index) {
		TerminalBoundingBox[] stateTerminals = terminals.get(state);
		if (stateTerminals == null || index < 0 || index >= stateTerminals.length)
			return null;
		return stateTerminals[index];
	}

	@Override
	public boolean accepts(ItemStack wireStack) {
		// Transmission connector: every Power Grid wire type may be attached
		return true;
	}
}
