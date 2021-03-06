package mekanism.common.upgrade.transmitter;

import mekanism.common.tile.transmitter.TileEntitySidedPipe.ConnectionType;

public class ThermodynamicConductorUpgradeData extends TransmitterUpgradeData {

    public final double heat;

    public ThermodynamicConductorUpgradeData(boolean redstoneReactive, ConnectionType[] connectionTypes, double heat) {
        super(redstoneReactive, connectionTypes);
        this.heat = heat;
    }
}