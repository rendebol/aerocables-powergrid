
Create: Aero Cables
=======

Connect your Simulated contraptions with cables.
![Airships connected AE system](https://cdn.modrinth.com/data/AHTfty0S/images/b36de815aa7b523b425a024eadb2878f5d998fa5.png)  

Cables are wrapped around ropes. Place the connector next to a rope winch or connector and link the two sides with the cable of your choice.

![Cables](https://cdn.modrinth.com/data/AHTfty0S/images/aca675b5721b5f6da92914828b829158cca242df.png)  

Supported cables:
- Energy (FE)
- Applied Energistics
- Refined Storage
- CC: Tweaked
- Create: Power Grid (this fork, see below)

Planned:
- Integrated Dynamics

Power Grid fork
---------------

This fork adds a cable for [Create: Power Grid](https://modrinth.com/mod/power-grid).

- **Power Grid Cable Spool** — two thin wires (red `+`, blue `-`) wound around the rope.
- **Power Grid Cable Connector** — the connector gets two Power Grid terminals (`+` and `-`).
  Attach regular Power Grid wires to them. `+` of one end is connected to `+` of the other end, `-` to `-`.
- The link is a real Power Grid conductor (simulated by Power Grid's circuit solver).
  Resistance = cable length x `pgCableResistancePerBlock` (server config, default 0.001 ohm per block, per wire).
- Recipe: Cable Spool surrounded by 4 Insulated Copper Wire (only if Power Grid is installed).

Requires Power Grid 0.6.x for NeoForge 1.21.1 (built against 0.6.2). The mod id stays `aerocables`,
so this jar **replaces** the original Aero Cables jar.

### Building

Datagen is required once (the base mod does not commit generated resources):

```
gradlew runData
gradlew build
```

The jar is in `build/libs`. On Windows you can simply run `build-mod.bat`.

### Credits / licenses

- Create: Aero Cables by tom5454 — MIT (`LICENSE`)
- Create: Power Grid by patryk3211 — Apache-2.0 (`src/main/resources/LICENSE-POWERGRID.txt`, `NOTICE-POWERGRID.txt`).
  `pg_cable_positive.png` / `pg_cable_negative.png` are tinted versions of Power Grid's `insulated_wire.png`.
  The connector model uses Power Grid's `device_connector` texture directly from the Power Grid jar.
