package com.tom.aerocables.compat.ae;

import java.util.ArrayList;
import java.util.List;

import com.tom.aerocables.AeroCables;

import appeng.api.networking.GridFlags;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.IManagedGridNode;
import appeng.me.service.helpers.ConnectionWrapper;

public class AENetworkHandler implements IGridNodeListener<AENetworkHandler> {
	private final IManagedGridNode mainNode = this.createMainNode()
			.setInWorldNode(false)
			.setTagName("proxy")
			.setIdlePowerUsage(0.0D);
	private List<ConnectionWrapper> connections = new ArrayList<>();

	protected IManagedGridNode createMainNode() {
		return GridHelper.createManagedNode(this, this).setFlags(new GridFlags[0]).setVisualRepresentation(AeroCables.AE_CABLE.item());
	}

	@Override
	public void onSaveChanges(AENetworkHandler p0, IGridNode p1) {
	}

	public void init(AECableConnectorBlockEntity connector) {
		mainNode.create(connector.getLevel(), null);
		mainNode.setOwningPlayerId(connector.getActionableNode().getOwningPlayerId());
		connect(connector);
	}

	public void connect(AECableConnectorBlockEntity connector) {
		IGridNode node = connector.getActionableNode();
		if(node != null) {
			connections.add(new ConnectionWrapper(GridHelper.createConnection(mainNode.getNode(), node)));
		}
	}

	public void destroy() {
		connections.forEach(c -> {
			if(c.getConnection() != null)c.getConnection().destroy();
		});
		connections.clear();
	}
}
