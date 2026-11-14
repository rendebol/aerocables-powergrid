package com.tom.aerocables.client;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder.WorldInstructions;

import com.tom.aerocables.AeroCables;
import com.tom.aerocables.common.CommonConnectorBlock;
import com.tom.aerocables.compat.fe.FECableConnectorBlockEntity;

import dev.simulated_team.simulated.content.blocks.rope.rope_connector.RopeConnectorBlockEntity;
import dev.simulated_team.simulated.content.blocks.rope.rope_winch.RopeWinchBlockEntity;
import dev.simulated_team.simulated.ponder.SmoothMovementUtils;
import dev.simulated_team.simulated.ponder.elements.rope.RopeStrandElement;
import dev.simulated_team.simulated.ponder.instructions.ChasingLineInstruction;
import dev.simulated_team.simulated.ponder.instructions.CreateRopeStrandInstruction;

public class CablePonder {

	public static void connector(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);

		Selection structure1 = util.select().fromTo(1, 1, 1, 5, 2, 1);
		Selection structure2 = util.select().fromTo(1, 1, 3, 5, 3, 3);

		BlockPos winch = new BlockPos(4, 2, 1);
		BlockPos connector = new BlockPos(1, 1, 1);

		BlockPos connector2 = new BlockPos(4, 3, 3);
		BlockPos connector3 = new BlockPos(1, 1, 3);

		double connectorRopeOffset = 4.0 / 16.0;

		scene.title("connector", "Using cable connectors");
		scene.configureBasePlate(0, 0, 7);
		scene.scaleSceneView(.75f);
		scene.setSceneOffsetY(-1);
		scene.showBasePlate();

		scene.idle(10);
		scene.world().showSection(structure1, Direction.DOWN);
		scene.world().showSection(structure2, Direction.DOWN);
		scene.idle(5);

		final RopeStrandElement initialRope = new RopeStrandElement(winch.getCenter(), connector.getCenter().subtract(0, connectorRopeOffset, 0), 4, 0, 1);
		scene.addInstruction(new CreateRopeStrandInstruction(initialRope));

		scene.world().modifyBlockEntity(winch, RopeWinchBlockEntity.class, be -> be.getRopeHolder().renderAttached = true);
		scene.world().modifyBlockEntity(connector, RopeConnectorBlockEntity.class, be -> be.getRopeHolder().renderAttached = true);

		initialRope.modify(30)
		.setSog(0.2)
		.setInterpolator(SmoothMovementUtils.elasticOut())
		.start(scene);

		scene.idle(20);

		final RopeStrandElement rope2 = new RopeStrandElement(connector2.getCenter(), connector3.getCenter().subtract(0, connectorRopeOffset, 0), 4, 0, 1);
		scene.addInstruction(new CreateRopeStrandInstruction(rope2));

		scene.world().modifyBlockEntity(connector2, RopeConnectorBlockEntity.class, be -> be.getRopeHolder().renderAttached = true);
		scene.world().modifyBlockEntity(connector3, RopeConnectorBlockEntity.class, be -> be.getRopeHolder().renderAttached = true);

		rope2.modify(30)
		.setSog(0.2)
		.setInterpolator(SmoothMovementUtils.elasticOut())
		.start(scene);

		scene.idle(20);

		Selection conn1 = util.select().fromTo(0, 1, 1, 0, 2, 1);
		Selection conn2 = util.select().fromTo(0, 1, 3, 0, 2, 3);
		Selection conn3 = util.select().fromTo(4, 3, 1, 5, 3, 1);
		Selection conn4 = util.select().fromTo(4, 4, 3, 5, 4, 3);

		Selection conn1r = util.select().fromTo(0, 0, 1, 0, 0, 1);
		Selection conn2r = util.select().fromTo(0, 0, 3, 0, 0, 3);

		var conn1Sel = scene.world().showIndependentSection(conn1, Direction.DOWN);
		scene.world().moveSection(conn1Sel, new Vec3(0, -1, 0), 0);
		scene.world().hideSection(conn1r, Direction.DOWN);
		scene.idle(5);

		scene.world().showSection(conn3, Direction.DOWN);
		scene.idle(5);

		var conn2Sel = scene.world().showIndependentSection(conn2, Direction.DOWN);
		scene.world().moveSection(conn2Sel, new Vec3(0, -1, 0), 0);
		scene.world().hideSection(conn2r, Direction.DOWN);
		scene.idle(5);

		scene.world().showSection(conn4, Direction.DOWN);
		scene.idle(5);

		scene.overlay().showText(80)
		.placeNearTarget()
		.pointAt(util.vector().centerOf(4, 3, 1))
		.text("Place the connector next to a Rope Winch ...");

		scene.idle(5);

