package mekanism.common.util.text;

import mekanism.api.EnumColor;
import mekanism.api.text.IHasTextComponent;
import mekanism.common.Upgrade;
import net.minecraft.util.text.ITextComponent;

public class UpgradeDisplay implements IHasTextComponent {

    private final Upgrade upgrade;
    private final int level;

    private UpgradeDisplay(Upgrade upgrade, int level) {
        this.upgrade = upgrade;
        this.level = level;
    }

    public static UpgradeDisplay of(Upgrade upgrade) {
        return of(upgrade, 0);
    }

    public static UpgradeDisplay of(Upgrade upgrade, int level) {
        return new UpgradeDisplay(upgrade, level);
    }

    @Override
    public ITextComponent getTextComponent() {
        if (upgrade.canMultiply() && level > 0) {
            return TextComponentUtil.build(upgrade.getColor(), "- ", upgrade.getName(), ": ", EnumColor.GREY, "x" + level);
        }
        return TextComponentUtil.build(upgrade.getColor(), "- ", upgrade.getName());
    }
}