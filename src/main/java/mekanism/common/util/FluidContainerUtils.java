package mekanism.common.util;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import mekanism.api.text.IHasTranslationKey;
import mekanism.common.base.LazyOptionalHelper;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.NonNullList;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.items.ItemHandlerHelper;

public final class FluidContainerUtils {

    public static boolean isFluidContainer(ItemStack stack) {
        return !stack.isEmpty() && stack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY).isPresent();
    }

    public static boolean canDrain(@Nullable FluidStack tankFluid, @Nullable FluidStack drainFluid) {
        return tankFluid != null && (drainFluid == null || tankFluid.isFluidEqual(drainFluid));
    }

    public static boolean canFill(@Nullable FluidStack tankFluid, @Nonnull FluidStack fillFluid) {
        return tankFluid == null || tankFluid.isFluidEqual(fillFluid);
    }

    public static FluidStack extractFluid(FluidTank tileTank, TileEntityMekanism tile, int slotID) {
        return extractFluid(tileTank, tile, slotID, FluidChecker.check(tileTank.getFluid()));
    }

    public static FluidStack extractFluid(FluidTank tileTank, TileEntityMekanism tile, int slotID, FluidChecker checker) {
        return new LazyOptionalHelper<>(FluidUtil.getFluidHandler(tile.getInventory().get(slotID))).getIfPresent(handler -> {
            FluidStack ret = extractFluid(tileTank.getCapacity() - tileTank.getFluidAmount(), handler, checker);
            tile.getInventory().set(slotID, handler.getContainer());
            return ret;
        });
    }

    public static FluidStack extractFluid(int needed, IFluidHandlerItem handler, FluidChecker checker) {
        if (handler == null) {
            return null;
        }
        FluidStack fluidStack = handler.drain(Integer.MAX_VALUE, false);
        if (fluidStack == null) {
            return null;
        }
        if (checker != null && !checker.isValid(fluidStack.getFluid())) {
            return null;
        }
        return handler.drain(needed, true);
    }

    public static int insertFluid(FluidStack fluid, IFluidHandler handler) {
        if (fluid == null || handler == null) {
            return 0;
        }
        return handler.fill(fluid, true);
    }

    public static void handleContainerItemFill(TileEntityMekanism tileEntity, FluidTank tank, int inSlot, int outSlot) {
        tank.setFluid(handleContainerItemFill(tileEntity, tileEntity.getInventory(), tank.getFluid(), inSlot, outSlot));
    }

    public static FluidStack handleContainerItemFill(TileEntity tileEntity, NonNullList<ItemStack> inventory, FluidStack stack, int inSlot, int outSlot) {
        if (stack != null) {
            ItemStack inputCopy = StackUtils.size(inventory.get(inSlot).copy(), 1);
            LazyOptionalHelper<IFluidHandlerItem> handlerHelper = new LazyOptionalHelper<>(FluidUtil.getFluidHandler(inputCopy));
            int drained = 0;
            if (handlerHelper.isPresent()) {
                IFluidHandlerItem handler = handlerHelper.getValue();
                drained = insertFluid(stack, handler);
                inputCopy = handler.getContainer();
            }
            if (!inventory.get(outSlot).isEmpty() && (!ItemHandlerHelper.canItemStacksStack(inventory.get(outSlot), inputCopy) ||
                                                      inventory.get(outSlot).getCount() == inventory.get(outSlot).getMaxStackSize())) {
                return stack;
            }
            stack.amount -= drained;
            if (inventory.get(outSlot).isEmpty()) {
                inventory.set(outSlot, inputCopy);
            } else if (ItemHandlerHelper.canItemStacksStack(inventory.get(outSlot), inputCopy)) {
                inventory.get(outSlot).grow(1);
            }
            inventory.get(inSlot).shrink(1);
            tileEntity.markDirty();
        }
        return stack;
    }

    public static void handleContainerItemEmpty(TileEntityMekanism tileEntity, FluidTank tank, int inSlot, int outSlot) {
        handleContainerItemEmpty(tileEntity, tank, inSlot, outSlot, null);
    }

    public static void handleContainerItemEmpty(TileEntityMekanism tileEntity, FluidTank tank, int inSlot, int outSlot, FluidChecker checker) {
        tank.setFluid(handleContainerItemEmpty(tileEntity, tileEntity.getInventory(), tank.getFluid(), tank.getCapacity() - tank.getFluidAmount(), inSlot, outSlot, checker));
    }

    public static FluidStack handleContainerItemEmpty(TileEntity tileEntity, NonNullList<ItemStack> inventory, FluidStack stored, int needed, int inSlot, int outSlot,
          final FluidChecker checker) {
        final Fluid storedFinal = stored != null ? stored.getFluid() : null;
        final ItemStack input = StackUtils.size(inventory.get(inSlot).copy(), 1);
        LazyOptionalHelper<IFluidHandlerItem> handlerHelper = new LazyOptionalHelper<>(FluidUtil.getFluidHandler(input));

        if (!handlerHelper.isPresent()) {
            return stored;
        }
        IFluidHandlerItem handler = handlerHelper.getValue();
        FluidStack ret = extractFluid(needed, handler, new FluidChecker() {
            @Override
            public boolean isValid(Fluid f) {
                return (checker == null || checker.isValid(f)) && (storedFinal == null || storedFinal == f);
            }
        });

        ItemStack inputCopy = handler.getContainer();
        if (!FluidUtil.getFluidContained(inputCopy).isPresent() && !inputCopy.isEmpty()) {
            if (!inventory.get(outSlot).isEmpty() && (!ItemHandlerHelper.canItemStacksStack(inventory.get(outSlot), inputCopy) ||
                                                      inventory.get(outSlot).getCount() == inventory.get(outSlot).getMaxStackSize())) {
                return stored;
            }
        }

        if (ret != null) {
            if (stored == null) {
                stored = ret;
            } else {
                stored.amount += ret.amount;
            }
            needed -= ret.amount;
            tileEntity.markDirty();
        }

        if (!FluidUtil.getFluidContained(inputCopy).isPresent() || needed == 0) {
            if (!inputCopy.isEmpty()) {
                if (inventory.get(outSlot).isEmpty()) {
                    inventory.set(outSlot, inputCopy);
                } else if (ItemHandlerHelper.canItemStacksStack(inventory.get(outSlot), inputCopy)) {
                    inventory.get(outSlot).grow(1);
                }
            }
            inventory.get(inSlot).shrink(1);
            tileEntity.markDirty();
        } else {
            inventory.set(inSlot, inputCopy);
        }
        return stored;
    }

    public static void handleContainerItem(TileEntityMekanism tileEntity, ContainerEditMode editMode, FluidTank tank, int inSlot, int outSlot) {
        handleContainerItem(tileEntity, editMode, tank, inSlot, outSlot, null);
    }

    public static void handleContainerItem(TileEntityMekanism tileEntity, ContainerEditMode editMode, FluidTank tank, int inSlot, int outSlot, FluidChecker checker) {
        tank.setFluid(handleContainerItem(tileEntity, tileEntity.getInventory(), editMode, tank.getFluid(), tank.getCapacity() - tank.getFluidAmount(), inSlot, outSlot, checker));
    }

    public static FluidStack handleContainerItem(TileEntity tileEntity, NonNullList<ItemStack> inventory, ContainerEditMode editMode, FluidStack stack, int needed,
          int inSlot, int outSlot, final FluidChecker checker) {
        if (editMode == ContainerEditMode.FILL || (editMode == ContainerEditMode.BOTH && !FluidUtil.getFluidContained(inventory.get(inSlot)).isPresent())) {
            return handleContainerItemFill(tileEntity, inventory, stack, inSlot, outSlot);
        } else if (editMode == ContainerEditMode.EMPTY || editMode == ContainerEditMode.BOTH) {
            return handleContainerItemEmpty(tileEntity, inventory, stack, needed, inSlot, outSlot, checker);
        }
        return stack;
    }

    public enum ContainerEditMode implements IHasTranslationKey {
        BOTH("mekanism.fluidedit.both"),
        FILL("mekanism.fluidedit.fill"),
        EMPTY("mekanism.fluidedit.empty");

        private String display;

        ContainerEditMode(String s) {
            display = s;
        }

        @Override
        public String getTranslationKey() {
            return display;
        }
    }

    public static class FluidChecker {

        public static FluidChecker check(FluidStack fluid) {
            final Fluid type = fluid != null ? fluid.getFluid() : null;

            return new FluidChecker() {
                @Override
                public boolean isValid(Fluid f) {
                    return type == null || type == f;
                }
            };
        }

        public static FluidChecker check(final Fluid type) {
            return new FluidChecker() {
                @Override
                public boolean isValid(Fluid f) {
                    return type == null || type == f;
                }
            };
        }

        public boolean isValid(Fluid f) {
            return true;
        }
    }
}