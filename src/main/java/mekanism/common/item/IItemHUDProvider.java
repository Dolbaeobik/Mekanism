package mekanism.common.item;

import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;

public interface IItemHUDProvider {
    
    public void addHUDStrings(List<ITextComponent> list, ItemStack stack);
}
