package mekanism.common.base;

import mekanism.common.Upgrade;
import mekanism.common.tile.component.TileComponentUpgrade;

public interface IUpgradeTile {

    TileComponentUpgrade getComponent();

    void recalculateUpgrades(Upgrade upgradeType);
}