		scene.overlay().showText(80)
		.attachKeyFrame()
		.placeNearTarget()
		.pointAt(util.vector().centerOf(0, 1, 3))
		.text("... or a Rope Connector.");

		scene.idle(90);

		scene.overlay().showControls(util.vector().centerOf(4, 4, 3), Pointing.DOWN, 60).withItem(new ItemStack(AeroCables.FE_CABLE.item()));
		scene.idle(6);

		scene.addInstruction(new ChasingLineInstruction(
				util.vector().centerOf(4, 4, 3).add(connectorRopeOffset, 0, 0),
				util.vector().centerOf(4, 4, 3).add(connectorRopeOffset, 0, 0),
				7,
				PonderPalette.GREEN.getColor(),
				"winch",
				0,
				41,
				SmoothMovementUtils.linear()));

		scene.idle(20);

		scene.overlay().showControls(util.vector().centerOf(0, 1, 3), Pointing.DOWN, 40).withItem(new ItemStack(AeroCables.FE_CABLE.item()));

		scene.idle(6);

		scene.addInstruction(new ChasingLineInstruction(
				util.vector().centerOf(0, 1, 3).subtract(0, connectorRopeOffset, 0),
				util.vector().centerOf(0, 1, 3).subtract(0, connectorRopeOffset, 0),
				6,
				PonderPalette.GREEN.getColor(),
				"connector",
				0,
				5,
				SmoothMovementUtils.linear()));

		scene.addInstruction(new ChasingLineInstruction(
				util.vector().centerOf(4, 4, 3).add(connectorRopeOffset, 0, 0),
				util.vector().centerOf(0, 1, 3).subtract(0, connectorRopeOffset, 0),
				1,
				PonderPalette.BLACK.getColor(),
				5,
				5,
				SmoothMovementUtils.quadraticRise()));

		scene.idle(15);

		CableStrandElement cable = placeCable(rope2, scene, new BlockPos(4, 4, 3), new BlockPos(0, 2, 3), winch, connector);
		cable.setStartRenderPos(new BlockPos(4, 4, 3), new BlockPos(0, 1, 3));

		scene.idle(15);

		scene.overlay().showText(80)
		.attachKeyFrame()
		.placeNearTarget()
		.pointAt(util.vector().centerOf(0, 1, 3))
		.text("Use a cable to connect the connectors");

		scene.idle(90);

		scene.overlay().showText(80)
		.attachKeyFrame()
		.placeNearTarget()
		.pointAt(util.vector().centerOf(4, 3, 1))
		.text("Cables work with Winches");

		scene.idle(10);

		CableStrandElement cable2 = placeCable(initialRope, scene, new BlockPos(4, 3, 1), new BlockPos(0, 2, 1), winch, connector);
		cable2.setStartRenderPos(new BlockPos(4, 3, 1), new BlockPos(0, 1, 1));
		cable2.setWinch(winch);

		scene.idle(80);

		scene.world().showSection(util.select().position(4,2,0), Direction.SOUTH);

		scene.idle(20);

		Selection kinetics = util.select().fromTo(4,2,0,4,2,1);

		scene.world().setKineticSpeed(kinetics, 32);

		initialRope.modify(30)
		.setSog(1.4)
		.setInterpolator(SmoothMovementUtils.quadraticRiseOut())
		.setLength(6)
		.start(scene);

		scene.idle(20);

		scene.world().setKineticSpeed(kinetics, 0);

		scene.idle(40);

		scene.world().setKineticSpeed(kinetics, -32);

		initialRope.modify(50)
		.setSog(0.1)
		.setInterpolator(SmoothMovementUtils.softElasticOut())
		.setLength(4)
		.start(scene);

		scene.idle(30);

		scene.world().setKineticSpeed(kinetics, 0);

		scene.idle(30);
	}

	private static CableStrandElement placeCable(RopeStrandElement rope, CreateSceneBuilder scene, BlockPos connectorA, BlockPos connectorB, BlockPos ropeA, BlockPos ropeB) {
		WorldInstructions world = scene.world();

		world.modifyBlock(connectorA, b -> CommonConnectorBlock.copy(AeroCables.FE_CABLE.block(), b), false);
		world.modifyBlock(connectorB, b -> CommonConnectorBlock.copy(AeroCables.FE_CABLE.block(), b), false);

		world.modifyBlockEntity(connectorA, FECableConnectorBlockEntity.class, be -> be.setLink(ropeA, connectorB, false));
		world.modifyBlockEntity(connectorB, FECableConnectorBlockEntity.class, be -> be.setLink(ropeB, connectorA, false));

		CableStrandElement cable = new CableStrandElement(rope, AeroCables.FE_CABLE.type.cableModel(), connectorA, connectorB);
		scene.addInstruction(new CreateCableStrandInstruction(cable));
		return cable;
	}
}
